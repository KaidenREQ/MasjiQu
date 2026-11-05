package com.masjid.tv

import android.content.Context

data class Config(
    val name: String, val khatib: String, val imam: String, val muadzin: String, val ticker: String,
    val lang: String, val city: City,
    val adzanMin: Int, val iqomahMin: Int, val shalatMin: Int, val jumatMin: Int, val jumatPre: Int, val ihtiyath: Int,
    val fajr: Double, val isha: Double, val hijriAdj: Int, val slideSec: Int, val dark: Boolean, val pin: String, val licensed: Boolean
)

object Cfg {
    // kind: t=teks, n=angka, s=dropdown, p=PIN
    class F(val key: String, val label: String, val def: String, val kind: String = "t", val opts: List<Pair<String, String>> = emptyList())

    private val methods = mapOf(
        "kemenag" to (20.0 to 18.0), "mwl" to (18.0 to 17.0), "isna" to (15.0 to 15.0), "egypt" to (19.5 to 17.5)
    )

    val fields = listOf(
        F("name", "Nama Masjid", "MASJID AL-IKHLAS"),
        F("khatib", "Khatib Jumat", "Ustadz Ahmad, Lc."),
        F("imam", "Imam Jumat", "Ustadz Fulan, S.Ag."),
        F("muadzin", "Muadzin Jumat", "Bpk. Abdullah"),
        F("ticker", "Teks Berjalan", "Luruskan dan rapatkan shaf  •  Matikan HP saat shalat  •  Infaq dan sedekah dapat disalurkan melalui pengurus masjid"),
        F("lang", "Bahasa tampilan TV", "id", "s", listOf("id" to "Indonesia", "en" to "English", "ar" to "العربية")),
        F("kota", "Kota / lokasi", "Bengkulu", "s", Cities.all.sortedBy { it.name }.map { it.name to it.name }),
        F("metode", "Metode hitung jadwal", "kemenag", "s", listOf("kemenag" to "Kemenag RI", "mwl" to "Muslim World League", "isna" to "ISNA", "egypt" to "Mesir")),
        F("adzanMin", "Durasi adzan (menit)", "3", "n"),
        F("iqomahMin", "Durasi iqomah (menit)", "10", "n"),
        F("gelap", "Layar gelap saat shalat", "1", "s", listOf("1" to "Ya", "0" to "Tidak")),
        F("shalatMin", "Lama layar gelap / shalat (menit)", "10", "n"),
        F("jumatMin", "Lama khutbah + shalat Jumat (menit)", "45", "n"),
        F("jumatPre", "Mode Jumat mulai (menit sebelum Dzuhur)", "15", "n"),
        F("ihtiyath", "Ihtiyath (menit)", "2", "n"),
        F("hijriAdj", "Koreksi tanggal Hijriah (hari)", "0", "n"),
        F("slideSec", "Detik per gambar", "8", "n"),
        F("pin", "PIN panel (4-8 angka)", "0000", "p")
    )

    private fun sp(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun map(c: Context): Map<String, String> {
        val s = sp(c)
        return fields.associate { it.key to (s.getString(it.key, it.def) ?: it.def) }
    }

    fun save(c: Context, m: Map<String, String>) {
        val e = sp(c).edit()
        fields.forEach { f ->
            val v = m[f.key]?.trim() ?: return@forEach
            if (f.key == "pin" && !Regex("\\d{4,8}").matches(v)) return@forEach
            e.putString(f.key, v)
        }
        e.apply()
    }

    // Tanpa lisensi: fitur inti dikunci ke nilai bawaan
    fun effective(c: Config, devId: String): Config = if (c.licensed) c else c.copy(
        name = "MASJID", khatib = "-", imam = "-", muadzin = "-",
        ticker = "Versi belum berlisensi  •  Kirim ID perangkat $devId ke penjual untuk mendapat kode lisensi",
        lang = "id", adzanMin = 3, iqomahMin = 10, shalatMin = 10, jumatMin = 45, jumatPre = 15, slideSec = 8, dark = false
    )

    fun load(c: Context): Config {
        val m = map(c)
        fun s(k: String) = m[k]!!
        fun i(k: String) = s(k).toIntOrNull() ?: fields.first { it.key == k }.def.toInt()
        val mt = methods[s("metode")] ?: methods.getValue("kemenag")
        return Config(
            s("name"), s("khatib"), s("imam"), s("muadzin"), s("ticker"), s("lang"), Cities.find(s("kota")),
            i("adzanMin"), i("iqomahMin"), i("shalatMin"), i("jumatMin"), i("jumatPre"), i("ihtiyath"),
            mt.first, mt.second, i("hijriAdj"), i("slideSec").coerceAtLeast(2), s("gelap") == "1", s("pin"), License.valid(c)
        )
    }
}
