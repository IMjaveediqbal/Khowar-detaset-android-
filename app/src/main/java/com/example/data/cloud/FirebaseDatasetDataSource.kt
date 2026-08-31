package com.example.data.cloud

import android.net.Uri
import com.example.data.model.LexiconEntry
import com.example.data.model.SentenceEntry
import com.example.data.model.SpeechRecording
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Optional cloud data source for the Khowar dataset.
 * Room remains the offline/local source of truth until the app explicitly enables sync.
 * Production Firestore rules must enforce authentication, validation and approved-only reads.
 */
class FirebaseDatasetDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun uploadLexicon(entry: LexiconEntry) {
        firestore.collection("lexicon").document(entry.id).set(entry.toCloudMap()).awaitTask()
    }

    suspend fun uploadSentence(entry: SentenceEntry) {
        firestore.collection("sentences").document(entry.id).set(entry.toCloudMap()).awaitTask()
    }

    suspend fun uploadSpeechMetadata(entry: SpeechRecording) {
        firestore.collection("speech").document(entry.id).set(entry.toCloudMap()).awaitTask()
    }

    suspend fun uploadAudio(recordId: String, localUri: Uri): String {
        val ref = storage.reference.child("speech/$recordId/audio")
        ref.putFile(localUri).awaitTask()
        return ref.downloadUrl.awaitTask()
    }

    private fun LexiconEntry.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "khowar" to khowarWord,
        "normalized_khowar" to normalizedKhowarWord,
        "transliteration" to transliteration,
        "english" to englishMeaning,
        "urdu" to urduMeaning,
        "part_of_speech" to partOfSpeech.name,
        "dialect" to dialectId,
        "region" to regionId,
        "source" to source,
        "contributor_id" to contributorId,
        "status" to status.name,
        "license" to licenseId,
        "created_at" to createdAt,
        "updated_at" to updatedAt,
        "published_at" to publishedAt
    )

    private fun SentenceEntry.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "khowar" to khowarText,
        "normalized_khowar" to normalizedText,
        "transliteration" to transliteration,
        "english" to englishTranslation,
        "urdu" to urduTranslation,
        "context" to context,
        "dialect" to dialectId,
        "region" to regionId,
        "source" to source,
        "contributor_id" to contributorId,
        "status" to status.name,
        "license" to licenseId,
        "created_at" to createdAt,
        "updated_at" to updatedAt
    )

    private fun SpeechRecording.toCloudMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "speaker_public_id" to speakerPublicId,
        "age_group" to speakerAgeGroup,
        "gender" to speakerGender,
        "native_speaker" to isNativeSpeaker,
        "audio_file_path" to audioFilePath,
        "duration_seconds" to durationSeconds,
        "sample_rate" to sampleRate,
        "channels" to channels,
        "format" to format,
        "transcript" to transcriptKhowar,
        "normalized_transcript" to normalizedTranscript,
        "transliteration" to transliteration,
        "english" to englishTranslation,
        "urdu" to urduTranslation,
        "dialect" to dialectId,
        "region" to regionId,
        "environment" to recordingEnvironment,
        "quality_score" to qualityScore,
        "contributor_id" to contributorId,
        "status" to status.name,
        "license" to licenseId,
        "created_at" to createdAt
    )
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }
