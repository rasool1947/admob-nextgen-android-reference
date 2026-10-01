# AdMob Next Gen SDK — Android Integration

A complete Android integration of the **Google Mobile Ads Next Gen SDK** (`ads-mobile-sdk:1.0.0`) using Clean Architecture, Koin DI, and MVVM.

---

## 📦 SDK

```gradle
implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.0.0")
```

---

## 🎯 Ad Formats Covered

| Format | Status |
|---|---|
| App Open Ad | ✅ |
| Banner Ad (Adaptive + Collapsible) | ✅ |
| Interstitial Ad | ✅ |
| Native Ad (Large + Small) | ✅ |
| Rewarded Ad | ✅ |
| Rewarded Interstitial Ad | ✅ |

---

## 🏗️ Architecture

```
Clean Architecture + MVVM + Koin DI
├── data/
│   ├── dataSources/local   → Cache (ConcurrentHashMap)
│   ├── dataSources/remote  → SDK load calls
│   ├── entities            → Data models
│   └── repositories        → Repository implementations
├── domain/
│   ├── repositories        → Interfaces
│   └── useCases            → Business logic
└── presentation/
    ├── enums               → Ad keys
    ├── viewModels          → LiveData + coroutines
    └── ui                  → Custom ad views
```

---

## 🔑 Key Changes from Old SDK

| Old SDK (`play-services-ads`) | New SDK (`ads-mobile-sdk`) |
|---|---|
| `AdRequest.Builder().build()` | `AdRequest.Builder(adUnitId).build()` |
| `InterstitialAd.load(context, id, request, cb)` | `InterstitialAd.load(request, cb)` |
| `FullScreenContentCallback` | `InterstitialAdEventCallback` |
| `AdLoader.Builder(context, id)` | `NativeAdLoader.load(NativeAdRequest, cb)` |
| `AdMobAdapter` bundle for collapsible | `BannerAdRequest.Builder.setGoogleExtrasBundle()` |
| App ID in `AndroidManifest.xml` | `MobileAds.initialize(context, InitializationConfig)` |
| Callbacks on main thread | **Callbacks on background thread** → use `postValue` / `Handler(Main)` |

---

## 🚀 Setup

1. Clone the repo
2. Open in Android Studio
3. Replace test ad IDs in `app/build.gradle.kts` with your real AdMob IDs
4. Add your `google-services.json` for Firebase (optional — app works without it)
5. Run on device/emulator

---

## 📋 Requirements

- Android min SDK: 24
- Compile SDK: 36
- Kotlin
- No Firebase required to run

---

## 📁 Project Structure

```
app/
├── ads/
│   ├── appOpen/       → App Open Ad
│   ├── banner/        → Banner Ad
│   ├── cmp/           → Consent (UMP)
│   ├── interstitial/  → Interstitial Ad
│   ├── natives/       → Native Ad
│   └── rewarded/      → Rewarded + Rewarded Interstitial
├── app/               → Screens (Entrance, Home, Feature, Settings, Premium)
├── di/                → Koin modules
└── utilities/         → Base classes, extensions, Firebase, SharedPrefs
```

---

## 📄 License

```
MIT License — free to use, modify, and distribute.
```
