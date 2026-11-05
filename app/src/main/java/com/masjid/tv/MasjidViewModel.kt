package com.masjid.tv

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

data class Prayer(val name: String, val time: LocalDateTime)
enum class Phase { NORMAL, ADZAN, IQOMAH, SHALAT }
data class Look(val logo: File? = null, val font: File? = null, val ver: Long = 0)

data class UiState(
    val name: String = "", val ticker: String = "", val clock: String = "--:--:--",
    val masehi: String = "", val hijri: String = "",
    val prayers: List<Prayer> = emptyList(), val nextIdx: Int = -1,
    val phase: Phase = Phase.NORMAL, val countdown: String = "00:00:00", val label: String = "",
    val remain: Long = 0, val jumat: Boolean = false, val dark: Boolean = false,
    val khatib: String = "", val imam: String = "", val muadzin: String = "",
    val ip: String = "", val slideSec: Int = 8, val lang: Lang.S = Lang.of("id"),
    val licensed: Boolean = true, val devId: String = ""
)

class MasjidViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val devId = License.deviceId(ctx)
    @Volatile private var cfg = Cfg.load(ctx)
    private val mediaDir = File(ctx.filesDir, "media").apply { mkdirs() }
    private val exts = setOf("jpg", "jpeg", "png", "webp", "mp4", "webm", "mkv")
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private val _media = MutableStateFlow(listMedia())
    val media: StateFlow<List<File>> = _media.asStateFlow()
    private val _look = MutableStateFlow(makeLook())
    val look: StateFlow<Look> = _look.asStateFlow()

    private var tone: ToneGenerator? = null
    private var cacheKey: Pair<LocalDate, Config>? = null
    private var cache: List<Prayer> = emptyList()
    private var lastBeep = -1L
    private var ip = ""
    private var server: WebServer? = null
    private val fmtClock = DateTimeFormatter.ofPattern("HH:mm:ss")

    init {
        try {
            server = WebServer(ctx, 8080) { reload() }.also { it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) }
        } catch (e: Exception) { }
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                try { tick() } catch (e: Exception) { }
                delay(1000 - System.currentTimeMillis() % 1000)
            }
        }
    }

    private fun listMedia(): List<File> =
        mediaDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in exts }?.sortedBy { it.name.lowercase() } ?: emptyList()

    private fun makeLook() = Look(
        File(ctx.filesDir, "logo.img").takeIf { it.exists() && cfg.licensed },
        File(ctx.filesDir, "font.ttf").takeIf { it.exists() && cfg.licensed },
        System.nanoTime()
    )

    fun reload() {
        cfg = Cfg.load(ctx)
        _media.value = listMedia()
        _look.value = makeLook()
    }

    private fun localIp(): String = try {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress ?: ""
    } catch (e: Exception) { "" }

    private fun beep() {
        try {
            if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        } catch (e: Exception) { }
    }

    private fun prayers(date: LocalDate, c: Config, l: Lang.S): List<Prayer> {
        val t = PrayerCalc.compute(date, c.city.lat, c.city.lng, c.city.tz, c.fajr, c.isha, c.ihtiyath)
        return l.prayers.zip(t).map { (n, tm) -> Prayer(n, date.atTime(tm)) }
    }

    private fun tick() {
        val c = Cfg.effective(cfg, devId)
        val l = Lang.of(c.lang)
        fun up(x: String) = if (c.lang == "ar") x else x.uppercase()
        val zone = ZoneOffset.ofTotalSeconds((c.city.tz * 3600).toInt())
        val now = LocalDateTime.now(zone).withNano(0)
        val today = now.toLocalDate()
        if (cacheKey != (today to c)) { cache = prayers(today, c, l); cacheKey = today to c }
        val ps = cache
        val fri = today.dayOfWeek == DayOfWeek.FRIDAY

        var phase = Phase.NORMAL
        var idx = -1
        var target = now
        var label = ""
        for ((i, p) in ps.withIndex()) {
            val aEnd = p.time.plusMinutes(c.adzanMin.toLong())
            val iEnd = aEnd.plusMinutes(c.iqomahMin.toLong())
            val sEnd = iEnd.plusMinutes(if (fri && i == 1) c.jumatMin.toLong() else c.shalatMin.toLong())
            if (now >= p.time && now < aEnd) { phase = Phase.ADZAN; target = aEnd; label = up("${l.adzan} ${p.name}") }
            else if (now >= aEnd && now < iEnd) { phase = Phase.IQOMAH; target = iEnd; label = up("${l.iqomah} ${p.name}") }
            else if (now >= iEnd && now < sEnd) { phase = Phase.SHALAT; target = sEnd; label = up("${l.shalat} ${p.name}") }
            if (phase != Phase.NORMAL) { idx = i; break }
        }
        if (phase == Phase.NORMAL) {
            idx = ps.indexOfFirst { it.time.isAfter(now) }
            target = if (idx == -1) { idx = 0; prayers(today.plusDays(1), c, l)[0].time } else ps[idx].time
            label = up("${l.toward} ${ps[idx].name}")
        }

        val remain = Duration.between(now, target).seconds.coerceAtLeast(0)
        // Beep tiap detik pada 5 detik terakhir (menuju adzan & iqomah)
        if (phase == Phase.NORMAL || phase == Phase.IQOMAH) {
            if (remain in 1..5 && remain != lastBeep) { lastBeep = remain; beep() }
            if (remain > 5) lastBeep = -1
        }

        val dz = ps[1].time
        val jumat = c.licensed && fri && now >= dz.minusMinutes(c.jumatPre.toLong()) &&
                now < dz.plusMinutes((c.adzanMin + c.iqomahMin).toLong())

        val hijri = try {
            val hd = HijrahDate.from(today.plusDays((c.hijriAdj + (if (now >= ps[3].time) 1 else 0)).toLong()))
            "${hd.get(ChronoField.DAY_OF_MONTH)} ${l.months[hd.get(ChronoField.MONTH_OF_YEAR) - 1]} ${hd.get(ChronoField.YEAR)} ${l.hijriSuffix}"
        } catch (e: Exception) { "" }

        if (ip.isEmpty() || now.second % 30 == 0) ip = localIp()

        _state.value = UiState(
            name = c.name, ticker = c.ticker, clock = now.format(fmtClock),
            masehi = now.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", l.locale)), hijri = hijri,
            prayers = ps, nextIdx = idx, phase = phase,
            countdown = "%02d:%02d:%02d".format(remain / 3600, (remain % 3600) / 60, remain % 60),
            label = label, remain = remain, jumat = jumat, dark = c.dark && phase == Phase.SHALAT,
            khatib = c.khatib, imam = c.imam, muadzin = c.muadzin, ip = ip, slideSec = c.slideSec, lang = l, licensed = c.licensed, devId = devId
        )
    }

    override fun onCleared() {
        server?.stop()
        tone?.release()
        super.onCleared()
    }
}
