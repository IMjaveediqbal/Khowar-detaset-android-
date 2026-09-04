package com.example.data.remote

import android.net.Uri
import com.example.data.model.ImageEntry
import com.example.data.model.KnowledgeEntry
import com.example.data.model.LexiconEntry
import com.example.data.model.SentenceEntry
import com.example.data.model.SpeechRecording
import com.example.data.model.StoryEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

/** Cloud mirror. Writes are immutable creates; an existing document is treated as an idempotent success. */
class FirebaseSyncService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private fun authenticatedUid(): String {
        val user = auth.currentUser ?: throw IllegalStateException("A signed-in Firebase account is required for cloud sync.")
        require(user.isEmailVerified) { "Verify your email before syncing dataset records." }
        return user.uid
    }

    private suspend fun createIdempotently(path: String, data: Map<String, Any?>) {
        try {
            firestore.document(path).create(data).await()
        } catch (error: FirebaseFirestoreException) {
            if (error.code != FirebaseFirestoreException.Code.ALREADY_EXISTS) throw error
        }
    }

    suspend fun syncLexicon(entry: LexiconEntry) {
        val uid = authenticatedUid()
        createIdempotently("lexicon/${entry.id}", entry.toMap(uid))
    }

    suspend fun syncSentence(entry: SentenceEntry) {
        val uid = authenticatedUid()
        createIdempotently("sentences/${entry.id}", entry.toMap(uid))
    }

    suspend fun syncSpeech(entry: SpeechRecording) {
        val uid = authenticatedUid()
        require(entry.audioFilePath.isNotBlank()) { "Speech recording has no local audio file." }
        val uri = toUri(entry.audioFilePath)
        val ref = storage.reference.child("speech/$uid/${entry.id}.audio")
        ref.putFile(uri).await()
        val cloudUrl = ref.downloadUrl.await().toString()
        createIdempotently("speech/${entry.id}", entry.toMap(uid, cloudUrl))
    }

    suspend fun syncStory(entry: StoryEntry) {
        val uid = authenticatedUid()
        createIdempotently("stories/${entry.id}", entry.toMap(uid))
    }

    suspend fun syncKnowledge(entry: KnowledgeEntry) {
        val uid = authenticatedUid()
        createIdempotently("knowledge/${entry.id}", entry.toMap(uid))
    }

    suspend fun syncImage(entry: ImageEntry) {
        val uid = authenticatedUid()
        require(entry.localUri.isNotBlank()) { "Image has no local URI." }
        val ref = storage.reference.child("images/$uid/${entry.id}")
        ref.putFile(toUri(entry.localUri)).await()
        val cloudUrl = ref.downloadUrl.await().toString()
        createIdempotently("images/${entry.id}", entry.toMap(uid, cloudUrl))
    }

    private fun toUri(source: String): Uri = when {
        source.startsWith("content://") || source.startsWith("file://") -> Uri.parse(source)
        else -> Uri.fromFile(File(source))
    }

    private fun LexiconEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "khowarWord" to khowarWord, "normalizedKhowarWord" to normalizedKhowarWord, "transliteration" to transliteration, "englishMeaning" to englishMeaning, "urduMeaning" to urduMeaning, "partOfSpeech" to partOfSpeech.name, "grammaticalCategory" to grammaticalCategory, "definition" to definition, "pronunciation" to pronunciation, "exampleSentenceKhowar" to exampleSentenceKhowar, "exampleSentenceEnglish" to exampleSentenceEnglish, "dialectId" to dialectId, "regionId" to regionId, "source" to source, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "isAiAssisted" to isAiAssisted, "aiModelUsed" to aiModelUsed, "createdAt" to createdAt, "updatedAt" to updatedAt, "publishedAt" to publishedAt)
    private fun SentenceEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "khowarText" to khowarText, "normalizedText" to normalizedText, "transliteration" to transliteration, "englishTranslation" to englishTranslation, "urduTranslation" to urduTranslation, "context" to context, "dialectId" to dialectId, "regionId" to regionId, "source" to source, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "isAiAssisted" to isAiAssisted, "createdAt" to createdAt, "updatedAt" to updatedAt)
    private fun SpeechRecording.toMap(ownerUid: String, cloudAudioUrl: String) = mapOf("ownerUid" to ownerUid, "id" to id, "speakerPublicId" to speakerPublicId, "speakerAgeGroup" to speakerAgeGroup, "speakerGender" to speakerGender, "isNativeSpeaker" to isNativeSpeaker, "audioFilePath" to audioFilePath, "cloudAudioUrl" to cloudAudioUrl, "durationSeconds" to durationSeconds, "sampleRate" to sampleRate, "channels" to channels, "format" to format, "transcriptKhowar" to transcriptKhowar, "normalizedTranscript" to normalizedTranscript, "transliteration" to transliteration, "englishTranslation" to englishTranslation, "urduTranslation" to urduTranslation, "dialectId" to dialectId, "regionId" to regionId, "recordingEnvironment" to recordingEnvironment, "qualityScore" to qualityScore, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
    private fun StoryEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "title" to title, "khowarText" to khowarText, "transliteration" to transliteration, "englishTranslation" to englishTranslation, "urduTranslation" to urduTranslation, "category" to category.name, "authorOrSpeaker" to authorOrSpeaker, "dialectId" to dialectId, "regionId" to regionId, "source" to source, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
    private fun KnowledgeEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "type" to type.name, "title" to title, "khowarContent" to khowarContent, "transliteration" to transliteration, "englishContent" to englishContent, "urduContent" to urduContent, "explanation" to explanation, "source" to source, "dialectId" to dialectId, "regionId" to regionId, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
    private fun ImageEntry.toMap(ownerUid: String, cloudImageUrl: String) = mapOf("ownerUid" to ownerUid, "id" to id, "title" to title, "description" to description, "khowarLabel" to khowarLabel, "englishLabel" to englishLabel, "culturalContext" to culturalContext, "localUri" to localUri, "cloudImageUrl" to cloudImageUrl, "photographerOrSource" to photographerOrSource, "regionId" to regionId, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
}
