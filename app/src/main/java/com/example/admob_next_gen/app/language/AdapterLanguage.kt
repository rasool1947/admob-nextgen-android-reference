package com.example.admob_next_gen.app.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.admob_next_gen.databinding.ItemLanguageBinding
import com.example.admob_next_gen.utilities.language.AppLanguage
import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors

class AdapterLanguage(
    private val languages: List<AppLanguage>,
    selectedCode: String,
    private val onSelected: (AppLanguage) -> Unit,
) : RecyclerView.Adapter<AdapterLanguage.ViewHolder>() {

    var selectedCode: String = selectedCode
        private set

    class ViewHolder(val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = languages.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val language = languages[position]
        val isSelected = language.code == selectedCode
        with(holder.binding) {
            mtvFlagLanguage.text = language.flag
            mtvNativeNameLanguage.text = language.nativeName
            mtvEnglishNameLanguage.text = language.englishName
            rbLanguage.isChecked = isSelected

            val strokeColor = if (isSelected) AppCompatR.attr.colorPrimary else MaterialR.attr.colorOutlineVariant
            cardLanguage.strokeColor = MaterialColors.getColor(cardLanguage, strokeColor)
            val density = root.resources.displayMetrics.density
            cardLanguage.strokeWidth = (density * if (isSelected) 2 else 1).toInt()
            cardLanguage.setOnClickListener { select(holder.bindingAdapterPosition) }
        }
    }

    private fun select(position: Int) {
        val language = languages.getOrNull(position) ?: return
        if (language.code == selectedCode) return
        val previous = languages.indexOfFirst { it.code == selectedCode }
        selectedCode = language.code
        if (previous >= 0) notifyItemChanged(previous)
        notifyItemChanged(position)
        onSelected(language)
    }
}
