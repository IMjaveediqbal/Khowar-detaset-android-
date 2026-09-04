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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

/** Cloud mirror. Every operation completes only after Firebase has acknowledged the write. */
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

    suspend fun syncLexicon(entry: LexiconEntry) {
        val uid = authenticatedUid()
        firestore.collection("lexicon").document(entry.id).set(entry.toMap(uid), SetOptions.merge()).await()
    }

    suspend fun syncSentence(entry: SentenceEntry) {
        val uid = authenticatedUid()
        firestore.collection("sentences").document(entry.id).set(entry.toMap(uid), SetOptions.merge()).await()
    }

    suspend fun syncSpeech(entry: SpeechRecording) {
        val uid = authenticatedUid()
        val uri = entry.audioFilePath.takeIf { it.isNotBlank() }?.let { source -> runCatching {
            if (source.startsWith("content://") || source.startsWith("file://")) Uri.parse(source) else Uri.fromFile(File(source))
        }.getOrNull() }
        val cloudUrl = if (uri == null) null else {
            val ref = storage.reference.child("speech/$uid/${entry.id}.audio")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }
        firestore.collection("speech").document(entry.id).set(entry.toMap(uid, cloudUrl), SetOptions.merge()).await()
    }

    suspend fun syncStory(entry: StoryEntry) {
        val uid = authenticatedUid()
        firestore.collection("stories").document(entry.id).set(entry.toMap(uid), SetOptions.merge()).await()
    }

    suspend fun syncKnowledge(entry: KnowledgeEntry) {
        val uid = authenticatedUid()
        firestore.collection("knowledge").document(entry.id).set(entry.toMap(uid), SetOptions.merge()).await()
    }

    suspend fun syncImage(entry: ImageEntry) {
        val uid = authenticatedUid()
        val uri = entry.localUri.takeIf { it.isNotBlank() }?.let { source -> runCatching {
            if (source.startsWith("content://") || source.startsWith("file://")) Uri.parse(source) else Uri.fromFile(File(source))
        }.getOrNull() }
        val cloudUrl = if (uri == null) null else {
            val ref = storage.reference.child("images/$uid/${entry.id}")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }
        firestore.collection("images").document(entry.id).set(entry.toMap(uid, cloudUrl), SetOptions.merge()).await()
    }

    private fun LexiconEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "khowarWord" to khowarWord, "normalizedKhowarWord" to normalizedKhowarWord, "transliteration" to transliteration, "englishMeaning" to englishMeaning, "urduMeaning" to urduMeaning, "partOfSpeech" to partOfSpeech.name, "grammaticalCategory" to grammaticalCategory, "definition" to definition, "pronunciation" to pronunciation, "exampleSentenceKhowar" to exampleSentenceKhowar, "exampleSentenceEnglish" to exampleSentenceEnglish, "dialectId" to dialectId, "regionId" to regionId, "source" to source, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "isAiAssisted" to isAiAssisted, "aiModelUsed" to aiModelUsed, "createdAt" to createdAt, "updatedAt" to updatedAt, "publishedAt" to publishedAt)
    private fun SentenceEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "khowarText" to khowarText, "normalizedText" to normalizedText, "transliteration" to transliteration, "englishTranslation" to englishTranslation, "urduTranslation" to urduTranslation, "context" to context, "dialectId" to dialectId, "regionId" to regionId, "source" to source, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "isAiAssisted" to isAiAssisted, "createdAt" to createdAt, "updatedAt" to updatedAt)
    private fun SpeechRecording.toMap(ownerUid: String, cloudAudioUrl: String?) = mapOf("ownerUid" to ownerUid, "id" to id, "speakerPublicId" to speakerPublicId, "speakerAgeGroup" to speakerAgeGroup, "speakerGender" to speakerGender, "isNativeSpeaker" to isNativeSpeaker, "audioFilePath" to audioFilePath, "cloudAudioUrl" to cloudAudioUrl, "durationSeconds" to durationSeconds, "sampleRate" to sampleRate, "channels" to channels, "format" to format, "transcriptKhowar" to transcriptKhowar, "normalizedTranscript" to normalizedTranscript, "transliteration" to transliteration, "englishTranslation" to englishTranslation, "urduTranslation" to urduTranslation, "dialectId" to dialectId, "regionId" to regionId, "recordingEnvironment" to recordingEnvironment, "qualityScore" to qualityScore, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
    private fun StoryEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "title" to title, "khowarText" to khowarText, "transliteration" to transliteration, "englishTranslation" to englishTranslation, "urduTranslation" to urduTranslation, "category" to category.name, "authorOrSpeaker" to authorOrSpeaker, "dialectId" to dialectId, "regionId" to regionId, "source" to source, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
    private fun KnowledgeEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "type" to type.name, "title" to title, "khowarContent" to khowarContent, "transliteration" to transliteration, "englishContent" to englishContent, "urduContent" to urduContent, "explanation" to explanation, "source" to source, "dialectId" to dialectId, "regionId" to regionId, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
    private fun ImageEntry.toMap(ownerUid: String, cloudImageUrl: String?) = mapOf("ownerUid" to ownerUid, "id" to id, "title" to title, "description" to description, "khowarLabel" to khowarLabel, "englishLabel" to englishLabel, "culturalContext" to culturalContext, "localUri" to localUri, "cloudImageUrl" to cloudImageUrl, "photographerOrSource" to photographerOrSource, "regionId" to regionId, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
}
