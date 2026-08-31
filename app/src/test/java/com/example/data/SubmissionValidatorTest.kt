package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionValidatorTest {
    @Test
    fun word_requires_khowar_text() {
        assertFalse(
            SubmissionValidator.validateWord("", "meaning", "", "native speaker").isSuccess
        )
    }

    @Test
    fun word_requires_translation() {
        assertFalse(
            SubmissionValidator.validateWord("کھوار", "", "", "native speaker").isSuccess
        )
    }

    @Test
    fun sentence_accepts_urdu_or_english_translation() {
        assertTrue(
            SubmissionValidator.validateSentence("کھوار جملہ", "", "اردو ترجمہ", "fieldwork").isSuccess
        )
    }

    @Test
    fun speech_rejects_invalid_duration() {
        assertFalse(
            SubmissionValidator.validateSpeech("recording.wav", 0.0, "کھوار").isSuccess
        )
        assertFalse(
            SubmissionValidator.validateSpeech("recording.wav", Double.NaN, "کھوار").isSuccess
        )
    }

    @Test
    fun speech_accepts_valid_recording() {
        assertTrue(
            SubmissionValidator.validateSpeech("recording.wav", 12.5, "کھوار جملہ").isSuccess
        )
    }
}
