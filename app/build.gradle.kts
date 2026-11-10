import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    //kotlin("kapt")
}

// Loads values from the project root .env file (if present) and exposes them to BuildConfig.
val envProperties = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}

fun envValue(name: String, defaultValue: String = ""): String {
    val value = envProperties.getProperty(name) ?: System.getenv(name) ?: defaultValue
    return value.trim().replace("\\", "\\\\").replace("\"", "\\\"")
}

android {
    namespace = "com.example.test"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.test"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Currency API configuration from .env
        buildConfigField(
            "String",
            "EXCHANGE_RATE_API_KEY",
            "\"${envValue("EXCHANGE_RATE_API_KEY")}\""
        )
        buildConfigField(
            "String",
            "EXCHANGE_RATE_API_BASE",
            "\"${envValue("EXCHANGE_RATE_API_BASE", "https://v6.exchangerate-api.com/v6")}\""
        )
        buildConfigField(
            "String",
            "CURRENCY_BACKEND_BASE_URL",
            "\"${envValue("CURRENCY_BACKEND_BASE_URL", "http://10.0.2.2:8000")}\""
        )
        buildConfigField(
            "String",
            "NEON_AUTH_BASE_URL",
            "\"${envValue("NEON_AUTH_BASE_URL")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Disable desugaring
        isCoreLibraryDesugaringEnabled = false
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Core library desugaring dependency removed
    
    // Core Kotlin + Coroutines
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.runtime.livedata)

    // Compose (BOM + individual modules)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Activity & Fragment KTX (fixes missing .aar errors)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.annotation)

    // Room (remove string‐interpolation duplicates)
    implementation(libs.androidx.room.runtime.v271)
    implementation(libs.androidx.room.ktx.v271)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // OkHttp (auth + currency HTTP clients)
    implementation(libs.okhttp)

    // Hilt
    implementation(libs.hilt.android.v2562)
    ksp(libs.hilt.android.compiler)

    // Hilt + Compose Navigation
    implementation(libs.androidx.hilt.navigation.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Lifecycle Runtime Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

//    // Material Design 3
//    implementation(libs.androidx.material3.v120)
//
//    // Material Icons
//    implementation(libs.androidx.material.icons.core)
//    implementation (libs.androidx.material.icons.extended)
//
//    // Foundation for layout
//    implementation (libs.androidx.foundation)
}