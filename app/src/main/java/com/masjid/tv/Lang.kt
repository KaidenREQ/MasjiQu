package com.masjid.tv

import java.util.Locale

object Lang {
    class S(
        val locale: Locale, val prayers: List<String>, val toward: String, val adzan: String, val iqomah: String,
        val shalat: String, val jumat: String, val khatib: String, val imam: String, val muadzin: String,
        val straighten: String, val jumatNote: String, val darkNote: String, val upload: String,
        val months: List<String>, val hijriSuffix: String
    )

    private val idn = S(
        Locale("id", "ID"), listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"), "MENUJU", "ADZAN", "IQOMAH",
        "SHALAT", "SHALAT JUMAT", "KHATIB", "IMAM", "MUADZIN",
        "Luruskan dan rapatkan shaf", "Nonaktifkan HP  •  Dengarkan khutbah dengan tenang", "Shalat sedang berlangsung",
        "Upload gambar/video dari HP",
        listOf("Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir", "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban", "Ramadhan", "Syawal", "Dzulqa'dah", "Dzulhijjah"),
        "H"
    )
    private val en = S(
        Locale.ENGLISH, listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"), "NEXT", "ADHAN", "IQAMAH",
        "PRAYER", "FRIDAY PRAYER", "KHATIB", "IMAM", "MUADHDHIN",
        "Straighten and close the rows", "Silence your phone  •  Listen to the sermon quietly", "Prayer in progress",
        "Upload images/videos from your phone",
        listOf("Muharram", "Safar", "Rabi' al-awwal", "Rabi' al-thani", "Jumada al-awwal", "Jumada al-thani", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"),
        "AH"
    )
    private val ar = S(
        Locale("ar"), listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء"), "القادمة", "الأذان", "الإقامة",
        "الصلاة", "صلاة الجمعة", "الخطيب", "الإمام", "المؤذن",
        "سوّوا صفوفكم", "أغلقوا هواتفكم  •  أنصتوا للخطبة", "الصلاة قائمة",
        "ارفع الصور/الفيديو من هاتفك",
        listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"),
        "هـ"
    )

    fun of(code: String): S = when (code) { "en" -> en; "ar" -> ar; else -> idn }
}
