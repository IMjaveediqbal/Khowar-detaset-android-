package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class UserRole {
    VISITOR,
    CONTRIBUTOR,
    VALIDATOR,
    EXPERT,
    RESEARCHER,
    MODERATOR,
    ADMIN,
    SUPER_ADMIN
}

enum class RecordStatus { DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, CHANGES_REQUESTED, ARCHIVED }

enum class PartOfSpeech { NOUN, VERB, ADJECTIVE, ADVERB, PRONOUN, PREPOSITION, POSTPOSITION, CONJUNCTION, INTERJECTION, PARTICLE, IDIOM, PROVERB, OTHER }

enum class KnowledgeType { PROVERB, IDIOM, TRADITION, CUSTOM, PLACE_NAME, FOOD_CULINARY, TRADITIONAL_CLOTHING, FOLK_MUSIC_INSTRUMENT, HISTORICAL_NOTE, FLORA_FAUNA }

enum class StoryCategory { FOLK_TALE, PERSONAL_NARRATIVE, CULTURAL_DESCRIPTION, ESSAY, CONVERSATION, POETRY, ORAL_HISTORY }

enum class DataStage { RAW, QUALITY_CHECKED, COMMUNITY_VERIFIED, EXPERT_VERIFIED, RESEARCH_READY, RELEASED }

@Entity(tableName = "users")
data class User(@PrimaryKey val id: String = UUID.randomUUID().toString(), val email: String, val displayName: String, val username: String, val role: UserRole = UserRole.CONTRIBUTOR, val preferredLanguage: String = "en", val region: String = "Chitral", val bio: String = "", val isPublicProfile: Boolean = true, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "regions")
data class Region(@PrimaryKey val id: String, val name: String, val nameKhowar: String, val province: String, val district: String)

@Entity(tableName = "dialects")
data class Dialect(@PrimaryKey val id: String, val name: String, val nameKhowar: String, val description: String)

@Entity(tableName = "licenses")
data class License(@PrimaryKey val id: String, val name: String, val identifier: String, val description: String, val allowsCommercial: Boolean, val requiresAttribution: Boolean, val requiresShareAlike: Boolean)

@Entity(tableName = "lexicon_entries")
data class LexiconEntry(@PrimaryKey val id: String = UUID.randomUUID().toString(), val khowarWord: String, val normalizedKhowarWord: String, val transliteration: String, val englishMeaning: String, val urduMeaning: String, val partOfSpeech: PartOfSpeech, val grammaticalCategory: String = "", val definition: String = "", val pronunciation: String = "", val exampleSentenceKhowar: String = "", val exampleSentenceEnglish: String = "", val dialectId: String = "Central", val regionId: String = "Chitral Upper", val source: String = "Native Speaker Contribution", val contributorId: String, val contributorName: String, val status: RecordStatus = RecordStatus.SUBMITTED, val licenseId: String = "CC-BY-SA-4.0", val isAiAssisted: Boolean = false, val aiModelUsed: String = "", val dataStage: DataStage = DataStage.RAW, val datasetVersion: String? = null, val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis(), val publishedAt: Long? = null)

@Entity(tableName = "sentences")
data class SentenceEntry(@PrimaryKey val id: String = UUID.randomUUID().toString(), val khowarText: String, val normalizedText: String, val transliteration: String, val englishTranslation: String, val urduTranslation: String, val context: String = "", val dialectId: String = "Central", val regionId: String = "Chitral Lower", val source: String = "Fieldwork / Oral Corpus", val contributorId: String, val contributorName: String, val status: RecordStatus = RecordStatus.SUBMITTED, val licenseId: String = "CC-BY-SA-4.0", val isAiAssisted: Boolean = false, val dataStage: DataStage = DataStage.RAW, val datasetVersion: String? = null, val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis())

@Entity(tableName = "speech_recordings")
data class SpeechRecording(@PrimaryKey val id: String = UUID.randomUUID().toString(), val speakerPublicId: String, val speakerAgeGroup: String, val speakerGender: String, val isNativeSpeaker: Boolean = true, val audioFilePath: String, val durationSeconds: Double, val sampleRate: Int = 44100, val channels: Int = 1, val format: String = "WAV", val transcriptKhowar: String, val normalizedTranscript: String, val transliteration: String, val englishTranslation: String, val urduTranslation: String, val dialectId: String = "Central", val regionId: String = "Chitral", val recordingEnvironment: String = "Quiet room", val qualityScore: Double = 0.0, val contributorId: String, val contributorName: String, val status: RecordStatus = RecordStatus.SUBMITTED, val licenseId: String = "CC-BY-SA-4.0", val dataStage: DataStage = DataStage.RAW, val datasetVersion: String? = null, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "stories")
data class StoryEntry(@PrimaryKey val id: String = UUID.randomUUID().toString(), val title: String, val khowarText: String, val transliteration: String, val englishTranslation: String, val urduTranslation: String, val category: StoryCategory = StoryCategory.FOLK_TALE, val authorOrSpeaker: String = "", val dialectId: String = "Central", val regionId: String = "Chitral", val source: String = "Oral Tradition", val contributorId: String, val contributorName: String, val status: RecordStatus = RecordStatus.SUBMITTED, val licenseId: String = "CC-BY-SA-4.0", val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "images")
data class ImageEntry(@PrimaryKey val id: String = UUID.randomUUID().toString(), val title: String, val description: String, val khowarLabel: String, val englishLabel: String, val culturalContext: String, val localUri: String, val photographerOrSource: String, val regionId: String = "Chitral", val contributorId: String, val contributorName: String, val status: RecordStatus = RecordStatus.SUBMITTED, val licenseId: String = "CC-BY-SA-4.0", val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "knowledge")
data class KnowledgeEntry(@PrimaryKey val id: String = UUID.randomUUID().toString(), val type: KnowledgeType, val title: String, val khowarContent: String, val transliteration: String, val englishContent: String, val urduContent: String, val explanation: String, val source: String = "Community Heritage", val dialectId: String = "Central", val regionId: String = "Chitral", val contributorId: String, val contributorName: String, val status: RecordStatus = RecordStatus.SUBMITTED, val licenseId: String = "CC-BY-SA-4.0", val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "consents")
data class ConsentRecord(@PrimaryKey val id: String = UUID.randomUUID().toString(), val contributorId: String, val subjectType: String, val subjectId: String, val consentType: String = "DATASET_PUBLICATION_CC_BY_SA", val consentVersion: String = "1.0", val isGranted: Boolean = true, val grantedAt: Long = System.currentTimeMillis(), val withdrawnAt: Long? = null)

@Entity(tableName = "validation_reviews")
data class ValidationReview(@PrimaryKey val id: String = UUID.randomUUID().toString(), val recordType: String, val recordId: String, val validatorId: String, val validatorName: String, val decision: String, val comments: String, val confidenceScore: Int = 5, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "dataset_versions")
data class DatasetVersion(@PrimaryKey val id: String = UUID.randomUUID().toString(), val versionNumber: String, val releaseName: String, val description: String, val recordCount: Int, val speechHours: Double, val speakerCount: Int = 0, val dialectCount: Int = 0, val validatedRecordCount: Int = 0, val researchReadyRecordCount: Int = 0, val license: String = "CC BY-SA 4.0", val status: String = "DRAFT", val createdBy: String, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "api_keys")
data class ApiKey(@PrimaryKey val id: String = UUID.randomUUID().toString(), val userId: String, val keyName: String, val rawKeyDisplay: String, val hashedKey: String, val rateLimitPerHour: Int = 1000, val lastUsedAt: Long? = null, val isRevoked: Boolean = false, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "audit_logs")
data class AuditLog(@PrimaryKey val id: String = UUID.randomUUID().toString(), val actorId: String, val actorName: String, val action: String, val entityType: String, val entityId: String, val details: String, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "moderation_reports")
data class ModerationReport(@PrimaryKey val id: String = UUID.randomUUID().toString(), val reporterId: String, val reporterName: String, val recordType: String, val recordId: String, val category: String, val description: String, val status: String = "PENDING", val resolutionNotes: String = "", val createdAt: Long = System.currentTimeMillis())
