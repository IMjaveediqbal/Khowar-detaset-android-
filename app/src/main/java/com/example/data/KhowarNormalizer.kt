package com.example.data

object KhowarNormalizer {
    /**
     * Normalizes Khowar Perso-Arabic text:
     * - Unifies Arabic/Perso-Arabic letters (Yeh, Kaf, Heh, Teh Marbuta, etc.)
     * - Removes optional Arabic diacritics/tashkeel (Fatha, Damma, Kasra, Sukun, Shadda)
     * - Removes zero-width characters
     * - Collapses multiple spaces
     */
    fun normalizeKhowarText(input: String): String {
        if (input.isBlank()) return ""
        var text = input.trim()

        // Normalize Yeh variants
        text = text.replace('\u064A', '\u06CC') // Arabic Yeh to Farsi/Urdu Yeh
        text = text.replace('\u0649', '\u06CC') // Alef Maksura to Yeh
        text = text.replace('\u06D2', '\u06CC') // Yeh Barree to standard Yeh for indexing
        text = text.replace('\u0626', '\u06CC') // Yeh with Hamza

        // Normalize Kaf variants
        text = text.replace('\u0643', '\u06A9') // Arabic Kaf to Keheh

        // Normalize Heh variants
        text = text.replace('\u0629', '\u06C1') // Teh Marbuta to Goal Heh
        text = text.replace('\u0647', '\u06C1') // Arabic Heh to Goal Heh
        text = text.replace('\u06BE', '\u06C1') // Do-Chashmi Heh normalized for indexing

        // Normalize Waw variants
        text = text.replace('\u0624', '\u0648') // Waw with Hamza to Waw

        // Remove Arabic diacritics / Tashkeel
        val diacriticsRegex = Regex("[\u064B-\u0652\u0670\u06DF-\u06E8\u06EA-\u06ED]")
        text = text.replace(diacriticsRegex, "")

        // Remove Zero Width characters
        text = text.replace("\u200C", "").replace("\u200D", "").replace("\u200E", "").replace("\u200F", "")

        // Collapse whitespace
        text = text.replace(Regex("\\s+"), " ")

        return text.trim()
    }

    /**
     * Normalizes Latin transliteration for fuzzy search matching
     */
    fun normalizeTransliteration(input: String): String {
        return input.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Rule-based heuristic transliterator for common Khowar phonetic representations
     */
    fun generateTransliterationHint(khowarArabic: String): String {
        val mapping = mapOf(
            'ا' to "a", 'ب' to "b", 'پ' to "p", 'ت' to "t", 'ٹ' to "t'",
            'ث' to "s", 'ج' to "j", 'چ' to "ch", 'څ' to "ts", 'ځ' to "dz",
            'ح' to "h", 'خ' to "kh", 'د' to "d", 'ڈ' to "d'", 'ذ' to "z",
            'ر' to "r", 'ڑ' to "r'", 'ز' to "z", 'ژ' to "zh", 'ڙ' to "z'",
            'س' to "s", 'ش' to "sh", 'ݰ' to "sh'", 'ص' to "s", 'ض' to "z",
            'ط' to "t", 'ظ' to "z", 'ع' to "a", 'غ' to "gh", 'ف' to "f",
            'ق' to "q", 'ک' to "k", 'گ' to "g", 'ل' to "l", 'م' to "m",
            'ن' to "n", 'ں' to "n", 'و' to "w", 'ہ' to "h", 'ھ' to "h",
            'ی' to "y", 'ے' to "e"
        )
        val sb = StringBuilder()
        for (char in khowarArabic) {
            val mapped = mapping[char]
            if (mapped != null) {
                sb.append(mapped)
            } else if (char.isWhitespace()) {
                sb.append(" ")
            }
        }
        return sb.toString().trim()
    }
}
