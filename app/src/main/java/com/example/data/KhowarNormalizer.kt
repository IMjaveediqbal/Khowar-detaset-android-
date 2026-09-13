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
            .replace('\u064A', '\u06CC')
            .replace('\u0649', '\u06CC')
            .replace('\u0643', '\u06A9')
            .replace('\u0629', '\u06C1')
            .replace('\u0647', '\u06C1')

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

    /**
     * Produces a deterministic best-effort Latin hint for Arabic/Perso-Arabic Khowar.
     * This is intentionally a hint, not an authoritative linguistic transliteration.
     */
    fun generateTransliterationHint(input: String): String {
        val normalized = normalizeKhowarText(input)
        if (normalized.isBlank()) return ""
        if (normalized.all { it.code < 128 && (it.isLetterOrDigit() || it.isWhitespace() || it in "' -") }) {
            return normalizeTransliteration(normalized)
        }

        val map = mapOf(
            'ا' to "a", 'آ' to "a", 'ب' to "b", 'پ' to "p", 'ت' to "t", 'ٹ' to "t",
            'ث' to "s", 'ج' to "j", 'چ' to "ch", 'ح' to "h", 'خ' to "kh", 'د' to "d",
            'ڈ' to "d", 'ذ' to "z", 'ر' to "r", 'ڑ' to "r", 'ز' to "z", 'ژ' to "zh",
            'س' to "s", 'ش' to "sh", 'ص' to "s", 'ض' to "z", 'ط' to "t", 'ظ' to "z",
            'ع' to "'", 'غ' to "gh", 'ف' to "f", 'ق' to "q", 'ک' to "k", 'گ' to "g",
            'ل' to "l", 'م' to "m", 'ن' to "n", 'ں' to "n", 'و' to "w", 'ہ' to "h",
            'ھ' to "h", 'ی' to "y", 'ے' to "e", 'ئ' to "y", 'ء' to "'"
        )

        return buildString(normalized.length * 2) {
            normalized.forEach { ch ->
                append(
                    when {
                        ch in map -> map.getValue(ch)
                        ch.code < 128 && (ch.isLetterOrDigit() || ch in "' -") -> ch.lowercaseChar()
                        ch.isWhitespace() -> ' '
                        else -> ' '
                    }
                )
            }
        }.replace(Regex("\\s+"), " ").trim()
    }
}
