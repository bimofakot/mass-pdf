# Mass PDF

Mass PDF adalah aplikasi pembaca PDF Android yang dirancang ulang sebagai pengalaman native modern dengan Jetpack Compose dan Material 3. Semua rendering dan pengolahan dokumen tetap dilakukan di perangkat; aplikasi tidak mengunggah file PDF ke server mana pun.

## Fitur utama

- Buka PDF dari penyimpanan lokal, picker Android, atau tautan `ACTION_VIEW` yang dibagikan aplikasi lain.
- Tampilan pembaca dengan zoom, scroll, navigasi halaman, dan pencarian teks di dalam dokumen.
- Riwayat dokumen terbaru beserta halaman terakhir yang dibuka.
- Browser file lokal dengan opsi buka, ubah nama, dan hapus PDF dengan konfirmasi.
- Berbagi file PDF melalui Android share sheet.
- Prompt pembaruan GitHub Release yang muncul saat aplikasi dibuka.
- Tema terang/gelap yang mengikuti sistem dan konfigurasi pilihan pengguna.

## Teknologi yang digunakan

- Jetpack Compose + Material 3 untuk UI utama.
- AndroidX PDF Compose sebagai renderer PDF resmi dan berlisensi terbuka.
- `SandboxedPdfLoader` untuk membuka dokumen secara aman.
- `PdfDocument.searchDocument` untuk pencarian teks PDF.
- `MANAGE_EXTERNAL_STORAGE` hanya dipertimbangkan sebagai akses luas awal pada Android 11+; aplikasi tetap tidak mengakses folder yang dilindungi seperti `Android/`, `data/`, `obb/`, maupun direktori aplikasi lain.
- Target API: minSdk 28 dan compileSdk 37 dengan SDK extension 19 sesuai kebutuhan AndroidX PDF Compose.

## Keputusan desain dan kebijakan file

- File diproses secara lokal dan offline.
- Akses file menggunakan SAF dan `ACTION_VIEW` dari aplikasi lain tetap didukung.
- Ketika Android menanyakan aplikasi default untuk PDF, Mass PDF tidak mengatur default secara diam-diam; pengguna memilih aplikasi default melalui resolver Android.
- Browser file hanya membuka lokasi yang diizinkan, memblokir direktori yang dilindungi, dan menolak alias/symlink yang tidak aman.
- Akses file luas dianggap eksperimen awal; arah masa depan tetap dapat beralih ke SAF yang lebih aman.

## Build dan rilis

Build dan pengujian dijalankan melalui GitHub Actions, bukan di komputer lokal. Pull request dan push ke `main` akan membangun APK debug dan mengunggahnya sebagai artifact. Saat tag rilis `v*` dibuat, workflow otomatis menandatangani APK dan merilisnya ke GitHub Release dengan nama file `mass-pdf.apk`.

## Menyiapkan signing key

Gunakan keystore yang sama untuk setiap rilis agar setiap pembaruan dapat dipasang sebagai update.

1. Buat keystore release di lokasi aman di luar repository dengan `keytool -genkeypair -v -keystore mass-pdf-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mass-pdf`.
2. Simpan backup terenkripsi secara terpisah dari repository.
3. Tambahkan secret GitHub Actions: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, dan `ANDROID_KEY_PASSWORD`.
4. Push tag seperti `v0.1.0` untuk memicu workflow rilis.

## Struktur utama

- [app/build.gradle.kts](app/build.gradle.kts): konfigurasi Android, dependency, dan versi.
- [app/src/main/java/com/massbim/pdf/MainActivity.kt](app/src/main/java/com/massbim/pdf/MainActivity.kt): logika buka PDF, pembaruan, izin, dan riwayat.
- [app/src/main/java/com/massbim/pdf/ui/MassPdfApp.kt](app/src/main/java/com/massbim/pdf/ui/MassPdfApp.kt): antarmuka Compose untuk library, browser file, PDF reader, dan pengaturan.
- [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml): teks antarmuka Indonesia.
- [.github/workflows/android.yml](.github/workflows/android.yml): pipeline CI/CD GitHub Actions.

## Catatan pengembangan

- Build dan pengujian harus dilakukan lewat workflow GitHub Actions.
- Hindari penggunaan local storage yang tidak aman; fokus pada file yang diizinkan oleh Android.
- Ikon aplikasi saat ini menggunakan vektor sementara karena logo resmi belum tersedia.

Dokumentasi dalam bahasa Inggris tersedia di [README_EN.md](README_EN.md).
