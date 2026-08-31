package com.example.data

import java.text.Normalizer

/** Utilities for safe, consistent Khowar search/indexing. */
object KhowarNormalizer {
    /**
     * Normalizes Khowar text for indexing and search.
     * Keep the original text in the dataset; this function is not a replacement for it.
     */
    fun normalizeKhowarText(input: String): String {
        if (input.isBlank()) return ""

        var text = Normalizer.normalize(input.trim(), Normalizer.Form.NFC)
            .replace('\u2018', '\u0027')
            .replace('\u2019', '\u0027')
            .replace('\u201C', '\u0022')
            .replace('\u201D', '\u0022')

        // Common Arabic/Perso-Arabic variants used when typing Khowar.
        text = text
            .replace('\u064A', '\u06CC') // Arabic Yeh -> Farsi Yeh
            .replace('\u0649', '\u06CC') // Alef Maksura -> Yeh
            .replace('\u06D2', '\u06CC') // Yeh Barree -> Yeh
            .replace('\u0643', '\u06A9') // Arabic Kaf -> Keheh
            .replace('\u0629', '\u06C1') // Teh Marbuta -> Goal Heh
            .replace('\u0647', '\u06C1') // Arabic Heh -> Goal Heh
            .replace('\u06BE', '\u06C1') // Do-Chashmi Heh -> Goal Heh
            .replace('\u0624', '\u0648') // Waw with Hamza -> Waw

        // Arabic diacritics/tashkeel.
        text = text.replace(Regex("[\\u064B-\\u0652\\u0670\\u06DF-\\u06E8\\u06EA-\\u06ED]"), "")

        // Zero-width and bidirectional control characters should not affect search.
        text = text
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace("\u200E", "")
            .replace("\u200F", "")
            .replace("\u202A", "")
            .replace("\u202B", "")
            .replace("\u202C", "")
            .replace("\u202D", "")
            .replace("\u202E", "")
            .replace("\u2066", "")
            .replace("\u2067", "")
            .replace("\u2068", "")
            .replace("\u2069", "")

        return text.replace(Regex("\\s+"), " ").trim()
    }

    /** Normalizes Latin transliteration for consistent search. */
    fun normalizeTransliteration(input: String): String =
        Normalizer.normalize(input.trim(), Normalizer.Form.NFKC)
            .lowercase()
            .replace('\u2018', '\u0027')
            .replace('\u2019', '\u0027')
            .replace(Regex("[^a-z0-9'\\s-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
