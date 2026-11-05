package com.masjid.tv

import android.content.Context
import android.provider.Settings
import android.util.Base64
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

// Lisensi offline: tanda tangan ECDSA P-256 atas "SMTV1|<ID perangkat>"
object License {
    // Tempel kunci publik hasil `python tools/license.py genkey` di antara tanda kutip
    private const val PUB = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE+dB6TIhNGUBsz5seeDXQXdYw6kAyh0JZDELEeoMutEUg8F0ccLDBAOf17avC+gZt39ivh38sS8Ow7u039/vGKQ=="

    private fun sp(c: Context) = c.getSharedPreferences("lic", Context.MODE_PRIVATE)

    fun deviceId(c: Context): String {
        val a = Settings.Secure.getString(c.contentResolver, Settings.Secure.ANDROID_ID) ?: "x"
        val h = MessageDigest.getInstance("SHA-256").digest(("smtv|$a").toByteArray())
            .joinToString("") { "%02X".format(it) }
        return h.substring(0, 12).chunked(4).joinToString("-")
    }

    private fun verify(c: Context, code: String): Boolean = try {
        if (PUB.isBlank() || code.isBlank()) {
            false
        } else {
            val pk = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.decode(PUB.trim(), Base64.DEFAULT)))
            val sg = Signature.getInstance("SHA256withECDSA")
            sg.initVerify(pk)
            sg.update(("SMTV1|" + deviceId(c)).toByteArray())
            sg.verify(Base64.decode(code, Base64.URL_SAFE))
        }
    } catch (e: Exception) { false }

    fun valid(c: Context): Boolean = verify(c, sp(c).getString("code", "") ?: "")

    fun save(c: Context, code: String) {
        val x = code.filter { !it.isWhitespace() }
        if (verify(c, x)) sp(c).edit().putString("code", x).apply()
    }
}
