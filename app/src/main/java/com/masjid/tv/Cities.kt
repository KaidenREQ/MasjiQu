package com.masjid.tv

data class City(val name: String, val lat: Double, val lng: Double, val tz: Double)

object Cities {
    val all = listOf(
        City("Banda Aceh", 5.5483, 95.3238, 7.0), City("Medan", 3.5952, 98.6722, 7.0),
        City("Padang", 0.9471, 100.4172, 7.0), City("Pekanbaru", 0.5071, 101.4478, 7.0),
        City("Jambi", -1.6101, 103.6131, 7.0), City("Palembang", -2.9761, 104.7754, 7.0),
        City("Bengkulu", -3.7928, 102.2608, 7.0), City("Bandar Lampung", -5.3971, 105.2668, 7.0),
        City("Pangkalpinang", -2.1316, 106.1169, 7.0), City("Tanjung Pinang", 0.9186, 104.4666, 7.0),
        City("Batam", 1.0456, 104.0305, 7.0), City("Jakarta", -6.2088, 106.8456, 7.0),
        City("Tangerang", -6.1783, 106.6319, 7.0), City("Bekasi", -6.2383, 106.9756, 7.0),
        City("Depok", -6.4025, 106.7942, 7.0), City("Bogor", -6.5971, 106.8060, 7.0),
        City("Bandung", -6.9175, 107.6191, 7.0), City("Serang", -6.1200, 106.1503, 7.0),
        City("Cirebon", -6.7320, 108.5523, 7.0), City("Semarang", -6.9667, 110.4167, 7.0),
        City("Yogyakarta", -7.7956, 110.3695, 7.0), City("Surakarta (Solo)", -7.5755, 110.8243, 7.0),
        City("Surabaya", -7.2575, 112.7521, 7.0), City("Malang", -7.9666, 112.6326, 7.0),
        City("Pontianak", -0.0263, 109.3425, 7.0), City("Palangkaraya", -2.2161, 113.9135, 7.0),
        City("Denpasar", -8.6705, 115.2126, 8.0), City("Mataram", -8.5833, 116.1167, 8.0),
        City("Kupang", -10.1772, 123.6070, 8.0), City("Banjarmasin", -3.3194, 114.5908, 8.0),
        City("Balikpapan", -1.2379, 116.8529, 8.0), City("Samarinda", -0.5022, 117.1536, 8.0),
        City("Tarakan", 3.3000, 117.6333, 8.0), City("Makassar", -5.1477, 119.4327, 8.0),
        City("Manado", 1.4748, 124.8421, 8.0), City("Palu", -0.8917, 119.8707, 8.0),
        City("Kendari", -3.9985, 122.5129, 8.0), City("Gorontalo", 0.5435, 123.0568, 8.0),
        City("Mamuju", -2.6740, 118.8888, 8.0), City("Ambon", -3.6954, 128.1814, 9.0),
        City("Ternate", 0.7833, 127.3667, 9.0), City("Jayapura", -2.5337, 140.7181, 9.0),
        City("Sorong", -0.8762, 131.2558, 9.0), City("Manokwari", -0.8615, 134.0620, 9.0),
        City("Merauke", -8.4932, 140.4018, 9.0),
        City("Mekkah", 21.3891, 39.8579, 3.0), City("Madinah", 24.4672, 39.6111, 3.0),
        City("Kuala Lumpur", 3.1390, 101.6869, 8.0), City("Singapura", 1.3521, 103.8198, 8.0),
        City("Bandar Seri Begawan", 4.9031, 114.9398, 8.0)
    )

    fun find(n: String): City = all.firstOrNull { it.name == n } ?: all.first { it.name == "Jakarta" }
}
