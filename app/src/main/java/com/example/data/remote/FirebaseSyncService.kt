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
import java.io.File

/** Cloud mirror. Writes require a real, verified Firebase account; no anonymous fallback. */
class FirebaseSyncService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private fun withAuthenticatedUser(action: (String) -> Unit) {
        val user = auth.currentUser
            ?: throw IllegalStateException("A signed-in Firebase account is required for cloud sync.")
        require(user.isEmailVerified) { "Verify your email before syncing dataset records." }
        action(user.uid)
    }

    fun syncLexicon(entry: LexiconEntry) = withAuthenticatedUser { uid ->
        firestore.collection("lexicon").document(entry.id).set(entry.toMap(uid), SetOptions.merge())
    }

    fun syncSentence(entry: SentenceEntry) = withAuthenticatedUser { uid ->
        firestore.collection("sentences").document(entry.id).set(entry.toMap(uid), SetOptions.merge())
    }

    fun syncSpeech(entry: SpeechRecording) = withAuthenticatedUser { uid ->
        val uri = entry.audioFilePath.takeIf { it.isNotBlank() }?.let { source -> runCatching {
            if (source.startsWith("content://") || source.startsWith("file://")) Uri.parse(source) else Uri.fromFile(File(source))
        }.getOrNull() }
        if (uri == null) {
            firestore.collection("speech").document(entry.id).set(entry.toMap(uid, null), SetOptions.merge())
        } else {
            storage.reference.child("speech/$uid/${entry.id}.audio").putFile(uri)
                .continueWithTask { it.result.storage.downloadUrl }
                .addOnSuccessListener { url -> firestore.collection("speech").document(entry.id).set(entry.toMap(uid, url.toString()), SetOptions.merge()) }
        }
    }

    fun syncStory(entry: StoryEntry) = withAuthenticatedUser { uid ->
        firestore.collection("stories").document(entry.id).set(entry.toMap(uid), SetOptions.merge())
    }

    fun syncKnowledge(entry: KnowledgeEntry) = withAuthenticatedUser { uid ->
        firestore.collection("knowledge").document(entry.id).set(entry.toMap(uid), SetOptions.merge())
    }

    fun syncImage(entry: ImageEntry) = withAuthenticatedUser { uid ->
        val uri = entry.localUri.takeIf { it.isNotBlank() }?.let { source -> runCatching {
            if (source.startsWith("content://") || source.startsWith("file://")) Uri.parse(source) else Uri.fromFile(File(source))
        }.getOrNull() }
        if (uri == null) {
            firestore.collection("images").document(entry.id).set(entry.toMap(uid, null), SetOptions.merge())
        } else {
            storage.reference.child("images/$uid/${entry.id}").putFile(uri)
                .continueWithTask { it.result.storage.downloadUrl }
                .addOnSuccessListener { url -> firestore.collection("images").document(entry.id).set(entry.toMap(uid, url.toString()), SetOptions.merge()) }
        }
    }

    private fun LexiconEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "khowarWord" to khowarWord, "normalizedKhowarWord" to normalizedKhowarWord, "transliteration" to transliteration, "englishMeaning" to englishMeaning, "urduMeaning" to urduMeaning, "partOfSpeech" to partOfSpeech.name, "grammaticalCategory" to grammaticalCategory, "definition" to definition, "pronunciation" to pronunciation, "exampleSentenceKhowar" to exampleSentenceKhowar, "exampleSentenceEnglish" to exampleSentenceEnglish, "dialectId" to dialectId, "regionId" to regionId, "source" to source, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "isAiAssisted" to isAiAssisted, "aiModelUsed" to aiModelUsed, "createdAt" to createdAt, "updatedAt" to updatedAt, "publishedAt" to publishedAt)
    private fun SentenceEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "khowarText" to khowarText, "normalizedText" to normalizedText, "transliteration" to transliteration, "englishTranslation" to englishTranslation, "urduTranslation" to urduTranslation, "context" to context, "dialectId" to dialectId, "regionId" to regionId, "source" to source, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "isAiAssisted" to isAiAssisted, "createdAt" to createdAt, "updatedAt" to updatedAt)
    private fun SpeechRecording.toMap(ownerUid: String, cloudAudioUrl: String?) = mapOf("ownerUid" to ownerUid, "id" to id, "speakerPublicId" to speakerPublicId, "speakerAgeGroup" to speakerAgeGroup, "speakerGender" to speakerGender, "isNativeSpeaker" to isNativeSpeaker, "audioFilePath" to audioFilePath, "cloudAudioUrl" to cloudAudioUrl, "durationSeconds" to durationSeconds, "sampleRate" to sampleRate, "channels" to channels, "format" to format, "transcriptKhowar" to transcriptKhowar, "normalizedTranscript" to normalizedTranscript, "transliteration" to transliteration, "englishTranslation" to englishTranslation, "urduTranslation" to urduTranslation, "dialectId" to dialectId, "regionId" to regionId, "recordingEnvironment" to recordingEnvironment, "qualityScore" to qualityScore, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
    private fun StoryEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "title" to title, "khowarText" to khowarText, "transliteration" to transliteration, "englishTranslation" to englishTranslation, "urduTranslation" to urduTranslation, "category" to category.name, "authorOrSpeaker" to authorOrSpeaker, "dialectId" to dialectId, "regionId" to regionId, "source" to source, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
    private fun KnowledgeEntry.toMap(ownerUid: String) = mapOf("ownerUid" to ownerUid, "id" to id, "type" to type.name, "title" to title, "khowarContent" to khowarContent, "transliteration" to transliteration, "englishContent" to englishContent, "urduContent" to urduContent, "explanation" to explanation, "source" to source, "dialectId" to dialectId, "regionId" to regionId, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
    private fun ImageEntry.toMap(ownerUid: String, cloudImageUrl: String?) = mapOf("ownerUid" to ownerUid, "id" to id, "title" to title, "description" to description, "khowarLabel" to khowarLabel, "englishLabel" to englishLabel, "culturalContext" to culturalContext, "localUri" to localUri, "cloudImageUrl" to cloudImageUrl, "photographerOrSource" to photographerOrSource, "regionId" to regionId, "contributorId" to contributorId, "contributorName" to contributorName, "status" to status.name, "licenseId" to licenseId, "createdAt" to createdAt)
}
