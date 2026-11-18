plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.massbim.pdf"
    compileSdk {
        version = release(37) {
            sdkExtension = 19
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.massbim.pdf"
        minSdk = 28
        targetSdk = 36
        // Workflow rilis mengisi nomor versi dari tag agar update dapat dipasang di atas versi lama.
        versionCode = providers.environmentVariable("ANDROID_VERSION_CODE").orNull?.toIntOrNull() ?: 3
        versionName = providers.environmentVariable("ANDROID_VERSION_NAME").orNull
            ?.removePrefix("v") ?: "0.1.2"
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

}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.03.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.pdf:pdf-compose:1.0.0-alpha18")
    implementation("androidx.pdf:pdf-document-service:1.0.0-alpha18")
    implementation("androidx.pdf:pdf-viewer:1.0.0-alpha18")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
