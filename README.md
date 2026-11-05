# MasjiQ

## Build APK (GitHub)
1. Repo lama: upload ulang semua isi zip ini (timpa file yang sama). Folder `.github` sudah ada di repo, tidak perlu di-upload lagi.
   Repo baru: upload semua isi, lalu buat manual file `.github/workflows/build.yml` (isi ada di bagian bawah).
2. Tab Actions > tunggu hijau > unduh artifact `SmartTvMasjid-APK` > ekstrak > `app-debug.apk`.

## Pemakaian
- Buka aplikasi sekali, izinkan "Tampil di atas aplikasi lain" (wajib untuk auto-start Android 10+).
- Alamat panel tampil kecil di bawah jam. Buka dari HP (satu WiFi): `http://IP:8080`
- **PIN awal: 0000.** Ganti di kolom "PIN panel" (4-8 angka), lalu SIMPAN. Login berlaku 24 jam di browser.
- Panel: nama masjid, khatib/imam/muadzin, teks berjalan, bahasa TV (Indonesia/English/Arab), kota (dropdown, tanpa koordinat), metode hitung, durasi adzan/iqomah, layar gelap saat shalat, logo, font, gambar & video.
- **Layar gelap:** setelah iqomah selesai layar jadi hitam selama "Lama layar gelap / shalat". Matikan lewat dropdown "Layar gelap saat shalat". Khusus Jumat: setelah iqomah gelap selama "Lama khutbah + shalat Jumat".
- **Logo:** upload PNG/JPG (kotak, latar transparan lebih bagus). Tampil di kiri header.
- **Font:** bawaan memakai font sistem. Upload file `.ttf`/`.otf` di panel untuk menggantinya (font Arab perlu dipilih yang mendukung huruf Arab jika bahasa Arab dipakai).
- Gambar/video tampil urut nama file (01-, 02-, ...).
- Kota di luar daftar: bilang saja, tambahkan di `Cities.kt`.

## Lisensi (untuk penjual)
Bayar sekali per masjid, terikat ID perangkat, diperiksa offline (ECDSA P-256).
Tanpa lisensi: nama masjid, khatib/imam/muadzin, teks berjalan, bahasa, durasi, mode Jumat, layar gelap, logo, font, gambar & video terkunci (panel menampilkan 🔒). Yang tetap bebas: kota, metode, ihtiyath, koreksi Hijriah, PIN.

Sekali saja (di PC, perlu Python):
1. `pip install cryptography`
2. `python tools/license.py genkey` -> tempel kunci publik yang tampil ke `PUB` di `app/src/main/java/com/masjid/tv/License.kt`.
3. Simpan `private.pem` baik-baik (cadangkan; hilang = tidak bisa menerbitkan lisensi). Jangan di-upload (sudah masuk `.gitignore`). Jadikan repo GitHub **private**.
4. Build ulang APK. Kunci publik harus sudah terisi sebelum APK dijual; kalau kosong, semua perangkat dianggap belum berlisensi.

Per pembeli:
1. Pembeli kirim "ID perangkat" dari panel (`IP:8080`) atau dari strip merah di layar TV.
2. `python tools/license.py issue XXXX-XXXX-XXXX` -> kirim kode yang keluar.
3. Pembeli tempel di kolom "Kode lisensi" lalu AKTIFKAN.
ID perangkat berubah jika TV Box di-factory reset; terbitkan kode baru untuk ID baru.

## Tes cepat
Ubah "Ihtiyath" agar shalat berikutnya tinggal beberapa menit, set adzan/iqomah ke 1 menit, lalu kembalikan nilai awal (Ihtiyath 2).

## build.yml
```yaml
name: Build APK
on:
  push:
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - name: Accept licenses
        run: yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses || true
      - uses: gradle/actions/setup-gradle@v3
        with: { gradle-version: "8.7" }
      - run: gradle assembleDebug --no-daemon
      - uses: actions/upload-artifact@v4
        with:
          name: SmartTvMasjid-APK
          path: app/build/outputs/apk/debug/app-debug.apk
```

## Kunci tanda tangan (tetap)
`app/masjiq.jks` dipakai di setiap build, jadi ID perangkat dan lisensi tidak berubah antar build, dan update bisa menimpa aplikasi lama. **Repo harus tetap private. Cadangkan `app/masjiq.jks` ke flashdisk** (hilang/berganti = tanda tangan berubah = ID berubah, lisensi pembeli harus diterbitkan ulang). Password ada di `app/build.gradle.kts`.
