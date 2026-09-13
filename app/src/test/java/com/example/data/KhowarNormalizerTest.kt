package com.example.data

import org.junit.Assert.*
import org.junit.Test

class KhowarNormalizerTest {
    @Test fun preservesDistinctLetters() {
        assertNotEquals(KhowarNormalizer.normalizeKhowarText("ے"), KhowarNormalizer.normalizeKhowarText("ی"))
        assertNotEquals(KhowarNormalizer.normalizeKhowarText("ھ"), KhowarNormalizer.normalizeKhowarText("ہ"))
        assertNotEquals(KhowarNormalizer.normalizeKhowarText("ؤ"), KhowarNormalizer.normalizeKhowarText("و"))
    }
    @Test fun arabicLettersReachHintMapping() {
        assertEquals("khwar", KhowarNormalizer.generateTransliterationHint("کھوار"))
        assertEquals("khowar", KhowarNormalizer.generateTransliterationHint("Khowar"))
    }
}
