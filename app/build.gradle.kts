plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.massbim.pdf"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.massbim.pdf"
        // Ubah minSdk hanya setelah memastikan seluruh pustaka tetap mendukung API yang dipilih.
        minSdk = 23
        targetSdk = 35
        // Workflow rilis mengisi nomor versi dari tag agar update dapat dipasang di atas versi lama.
        versionCode = providers.environmentVariable("ANDROID_VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = providers.environmentVariable("ANDROID_VERSION_NAME").orNull
            ?.removePrefix("v") ?: "0.1.0"
    }

    signingConfigs {
        create("release") {
            // Kredensial rilis diambil dari GitHub Secrets melalui environment variable workflow.
            val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
            val keystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
            val keyAliasValue = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
            val keyPasswordValue = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull

            if (keystorePath != null && keystorePassword != null &&
                keyAliasValue != null && keyPasswordValue != null
            ) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Versi viewer dan parser teks dipasangkan agar gestur serta pencarian teks tersedia.
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    testImplementation("junit:junit:4.13.2")
}
