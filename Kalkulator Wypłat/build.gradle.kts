import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

// Wczytywanie zmiennych z local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.example.kalkulatorwyplat"
    compileSdk = 36

    defaultConfig {
        manifestPlaceholders += mapOf()
        applicationId = "kalkulator.wyplat"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Puste miejsce na ID aplikacji AdMob (aby uniknąć błędów przy braku konfiguracji)
        manifestPlaceholders["adMobAppId"] = ""
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("MYAPP_RELEASE_STORE_FILE", "brak-sciezki"))
            storePassword = localProperties.getProperty("MYAPP_RELEASE_STORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("MYAPP_RELEASE_KEY_ALIAS", "")
            keyPassword = localProperties.getProperty("MYAPP_RELEASE_KEY_PASSWORD", "")
        }
    }

    // Wymagane, aby kompilator generował klasę BuildConfig z naszymi zmiennymi
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            // W trybie Debug ZAWSZE używamy testowych ID z dokumentacji Google
            manifestPlaceholders["adMobAppId"] = "ca-app-pub-3940256099942544~3347511713"

            buildConfigField("String", "AD_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_ADSTART_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
        }

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Używamy naszej konfiguracji podpisywania
            signingConfig = signingConfigs.getByName("release")

            // Pobieranie prawdziwych ID z local.properties dla wersji Release
            val adMobAppId = localProperties.getProperty("AD_APP_ID") ?: ""
            val bannerId = localProperties.getProperty("AD_BANNER_ID") ?: ""
            val interstitialId = localProperties.getProperty("AD_INTERSTITIAL_ID") ?: ""
            val adStartId = localProperties.getProperty("AD_ADSTART_ID") ?: ""

            manifestPlaceholders["adMobAppId"] = adMobAppId
            buildConfigField("String", "AD_BANNER_ID", "\"$bannerId\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"$interstitialId\"")
            buildConfigField("String", "AD_ADSTART_ID", "\"$adStartId\"")
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
    // Podstawowe i UI Legacy
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose Core
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.compose.material.icons.extended)

    // Lifecycle & Navigation
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.runtime.livedata)

    // Baza danych Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Sieć i Dane
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)

    // Obrazy
    implementation(libs.glide)
    implementation(libs.glide.compose)

    // Usługi: Reklamy, Płatności, Firebase
    implementation(libs.play.services.ads)
    implementation(libs.androidx.billing.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.google.ads.ump)
}