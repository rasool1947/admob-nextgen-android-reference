plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.nextgen.ads"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// The Next-Gen SDK replaces the legacy SDK; having both on the classpath causes duplicate-class errors.
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

dependencies {
    // Exposed as `api` so app code can use SDK types (AdView, NativeAdView, ...) directly.
    api(libs.google.ads.mobile.sdk)
    api(libs.google.ump)

    // The Ads SDK pulls WorkManager 2.7.0 (Room 2.2.5), whose WorkDatabase is stripped by R8 full mode
    // and crashes release builds on launch. A newer WorkManager ships the right keep rules.
    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    // Android's org.json is a stub in local unit tests; use the real one.
    testImplementation(libs.org.json)
}

// Published through JitPack (https://jitpack.io): pushing a git tag such as 0.0.1 makes
// `com.github.rasool1947:admob-nextgen-android-reference:0.0.1` available. JitPack replaces the
// group and version below with its own; they matter only for publishToMavenLocal.
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.rasool1947"
            artifactId = "ads"
            version = "0.0.2"
            afterEvaluate { from(components["release"]) }
        }
    }
}
