package com.example.admob_next_gen.app.onBoarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.example.admob_next_gen.R
import com.example.admob_next_gen.databinding.ItemOnboardingPageBinding

data class OnBoardingPage(val illustration: String, @StringRes val title: Int, @StringRes val description: Int) {
    companion object {
        /** Content of every page; the ads control decides how many are shown (`onboarding.pages`). */
        val all = listOf(
            OnBoardingPage("🚀", R.string.ob_title_1, R.string.ob_desc_1),
            OnBoardingPage("🧩", R.string.ob_title_2, R.string.ob_desc_2),
            OnBoardingPage("🎬", R.string.ob_title_3, R.string.ob_desc_3),
            OnBoardingPage("✅", R.string.ob_title_4, R.string.ob_desc_4),
        )
    }
}

class AdapterOnBoarding(private val pages: List<OnBoardingPage>) : RecyclerView.Adapter<AdapterOnBoarding.ViewHolder>() {

    class ViewHolder(val binding: ItemOnboardingPageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = pages.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = pages[position]
        with(holder.binding) {
            mtvIllustrationPage.text = page.illustration
            mtvTitlePage.setText(page.title)
            mtvDescPage.setText(page.description)
        }
    }
}
