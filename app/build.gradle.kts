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
    "admob_app_open_splash_id" to "ca-app-pub-3940256099942544/9257395921",
    "admob_app_open_resume_id" to "ca-app-pub-3940256099942544/9257395921",
    "admob_inter_splash_id" to "ca-app-pub-3940256099942544/1033173712",
    "admob_inter_main_id" to "ca-app-pub-3940256099942544/1033173712",
    "admob_inter_on_boarding_id" to "ca-app-pub-3940256099942544/1033173712",
    "admob_rewarded_ai_feature_id" to "ca-app-pub-3940256099942544/5224354917",
    "admob_rewarded_inter_ai_feature_id" to "ca-app-pub-3940256099942544/5354046379",
    // Ad slots: one native + one banner unit each (the ads control picks which one shows)
    "admob_native_splash_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_splash_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_language_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_language_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_on_boarding_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_on_boarding_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_ob1_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_ob1_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_ob2_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_ob2_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_ob3_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_ob3_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_ob4_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_ob4_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_main_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_main_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_home_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_home_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_explore_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_explore_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_history_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_history_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_settings_id" to "ca-app-pub-3940256099942544/2247696110",
    "admob_banner_settings_id" to "ca-app-pub-3940256099942544/9214589741",
    "admob_native_feature_id" to "ca-app-pub-3940256099942544/2247696110",
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
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Lifecycle & Coroutines
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Firebase (no-op at runtime unless google-services.json is present)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)

    // Navigation components
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Koin for dependency injection
    implementation(libs.koin.android)

    // Ads module (brings in the AdMob Next-Gen SDK + User Messaging Platform)
    implementation(project(":ads"))
}
