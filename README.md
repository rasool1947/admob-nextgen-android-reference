# AdMob Next-Gen Android Reference

A ready-to-copy starter for Android apps monetized with the **Google Mobile Ads Next-Gen SDK**
(`com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0`). It contains:

- **`:ads`**: a reusable Android library module with consent, every ad format, preloading, shimmer
  placeholders, frequency capping and a JSON "ads control" that turns any ad on, off or into
  another type at runtime (Firebase Remote Config ready).
- **`:app`**: a sample app built on the flow most utility apps use:
  **Splash → Language → Onboarding → Main (bottom navigation)**, in English, Urdu and Arabic (RTL).

Debug builds use Google's test ad units, so the project runs as-is.

---

## Contents

1. [The app flow and its ads](#1-the-app-flow-and-its-ads)
2. [Run it](#2-run-it)
3. [The ads control (JSON)](#3-the-ads-control-json)
4. [Ad units and placements](#4-ad-units-and-placements)
5. [Using `:ads` in a new app](#5-using-ads-in-a-new-app)
6. [`:ads` API cheat sheet](#6-ads-api-cheat-sheet)
7. [Project structure](#7-project-structure)
8. [Reading the ad logs](#8-reading-the-ad-logs)
9. [Notes and gotchas](#9-notes-and-gotchas)

---

## 1. The app flow and its ads

```
Splash ──► Language ──► Onboarding (pages) ──► Main ──► Home / Explore / History / Settings
  │           │             │                    │
  │ full-screen ad          │ ad per page        │ ad below the bottom navigation
  │ + bottom ad │ bottom ad │ (or one shared)    │ ad inside each tab
  │           │             │ "Get Started" inter │ interstitial on navigation (paced)
  └──────── consent first                        └ App Open on return ("Welcome back" first)
```

| Screen | Ads |
|---|---|
| **Splash** | App Open **or** interstitial (or none) + a bottom native/banner. Both load together; the splash waits for both (max 25 s, counted after consent), keeps a loaded bottom ad on screen 2 s, then shows the full-screen ad and moves on. A progress bar shows the wait. |
| **Language** | Bottom native/banner. Shown on first run and from Settings. |
| **Onboarding** | One ad per page or one shared ad; interstitial on "Get Started". |
| **Main** | Ad below the bottom navigation, an ad inside each tab, interstitial on screen navigation (every *n*th click, optional first click, minimum interval). |
| **Return to the app** | "Welcome back" screen for 1 s, then an App Open ad. |

Rules built in:

- **First flow**: until the user reaches Main from onboarding once, every launch runs
  Splash → Language → Onboarding → Main again (the language picked earlier is preselected).
  After that: Splash → Main.
- **Preload chain** (first flow only): Splash loads the Language ad, Language loads onboarding
  page 1's ad, each onboarding page loads the next page's, and the last page loads the "Get Started"
  interstitial, so each ad appears instantly. Only ads that are on in the ads control are loaded; the
  main screen loads its own ads.
- **Kept ads**: a native/banner ad whose screen closes is kept for that screen (per placement) and
  shown again at once when the screen reopens (back from another screen, a tab switch, rotation).
  JSON `cache` decides whether a new one is requested:
  - never seen yet, or seen less than `reuse_shown_sec` (30) ago → shown again, **no request**;
  - seen longer ago → shown again **and** a new ad is requested in the background and swapped in
    when it arrives (a fresh impression; on failure the kept one stays). If the screen closes before
    it arrives, the new never-seen ad is kept for next time, so no request is wasted;
  - loaded more than `max_age_min` (50) ago → destroyed, normal load.
  Nothing is loaded in the background for a screen the user doesn't come back to.
- **Loaders**: native/banner slots show a shimmer of the ad's size while loading;
  interstitials/rewarded show a "Loading ad…" dialog for 1 s first.
- **Safety**: a double tap never shows two ads or navigates under an ad; ads keep a gap from
  navigation and buttons; every ad is destroyed with its screen.

## 2. Run it

Requirements: Android Studio (AGP 9), JDK 17+, min SDK 24, compile SDK 36.

```bash
./gradlew :app:installDebug     # test ads, consent form forced to EEA on test devices
./gradlew testDebugUnitTest     # ads control parser + interstitial pacing tests
```

**Your test device**: run once, then copy the hashed id from logcat
(`UserMessagingPlatform ... addTestDeviceHashedId("…")`) into `TEST_DEVICE_IDS` in
`MainApplication.kt`. Test devices get test ads and, in debug, the EEA consent form.

**Release**: copy `admob.properties.example` to `admob.properties` (git-ignored) and fill in your
App ID and ad unit IDs. Any missing key falls back to Google's test ID and Gradle prints a warning.

**Firebase (optional)**: drop `google-services.json` into `app/`; the Google Services and
Crashlytics plugins are applied only when the file exists. Remote Config then drives the ads
control (section 3) and paid impressions are logged to Analytics as `ad_paid` (see
`FirebaseAdRevenue`).

## 3. The ads control (JSON)

Everything about *which* ad shows *where* lives in one JSON document. The default is in
`app/.../ads/LocalAdsControl.kt`; Firebase Remote Config can replace it without an app update:

1. Add `app/google-services.json` (Firebase project with Remote Config enabled).
2. In the Firebase console, create the Remote Config parameter **`ads_config`** (type JSON or
   String) and paste the JSON below as its value. Publish.

`RemoteAdsControl` (`app/.../utilities/firebase/`) does the rest:

- `Application.onCreate()` applies the local JSON, then the value activated on the previous launch
  (cached by Firebase), then fetches a new one (debug: every launch; release: at most hourly).
- The splash waits for the fetch up to 4 s (in parallel with consent) before choosing its ads, so a
  published change works from the next launch screen. If the fetch is slow or fails, the cached or
  local JSON is used.
- Without `google-services.json` nothing is fetched and the local JSON is used.

```kotlin
AdsControlStore.update(LocalAdsControl.JSON, source = "local")  // MainApplication
RemoteAdsControl.fetch(this, BuildConfig.DEBUG)
```

Every field is optional. A missing or invalid value keeps its default and is logged as a warning,
so a typo in Remote Config never breaks the app.

```json
{
  "ads_enabled": true,
  "app_open_resume": true,

  "splash": {
    "fullscreen": "app_open",
    "timeout_sec": 25,
    "bottom_first_ms": 2000,
    "bottom": { "type": "native", "style": "medium" }
  },

  "language": { "bottom": { "type": "native", "style": "medium" } },

  "onboarding": {
    "mode": "per_page",
    "pages": [
      { "type": "native", "style": "large" },
      { "type": "banner", "style": "medium_rectangle" },
      { "type": "native", "style": "medium" },
      { "type": "native", "style": "small" }
    ],
    "shared": { "type": "native", "style": "medium" },
    "get_started_inter": true
  },

  "main": {
    "bottom": { "type": "banner", "style": "adaptive" },
    "tabs": {
      "home": { "type": "native", "style": "medium" },
      "explore": { "type": "banner", "style": "inline_adaptive" },
      "history": { "type": "native", "style": "small" },
      "settings": "off"
    },
    "inter": { "enabled": true, "every_nth": 3, "show_on_first_click": false, "min_interval_sec": 30 }
  },
  "cache": { "reuse_shown_sec": 30, "max_age_min": 50 }
}
```

| Field | Values | Default | Meaning |
|---|---|---|---|
| `ads_enabled` | `true` / `false` | `true` | Master switch. `false` turns every ad off at once. |
| `app_open_resume` | `true` / `false` | `true` | App Open ad (after "Welcome back") when the user returns to the app. |
| `splash.fullscreen` | `app_open` / `inter` / `off` | `app_open` | Full-screen ad at launch. Google recommends App Open for launch. |
| `splash.timeout_sec` | seconds | `25` | Longest the splash waits for its ads, counted after consent. |
| `splash.bottom_first_ms` | ms | `2000` | A loaded bottom ad stays visible at least this long before the full-screen ad. |
| `splash.bottom`, `language.bottom`, `main.bottom` | slot | see JSON | Native/banner/off at that position. |
| `onboarding.mode` | `per_page` / `shared` | `per_page` | Own ad per page (`pages`) or one ad for all pages (`shared`). |
| `onboarding.pages` | list of slots | 4 natives | One entry per page; **its length is the number of onboarding pages** (up to the 4 designed pages). |
| `onboarding.get_started_inter` | `true` / `false` | `true` | Interstitial on "Get Started". |
| `main.tabs.<tab>` | slot | off | Ad inside a tab: `home`, `explore`, `history`, `settings`. |
| `main.inter.enabled` | `true` / `false` | `true` | Interstitial on navigation inside Main. |
| `main.inter.every_nth` | ≥ 1 | `3` | Show on every *n*th click since the last interstitial. |
| `main.inter.show_on_first_click` | `true` / `false` | `false` | Also show on the very first click. |
| `main.inter.min_interval_sec` | seconds | `30` | Never closer than this to the previous full-screen ad of any kind (App Open included). |
| `cache.reuse_shown_sec` | seconds | `30` | A kept native/banner seen less than this ago is shown again with no request; seen longer ago, it shows while a new one loads and is swapped in. `0` = refresh on every return. |
| `cache.max_age_min` | minutes | `50` | A kept ad loaded longer ago than this is destroyed instead of shown. `0` = keep nothing. |

**Slot** = `{ "type": "native", "style": … }`, `{ "type": "banner", "style": … }` or `"off"`:

| Type | Styles |
|---|---|
| `native` | `small` (no media), `medium` (130 dp media), `large` (200 dp media, price/store) |
| `banner` | `standard` 320×50, `large` 320×100, `medium_rectangle` 300×250, `adaptive` (anchored, Google's large size ≈ 113 dp tall), `inline_adaptive` (in content, ≤ 250 dp), `collapsible_top`, `collapsible_bottom` |

## 4. Ad units and placements

A **placement** is one place an ad can appear; each has its own AdMob ad unit, so revenue is
reported per place. Placements are declared in `app/.../ads/AppAdPlacements.kt`, IDs come from
`app/build.gradle.kts` (test) and `admob.properties` (release):

| Placement | Format | `admob.properties` key |
|---|---|---|
| `app_open_splash` | App Open at launch | `admob_app_open_splash_id` |
| `app_open_resume` | App Open on returning to the app | `admob_app_open_resume_id` |
| `inter_splash` | Interstitial | `admob_inter_splash_id` |
| `inter_on_boarding` | Interstitial | `admob_inter_on_boarding_id` |
| `inter_main` | Interstitial | `admob_inter_main_id` |
| `rewarded_ai_feature` | Rewarded | `admob_rewarded_ai_feature_id` |
| `rewarded_inter_ai_feature` | Rewarded interstitial | `admob_rewarded_inter_ai_feature_id` |
| `native_splash` / `banner_splash` | Native / Banner | `admob_native_splash_id` / `admob_banner_splash_id` |
| `native_language` / `banner_language` | Native / Banner | `admob_native_language_id` / `admob_banner_language_id` |
| `native_on_boarding` / `banner_on_boarding` | Native / Banner (onboarding, shared mode) | `admob_native_on_boarding_id` / `admob_banner_on_boarding_id` |
| `native_ob1`…`native_ob4` / `banner_ob1`…`banner_ob4` | Native / Banner (one pair per onboarding page) | `admob_native_ob1_id`… / `admob_banner_ob1_id`… |
| `native_main` / `banner_main` | Native / Banner | `admob_native_main_id` / `admob_banner_main_id` |
| `native_home` / `banner_home`, `native_explore`…, `native_history`…, `native_settings`… | Native / Banner (one pair per main tab) | `admob_native_home_id` / `admob_banner_home_id`, … |
| `native_feature` | Native (Feature screen) | `admob_native_feature_id` |
| `native_activity_one` / `banner_activity_one`, `inter_activity_one` | Activities ads test, screen 1 | `admob_native_activity_one_id` / `admob_banner_activity_one_id`, `admob_inter_activity_one_id` |
| `native_activity_two` / `banner_activity_two`, `rewarded_activity_two` | Activities ads test, screen 2 | `admob_native_activity_two_id` / `admob_banner_activity_two_id`, `admob_rewarded_activity_two_id` |

Each **slot** (splash, language, each onboarding page, main, tab) has a native *and* a banner placement
because the ads control can switch its type at runtime (`AppAdSlot`). The ad cache is per placement,
so every screen that has its own placement also has its own cache.

## 5. Using `:ads` in a new app

1. Add the module, either way:
   - **From JitPack** (no copying). In `settings.gradle.kts` → `dependencyResolutionManagement.repositories`
     add `maven("https://jitpack.io")`, then in the app:

     ```kotlin
     implementation("com.github.rasool1947:admob-nextgen-android-reference:0.0.2")
     ```
     A new release = push a new git tag (e.g. `0.0.3`); JitPack builds it on first request
     (`jitpack.yml`). Status: https://jitpack.io/#rasool1947/admob-nextgen-android-reference
   - **As source**: copy the `ads/` folder, add `include(":ads")` to `settings.gradle.kts`, then
     `implementation(project(":ads"))` in the app, and copy the `[versions]`/`[libraries]` entries it
     uses from `gradle/libs.versions.toml`.

   Either way, also copy the `play-services-ads` exclusion from `app/build.gradle.kts` (the legacy SDK
   must never be on the classpath next to the Next-Gen one).
2. Manifest: `com.google.android.gms.ads.APPLICATION_ID` meta-data (UMP still reads it there).
3. `Application.onCreate()`:

   ```kotlin
   AdsSdk.configure(this, AdsConfig(
       appId = getString(R.string.admob_app_id),
       placements = AppAdPlacements.create(this, prefs),
       isPremium = { prefs.isAppPurchased },          // true = no ads at all
       testDeviceIds = listOf("YOUR_HASHED_ID"),
       isDebug = BuildConfig.DEBUG,
       onAdPaid = { revenue -> /* analytics */ },
   ))
   AdsControlStore.update(LocalAdsControl.JSON, source = "local")
   ```
4. Launcher activity `onCreate`: `AdsSdk.gatherConsent(this)`. The splash waits for the result
   with `AdsSdk.gatherConsent(activity) { canLoadAds -> … }`. Show a "Privacy settings" entry
   when `AdsSdk.isPrivacyOptionsRequired` (`AdsSdk.showPrivacyOptionsForm(activity)`).
5. Put an `AdSlotView` wherever a native/banner may go and load it with its slot from the control.
6. Use `FullScreenAds` for interstitial/rewarded/App Open, `InterstitialPacer` for navigation
   interstitials, and `AppOpenOnResume.enable("app_open_resume")` once the splash is done.

The sample screens (`app/.../app/`) show each step in a real flow; `AdPreloadChain` and
`MainInterstitial` are the app-side glue worth copying.

**Activity-based apps** work the same way (sample: Settings → "Activities ads test", debug builds;
`app/.../app/activities/`):

- pass the Activity itself as the lifecycle owner: `adSlot.load(this, slot, appSlot)`;
- an Activity you come back to (back from another Activity) isn't recreated, so call
  `adSlot.onShownAgain()` from `onRestart()` to apply the `cache` rule (like a tab shown again);
- call `AdsSdk.gatherConsent(this)` in the launcher Activity (safe in every Activity: once per process).

## 6. `:ads` API cheat sheet

All calls are main-thread; all callbacks arrive on the main thread.

```kotlin
// Native / banner slot driven by the ads control (layout: <com.nextgen.ads.slot.AdSlotView .../>)
binding.adSlot.load(viewLifecycleOwner, AdsControlStore.current.language.bottom, "native_language", "banner_language")
AdSlotView.preload(slot, nativeKey, bannerKey)   // one screen early
AdSlotView.stopPreload(nativeKey, bannerKey)

// Full-screen formats (preloaded with the SDK preloaders, one ad buffered per placement)
FullScreenAds.preload("inter_main")
FullScreenAds.whenReady("app_open_splash", timeoutMillis) { isReady -> }
FullScreenAds.show(activity, "inter_main", listener)          // onAdFinished() always called once
FullScreenAds.showWithLoading(activity, "inter_main", listener) // 1 s "Loading ad…" first
FullScreenAds.stop("inter_on_boarding")

// Pacing (every nth click, first click, min interval since ANY full-screen ad)
val pacer = InterstitialPacer({ AdsControlStore.current.main.inter })
if (pacer.onClick()) /* show, then */ pacer.onShown()

// App Open on returning to the app (with "Welcome back"; follows app_open_resume)
AppOpenOnResume.enable("app_open_resume")
AppOpenOnResume.welcomeBackMillis = 1_000   // 0 = no welcome screen
AppOpenOnResume.skipNextResume()            // before opening share sheet, billing, …

// Lower level, without the ads control
NativeAds.loadInto(nativeAdTemplateView, viewLifecycleOwner, "native_feature")
BannerAds.load(container, viewLifecycleOwner, "banner_main", BannerSize.Anchored)
```

`NativeAdTemplateView` (`app:nativeTemplate="small|medium|large"`) can also be used directly in XML.

## 7. Project structure

```
ads/                      reusable library (namespace com.nextgen.ads)
├── AdsSdk.kt             configure, consent → init, privacy options, Ad Inspector
├── config/               AdsConfig, AdPlacement, AdRevenue
├── consent/              UMP consent manager
├── control/              AdsControl model, AdsControlParser (JSON), AdsControlStore
├── slot/                 AdSlotView (native/banner/off + shimmer + preload)
├── nativead/             NativeAds (preloader), NativeAdTemplateView (small/medium/large)
├── banner/               BannerAds (preloader), BannerSize
├── fullscreen/           FullScreenAds, AppOpenOnResume, InterstitialPacer, loading dialogs
└── internal/             logging, main-thread dispatch, shimmer

app/
├── ads/                  AppAdPlacements, AppAdSlot, LocalAdsControl (JSON), AdPreloadChain, MainInterstitial
├── app/splash|language|onBoarding|main|feature|premium/   screens
├── utilities/            base classes, language (AppLanguage), prefs, Firebase (Remote Config, revenue)
└── res/values{,-ur,-ar}/ strings in English, Urdu, Arabic
```

## 8. Reading the ad logs

Debug builds log every ad event as one line under the logcat tag **`AdsFlow`**: screen, placement,
format, event. Loaded ads are logged as warnings (yellow in Android Studio), failures as errors (red),
the rest as debug.
Filter Logcat by `tag:AdsFlow` to follow each ad:

```
Language      │ native_language           │ Native        │ 📦 PRELOADING
Splash        │ native_splash             │ Native        │ ⏳ LOADING
Splash        │ app_open_splash           │ App Open      │ 📦 PRELOADING
Splash        │ native_splash             │ Native        │ ✅ LOADED
Language      │ native_language           │ Native        │ 📦 READY in cache
Splash        │ native_splash             │ Native        │ 👁 IMPRESSION
Splash        │ app_open_splash           │ App Open      │ ✅ LOADED (in cache)
Language      │ native_language           │ Native        │ ♻️ LOADED from cache
```

| Event | Meaning |
|---|---|
| 📦 PRELOADING | Loading in the background for a later screen (the SDK preloader is the ad cache). |
| 📦 READY in cache | A preloaded ad is waiting and will show instantly (logged once; the SDK's silent refills after each use are not). |
| ✅ LOADED (in cache) | Full-screen ads (App Open, interstitial, rewarded) always load through the cache: this is their "loaded". |
| ⏳ LOADING / ✅ LOADED | Requested for the screen now (nothing in the cache) / arrived. |
| ♻️ LOADED from cache | Taken from the cache, no waiting. |
| ♻️ SHOWN again (kept) | The ad this screen showed before (kept when the screen closed): no new request. Detail says when it was last seen. |
| 🔄 SHOWN again + REFRESHING | The kept ad was seen longer than `cache.reuse_shown_sec` ago: it shows while a new one loads. |
| ✅ LOADED → swapped in | The refreshed ad arrived and replaced the kept one (a fresh impression follows). |
| ❌ FAILED | Load, preload or show failed; the AdMob error code and message follow. |
| ⛔ SKIPPED | Not requested: slot off in the ads control, no consent, premium user, placement disabled. |
| 🚫 NOT SHOWN | A full-screen ad was due but couldn't show (none ready, another ad on screen, …). |
| 👁 IMPRESSION, 👆 CLICKED, ✖️ CLOSED, 🎁 REWARD earned | What the user saw and did (a banner logs only its first impression, not every refresh). |

The screen names come from `AdPlacement(screen = …)` in `AppAdPlacements` (each onboarding page has
its own placements: OB1, OB2, …). The tag `NextGenAds`
keeps the detailed technical log (consent, SDK init, preload buffers) for debugging the module.

## 9. Notes and gotchas

- **Policy**: Google prefers App Open over an interstitial at launch; keep ads away from
  navigation and buttons (the layouts keep an 8 dp gap); rewarded interstitials need an intro with
  an opt-out (see the Premium screen).
- **Native ad validator**: on test devices the SDK shows an "AdMob native ad validator" popup next
  to native ads. Turn it off with `AdsConfig(nativeAdValidatorEnabled = false)`.
- **Release builds / R8**: the Ads SDK pulls WorkManager 2.7.0, whose database R8 strips (crash on
  launch). `:ads` pins a newer WorkManager; keep that line when updating.
- **Legacy SDK**: `play-services-ads` must not be on the classpath with the Next-Gen SDK; both
  modules exclude it.
- **Threads**: the Next-Gen SDK calls back on background threads; `:ads` moves every callback to
  the main thread before it reaches your code.
- **Consent first**: no ad request is made before UMP says ads may be requested; the SDK is
  initialized right after.

## License

MIT. Free to use, modify and distribute.
