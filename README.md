# Mass PDF

Pembaca PDF Android yang ringan, offline, dan dibuat ulang dengan Jetpack Compose dan Material 3. Semua rendering dan pengolahan dokumen dilakukan di perangkat; file PDF tidak pernah diunggah ke server mana pun.

## Ringkasan

Mass PDF menggabungkan pustaka dokumen terbaru, browser file lokal, dan pembaca PDF dalam satu aplikasi. Riwayat, halaman terakhir, serta pin tersimpan di perangkat, sehingga PDF yang sering dibuka bisa dilanjutkan tanpa mencari ulang.

Status: aktif dikembangkan. Rilis terbaru **v0.1.3** untuk Android 9 (API 28) ke atas.

## Fitur utama

- Membuka PDF dari picker Android, tautan `ACTION_VIEW`, atau browser file bawaan.
- Pembaca dengan zoom, scroll, navigasi halaman, pencarian teks, dan indikator halaman yang tersinkron.
- Melanjutkan halaman terakhir yang dibaca, termasuk saat PDF dimuat lambat atau dibuka ulang dari aplikasi lain.
- Thumbnail halaman pertama PDF di Beranda dan daftar file.
- Disematkan berbentuk 2×2: empat dokumen per halaman, digeser per halaman, maksimal 20 di panel, dan halaman **Lihat semua** untuk pin lainnya.
- Empat opsi sortir: terakhir dibuka, nama file, tanggal file, dan ukuran file.
- Riwayat maksimal 30 dokumen yang tidak disematkan; semua dokumen yang disematkan tetap tersimpan.
- Browser file dengan indeks MediaStore, filter Semua/PDF/Folder, pencarian nama, serta ubah nama dan hapus dengan konfirmasi.
- Berbagi PDF lewat share sheet Android.
- Cache thumbnail terpisah dan terbatas; dapat dihapus dari Pengaturan tanpa menghapus PDF atau riwayat.
- Tema terang, gelap, atau mengikuti sistem, ditambah ukuran teks antarmuka.
- Formulir dukungan lewat email, riwayat versi, dan pemeriksaan pembaruan dari GitHub Release.

## Teknologi

- Kotlin dan Jetpack Compose dengan Material 3.
- AndroidX PDF Compose (`pdf-compose`, `pdf-document-service`, `pdf-viewer`) 1.0.0-alpha18 sebagai renderer resmi.
- `SandboxedPdfLoader` untuk membuka dokumen; thumbnail dirender dari dokumen terpisah agar tidak mengganggu pembaca.
- `MediaStore` sebagai indeks browser file dan `FileProvider` untuk berbagi dokumen.
- Compose BOM 2026.03.01, minSdk 28, targetSdk 36, compileSdk 37 dengan SDK extension 19.
- JDK 17 dan Gradle 9.6.0; R8 dengan shrink resource untuk build rilis.
- JUnit 4 untuk pengujian unit murni.

## Prasyarat

- JDK 17.
- Android SDK Platform 37 (`platforms;android-37.0`) dan Build Tools 36.0.0.
- Perangkat atau emulator Android 9 ke atas.
- Keystore rilis hanya diperlukan untuk membuat APK bertanda tangan secara lokal.

## Instalasi dan menjalankan

```sh
git clone https://github.com/bimofakot/mass-pdf.git
cd mass-pdf
gradle assembleDebug
```

Pasang `app/build/outputs/apk/debug/app-debug.apk` di perangkat, lalu buka aplikasi dan izinkan akses penyimpanan saat diminta.

Build lokal dapat memakai Gradle yang terpasang, misalnya:

```sh
gradle assembleDebug testDebugUnitTest lintDebug --no-daemon --max-workers=2 \
  -Pkotlin.compiler.execution.strategy=in-process
```

## Cara pakai

1. **Beranda** menampilkan dokumen terbaru dan yang disematkan. Tombol utama membuka picker PDF.
2. Ketuk ikon pin pada kartu untuk menyematkan dokumen; panel Disematkan menampilkan empat dokumen per halaman dan **Lihat semua** membuka daftar penuh.
3. Tombol **Urutkan** di Beranda dan tab File memakai pilihan sortir yang sama.
4. Tab **File** membuka browser penyimpanan. Folder hanya muncul jika berisi PDF; folder berawalan titik disembunyikan. `Android/media` boleh dibuka, sedangkan `Android/data`, `Android/obb`, dan direktori sistem lain ditolak.
5. Di dalam pembaca, back kembali ke layar asal. Posisi folder dan mode **Lihat semua** dipertahankan saat pembaca dibuka dan ditutup.
6. **Pengaturan → Penyimpanan** menyediakan hapus cache thumbnail. Tindakan ini hanya menghapus gambar pratinjau.
7. **Pengaturan → Tentang** berisi formulir dukungan, riwayat versi, dan pemeriksaan pembaruan.

## Tangkapan layar

Tangkapan layar belum disertakan karena build rilis masih harus dijalankan di perangkat. Gambar dari versi 0.1.3 akan ditambahkan setelah pengujian perangkat selesai; konsep desain di `catatan/` bersifat ilustratif dan bukan hasil aplikasi.

## Akses file dan privasi

- Aplikasi memproses PDF sepenuhnya di perangkat; tidak ada unggahan dokumen, iklan, atau analitik.
- Satu-satunya koneksi keluar adalah pemeriksaan pembaruan ke GitHub Releases saat aplikasi dibuka atau diminta pengguna.
- Riwayat, halaman terakhir, pin, tema, dan ukuran teks disimpan di penyimpanan privat aplikasi.
- Thumbnail disimpan di direktori cache privat, dibatasi ukuran dan jumlahnya, serta dapat dihapus tanpa menyentuh PDF.
- Browser file hanya membuka akar penyimpanan yang diizinkan, menolak symlink dan path keluar dari akar, serta melewati folder berawalan titik.
- `Android/media` dibuka agar PDF dari unduhan aplikasi seperti WhatsApp dapat ditemukan; `Android/data`, `Android/obb`, dan direktori sistem lain tetap ditolak.
- Pembagian file memakai `FileProvider` sehingga aplikasi lain hanya menerima akses ke file yang dipilih.

## Pemecahan masalah

- **PDF tidak muncul di Beranda:** PDF tampil setelah dibuka minimal sekali. Di tab File, pastikan izin penyimpanan sudah aktif.
- **Folder terasa kosong:** browser hanya menampilkan folder yang berisi PDF. Folder tanpa PDF tidak ditampilkan dan tidak dapat ditelusuri lebih jauh.
- **Thumbnail tidak muncul:** aplikasi kembali ke ikon PDF bila dokumen terkunci, rusak, atau izinnya dicabut. Coba hapus cache dari Pengaturan lalu buka ulang file.
- **Pembaruan tidak bisa dipasang:** APK harus memakai keystore dan nomor versi yang lebih tinggi. APK dari sumber tanda tangan berbeda harus dipasang ulang dari awal.
- **Laporan:** gunakan formulir dukungan di Pengaturan → Tentang dan sertakan langkah yang memicu masalah.

## Batasan saat ini

- Hanya membaca PDF; belum ada penggabungan, pengeditan, anotasi, atau konversi.
- Belum ada sinkronisasi cloud atau berbagi otomatis antar perangkat.
- Keputusan distribusi Google Play masih terbuka karena kebijakan izin akses penyimpanan luas.
- Tampilan dan perilaku pada perangkat lipat serta tablet besar belum diuji khusus.

## Konfigurasi environment

Build rilis membaca variabel berikut:

| Variabel | Kegunaan |
| --- | --- |
| `JAVA_HOME` | Lokasi JDK 17 |
| `ANDROID_HOME` | Lokasi Android SDK |
| `ANDROID_KEYSTORE_PATH` | Path keystore rilis |
| `ANDROID_KEYSTORE_PASSWORD` | Password keystore |
| `ANDROID_KEY_ALIAS` | Alias kunci |
| `ANDROID_KEY_PASSWORD` | Password kunci |
| `ANDROID_VERSION_NAME` | Nama versi, misalnya `v0.1.3` |
| `ANDROID_VERSION_CODE` | Nomor versi naik untuk pembaruan |

Tanpa keempat variabel signing, build rilis tidak dapat ditandatangani. Rahasia tidak disimpan di repository, catatan, maupun log; di CI nilainya berasal dari GitHub Secrets.

## Struktur folder

- [app/src/main/java/com/massbim/pdf/MainActivity.kt](app/src/main/java/com/massbim/pdf/MainActivity.kt): intent masuk, izin penyimpanan, pembaruan, dan status aplikasi.
- [app/src/main/java/com/massbim/pdf/ui/MassPdfApp.kt](app/src/main/java/com/massbim/pdf/ui/MassPdfApp.kt): Beranda, browser file, pembaca, dan Pengaturan.
- [app/src/main/java/com/massbim/pdf/ui/PdfThumbnail.kt](app/src/main/java/com/massbim/pdf/ui/PdfThumbnail.kt): komponen thumbnail dan dialog hapus cache.
- [app/src/main/java/com/massbim/pdf/data/LibraryDocuments.kt](app/src/main/java/com/massbim/pdf/data/LibraryDocuments.kt): sortir, paging pin, dan retensi riwayat.
- [app/src/main/java/com/massbim/pdf/storage/PdfFileRepository.kt](app/src/main/java/com/massbim/pdf/storage/PdfFileRepository.kt): indeks MediaStore dan operasi file yang aman.
- [app/src/main/java/com/massbim/pdf/storage/PdfThumbnailRepository.kt](app/src/main/java/com/massbim/pdf/storage/PdfThumbnailRepository.kt): render dan cache thumbnail terbatas.
- [app/src/main/java/com/massbim/pdf/ui/theme](app/src/main/java/com/massbim/pdf/ui/theme): warna, tipografi, dan tema.
- [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml): teks antarmuka Indonesia.
- [.github/workflows/android.yml](.github/workflows/android.yml): pipeline build, test, dan rilis.
- `catatan/`: catatan kerja lokal, tidak di-push.

## Perintah penting

```sh
gradle assembleDebug          # APK debug
gradle testDebugUnitTest      # unit test
gradle lintDebug              # pemeriksaan lint
gradle assembleRelease        # APK rilis bertanda tangan
```

## Rilis dan deploy

- Pull request menjalankan build debug dan unit test; artifact debug diunggah ke GitHub Actions.
- Push ke `main` menjalankan verifikasi yang sama.
- Tag dengan format `vMAJOR.MINOR.PATCH` memicu workflow rilis: APK ditandatangani dengan GitHub Secrets, lalu diterbitkan sebagai GitHub Release `mass-pdf.apk`.
- `ANDROID_VERSION_NAME` diambil dari nama tag, sedangkan `ANDROID_VERSION_CODE` dari nomor run workflow agar pembaruan dapat dipasang di atas versi lama.
- Uji APK rilis di perangkat sebelum mengumumkan versi baru.

## Bantuan

- Situs: [massbim.my.id](https://massbim.my.id)
- Email dukungan: andraani30@gmail.com
- Untuk laporan, sertakan versi aplikasi, tipe perangkat, dan langkah yang memicu masalah.

## Pemelihara dan kontribusi

Dikelola oleh Massbim. Perubahan dikerjakan di branch terpisah dengan commit bergaya Conventional Commits, lalu digabung lewat satu pull request. Build, test, dan lint harus lulus sebelum penggabungan.

## Lisensi, status, dan versi

Project pribadi, belum dilisensikan. Kode dan aset tidak disediakan untuk penggunaan ulang tanpa izin.

- Versi: **0.1.3**
- Platform: Android 9+ (minSdk 28)
- Distribusi: GitHub Releases

Dokumentasi versi Inggris tersedia di [README_EN.md](README_EN.md) dan mungkin belum mengikuti pembaruan terakhir.
