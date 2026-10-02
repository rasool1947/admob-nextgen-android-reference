package com.example.admob_next_gen.utilities.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** One entry of the language picker. The app is translated into en, ur and ar; others fall back to English. */
data class AppLanguage(val code: String, val nativeName: String, val englishName: String, val flag: String) {

    companion object {
        val all = listOf(
            AppLanguage("en", "English", "English", "🇺🇸"),
            AppLanguage("ur", "اردو", "Urdu", "🇵🇰"),
            AppLanguage("ar", "العربية", "Arabic", "🇸🇦"),
            AppLanguage("hi", "हिन्दी", "Hindi", "🇮🇳"),
            AppLanguage("es", "Español", "Spanish", "🇪🇸"),
            AppLanguage("fr", "Français", "French", "🇫🇷"),
            AppLanguage("de", "Deutsch", "German", "🇩🇪"),
            AppLanguage("tr", "Türkçe", "Turkish", "🇹🇷"),
            AppLanguage("pt", "Português", "Portuguese", "🇧🇷"),
            AppLanguage("id", "Bahasa Indonesia", "Indonesian", "🇮🇩"),
        )

        /** Language the app runs in now: the one picked in-app, else the phone's, else English. */
        fun current(): AppLanguage {
            val tag = AppCompatDelegate.getApplicationLocales()[0]?.language ?: Locale.getDefault().language
            return all.firstOrNull { it.code == normalize(tag) } ?: all.first()
        }

        /** Switches the app language; AppCompat recreates the activity and remembers the choice. */
        fun apply(language: AppLanguage) {
            if (AppCompatDelegate.getApplicationLocales()[0]?.language?.let(::normalize) == language.code) return
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.code))
        }

        // Older Java reports Indonesian as the legacy code "in".
        private fun normalize(tag: String) = if (tag == "in") "id" else tag
    }
}
