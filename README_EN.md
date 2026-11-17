# Mass PDF

Mass PDF is a native Android PDF reader. Documents are selected from the device and rendered locally; the app does not upload PDF files. An internet connection is used only to check GitHub Releases and notify the user about updates.

## Features

- Open PDF files with Android's document picker.
- Navigate pages, swipe, zoom, and pan.
- Search selectable PDF text and jump to a matching page.
- Keep up to 10 recently opened documents on the device.
- Share PDFs through Android's share sheet.
- Check for a newer version when the app launches. Tapping **Later** dismisses the prompt for that session; it is offered again the next time the app opens.

The viewer uses [AndroidPdfViewer](https://github.com/mhiew/AndroidPdfViewer) 3.2.0-beta.3 (Pdfium) for rendering and gestures. [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) 2.0.27.0 extracts text for search. The minimum Android version is API 23 (Android 6.0). Scanned, image-only PDFs do not support OCR yet.

## Getting an APK

Builds and tests run in GitHub Actions, not on a local computer. Pull requests and pushes to `main` produce a debug APK as a workflow artifact. To publish a signed release APK, push a version tag such as `v0.1.0`. The APK version name follows the tag, while its `versionCode` comes from the GitHub workflow run number so that later updates can be installed. After CI succeeds, GitHub Actions creates a GitHub Release and attaches the APK.

## Setting up release signing

The same signing key must be used for every release so users can install new versions as updates.

1. Generate a release keystore in a secure location outside this repository with `keytool -genkeypair -v -keystore mass-pdf-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mass-pdf`. `keytool` prompts for passwords; do not put passwords in the command.
2. Create a separate encrypted backup, such as an encrypted password-manager attachment or encrypted offline storage. Do not commit the keystore or include it in a public artifact.
3. Add these repository Actions secrets: `ANDROID_KEYSTORE_BASE64` (the keystore encoded as base64), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Do not share credentials in issues or logs.
4. Push a `v*` tag to run the release workflow. The release job fails if any signing secret is missing.

With GNU `base64`, encode the file using `base64 -w 0 mass-pdf-release.jks`, then enter the output as the `ANDROID_KEYSTORE_BASE64` secret. Keep the original and its backup private; never place them in the repository.

## Development and maintenance

- Dependencies, `applicationId`, app version, `minSdk`, and signing configuration are in [app/build.gradle.kts](app/build.gradle.kts). Verify library support before changing `minSdk`.
- File opening, search, recents, sharing, and update checks are in [MainActivity.kt](app/src/main/java/com/massbim/pdf/MainActivity.kt). The GitHub Releases URL and recent-document limit are marked by comments in the `companion object`.
- Indonesian UI text is centralized in [strings.xml](app/src/main/res/values/strings.xml).
- CI builds/tests and tag publishing are defined in [android.yml](.github/workflows/android.yml).

CI uses JDK 17 and Gradle 8.9. You can develop in Android Studio; the published APK is built by GitHub Actions.

The Indonesian documentation is in [README.md](README.md).
