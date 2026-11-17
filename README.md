# Mass PDF

Mass PDF adalah aplikasi pembaca PDF native untuk Android. Dokumen dipilih dari perangkat dan dirender secara lokal; aplikasi tidak mengunggah file PDF. Koneksi internet hanya digunakan untuk memeriksa GitHub Releases dan memberi tahu jika ada update.

## Fitur

- Membuka PDF melalui document picker Android.
- Navigasi halaman, swipe, zoom, dan geser dokumen.
- Mencari teks PDF yang dapat dipilih, lalu melompat ke halaman yang cocok.
- Menyimpan hingga 10 dokumen terbaru di perangkat.
- Membagikan PDF melalui share sheet Android.
- Memeriksa versi baru saat aplikasi dibuka. Tombol **Nanti** menutup ajakan untuk sesi tersebut; ajakan ditampilkan lagi saat aplikasi dibuka berikutnya.

Viewer menggunakan [AndroidPdfViewer](https://github.com/mhiew/AndroidPdfViewer) 3.2.0-beta.3 (Pdfium) untuk rendering dan gestur. [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) 2.0.27.0 mengekstrak teks untuk fitur pencarian. Versi Android minimum adalah API 23 (Android 6.0). PDF hasil scan yang hanya berisi gambar belum mendukung OCR.

## Mendapatkan APK

Build dan pengujian berjalan melalui GitHub Actions, bukan di komputer lokal. Pull request dan push ke `main` menghasilkan APK debug sebagai workflow artifact. Untuk menerbitkan APK release bertanda tangan, push tag versi seperti `v0.1.0`. Nama versi APK mengikuti tag, sedangkan `versionCode` diambil dari nomor workflow GitHub agar update berikutnya dapat dipasang. Setelah CI berhasil, GitHub Actions membuat GitHub Release dan melampirkan APK.

## Menyiapkan signing key

Signing key yang sama harus digunakan pada setiap rilis agar versi baru dapat dipasang sebagai update.

1. Buat keystore release di lokasi aman di luar repository dengan `keytool -genkeypair -v -keystore mass-pdf-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mass-pdf`. `keytool` akan meminta password secara interaktif; jangan tulis password di command.
2. Buat cadangan terenkripsi yang disimpan terpisah, misalnya di lampiran password manager terenkripsi atau media offline terenkripsi. Jangan commit keystore maupun memasukkannya ke artifact publik.
3. Tambahkan repository Actions secrets: `ANDROID_KEYSTORE_BASE64` (isi keystore yang di-encode base64), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, dan `ANDROID_KEY_PASSWORD`. Jangan membagikan kredensial di issue atau log.
4. Push tag `v*` untuk menjalankan workflow rilis. Job rilis akan gagal jika ada secret signing yang belum diisi.

Dengan GNU `base64`, encode file memakai `base64 -w 0 mass-pdf-release.jks`, lalu masukkan hasilnya ke secret `ANDROID_KEYSTORE_BASE64`. Simpan file asli dan cadangannya secara privat; jangan letakkan di repository.

## Pengembangan dan pemeliharaan

- Dependensi, `applicationId`, versi aplikasi, `minSdk`, dan konfigurasi signing ada di [app/build.gradle.kts](app/build.gradle.kts). Pastikan dukungan pustaka sebelum mengubah `minSdk`.
- Alur buka file, pencarian, daftar terbaru, berbagi, dan pemeriksaan update ada di [MainActivity.kt](app/src/main/java/com/massbim/pdf/MainActivity.kt). URL GitHub Releases dan batas jumlah dokumen terbaru ditandai komentar di bagian `companion object`.
- Teks antarmuka berbahasa Indonesia dipusatkan di [strings.xml](app/src/main/res/values/strings.xml).
- Build dan tes CI serta publikasi tag didefinisikan di [android.yml](.github/workflows/android.yml).

CI memakai JDK 17 dan Gradle 8.9. Pengembangan dapat dilakukan melalui Android Studio; proses build APK yang dipublikasikan tetap dijalankan oleh GitHub Actions.

Dokumentasi berbahasa Inggris tersedia di [README_EN.md](README_EN.md).
