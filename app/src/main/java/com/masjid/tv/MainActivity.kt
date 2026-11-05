package com.masjid.tv

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter

private val BG = Color(0xFF08241B)
private val PANEL = Color(0xFF0E3326)
private val GOLD = Color(0xFFC9A24A)
private val LINE = Color(0x80C9A24A)
private val CREAM = Color(0xFFF3EBD3)
private val BRICK = Color(0xFF9B3B2A)
private val ALERT = Color(0xFFF2705A)
private val DIM = Color(0xFF6F8F80)
private val VID = setOf("mp4", "webm", "mkv")

class Fonts(val title: FontFamily, val digits: FontFamily)

val LocalFonts = staticCompositionLocalOf { Fonts(FontFamily.Serif, FontFamily.Monospace) }

fun makeFonts(f: File?): Fonts {
    if (f != null) {
        try {
            val ff = FontFamily(Typeface.createFromFile(f))
            return Fonts(ff, ff)
        } catch (e: Throwable) { }
    }
    return Fonts(
        FontFamily(Typeface.create(Typeface.SERIF, Typeface.BOLD)),
        FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))
    )
}

@Composable
fun T(
    text: String, size: Int, color: Color, digits: Boolean = false,
    align: TextAlign? = null, mod: Modifier = Modifier, lines: Int = Int.MAX_VALUE
) {
    val f = LocalFonts.current
    Text(
        text, modifier = mod, color = color, fontSize = size.sp,
        fontFamily = if (digits) f.digits else f.title,
        textAlign = align, maxLines = lines, softWrap = lines != 1
    )
}

class MainActivity : ComponentActivity() {
    private val vm: MasjidViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        askOverlayOnce()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = PANEL)) {
                val s by vm.state.collectAsState()
                val media by vm.media.collectAsState()
                val look by vm.look.collectAsState()
                val fonts = remember(look.ver) { makeFonts(look.font) }
                LaunchedEffect(s.dark) {
                    val lp = window.attributes
                    lp.screenBrightness = if (s.dark) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    window.attributes = lp
                }
                CompositionLocalProvider(LocalFonts provides fonts) { MasjidScreen(s, media, look) }
            }
        }
    }

    // Izin "tampil di atas aplikasi lain" agar auto-start berfungsi di Android 10+
    private fun askOverlayOnce() {
        val sp = getSharedPreferences("app", MODE_PRIVATE)
        if (sp.getBoolean("asked", false)) return
        sp.edit().putBoolean("asked", true).apply()
        try {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        } catch (e: Exception) { }
    }
}

@Composable
fun MasjidScreen(s: UiState, media: List<File>, look: Look) {
    if (s.dark) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            T(s.lang.darkNote, 26, Color(0xFF262626))
        }
        return
    }
    Column(Modifier.fillMaxSize().background(BG)) {
        Header(s, look)
        if (!s.licensed) Box(Modifier.fillMaxWidth().background(BRICK).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
            T("BELUM BERLISENSI  •  ID: ${s.devId}  •  fitur terkunci", 16, CREAM, lines = 1)
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(2f).fillMaxHeight().background(Color.Black).border(1.dp, LINE)) {
                when {
                    s.jumat -> JumatInfo(s)
                    s.phase == Phase.SHALAT -> ShalatInfo(s)
                    else -> MediaPager(media, s)
                }
            }
            Side(s, Modifier.weight(1f).fillMaxHeight())
        }
        Footer(s.ticker)
    }
}

@Composable
fun Header(s: UiState, look: Look) {
    Column {
        Row(
            Modifier.fillMaxWidth().background(PANEL).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (look.logo != null) {
                LogoImg(look)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                T(s.name, 32, GOLD, lines = 1)
                T(s.masehi, 17, CREAM, lines = 1)
                T(s.hijri, 17, GOLD, lines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                T(s.clock, 58, CREAM, digits = true, lines = 1)
                if (s.ip.isNotEmpty()) T("http://${s.ip}:8080", 11, DIM, lines = 1)
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(GOLD))
    }
}

@Composable
fun LogoImg(look: Look) {
    val bmp by produceState<ImageBitmap?>(null, look.ver) {
        value = look.logo?.let { withContext(Dispatchers.IO) { decode(it) } }
    }
    bmp?.let { Image(it, null, Modifier.size(64.dp), contentScale = ContentScale.Fit) }
}

@Composable
fun Side(s: UiState, mod: Modifier) {
    val fmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val blink = rememberInfiniteTransition(label = "blink")
    val alpha by blink.animateFloat(1f, 0.25f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "a")
    val last5 = s.remain in 1..5 && (s.phase == Phase.NORMAL || s.phase == Phase.IQOMAH)
    val bg = when (s.phase) { Phase.ADZAN -> GOLD; Phase.IQOMAH -> BRICK; else -> PANEL }
    val labelC = when (s.phase) { Phase.ADZAN -> BG; Phase.IQOMAH -> CREAM; else -> GOLD }
    val timeBase = when (s.phase) { Phase.ADZAN -> BG; else -> CREAM }
    Column(mod.background(PANEL).border(1.dp, LINE)) {
        Column(Modifier.fillMaxWidth().background(bg).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            T(s.label, 20, labelC, align = TextAlign.Center)
            if (s.phase != Phase.SHALAT) {
                val c = if (last5) (if (s.phase == Phase.NORMAL) ALERT else timeBase).copy(alpha = alpha) else timeBase
                T(s.countdown, 48, c, digits = true, lines = 1)
            }
            if (s.phase == Phase.IQOMAH) T(s.lang.straighten, 16, CREAM, align = TextAlign.Center)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(GOLD))
        Column(Modifier.weight(1f).fillMaxWidth()) {
            s.prayers.forEachIndexed { i, p ->
                val act = i == s.nextIdx
                val tc = if (act) BG else CREAM
                Row(
                    Modifier.weight(1f).fillMaxWidth().background(if (act) GOLD else PANEL).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    T(p.name, 24, tc, lines = 1)
                    T(p.time.format(fmt), 34, tc, digits = true, lines = 1)
                }
                if (i < s.prayers.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(LINE))
            }
        }
    }
}

@Composable
fun JumatInfo(s: UiState) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        T(s.lang.jumat, 40, GOLD)
        JumatRow(s.lang.khatib, s.khatib)
        JumatRow(s.lang.imam, s.imam)
        JumatRow(s.lang.muadzin, s.muadzin)
        T(s.lang.jumatNote, 18, DIM, align = TextAlign.Center)
    }
}

@Composable
fun JumatRow(label: String, name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        T(label, 18, GOLD)
        T(name, 34, CREAM, align = TextAlign.Center)
    }
}

@Composable
fun ShalatInfo(s: UiState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        T(s.lang.darkNote, 36, GOLD, align = TextAlign.Center)
    }
}

@Composable
fun MediaPager(media: List<File>, s: UiState) {
    if (!s.licensed) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            T("BELUM BERLISENSI\nID perangkat: ${s.devId}\nMasukkan kode lisensi di http://${s.ip}:8080", 24, CREAM, align = TextAlign.Center)
        }
        return
    }
    if (media.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            T("${s.lang.upload}\nhttp://${s.ip}:8080", 24, CREAM, align = TextAlign.Center)
        }
        return
    }
    var idx by remember { mutableIntStateOf(0) }
    val isVideo = media[idx % media.size].extension.lowercase() in VID
    if (!isVideo) LaunchedEffect(idx, media.size) { delay(s.slideSec * 1000L); idx++ }
    AnimatedContent(
        targetState = idx,
        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
        label = "pager"
    ) { i ->
        val f = media[i % media.size]
        if (f.extension.lowercase() in VID) VideoSlide(f) { idx++ } else ImageSlide(f)
    }
}

@Composable
fun VideoSlide(f: File, onDone: () -> Unit) {
    val done by rememberUpdatedState(onDone)
    AndroidView(
        factory = { c ->
            VideoView(c).apply {
                setVideoPath(f.absolutePath)
                setOnCompletionListener { done() }
                setOnErrorListener { _, _, _ -> postDelayed({ done() }, 3000); true }
                start()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ImageSlide(f: File) {
    val bmp by produceState<ImageBitmap?>(null, f) { value = withContext(Dispatchers.IO) { decode(f) } }
    bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
}

private fun decode(f: File): ImageBitmap? = try {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, o)
    var ss = 1
    while (o.outWidth / ss > 2000 || o.outHeight / ss > 2000) ss *= 2
    BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = ss })?.asImageBitmap()
} catch (e: Throwable) { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Footer(t: String) {
    val txt = remember(t) { "$t      •      ".repeat(4) }
    Box(Modifier.fillMaxWidth().height(40.dp).background(GOLD), contentAlignment = Alignment.CenterStart) {
        T(txt, 24, BG, mod = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE, velocity = 110.dp), lines = 1)
    }
}
