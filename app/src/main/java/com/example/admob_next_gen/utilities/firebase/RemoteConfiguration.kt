//package com.example.admob_next_gen.utilities.firebase
//
//import android.util.Log
//import com.google.firebase.Firebase
//import com.google.firebase.remoteconfig.ConfigUpdate
//import com.google.firebase.remoteconfig.ConfigUpdateListener
//import com.google.firebase.remoteconfig.FirebaseRemoteConfig
//import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
//import com.google.firebase.remoteconfig.remoteConfig
//import com.google.firebase.remoteconfig.remoteConfigSettings
//import com.example.admob_next_gen.utilities.firebase.FirebaseUtils.recordException
//import com.example.admob_next_gen.utilities.manager.InternetManager
//import com.example.admob_next_gen.utilities.manager.SharedPreferenceUtils
//import com.example.admob_next_gen.utilities.utils.Constants.TAG
//import com.example.admob_next_gen.utilities.utils.Constants.TAG_REMOTE
//import com.google.firebase.remoteconfig.get
//
//
//class RemoteConfiguration(private val internetManager: InternetManager, private val sharedPreferenceUtils: SharedPreferenceUtils) {
//
//    private val remoteConfig: FirebaseRemoteConfig by lazy { Firebase.remoteConfig }
//
//    private fun initSettings() {
//        val configSettings = remoteConfigSettings {
//            fetchTimeoutInSeconds = 10
//            minimumFetchIntervalInSeconds = 0L
//        }
//        remoteConfig.setConfigSettingsAsync(configSettings)
//    }
//
//    fun checkRemoteConfig(fetchCallback: (Boolean) -> Unit) {
//        try {
//            initSettings()
//        } catch (e: Exception) {
//            Log.e(TAG_REMOTE, "RemoteConfiguration: Firebase not initialized, skipping remote config")
//            fetchCallback.invoke(true)
//            return
//        }
//
//        var callback: ((Boolean) -> Unit)? = fetchCallback
//        if (!internetManager.isInternetConnected) {
//            Log.e(TAG_REMOTE, "RemoteConfiguration: checkRemoteConfig: No Internet Found")
//            fetchCallback.invoke(false)
//            callback = null
//        }
//
//        try {
//            addLiveUpdateListener()
//            fetchRemoteValues(callback)
//        } catch (e: Exception) {
//            Log.e(TAG_REMOTE, "RemoteConfiguration: Firebase error: ${e.message}")
//            fetchCallback.invoke(true)
//        }
//    }
//
//    private fun addLiveUpdateListener() {
//        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
//            override fun onUpdate(configUpdate: ConfigUpdate) {
//                Log.d(TAG_REMOTE, "RemoteConfiguration: addLiveUpdateListener: onUpdate: Fetched Successfully")
//                remoteConfig.activate().addOnSuccessListener { isSuccessful ->
//                    if (isSuccessful) updateRemoteValues()
//                }
//            }
//
//            override fun onError(error: FirebaseRemoteConfigException) {
//                Log.e(TAG_REMOTE, "RemoteConfiguration: addLiveUpdateListener: Error: ", error)
//            }
//        })
//    }
//
//    private fun fetchRemoteValues(callback: ((Boolean) -> Unit)?) {
//        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
//            when (task.isSuccessful) {
//                true -> updateRemoteValues()
//                false -> Log.e(TAG_REMOTE, "RemoteConfiguration: fetchRemoteValues: Failure: ", task.exception)
//            }
//            callback?.invoke(task.isSuccessful)
//        }
//    }
//
//    private fun updateRemoteValues() {
//        // Save this value anywhere
//        sharedPreferenceUtils.apply {
//            try {
//                rcAppOpen = remoteConfig[appOpen].asLong().toInt()
//                rcAppOpenSplash = remoteConfig[appOpenSplash].asLong().toInt()
//
//                rcBannerHome = remoteConfig[bannerHome].asLong().toInt()
//
//                rcInterOnBoarding = remoteConfig[interOnBoarding].asLong().toInt()
//                rcInterFeature = remoteConfig[interFeature].asLong().toInt()
//
//                rcRewardedAiFeature = remoteConfig[rewardedAiFeature].asLong().toInt()
//                rcRewardedInterAiFeature = remoteConfig[rewardedInterAiFeature].asLong().toInt()
//
//                rcNativeLanguage = remoteConfig[nativeLanguage].asLong().toInt()
//                rcNativeOnBoarding = remoteConfig[nativeOnBoarding].asLong().toInt()
//                rcNativeHome = remoteConfig[nativeHome].asLong().toInt()
//                rcNativeFeature = remoteConfig[nativeFeature].asLong().toInt()
//                rcNativeExit = remoteConfig[nativeExit].asLong().toInt()
//
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcAppOpen -> ${remoteConfig[appOpen].asLong().toInt()}")
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcAppOpenSplash -> ${remoteConfig[appOpenSplash].asLong().toInt()}")
//
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcBannerHome -> ${remoteConfig[bannerHome].asLong().toInt()}")
//
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcInterOnBoarding -> ${remoteConfig[interOnBoarding].asLong().toInt()}")
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcInterFeature -> ${remoteConfig[interFeature].asLong().toInt()}")
//
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcRewardedAiFeature -> ${remoteConfig[rewardedAiFeature].asLong().toInt()}")
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcRewardedInterAiFeature -> ${remoteConfig[rewardedInterAiFeature].asLong().toInt()}")
//
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcNativeLanguage -> ${remoteConfig[nativeLanguage].asLong().toInt()}")
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcNativeOnBoarding -> ${remoteConfig[nativeOnBoarding].asLong().toInt()}")
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcNativeHome -> ${remoteConfig[nativeHome].asLong().toInt()}")
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcNativeFeature -> ${remoteConfig[nativeFeature].asLong().toInt()}")
//                Log.i(TAG_REMOTE, "RemoteConfiguration: rcNativeExit -> ${remoteConfig[nativeExit].asLong().toInt()}")
//            } catch (ex: Exception) {
//                ex.recordException("RemoteConfiguration: updateRemoteValues")
//            }
//        }
//        Log.d(TAG, "RemoteConfiguration: updateRemoteValues: Fetched Successfully")
//    }
//}