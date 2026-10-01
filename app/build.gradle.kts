import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Firebase is optional: drop a google-services.json into app/ to enable Analytics + Crashlytics.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

/* ---------------------------------------- AdMob IDs ---------------------------------------- */

// Google's official sample IDs: https://developers.google.com/admob/android/next-gen/test-ads
// Used for every debug build, and as the release fallback for any key missing from admob.properties.
val testAdIds = linkedMapOf(
    "admob_app_id" to "ca-app-pub-3940256099942544~3347511713",
    "admob_app_open_id" to "ca-app-pub-3940256099942544/9257395921",
    "admob_banner_home_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_inter_splash_id" to "ca-app-pub-3940256099942544/1033173712",
    "admob_inter_on_boarding_id" to "ca-app-pub-3940256099942544/1033173712",
    "admob_rewarded_ai_feature_id" to "ca-app-pub-3940256099942544/5224354917",
    "admob_rewarded_inter_ai_feature_id" to "ca-app-pub-3940256099942544/5354046379",
    "admob_native_language_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_native_on_boarding_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_native_home_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_native_full_screen_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_native_settings_id" to "ca-app-pub-3940256099942544/2247696110",
)

// Production IDs live in <root>/admob.properties (see admob.properties.example), keyed like testAdIds.
val releaseAdIds = Properties().apply {
    rootProject.file("admob.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.example.admob_next_gen"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.admob_next_gen"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        testAdIds.forEach { (key, id) -> resValue("string", key, id) }
    }

    buildTypes {
        release {
            val missingKeys = testAdIds.keys.filterNot { releaseAdIds.containsKey(it) }
            if (missingKeys.isNotEmpty()) {
                logger.warn("w: AdMob: admob.properties is missing $missingKeys - release build falls back to TEST ad IDs.")
            }
            testAdIds.forEach { (key, testId) -> resValue("string", key, releaseAdIds.getProperty(key, testId).trim()) }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
        resValues = true
    }
}

// The Next-Gen SDK replaces the legacy SDK; having both on the classpath causes duplicate-class errors.
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Lifecycle & Coroutines
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // SDP layout size && SSP Text sizes
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)

    // Firebase (no-op at runtime unless google-services.json is present)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Navigation components
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Koin for dependency injection
    implementation(libs.koin.android)

    // AdMob Next-Gen SDK + User Messaging Platform (consent)
    implementation(libs.google.ads.mobile.sdk)
    implementation(libs.google.ump)
}
