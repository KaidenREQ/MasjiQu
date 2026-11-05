package com.masjid.tv

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.*

object PrayerCalc {
    private fun r(d: Double) = Math.toRadians(d)
    private fun dg(x: Double) = Math.toDegrees(x)
    private fun fa(a: Double) = ((a % 360) + 360) % 360
    private fun fh(a: Double) = ((a % 24) + 24) % 24

    private fun julian(y: Int, m: Int, d: Int): Double {
        var yy = y; var mm = m
        if (mm <= 2) { yy -= 1; mm += 12 }
        val a = floor(yy / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (yy + 4716)) + floor(30.6001 * (mm + 1)) + d + b - 1524.5
    }

    // (deklinasi derajat, equation of time jam)
    private fun sun(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fa(357.529 + 0.98560028 * d)
        val q = fa(280.459 + 0.98564736 * d)
        val l = fa(q + 1.915 * sin(r(g)) + 0.020 * sin(r(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = fh(dg(atan2(cos(r(e)) * sin(r(l)), cos(r(l)))) / 15)
        var eqt = q / 15 - ra
        if (eqt > 12) eqt -= 24 else if (eqt < -12) eqt += 24
        return dg(asin(sin(r(e)) * sin(r(l)))) to eqt
    }

    // Urutan: Subuh, Dzuhur, Ashar, Maghrib, Isya
    fun compute(date: LocalDate, lat: Double, lng: Double, tz: Double, fajr: Double, isha: Double, ihti: Int): List<LocalTime> {
        val jd = julian(date.year, date.monthValue, date.dayOfMonth) - lng / 360.0
        fun noon(t: Double) = fh(12 - sun(jd + t).second)
        fun ang(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sun(jd + t).first
            val x = (-sin(r(angle)) - sin(r(decl)) * sin(r(lat))) / (cos(r(decl)) * cos(r(lat)))
            val tt = dg(acos(x.coerceIn(-1.0, 1.0))) / 15
            return noon(t) + (if (ccw) -tt else tt)
        }
        fun asr(t: Double): Double {
            val decl = sun(jd + t).first
            return ang(-dg(atan(1.0 / (1.0 + tan(r(abs(lat - decl)))))), t, false)
        }
        var f = 5.0; var dh = 12.0; var a = 13.0; var m = 18.0; var i = 18.0
        repeat(2) {
            f = ang(fajr, f / 24, true)
            dh = noon(dh / 24)
            a = asr(a / 24)
            m = ang(0.833, m / 24, false)
            i = ang(isha, i / 24, false)
        }
        val adj = tz - lng / 15
        return listOf(f, dh, a, m, i).map {
            val h = fh(it + adj)
            LocalTime.ofSecondOfDay(((ceil(h * 60 - 1e-6).toLong() + ihti) * 60) % 86400)
        }
    }
}
