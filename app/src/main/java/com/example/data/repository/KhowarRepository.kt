package com.example.data.repository

import com.example.data.SubmissionValidator
import androidx.room.withTransaction
import com.example.data.remote.DatasetCodec
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.withLock
import com.example.data.remote.DatasetSyncWorker
import com.example.data.KhowarNormalizer
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

data class DatasetStatistics(
    val totalWords: Int = 0,
    val totalSentences: Int = 0,
    val totalSpeechRecordings: Int = 0,
    val totalSpeechHours: Double = 0.0,
    val totalStories: Int = 0,
    val totalKnowledge: Int = 0,
    val totalImages: Int = 0,
    val totalContributors: Int = 0,
    val totalApprovedRecords: Int = 0,
    val pendingReviewCount: Int = 0
)

class KhowarRepository(private val database: AppDatabase) {
    private val lexiconDao = database.lexiconDao()
    private val sentenceDao = database.sentenceDao()
    private val speechDao = database.speechDao()
    private val storyDao = database.storyDao()
    private val imageDao = database.imageDao()
    private val knowledgeDao = database.knowledgeDao()
    private val userDao = database.userDao()
    private val validationDao = database.validationDao()
    private val consentDao = database.consentDao()
    private val metadataDao = database.metadataDao()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun registerOrLoginUser(email: String, displayName: String, username: String, role: UserRole, region: String): User = withContext(Dispatchers.IO) {
        val account = FirebaseAuth.getInstance().currentUser ?: error("Sign in first.")
        require(!account.isAnonymous) { "Use a verified sign-in method before contributing." }
        val result = FirebaseFunctions.getInstance().getHttpsCallable("saveProfile").call(mapOf("displayName" to displayName, "username" to username, "region" to region)).await().data as Map<*, *>
        val user = User(id = account.uid, email = account.email.orEmpty(), displayName = displayName.trim(), username = username.trim(), region = region, role = UserRole.valueOf(result["role"].toString()))
        userDao.insert(user)
        _currentUser.value = user
        user
    }

    private suspend fun enqueue(collection: String, id: String, record: Any, uid: String) {
        val authUid = FirebaseAuth.getInstance().currentUser?.uid
        require(authUid == uid) { "Account changed; sign in again." }
        database.cloudDao().put(CloudOperation("$collection/$id", collection, id, uid, DatasetCodec.encode(record)))
    }

    suspend fun purgeOtherPrivate(uid: String) = database.withTransaction {
        lexiconDao.purgeOtherPrivate(uid); sentenceDao.purgeOtherPrivate(uid); speechDao.purgeOtherPrivate(uid)
        storyDao.purgeOtherPrivate(uid); knowledgeDao.purgeOtherPrivate(uid); imageDao.purgeOtherPrivate(uid)
    }

    fun setCurrentUser(user: User?) { _currentUser.value = user }

    val datasetStatistics: Flow<DatasetStatistics> = combine(
        lexiconDao.countApproved(), sentenceDao.countApproved(), speechDao.countApproved(),
        speechDao.totalApprovedDurationSeconds(), storyDao.countApproved(), knowledgeDao.countApproved(),
        imageDao.countApproved(), userDao.countActiveContributors(), lexiconDao.getReviewQueue(),
        sentenceDao.getReviewQueue(), speechDao.getReviewQueue(), storyDao.getReviewQueue(), knowledgeDao.getReviewQueue(), imageDao.getReviewQueue()
    ) { args: Array<Any?> ->
        val wordCount = args[0] as? Int ?: 0
        val sentenceCount = args[1] as? Int ?: 0
        val speechCount = args[2] as? Int ?: 0
        val durationSeconds = args[3] as? Double ?: 0.0
        val stories = args[4] as? Int ?: 0
        val knowledge = args[5] as? Int ?: 0
        val images = args[6] as? Int ?: 0
        val contributors = args[7] as? Int ?: 0
        val pendingLexicon = (args[8] as? List<*>)?.size ?: 0
        val pendingSentences = (args[9] as? List<*>)?.size ?: 0
        val pendingSpeech = (args[10] as? List<*>)?.size ?: 0
        DatasetStatistics(
            totalWords = wordCount, totalSentences = sentenceCount, totalSpeechRecordings = speechCount,
            totalSpeechHours = durationSeconds / 3600.0, totalStories = stories, totalKnowledge = knowledge,
            totalImages = images, totalContributors = contributors,
            totalApprovedRecords = wordCount + sentenceCount + speechCount + stories + knowledge + images,
            pendingReviewCount = pendingLexicon + pendingSentences + pendingSpeech + (args[11] as? List<*>) .orEmpty().size + (args[12] as? List<*>).orEmpty().size + (args[13] as? List<*>).orEmpty().size
        )
    }.flowOn(Dispatchers.IO)

    val approvedLexicon = lexiconDao.getAllApproved()
    val approvedSentences = sentenceDao.getAllApproved()
    val approvedSpeech = speechDao.getAllApproved()
    val approvedStories = storyDao.getAllApproved()
    val approvedImages = imageDao.getAllApproved()
    val approvedKnowledge = knowledgeDao.getAllApproved()

    val lexiconReviewQueue = lexiconDao.getReviewQueue()
    val sentenceReviewQueue = sentenceDao.getReviewQueue()
    val speechReviewQueue = speechDao.getReviewQueue()
    val storyReviewQueue = storyDao.getReviewQueue()
    val imageReviewQueue = imageDao.getReviewQueue()
    val knowledgeReviewQueue = knowledgeDao.getReviewQueue()

    val allDialects = metadataDao.getAllDialects()
    val allRegions = metadataDao.getAllRegions()
    val allLicenses = metadataDao.getAllLicenses()
    val datasetVersions = metadataDao.getDatasetVersions()
    val allAuditLogs = metadataDao.getAuditLogs()
    val allModerationReports = metadataDao.getModerationReports()
    val allUsers = userDao.getAllUsers()

    fun getApiKeysForUser(userId: String) = metadataDao.getApiKeysForUser(userId)
    fun getUserContributionsLexicon(userId: String) = lexiconDao.getByContributor(userId)
    fun getUserContributionsSentences(userId: String) = sentenceDao.getByContributor(userId)
    fun getUserContributionsSpeech(userId: String) = speechDao.getByContributor(userId)
    fun getUserContributionsStories(userId: String) = storyDao.getByContributor(userId)
    fun getUserContributionsKnowledge(userId: String) = knowledgeDao.getByContributor(userId)
    fun getUserContributionsImages(userId: String) = imageDao.getByContributor(userId)

    suspend fun checkLexiconDuplicate(khowarWord: String): List<LexiconEntry> = withContext(Dispatchers.IO) {
        lexiconDao.findDuplicates(KhowarNormalizer.normalizeKhowarText(khowarWord))
    }

    suspend fun checkSentenceDuplicate(sentence: String): List<SentenceEntry> = withContext(Dispatchers.IO) {
        sentenceDao.findDuplicates(KhowarNormalizer.normalizeKhowarText(sentence))
    }

    suspend fun submitWord(khowarWord: String, transliteration: String, englishMeaning: String, urduMeaning: String,
        partOfSpeech: PartOfSpeech, grammaticalCategory: String, definition: String, pronunciation: String,
        exampleKhowar: String, exampleEnglish: String, dialectId: String, regionId: String, source: String,
        licenseId: String, isAiAssisted: Boolean, aiModel: String): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in or select a contributor profile first."))
        SubmissionValidator.validateWord(khowarWord, englishMeaning, urduMeaning, source).getOrThrow()
        val normalized = KhowarNormalizer.normalizeKhowarText(khowarWord)
        if (normalized.isBlank()) return@withContext Result.failure(Exception("Khowar word cannot be empty."))
        val entry = LexiconEntry(
            khowarWord = khowarWord.trim(), normalizedKhowarWord = normalized,
            transliteration = transliteration.trim(),
            englishMeaning = englishMeaning.trim(), urduMeaning = urduMeaning.trim(), partOfSpeech = partOfSpeech,
            grammaticalCategory = grammaticalCategory.trim(), definition = definition.trim(), pronunciation = pronunciation.trim(),
            exampleSentenceKhowar = exampleKhowar.trim(), exampleSentenceEnglish = exampleEnglish.trim(), dialectId = dialectId,
            regionId = regionId, source = source.trim(), contributorId = user.id, contributorName = user.displayName,
            status = RecordStatus.SUBMITTED, licenseId = licenseId, isAiAssisted = isAiAssisted, aiModelUsed = aiModel.trim()
        )
        database.withTransaction {
        lexiconDao.insert(entry)
        recordConsent(user.id, "LEXICON", entry.id, "DATASET_PUBLICATION_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_WORD", "LEXICON", entry.id, "Submitted word '${entry.khowarWord}'")
            enqueue("lexicon", entry.id, entry, user.id)
        }
        Result.success(entry.id)
    }

    suspend fun submitSentence(khowarText: String, transliteration: String, englishTranslation: String, urduTranslation: String,
        context: String, dialectId: String, regionId: String, source: String, licenseId: String): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        SubmissionValidator.validateSentence(khowarText, englishTranslation, urduTranslation, source).getOrThrow()
        val normalized = KhowarNormalizer.normalizeKhowarText(khowarText)
        if (normalized.isBlank()) return@withContext Result.failure(Exception("Sentence cannot be empty."))
        val entry = SentenceEntry(
            khowarText = khowarText.trim(), normalizedText = normalized,
            transliteration = transliteration.trim(),
            englishTranslation = englishTranslation.trim(), urduTranslation = urduTranslation.trim(), context = context.trim(),
            dialectId = dialectId, regionId = regionId, source = source.trim(), contributorId = user.id,
            contributorName = user.displayName, status = RecordStatus.SUBMITTED, licenseId = licenseId
        )
        database.withTransaction {
        sentenceDao.insert(entry)
        recordConsent(user.id, "SENTENCE", entry.id, "DATASET_PUBLICATION_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_SENTENCE", "SENTENCE", entry.id, "Submitted sentence '${entry.khowarText.take(30)}...'")
            enqueue("sentences", entry.id, entry, user.id)
        }
        Result.success(entry.id)
    }

    suspend fun submitSpeech(speakerAgeGroup: String, speakerGender: String, isNativeSpeaker: Boolean, audioFilePath: String,
        durationSeconds: Double, transcriptKhowar: String, transliteration: String, englishTranslation: String,
        urduTranslation: String, dialectId: String, regionId: String, recordingEnvironment: String,
        licenseId: String): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        SubmissionValidator.validateSpeech(audioFilePath, durationSeconds, transcriptKhowar).getOrThrow()
        require(java.io.File(audioFilePath).isFile) { "Recording file is missing." }
        if (durationSeconds <= 0.0) return@withContext Result.failure(Exception("Audio duration must be greater than zero."))
        val norm = KhowarNormalizer.normalizeKhowarText(transcriptKhowar)
        val entry = SpeechRecording(
            speakerPublicId = "SPK-" + MessageDigest.getInstance("SHA-256").digest(user.id.toByteArray()).joinToString("") { "%02x".format(it) }.take(24), speakerAgeGroup = speakerAgeGroup.trim(),
            speakerGender = speakerGender.trim(), isNativeSpeaker = isNativeSpeaker, audioFilePath = audioFilePath,
            durationSeconds = durationSeconds, transcriptKhowar = transcriptKhowar.trim(), normalizedTranscript = norm,
            transliteration = transliteration.trim(), englishTranslation = englishTranslation.trim(), urduTranslation = urduTranslation.trim(),
            dialectId = dialectId, regionId = regionId, recordingEnvironment = recordingEnvironment.trim(),
            contributorId = user.id, contributorName = user.displayName, status = RecordStatus.SUBMITTED, licenseId = licenseId
        )
        database.withTransaction {
        speechDao.insert(entry)
        recordConsent(user.id, "SPEECH", entry.id, "VOICE_RECORDING_CONSENT_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_SPEECH", "SPEECH", entry.id, "Submitted voice recording of $durationSeconds s")
            enqueue("speech", entry.id, entry, user.id)
        }
        Result.success(entry.id)
    }

    suspend fun submitStory(title: String, khowarText: String, transliteration: String, englishTranslation: String,
        urduTranslation: String, category: StoryCategory, authorOrSpeaker: String, dialectId: String, regionId: String,
        source: String, licenseId: String): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        require(title.isNotBlank() && khowarText.isNotBlank() && khowarText.length <= 100_000) { "Title and story text are required." }
        val entry = StoryEntry(title = title.trim(), khowarText = khowarText.trim(), transliteration = transliteration.trim(),
            englishTranslation = englishTranslation.trim(), urduTranslation = urduTranslation.trim(), category = category,
            authorOrSpeaker = authorOrSpeaker.trim(), dialectId = dialectId, regionId = regionId, source = source.trim(),
            contributorId = user.id, contributorName = user.displayName, status = RecordStatus.SUBMITTED, licenseId = licenseId)
        database.withTransaction {
        storyDao.insert(entry)
        recordConsent(user.id, "STORY", entry.id, "DATASET_PUBLICATION_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_STORY", "STORY", entry.id, "Submitted story '$title'")
            enqueue("stories", entry.id, entry, user.id)
        }
        Result.success(entry.id)
    }

    suspend fun submitKnowledge(type: KnowledgeType, title: String, khowarContent: String, transliteration: String,
        englishContent: String, urduContent: String, explanation: String, source: String, dialectId: String,
        regionId: String, licenseId: String): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        require(title.isNotBlank() && khowarContent.isNotBlank() && khowarContent.length <= 100_000) { "Title and Khowar content are required." }
        val entry = KnowledgeEntry(type = type, title = title.trim(), khowarContent = khowarContent.trim(), transliteration = transliteration.trim(),
            englishContent = englishContent.trim(), urduContent = urduContent.trim(), explanation = explanation.trim(),
            source = source.trim(), dialectId = dialectId, regionId = regionId, contributorId = user.id,
            contributorName = user.displayName, status = RecordStatus.SUBMITTED, licenseId = licenseId)
        database.withTransaction {
        knowledgeDao.insert(entry)
        recordConsent(user.id, "KNOWLEDGE", entry.id, "DATASET_PUBLICATION_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_KNOWLEDGE", "KNOWLEDGE", entry.id, "Submitted knowledge item '$title'")
            enqueue("knowledge", entry.id, entry, user.id)
        }
        Result.success(entry.id)
    }

    suspend fun submitImage(title: String, description: String, khowarLabel: String, englishLabel: String, culturalContext: String,
        localUri: String, photographerOrSource: String, regionId: String, licenseId: String): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        require(java.io.File(localUri).isFile) { "Select a real image first." }
        if (localUri.isBlank()) return@withContext Result.failure(Exception("Image URI cannot be empty."))
        val entry = ImageEntry(title = title.trim(), description = description.trim(), khowarLabel = khowarLabel.trim(),
            englishLabel = englishLabel.trim(), culturalContext = culturalContext.trim(), localUri = localUri,
            photographerOrSource = photographerOrSource.trim(), regionId = regionId, contributorId = user.id,
            contributorName = user.displayName, status = RecordStatus.SUBMITTED, licenseId = licenseId)
        database.withTransaction {
        imageDao.insert(entry)
        recordConsent(user.id, "IMAGE", entry.id, "DATASET_PUBLICATION_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_IMAGE", "IMAGE", entry.id, "Submitted image item '$title'")
            enqueue("images", entry.id, entry, user.id)
        }
        Result.success(entry.id)
    }

    suspend fun reviewRecord(recordType: String, recordId: String, decision: String, comments: String, confidenceScore: Int): Result<Unit> = runCatching {
        FirebaseFunctions.getInstance().getHttpsCallable("reviewSubmission").call(mapOf("collection" to DatasetCodec.collection(recordType), "recordId" to recordId, "decision" to decision, "comments" to comments, "confidenceScore" to confidenceScore)).await()
        Unit
    }

    private suspend fun recordConsent(contributorId: String, subjectType: String, subjectId: String, consentType: String) {
        consentDao.insertConsent(ConsentRecord(contributorId = contributorId, subjectType = subjectType, subjectId = subjectId, consentType = consentType, isGranted = true))
    }

    suspend fun withdrawConsent(subjectType: String, subjectId: String): Result<Unit> = runCatching { DatasetSyncWorker.lock.withLock {
        val uid = _currentUser.value?.id ?: error("Sign in first.")
        val collections = if (subjectType == "ALL_USER_RECORDS") DatasetCodec.collections else listOf(DatasetCodec.collection(subjectType))
        // Stop pending local uploads before asking the server to archive records.
        for (operation in database.cloudDao().pending(uid)) {
            if (operation.collection in collections && (subjectType == "ALL_USER_RECORDS" || operation.recordId == subjectId)) {
                database.cloudDao().put(operation.copy(state = "WITHDRAWN", error = "Consent withdrawn"))
            }
        }
        for (collection in collections) {
            do {
                val response = FirebaseFunctions.getInstance().getHttpsCallable("withdrawConsent").call(mapOf("collection" to collection, "recordId" to subjectId.takeUnless { subjectType == "ALL_USER_RECORDS" })).await().data as Map<*, *>
            } while (response["remaining"] == true)
        }
        val localId = subjectId.takeUnless { subjectType == "ALL_USER_RECORDS" }
        database.withTransaction {
            if("lexicon" in collections) lexiconDao.archiveOwned(uid,localId)
            if("sentences" in collections) sentenceDao.archiveOwned(uid,localId)
            if("speech" in collections) speechDao.archiveOwned(uid,localId)
            if("stories" in collections) storyDao.archiveOwned(uid,localId)
            if("knowledge" in collections) knowledgeDao.archiveOwned(uid,localId)
            if("images" in collections) imageDao.archiveOwned(uid,localId)
        }
        for (consent in consentDao.getConsentsForUser(uid).first()) {
            if (subjectType == "ALL_USER_RECORDS" || consent.subjectId == subjectId) consentDao.updateConsent(consent.copy(isGranted = false, withdrawnAt = System.currentTimeMillis()))
        }
        Unit
    } }

    suspend fun generateApiKey(keyName: String): Result<Pair<String, ApiKey>> = Result.failure(IllegalStateException("Research API access is planned. No live API credentials are issued by this app."))

    suspend fun revokeApiKey(apiKey: ApiKey): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Sign in required."))
        if (apiKey.userId != user.id && user.role !in setOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)) return@withContext Result.failure(Exception("Unauthorized to revoke this API key."))
        metadataDao.updateApiKey(apiKey.copy(isRevoked = true))
        logAudit(user.id, user.displayName, "REVOKE_API_KEY", "API_KEY", apiKey.id, "Revoked research API key")
        Result.success(Unit)
    }

    suspend fun createDatasetVersion(versionNumber: String, releaseName: String, description: String): Result<Unit> = runCatching {
        FirebaseFunctions.getInstance().getHttpsCallable("createDatasetDraft").call(mapOf("version" to versionNumber, "name" to releaseName, "description" to description)).await()
        Unit
    }

    suspend fun submitReport(recordType: String, recordId: String, category: String, description: String): Result<Unit> = runCatching {
        FirebaseFunctions.getInstance().getHttpsCallable("reportDataset").call(mapOf("collection" to DatasetCodec.collection(recordType), "recordId" to recordId, "description" to description, "category" to category)).await()
        Unit
    }

    suspend fun generateExport(format: String): String = withContext(Dispatchers.IO) {
        val records = mutableListOf<org.json.JSONObject>()
        for (collection in DatasetCodec.collections) {
            var cursor: String? = null
            do {
                val response = FirebaseFunctions.getInstance().getHttpsCallable("listDataset").call(mapOf("collection" to collection, "export" to true, "cursor" to cursor)).await().data as Map<*, *>
                for (raw in response["records"] as? List<*> ?: emptyList<Any>()) {
                    val row = org.json.JSONObject(raw as Map<*, *>).put("collection", collection)
                    records.add(row)
                }
                cursor = response["cursor"] as? String
            } while (cursor != null)
        }
        val canonical = records.sortedBy { it.getString("collection") + "/" + it.getString("id") }
        val payload = canonical.joinToString("\n") { it.toString() }
        val checksum = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        when(format.uppercase()) {
            "JSONL" -> payload + if (payload.isNotEmpty()) "\n" else ""
            "CSV" -> {
                fun cell(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
                "collection,id,license,status,data_stage,created_at,published_at,record_json\n" + canonical.joinToString("\n") { row ->
                    listOf("collection","id","licenseId","status","dataStage","createdAt","publishedAt").map { cell(row.optString(it)) }.plus(cell(row.toString())).joinToString(",")
                }
            }
            "JSON" -> org.json.JSONObject().put("schema_version", "1.0").put("exported_at",System.currentTimeMillis()).put("total_records",canonical.size).put("records_jsonl_sha256", checksum).put("records",org.json.JSONArray(canonical)).toString(2)
            else -> error("Unsupported export format")
        }
    }

    private suspend fun logAudit(actorId: String, actorName: String, action: String, entityType: String, entityId: String, details: String) {
        metadataDao.insertAuditLog(AuditLog(actorId = actorId, actorName = actorName, action = action, entityType = entityType, entityId = entityId, details = details))
    }
}