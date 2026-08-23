package com.example.ai

import com.example.data.KhowarNormalizer
import com.example.data.model.PartOfSpeech

data class AiSuggestionResult(
    val transliteration: String,
    val suggestedPos: PartOfSpeech,
    val grammaticalNotes: String,
    val modelName: String = "LinguisticAI-Khowar-Assist-v1"
)

object AiAssistanceService {

    fun suggestTransliterationAndGrammar(khowarWord: String, englishMeaning: String): AiSuggestionResult {
        val translit = KhowarNormalizer.generateTransliterationHint(khowarWord)
        
        // Simple grammatical heuristic
        val lowerMeaning = englishMeaning.lowercase()
        val pos = when {
            lowerMeaning.startsWith("to ") -> PartOfSpeech.VERB
            lowerMeaning.endsWith("ly") -> PartOfSpeech.ADVERB
            lowerMeaning.contains("good") || lowerMeaning.contains("big") || lowerMeaning.contains("small") || lowerMeaning.contains("red") -> PartOfSpeech.ADJECTIVE
            lowerMeaning.contains("in ") || lowerMeaning.contains("on ") || lowerMeaning.contains("under") -> PartOfSpeech.POSTPOSITION
            else -> PartOfSpeech.NOUN
        }

        val notes = when (pos) {
            PartOfSpeech.VERB -> "Infinitive verb candidate. In Khowar, verbal stems often end in -ik or -ak."
            PartOfSpeech.NOUN -> "Standard nominal entry. Review gender / plural declension."
            PartOfSpeech.ADJECTIVE -> "Descriptive adjective. Check agreement with noun gender/class."
            else -> "Linguistic lexical particle."
        }

        return AiSuggestionResult(
            transliteration = translit,
            suggestedPos = pos,
            grammaticalNotes = notes,
            modelName = "LinguisticAI-Khowar-Assist-v1"
        )
    }
}
