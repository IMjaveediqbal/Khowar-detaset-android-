package com.example.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeBackend
import com.google.firebase.ai.ai

/**
 * Real Gemini-backed linguistic assistant.
 * AI suggestions must remain suggestions and must never automatically approve dataset records.
 */
class KhowarGeminiService(
    private val modelName: String = "gemini-3.7-flash"
) {
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(modelName)

    suspend fun suggestForLexicon(
        khowarWord: String,
        englishMeaning: String,
        urduMeaning: String,
        context: String = ""
    ): String? {
        val prompt = """
            You are a linguistic assistant helping document Khowar, an endangered language of Chitral.
            Do not invent facts. If uncertain, explicitly say so.
            The human validator makes the final decision.

            Khowar word: $khowarWord
            English meaning: $englishMeaning
            Urdu meaning: $urduMeaning
            Context: $context

            Return concise suggestions for:
            1. transliteration
            2. likely part of speech
            3. spelling/normalization concerns
            4. one improved example sentence if enough information is available
            5. uncertainty or information that requires native-speaker verification
        """.trimIndent()

        return runCatching { model.generateContent(prompt).text?.trim() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    suspend fun explainKhowar(text: String): String? {
        val prompt = """
            Explain this Khowar text for a language learner.
            Preserve the original Khowar text exactly. Clearly separate translation from interpretation.
            Do not claim certainty about dialect or grammar without evidence.

            Text: $text
        """.trimIndent()

        return runCatching { model.generateContent(prompt).text?.trim() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
