package com.masjid.tv

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import java.io.File
import java.util.UUID

// Panel pengaturan: http://IP-TV:8080 (butuh PIN)
class WebServer(private val ctx: Context, port: Int, private val onChange: () -> Unit) : NanoHTTPD(port) {
    private val dir = File(ctx.filesDir, "media").apply { mkdirs() }
    private val logoF = File(ctx.filesDir, "logo.img")
    private val fontF = File(ctx.filesDir, "font.ttf")
    private val imgExt = setOf("jpg", "jpeg", "png", "webp")
    private val mediaExt = imgExt + setOf("mp4", "webm", "mkv")
    private val token = UUID.randomUUID().toString()
    private val free = setOf("kota", "metode", "ihtiyath", "hijriAdj", "pin")

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun clean(s: String) = s.substringAfterLast('/').substringAfterLast('\\').replace(Regex("[^A-Za-z0-9._-]"), "_").replace("..", "_")
    private fun back(): Response =
        newFixedLengthResponse(Response.Status.REDIRECT_SEE_OTHER, "text/plain", "").also { it.addHeader("Location", "/") }
    private fun html(b: String): Response = newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", b)
    private fun authed(s: IHTTPSession) = s.headers["cookie"]?.contains("mt=$token") == true

    private fun put(tmp: String?, name: String, dest: File, ok: Set<String>) {
        if (tmp != null && name.substringAfterLast('.', "").lowercase() in ok) {
            File(tmp).copyTo(dest, true)
            File(tmp).delete()
        }
    }

    override fun serve(s: IHTTPSession): Response = try {
        val post = s.method == Method.POST
        val files = HashMap<String, String>()
        if (post) s.parseBody(files)
        if (post && s.uri == "/login") {
            if (s.parms["pin"]?.trim() == Cfg.map(ctx)["pin"]) {
                back().also { it.addHeader("Set-Cookie", "mt=$token; Path=/; Max-Age=86400") }
            } else {
                Thread.sleep(1500)
                html(login(true))
            }
        } else if (!authed(s)) {
            html(login(false))
        } else if (post) {
            val n = clean(s.parms["file"] ?: s.parms["name"] ?: "")
            val lic = License.valid(ctx)
            when (s.uri) {
                "/save" -> Cfg.save(ctx, if (lic) s.parms else s.parms.filterKeys { it in free })
                "/license" -> License.save(ctx, s.parms["code"] ?: "")
                "/upload" -> if (lic) put(files["file"], n, File(dir, n), mediaExt)
                "/delete" -> File(dir, n).delete()
                "/logo" -> if (lic) put(files["file"], n, logoF, imgExt)
                "/logodel" -> logoF.delete()
                "/font" -> if (lic) put(files["file"], n, fontF, setOf("ttf", "otf"))
                "/fontdel" -> fontF.delete()
            }
            onChange()
            back()
        } else {
            html(page())
        }
    } catch (e: Exception) {
        newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.toString())
    }

    private val css = "body{font-family:sans-serif;background:#08241b;color:#f3ebd3;padding:16px;max-width:640px;margin:auto}" +
        "label{display:block;margin-top:12px;color:#c9a24a;font-size:14px}" +
        "input[type=text],input[type=number],input[type=password],select{width:100%;box-sizing:border-box;padding:12px;font-size:16px;border-radius:6px;border:0}" +
        "button{margin-top:16px;padding:14px;width:100%;font-size:17px;font-weight:bold;background:#c9a24a;color:#08241b;border:0;border-radius:6px}" +
        "button.d{background:#9b3b2a;color:#fff;width:auto;padding:6px 12px;font-size:14px;margin:0}" +
        "li{margin:8px 0;display:flex;justify-content:space-between;align-items:center}ul{padding:0;list-style:none}h2{margin-top:32px}"

    private fun login(err: Boolean) = "<!doctype html><html><head><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><style>$css</style></head><body>" +
        "<h2>Masuk</h2>${if (err) "<p>PIN salah</p>" else ""}<form method=post action=/login><label>PIN</label>" +
        "<input type=password name=pin inputmode=numeric autofocus><button>MASUK</button></form></body></html>"

    private fun upForm(action: String, accept: String) =
        "<form method=post action=$action enctype=multipart/form-data><input type=file name=file accept='$accept'><button>UPLOAD</button></form>"

    private fun delForm(action: String, label: String) =
        "<form method=post action=$action><button class=d>$label</button></form>"

    private fun page(): String = buildString {
        append("<!doctype html><html><head><meta charset=utf-8><meta name=viewport content='width=device-width,initial-scale=1'><title>MasjiQ</title><style>$css</style></head><body>")
        val lic = License.valid(ctx)
        append("<h2 style='margin-top:0'>Lisensi</h2><p>ID perangkat: <b>${License.deviceId(ctx)}</b></p><p>Status: <b>${if (lic) "BERLISENSI" else "BELUM BERLISENSI (fitur terkunci)"}</b></p>")
        if (!lic) append("<form method=post action=/license><label>Kode lisensi</label><input type=text name=code><button>AKTIFKAN</button></form>")
        append("<h2>Pengaturan</h2><form method=post action=/save>")
        val m = Cfg.map(ctx)
        Cfg.fields.forEach { f ->
            val v = m[f.key] ?: f.def
            val lock = !lic && f.key !in free
            val dis = if (lock) " disabled" else ""
            append("<label>${esc(f.label)}${if (lock) " 🔒" else ""}</label>")
            when (f.kind) {
                "s" -> {
                    append("<select name=${f.key}$dis>")
                    f.opts.forEach { (k, l) -> append("<option value=\"${esc(k)}\"${if (k == v) " selected" else ""}>${esc(l)}</option>") }
                    append("</select>")
                }
                "n" -> append("<input type=number step=any name=${f.key}$dis value=\"${esc(v)}\">")
                else -> append("<input type=text name=${f.key}$dis value=\"${esc(v)}\">")
            }
        }
        append("<button>SIMPAN</button></form>")
        if (!lic) append("<p>🔒 Logo, font, gambar dan video terkunci sampai berlisensi.</p>")
        append("<h2>Logo masjid</h2>")
        if (logoF.exists()) append("<p>Logo terpasang.</p>${delForm("/logodel", "Hapus logo")}")
        append(upForm("/logo", "image/*"))
        append("<h2>Font (opsional)</h2><p>Upload file .ttf / .otf.</p>")
        if (fontF.exists()) append("<p>Font kustom terpasang.</p>${delForm("/fontdel", "Kembali ke font bawaan")}")
        append(upForm("/font", ".ttf,.otf"))
        append("<h2>Gambar / Video</h2><p>Tampil urut nama file (01-, 02-, ...). Format: jpg, png, webp, mp4.</p><ul>")
        dir.listFiles()?.sortedBy { it.name.lowercase() }?.forEach { f ->
            append("<li>${esc(f.name)}<form method=post action=/delete><input type=hidden name=name value=\"${esc(f.name)}\"><button class=d>Hapus</button></form></li>")
        }
        append("</ul>${upForm("/upload", "image/*,video/mp4,video/webm")}</body></html>")
    }
}
