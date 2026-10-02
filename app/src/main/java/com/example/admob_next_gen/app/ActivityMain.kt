package com.example.admob_next_gen.app

import com.example.admob_next_gen.databinding.ActivityMainBinding
import com.example.admob_next_gen.utilities.base.activities.BaseActivity
import com.nextgen.ads.AdsSdk

class ActivityMain : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    override fun onCreated() {
        // Runs once per process. Here (not only on the splash) so ads also work when Android
        // restores the app directly on a later screen after killing it in the background.
        AdsSdk.gatherConsent(this)
    }
}
