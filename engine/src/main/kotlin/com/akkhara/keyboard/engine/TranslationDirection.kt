package com.akkhara.keyboard.engine

/** Which input language is translated into which. See GLOSSARY.md, "Translation direction". */
enum class TranslationDirection(val from: InputLanguage, val to: InputLanguage) {
    MyanmarToEnglish(InputLanguage.Myanmar, InputLanguage.English),
    EnglishToMyanmar(InputLanguage.English, InputLanguage.Myanmar),
}
