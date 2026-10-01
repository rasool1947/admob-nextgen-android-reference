package com.example.admob_next_gen.app.premium

import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdPlacements
import com.example.admob_next_gen.databinding.FragmentPremiumBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.example.admob_next_gen.utilities.extensions.popFrom
import com.nextgen.ads.fullscreen.FullScreenAdListener
import com.nextgen.ads.fullscreen.FullScreenAds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Rewarded formats, both started only by an explicit tap:
 * - Rewarded: "Watch ad to unlock" -> opens the feature only if the reward was earned.
 * - Rewarded interstitial: Google requires an intro with a way to opt out before the ad starts,
 *   so a countdown dialog with "No thanks" comes first.
 */
class FragmentPremium : BaseFragment<FragmentPremiumBinding>(FragmentPremiumBinding::inflate) {

    override fun onViewCreated() {
        FullScreenAds.preload(AppAdPlacements.REWARDED_AI_FEATURE)
        FullScreenAds.preload(AppAdPlacements.REWARDED_INTER_AI_FEATURE)

        binding.mbBackPremium.setOnClickListener { popFrom(R.id.fragmentPremium) }
        binding.mbWatchRewardedPremium.setOnClickListener { showRewarded() }
        binding.mbBonusPremium.setOnClickListener { showBonusIntro() }
    }

    /* ----------------------------------------- Rewarded ----------------------------------------- */

    private fun showRewarded() {
        FullScreenAds.show(requireActivity(), AppAdPlacements.REWARDED_AI_FEATURE, rewardListener { _ ->
            navigateTo(R.id.fragmentPremium, R.id.action_fragmentPremium_to_fragmentFeature)
        })
    }

    /* ----------------------------------- Rewarded interstitial ----------------------------------- */

    private fun showBonusIntro() {
        if (!FullScreenAds.isReady(AppAdPlacements.REWARDED_INTER_AI_FEATURE)) {
            return toast(R.string.reward_ad_not_available)
        }

        var secondsLeft = INTRO_SECONDS
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.bonus_intro_title)
            .setMessage(getString(R.string.bonus_intro_message, secondsLeft))
            .setNegativeButton(R.string.bonus_intro_skip, null)
            .show()

        val countdown = viewLifecycleOwner.lifecycleScope.launch {
            while (secondsLeft > 0) {
                dialog.setMessage(getString(R.string.bonus_intro_message, secondsLeft))
                delay(1000)
                secondsLeft--
            }
            dialog.dismiss()
            showBonusAd()
        }
        // "No thanks", back or tapping outside cancels the ad.
        dialog.setOnDismissListener { if (secondsLeft > 0) countdown.cancel() }
    }

    private fun showBonusAd() {
        FullScreenAds.show(requireActivity(), AppAdPlacements.REWARDED_INTER_AI_FEATURE, rewardListener { reward ->
            binding.mtvBonusResultPremium.text = getString(R.string.premium_bonus_earned, reward.amount, reward.type)
            binding.mtvBonusResultPremium.isVisible = true
        })
    }

    /* ------------------------------------------- Shared ------------------------------------------- */

    /** Grants the reward after the ad closes, and only if it was earned. */
    private fun rewardListener(onRewardEarned: (RewardItem) -> Unit) = object : FullScreenAdListener {
        private var earnedReward: RewardItem? = null
        private var wasShown = false

        override fun onAdShowed() {
            wasShown = true
        }

        override fun onUserEarnedReward(reward: RewardItem) {
            earnedReward = reward
        }

        override fun onAdFinished() {
            if (view == null) return
            val reward = earnedReward
            when {
                reward != null -> onRewardEarned(reward)
                wasShown -> toast(R.string.reward_not_earned)
                else -> toast(R.string.reward_ad_not_available)
            }
        }
    }

    private fun toast(message: Int) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
    }

    private companion object {
        const val INTRO_SECONDS = 5
    }
}
