# Mass PDF

Mass PDF is a modern Android PDF reader redesigned as a native Jetpack Compose + Material 3 experience. Documents are opened, rendered, searched, and managed locally; the app never uploads PDF files to a remote server.

## Core features

- Open PDFs from local storage, Android document pickers, and file URIs passed via `ACTION_VIEW`.
- Read PDFs with zoom, pan, page navigation, and in-document text search.
- Keep a recent document list with the last page opened for each file.
- Browse local files, rename PDFs, and delete them with confirmation.
- Share PDFs through the Android share sheet.
- Prompt for GitHub Releases updates when the app opens.
- Support light/dark themes with either system-following or explicit user selection.

## Stack and constraints

- Jetpack Compose + Material 3 for the app shell and screens.
- AndroidX PDF Compose as the official, open-source PDF renderer.
- `SandboxedPdfLoader` for document loading and isolation.
- `PdfDocument.searchDocument` for PDF text search.
- `MANAGE_EXTERNAL_STORAGE` is used only as an initial broad-access experiment for Android 11+, while protected directories such as `Android/`, `data/`, `obb/`, and other app-specific folders remain out of bounds.
- Target API: minSdk 28 and compileSdk 37 with SDK extension 19 as required by AndroidX PDF Compose.

## File access policy

- PDFs are processed locally and offline.
- File access supports both SAF and external app `ACTION_VIEW` flows.
- The app never silently sets itself as the default PDF handler; Android's resolver remains the user-controlled path for default-app selection.
- The file browser only enumerates allowed locations, rejects symlinks and unsafe paths, and blocks protected system directories.
- Broad all-files access is treated as an initial experiment; a safer SAF-first future remains possible.

## Build and release flow

Builds and tests run via GitHub Actions instead of a local machine. Pull requests and pushes to `main` produce a debug APK artifact. When a `v*` tag is pushed, the workflow signs and publishes the APK as `mass-pdf.apk` in a GitHub Release.

## Release signing setup

Use the same keystore for every release so new builds can install as updates.

1. Generate a release keystore outside the repository with `keytool -genkeypair -v -keystore mass-pdf-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mass-pdf`.
2. Keep a separate encrypted backup outside the repo.
3. Add these GitHub Actions secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.
4. Push a tag such as `v0.1.0` to trigger the release pipeline.

## Main project files

- [app/build.gradle.kts](app/build.gradle.kts): Android module configuration and dependency versions.
- [app/src/main/java/com/massbim/pdf/MainActivity.kt](app/src/main/java/com/massbim/pdf/MainActivity.kt): file-opening, recent-documents, update checks, and permission logic.
- [app/src/main/java/com/massbim/pdf/ui/MassPdfApp.kt](app/src/main/java/com/massbim/pdf/ui/MassPdfApp.kt): Compose screens and reader experience.
- [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml): Indonesian UI strings.
- [.github/workflows/android.yml](.github/workflows/android.yml): GitHub Actions CI/CD pipeline.

## Development notes

- CI and validation should happen through GitHub Actions, not local machine builds.
- Keep file handling inside Android-approved locations to preserve the app's storage policy.
- The app icon uses a temporary vector placeholder until the final brand asset is provided.

The Indonesian documentation is in [README.md](README.md).
