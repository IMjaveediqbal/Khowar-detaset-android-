package com.example.data

/**
 * Validation rules shared by contribution screens before records are persisted.
 * These rules intentionally validate structure, not linguistic correctness;
 * linguistic correctness belongs to the human validation workflow.
 */
object SubmissionValidator {
    private const val MAX_SHORT_TEXT = 500
    private const val MAX_LONG_TEXT = 100_000
    private const val MAX_SOURCE = 2_000

    fun validateWord(
        khowarWord: String,
        englishMeaning: String,
        urduMeaning: String,
        source: String
    ): Result<Unit> = validateCommon(source).fold(
        onSuccess = {
            when {
                khowarWord.trim().isEmpty() -> failure("Khowar word cannot be empty.")
                khowarWord.trim().length > MAX_SHORT_TEXT -> failure("Khowar word is too long.")
                englishMeaning.trim().isEmpty() && urduMeaning.trim().isEmpty() ->
                    failure("Provide an English or Urdu meaning.")
                englishMeaning.length > MAX_SHORT_TEXT || urduMeaning.length > MAX_SHORT_TEXT ->
                    failure("Meaning is too long.")
                else -> Result.success(Unit)
            }
        },
        onFailure = { Result.failure(it) }
    )

    fun validateSentence(
        khowarText: String,
        englishTranslation: String,
        urduTranslation: String,
        source: String
    ): Result<Unit> = validateCommon(source).fold(
        onSuccess = {
            when {
                khowarText.trim().isEmpty() -> failure("Sentence cannot be empty.")
                khowarText.trim().length > MAX_LONG_TEXT -> failure("Sentence is too long.")
                englishTranslation.trim().isEmpty() && urduTranslation.trim().isEmpty() ->
                    failure("Provide an English or Urdu translation.")
                englishTranslation.length > MAX_LONG_TEXT || urduTranslation.length > MAX_LONG_TEXT ->
                    failure("Translation is too long.")
                else -> Result.success(Unit)
            }
        },
        onFailure = { Result.failure(it) }
    )

    fun validateSpeech(
        audioFilePath: String,
        durationSeconds: Double,
        transcriptKhowar: String
    ): Result<Unit> = when {
        audioFilePath.trim().isEmpty() -> failure("Audio file is required.")
        !durationSeconds.isFinite() || durationSeconds <= 0.0 -> failure("Audio duration must be greater than zero.")
        durationSeconds > 60.0 * 60.0 -> failure("A single recording cannot exceed 60 minutes.")
        transcriptKhowar.trim().isEmpty() -> failure("Speech transcript cannot be empty.")
        transcriptKhowar.length > MAX_LONG_TEXT -> failure("Transcript is too long.")
        else -> Result.success(Unit)
    }

    private fun validateCommon(source: String): Result<Unit> = when {
        source.trim().isEmpty() -> failure("Source is required.")
        source.length > MAX_SOURCE -> failure("Source information is too long.")
        else -> Result.success(Unit)
    }

    private fun failure(message: String): Result<Unit> = Result.failure(IllegalArgumentException(message))
}
