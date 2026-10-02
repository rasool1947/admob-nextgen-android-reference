package com.example.admob_next_gen.app.main.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.admob_next_gen.R
import com.example.admob_next_gen.ads.AppAdSlot
import com.example.admob_next_gen.ads.MainInterstitial
import com.example.admob_next_gen.ads.MainTabKeys
import com.example.admob_next_gen.ads.load
import com.example.admob_next_gen.databinding.FragmentExploreBinding
import com.example.admob_next_gen.databinding.ItemExploreBinding
import com.example.admob_next_gen.utilities.base.fragments.BaseFragment
import com.example.admob_next_gen.utilities.extensions.navigateTo
import com.nextgen.ads.control.AdsControlStore

/** Explore tab: a sample content list. */
class FragmentExplore : BaseFragment<FragmentExploreBinding>(FragmentExploreBinding::inflate) {

    override fun onViewCreated() {
        binding.adSlotExplore.load(viewLifecycleOwner, AdsControlStore.current.main.tab(MainTabKeys.EXPLORE), AppAdSlot.EXPLORE_TAB)

        val titles = resources.getStringArray(R.array.explore_titles)
        val descriptions = resources.getStringArray(R.array.explore_descriptions)
        val items = titles.indices.map { i -> Item(ICONS[i % ICONS.size], titles[i], descriptions[i]) }
        binding.rvExplore.adapter = AdapterExplore(items) { openItem() }
    }

    private data class Item(val icon: String, val title: String, val description: String)

    /** Main-screen navigation: the ads control decides whether an interstitial comes first. */
    private fun openItem() {
        MainInterstitial.showThen(requireActivity()) { navigateTo(R.id.fragmentMain, R.id.action_fragmentMain_to_fragmentFeature) }
    }

    private class AdapterExplore(
        private val items: List<Item>,
        private val onClick: (Item) -> Unit,
    ) : RecyclerView.Adapter<AdapterExplore.ViewHolder>() {

        class ViewHolder(val binding: ItemExploreBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemExploreBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.mtvIconExplore.text = item.icon
            holder.binding.mtvTitleExplore.text = item.title
            holder.binding.mtvDescExplore.text = item.description
            holder.binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        val ICONS = listOf("📷", "📄", "🎙️", "🔳", "📏", "🖼️")
    }
}
