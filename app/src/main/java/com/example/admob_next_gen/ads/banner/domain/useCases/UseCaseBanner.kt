package com.example.admob_next_gen.ads.banner.domain.useCases

import android.content.Context
import android.util.Log
import  com.example.admob_next_gen.R
 import com.google.android.libraries.ads.mobile.sdk.banner.AdView
 import com.example.admob_next_gen.ads.banner.data.entities.ItemBannerAd
import com.example.admob_next_gen.ads.banner.data.repositories.RepositoryBannerImpl
import com.example.admob_next_gen.utilities.manager.InternetManager
import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
import com.example.admob_next_gen.utilities.utils.Constants.TAG_ADS
import com.nextgen.ads.AdsSdk
import com.example.admob_next_gen.ads.banner.presentation.enums.BannerAdKey
import com.example.admob_next_gen.ads.banner.presentation.enums.BannerAdType


/**
 * Date: 1/17/2025
 *
 */

class UseCaseBanner(
    private val repositoryBannerImpl: RepositoryBannerImpl,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager,
    private val context: Context
) {

    @Volatile
    private var loadingKeys = mutableSetOf<String>() // per-key loading flag

    private fun checkRemoteConfig(bannerAdKey: BannerAdKey): Boolean {
        val value = when (bannerAdKey) {
            BannerAdKey.SPLASH -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.LANGUAGE -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.REGION -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.ON_BOARDING -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.HOME_TAB -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.COLLECTION -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.VIP -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.SEE_ALL -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.AVATAR -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.CALL_BUTTON -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.CONTACT_LIST -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.ADD_CONTACT -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.CONTACT_DETAIL -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.APPLY_THEME -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.THEME_SUCCESS -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.SETTING -> sharedPreferenceUtils.rcBannerHome
            BannerAdKey.DIALER -> sharedPreferenceUtils.rcBannerHome
        }

        Log.e("TAG_AD_CHECK", "$bannerAdKey remote value = $value")

        return value != 0
    }

    private fun getAdId(bannerAdKey: BannerAdKey): String {
        return when (bannerAdKey) {
            BannerAdKey.SPLASH        -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.LANGUAGE      -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.REGION        -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.ON_BOARDING   -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.HOME_TAB      -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.COLLECTION    -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.VIP           -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.SEE_ALL       -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.AVATAR        -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.CALL_BUTTON   -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.CONTACT_LIST  -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.ADD_CONTACT   -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.CONTACT_DETAIL -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.APPLY_THEME    -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.THEME_SUCCESS  -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.DIALER        -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.SETTING       -> context.getString(R.string.admob_banner_home_id).trim()
        }
    }

    private fun getAdType(bannerAdKey: BannerAdKey): BannerAdType {
        val rcValue = when (bannerAdKey) {
            BannerAdKey.SPLASH        ->4// sharedPreferenceUtils.rcInlineBannerTypeSplash
            BannerAdKey.LANGUAGE      -> 4//sharedPreferenceUtils.rcInlineBannerTypeLanguage
            BannerAdKey.REGION        -> 4//sharedPreferenceUtils.rcInlineBannerTypeRegion
            BannerAdKey.ON_BOARDING   -> 4//sharedPreferenceUtils.rcInlineBannerTypeOnBoarding
            BannerAdKey.HOME_TAB      -> 4//sharedPreferenceUtils.rcInlineBannerTypeHomeTab
            BannerAdKey.COLLECTION    -> 4//sharedPreferenceUtils.rcInlineBannerTypeCollection
            BannerAdKey.VIP           -> 4//sharedPreferenceUtils.rcInlineBannerTypeVip
            BannerAdKey.SEE_ALL       -> 4//sharedPreferenceUtils.rcInlineBannerTypeSeeAll
            BannerAdKey.AVATAR        -> 4//sharedPreferenceUtils.rcInlineBannerTypeAvatar
            BannerAdKey.CALL_BUTTON   -> 4//sharedPreferenceUtils.rcInlineBannerTypeCallButton
            BannerAdKey.CONTACT_LIST  -> 4//sharedPreferenceUtils.rcInlineBannerTypeContactList
            BannerAdKey.ADD_CONTACT   ->  4//sharedPreferenceUtils.rcInlineBannerTypeAddContact
            BannerAdKey.CONTACT_DETAIL -> 4//sharedPreferenceUtils.rcInlineBannerTypeContactDetail
            BannerAdKey.APPLY_THEME    -> 4//sharedPreferenceUtils.rcInlineBannerTypeApplyTheme
            BannerAdKey.THEME_SUCCESS  -> 4//sharedPreferenceUtils.rcInlineBannerTypeThemeSuccess
            BannerAdKey.DIALER        ->  4//sharedPreferenceUtils.rcInlineBannerTypeDIALER
            BannerAdKey.SETTING       -> 4//sharedPreferenceUtils.rcInlineBannerTypeSetting
        }
        return BannerAdType.fromInt(rcValue)
    }

    /**
     * Build the per-consumer cache/loading identity. Remote config, ad id and ad type
     * are still resolved from [bannerAdKey], but the cache entry and the "already
     * loading" guard are keyed by [cacheKey]. Screens that render the same key on
     * multiple simultaneously-alive views (e.g. ViewPager pages) pass a unique
     * [instanceTag] so they each get their own AdView instead of fighting over one.
     */
    private fun cacheKey(bannerAdKey: BannerAdKey, instanceTag: String?): String =
        if (instanceTag.isNullOrEmpty()) bannerAdKey.value else "${bannerAdKey.value}#$instanceTag"

    fun loadBannerAd(adView: AdView, bannerAdKey: BannerAdKey, instanceTag: String? = null, callback: (ItemBannerAd?) -> Unit) {
        val bannerAdType = getAdType(bannerAdKey)
        val key = cacheKey(bannerAdKey, instanceTag)
        Log.d(TAG_ADS, "  bannerAdType -> $bannerAdType ")
        validateAndLoadAd(bannerAdKey, key, callback) { adId ->
            loadingKeys.add(key)
            repositoryBannerImpl.fetchBannerAd(adKey = key, adId = adId, bannerAdType = bannerAdType, adView = adView) {
                loadingKeys.remove(key)
                callback.invoke(it)
            }
        }
    }

    private fun validateAndLoadAd(bannerAdKey: BannerAdKey, key: String, callback: (ItemBannerAd?) -> Unit, loadAdAction: (adId: String) -> Unit) {
        val isRemoteEnable = checkRemoteConfig(bannerAdKey)
        val adId = getAdId(bannerAdKey)

        when {
            AdsSdk.canLoadAds.not() -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Ads not allowed (no consent / SDK not initialized / premium)")
                callback.invoke(null)
            }

            sharedPreferenceUtils.isAppPurchased -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Premium user")
                callback.invoke(null)
            }

            isRemoteEnable.not() -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Remote config is off")
                callback.invoke(null)
            }

            internetManager.isInternetConnected.not() -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Internet is not connected")
                callback.invoke(null)
            }

            adId.isEmpty() -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Ad id is empty")
                callback.invoke(null)
            }

            loadingKeys.contains(key) -> {
                // A load for this exact key is already in flight; resolving with null
                // lets the caller's UI settle (hide shimmer) instead of hanging forever.
                Log.e(TAG_ADS, "$key -> loadBanner: Ad is already loading for this key")
                callback.invoke(null)
            }

            else -> {
                loadAdAction(adId)
            }
        }
    }

    fun destroyBanner(bannerAdKey: BannerAdKey, instanceTag: String? = null): Boolean {
        val key = cacheKey(bannerAdKey, instanceTag)
        val isDestroyed = repositoryBannerImpl.destroyBanner(key)
        if (isDestroyed)
            Log.e(TAG_ADS, "$key -> destroyBanner: destroyed")
        return isDestroyed
    }
}