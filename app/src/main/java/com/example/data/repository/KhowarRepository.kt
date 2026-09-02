package com.example.data.repository

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

    // Current Session User
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
    }

    /**
     * Public registration always creates a CONTRIBUTOR account.
     * Privileged roles must be assigned through an administrative workflow;
     * accepting a caller-supplied role here would allow privilege escalation.
     */
    suspend fun registerOrLoginUser(email: String, displayName: String, username: String, role: UserRole, region: String): User {
        return withContext(Dispatchers.IO) {
            val existing = userDao.getUserByEmail(email.trim().lowercase())
            if (existing != null) {
                _currentUser.value = existing
                existing
            } else {
                val newUser = User(
                    id = UUID.randomUUID().toString(),
                    email = email.trim().lowercase(),
                    displayName = displayName.trim(),
                    username = username.trim().ifEmpty { displayName.lowercase().replace(" ", "_") },
                    role = UserRole.CONTRIBUTOR,
                    region = region,
                    preferredLanguage = "en",
                    createdAt = System.currentTimeMillis()
                )
                userDao.insert(newUser)
                _currentUser.value = newUser
                logAudit(newUser.id, newUser.displayName, "USER_REGISTER", "USER", newUser.id, "Registered as CONTRIBUTOR")
                newUser
            }
        }
    }

    val datasetStatistics: Flow<DatasetStatistics> = combine(
        lexiconDao.countApproved(),
        sentenceDao.countApproved(),
        speechDao.countApproved(),
        speechDao.totalApprovedDurationSeconds(),
        storyDao.countApproved(),
        knowledgeDao.countApproved(),
        imageDao.countApproved(),
        userDao.countActiveContributors(),
        lexiconDao.getReviewQueue(),
        sentenceDao.getReviewQueue(),
        speechDao.getReviewQueue()
    ) { args: Array<Any?> ->
        val wordCount = (args[0] as? Int) ?: 0
        val sentenceCount = (args[1] as? Int) ?: 0
        val speechCount = (args[2] as? Int) ?: 0
        val totalDurationSec = (args[3] as? Double) ?: 0.0
        val storyCount = (args[4] as? Int) ?: 0
        val knowCount = (args[5] as? Int) ?: 0
        val imgCount = (args[6] as? Int) ?: 0
        val contributorCount = (args[7] as? Int) ?: 0
        val lexQ = (args[8] as? List<*>) ?: emptyList<Any>()
        val senQ = (args[9] as? List<*>) ?: emptyList<Any>()
        val spQ = (args[10] as? List<*>) ?: emptyList<Any>()

        DatasetStatistics(
            totalWords = wordCount,
            totalSentences = sentenceCount,
            totalSpeechRecordings = speechCount,
            totalSpeechHours = totalDurationSec / 3600.0,
            totalStories = storyCount,
            totalKnowledge = knowCount,
            totalImages = imgCount,
            totalContributors = contributorCount,
            totalApprovedRecords = wordCount + sentenceCount + speechCount + storyCount + knowCount + imgCount,
            pendingReviewCount = lexQ.size + senQ.size + spQ.size
        )
    }.flowOn(Dispatchers.IO)

    val approvedLexicon: Flow<List<LexiconEntry>> = lexiconDao.getAllApproved()
    val approvedSentences: Flow<List<SentenceEntry>> = sentenceDao.getAllApproved()
    val approvedSpeech: Flow<List<SpeechRecording>> = speechDao.getAllApproved()
    val approvedStories: Flow<List<StoryEntry>> = storyDao.getAllApproved()
    val approvedImages: Flow<List<ImageEntry>> = imageDao.getAllApproved()
    val approvedKnowledge: Flow<List<KnowledgeEntry>> = knowledgeDao.getAllApproved()

    val lexiconReviewQueue: Flow<List<LexiconEntry>> = lexiconDao.getReviewQueue()
    val sentenceReviewQueue: Flow<List<SentenceEntry>> = sentenceDao.getReviewQueue()
    val speechReviewQueue: Flow<List<SpeechRecording>> = speechDao.getReviewQueue()
    val storyReviewQueue: Flow<List<StoryEntry>> = storyDao.getReviewQueue()
    val imageReviewQueue: Flow<List<ImageEntry>> = imageDao.getReviewQueue()
    val knowledgeReviewQueue: Flow<List<KnowledgeEntry>> = knowledgeDao.getReviewQueue()

    val allDialects: Flow<List<Dialect>> = metadataDao.getAllDialects()
    val allRegions: Flow<List<Region>> = metadataDao.getAllRegions()
    val allLicenses: Flow<List<License>> = metadataDao.getAllLicenses()
    val datasetVersions: Flow<List<DatasetVersion>> = metadataDao.getDatasetVersions()
    val allAuditLogs: Flow<List<AuditLog>> = metadataDao.getAuditLogs()
    val allModerationReports: Flow<List<ModerationReport>> = metadataDao.getModerationReports()
    val allUsers: Flow<List<User>> = userDao.getAllUsers()

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

    suspend fun submitWord(
        khowarWord: String,
        transliteration: String,
        englishMeaning: String,
        urduMeaning: String,
        partOfSpeech: PartOfSpeech,
        grammaticalCategory: String,
        definition: String,
        pronunciation: String,
        exampleKhowar: String,
        exampleEnglish: String,
        dialectId: String,
        regionId: String,
        source: String,
        licenseId: String,
        isAiAssisted: Boolean,
        aiModel: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in or select a contributor profile first."))
        val normalized = KhowarNormalizer.normalizeKhowarText(khowarWord)
        if (normalized.isBlank()) return@withContext Result.failure(Exception("Khowar word cannot be empty."))
        val entry = LexiconEntry(
            khowarWord = khowarWord.trim(), normalizedKhowarWord = normalized,
            transliteration = transliteration.trim().ifEmpty { KhowarNormalizer.generateTransliterationHint(khowarWord) },
            englishMeaning = englishMeaning.trim(), urduMeaning = urduMeaning.trim(), partOfSpeech = partOfSpeech,
            grammaticalCategory = grammaticalCategory.trim(), definition = definition.trim(), pronunciation = pronunciation.trim(),
            exampleSentenceKhowar = exampleKhowar.trim(), exampleSentenceEnglish = exampleEnglish.trim(), dialectId = dialectId,
            regionId = regionId, source = source.trim(), contributorId = user.id, contributorName = user.displayName,
            status = RecordStatus.SUBMITTED, licenseId = licenseId, isAiAssisted = isAiAssisted, aiModelUsed = aiModel
        )
        lexiconDao.insert(entry)
        recordConsent(user.id, "LEXICON", entry.id, "DATASET_PUBLICATION_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_WORD", "LEXICON", entry.id, "Submitted word '${entry.khowarWord}'")
        Result.success(entry.id)
    }

    suspend fun submitSentence(
        khowarText: String, transliteration: String, englishTranslation: String, urduTranslation: String,
        context: String, dialectId: String, regionId: String, source: String, licenseId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        val normalized = KhowarNormalizer.normalizeKhowarText(khowarText)
        if (normalized.isBlank()) return@withContext Result.failure(Exception("Sentence cannot be empty."))
        val entry = SentenceEntry(
            khowarText = khowarText.trim(), normalizedText = normalized,
            transliteration = transliteration.trim().ifEmpty { KhowarNormalizer.generateTransliterationHint(khowarText) },
            englishTranslation = englishTranslation.trim(), urduTranslation = urduTranslation.trim(), context = context.trim(),
            dialectId = dialectId, regionId = regionId, source = source.trim(), contributorId = user.id,
            contributorName = user.displayName, status = RecordStatus.SUBMITTED, licenseId = licenseId
        )
        sentenceDao.insert(entry)
        recordConsent(user.id, "SENTENCE", entry.id, "DATASET_PUBLICATION_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_SENTENCE", "SENTENCE", entry.id, "Submitted sentence '${entry.khowarText.take(30)}...'")
        Result.success(entry.id)
    }

    suspend fun submitSpeech(
        speakerAgeGroup: String, speakerGender: String, isNativeSpeaker: Boolean, audioFilePath: String,
        durationSeconds: Double, transcriptKhowar: String, transliteration: String, englishTranslation: String,
        urduTranslation: String, dialectId: String, regionId: String, recordingEnvironment: String, licenseId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        if (durationSeconds <= 0.0) return@withContext Result.failure(Exception("Audio duration must be greater than zero."))
        val norm = KhowarNormalizer.normalizeKhowarText(transcriptKhowar)
        if (norm.isBlank()) return@withContext Result.failure(Exception("Speech transcript cannot be empty."))
        val entry = SpeechRecording(
            speakerPublicId = "SPK-${UUID.randomUUID().toString().take(8).uppercase()}", speakerAgeGroup = speakerAgeGroup,
            speakerGender = speakerGender, isNativeSpeaker = isNativeSpeaker, audioFilePath = audioFilePath,
            durationSeconds = durationSeconds, transcriptKhowar = transcriptKhowar.trim(), normalizedTranscript = norm,
            transliteration = transliteration.trim(), englishTranslation = englishTranslation.trim(), urduTranslation = urduTranslation.trim(),
            dialectId = dialectId, regionId = regionId, recordingEnvironment = recordingEnvironment,
            contributorId = user.id, contributorName = user.displayName, status = RecordStatus.SUBMITTED, licenseId = licenseId
        )
        speechDao.insert(entry)
        recordConsent(user.id, "SPEECH", entry.id, "VOICE_RECORDING_CONSENT_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_SPEECH", "SPEECH", entry.id, "Submitted voice recording of $durationSeconds s")
        Result.success(entry.id)
    }

    suspend fun submitStory(
        title: String, khowarText: String, transliteration: String, englishTranslation: String, urduTranslation: String,
        category: StoryCategory, authorOrSpeaker: String, dialectId: String, regionId: String, source: String, licenseId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        val entry = StoryEntry(
            title = title.trim(), khowarText = khowarText.trim(), transliteration = transliteration.trim(),
            englishTranslation = englishTranslation.trim(), urduTranslation = urduTranslation.trim(), category = category,
            authorOrSpeaker = authorOrSpeaker.trim(), dialectId = dialectId, regionId = regionId, source = source.trim(),
            contributorId = user.id, contributorName = user.displayName, status = RecordStatus.SUBMITTED, licenseId = licenseId
        )
        storyDao.insert(entry)
        recordConsent(user.id, "STORY", entry.id, "DATASET_PUBLICATION_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_STORY", "STORY", entry.id, "Submitted story '$title'")
        Result.success(entry.id)
    }

    suspend fun submitKnowledge(
        type: KnowledgeType, title: String, khowarContent: String, transliteration: String,
        englishContent: String, urduContent: String, explanation: String, source: String,
        dialectId: String, regionId: String, licenseId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        val entry = KnowledgeEntry(
            type = type, title = title.trim(), khowarContent = khowarContent.trim(), transliteration = transliteration.trim(),
            englishContent = englishContent.trim(), urduContent = urduContent.trim(), explanation = explanation.trim(),
            source = source.trim(), dialectId = dialectId, regionId = regionId, contributorId = user.id,
            contributorName = user.displayName, status = RecordStatus.SUBMITTED, licenseId = licenseId
        )
        knowledgeDao.insert(entry)
        recordConsent(user.id, "KNOWLEDGE", entry.id, "DATASET_PUBLICATION_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_KNOWLEDGE", "KNOWLEDGE", entry.id, "Submitted knowledge item '$title'")
        Result.success(entry.id)
    }

    suspend fun submitImage(
        title: String, description: String, khowarLabel: String, englishLabel: String,
        culturalContext: String, localUri: String, photographerOrSource: String, regionId: String, licenseId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        if (localUri.isBlank()) return@withContext Result.failure(Exception("Image URI cannot be empty."))
        val entry = ImageEntry(
            title = title.trim(), description = description.trim(), khowarLabel = khowarLabel.trim(), englishLabel = englishLabel.trim(),
            culturalContext = culturalContext.trim(), localUri = localUri, photographerOrSource = photographerOrSource.trim(),
            regionId = regionId, contributorId = user.id, contributorName = user.displayName,
            status = RecordStatus.SUBMITTED, licenseId = licenseId
        )
        imageDao.insert(entry)
        recordConsent(user.id, "IMAGE", entry.id, "DATASET_PUBLICATION_CC_BY_SA")
        logAudit(user.id, user.displayName, "SUBMIT_IMAGE", "IMAGE", entry.id, "Submitted image item '$title'")
        Result.success(entry.id)
    }

    suspend fun reviewRecord(
        recordType: String, recordId: String, decision: String, comments: String, confidenceScore: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val validator = _currentUser.value ?: return@withContext Result.failure(Exception("Must be signed in to validate."))
        if (validator.role != UserRole.VALIDATOR && validator.role != UserRole.ADMIN && validator.role != UserRole.SUPER_ADMIN) {
            return@withContext Result.failure(Exception("Validator role required."))
        }
        val normalizedType = recordType.trim().uppercase()
        val normalizedDecision = decision.trim().uppercase()
        if (normalizedDecision !in setOf("APPROVED", "REJECTED", "CHANGES_REQUESTED")) {
            return@withContext Result.failure(Exception("Invalid validation decision."))
        }
        if (confidenceScore !in 1..5) {
            return@withContext Result.failure(Exception("Confidence score must be between 1 and 5."))
        }
        if (validationDao.hasReviewed(normalizedType, recordId, validator.id)) {
            return@withContext Result.failure(Exception("You have already reviewed this record."))
        }

        val status = when (normalizedDecision) {
            "APPROVED" -> RecordStatus.APPROVED
            "REJECTED" -> RecordStatus.REJECTED
            else -> RecordStatus.CHANGES_REQUESTED
        }

        when (normalizedType) {
            "LEXICON" -> {
                val entry = lexiconDao.getById(recordId) ?: return@withContext Result.failure(Exception("Record not found."))
                if (entry.contributorId == validator.id) return@withContext Result.failure(Exception("Self-validation prohibited."))
                lexiconDao.update(entry.copy(status = status, publishedAt = if (status == RecordStatus.APPROVED) System.currentTimeMillis() else null, updatedAt = System.currentTimeMillis()))
            }
            "SENTENCE" -> {
                val entry = sentenceDao.getById(recordId) ?: return@withContext Result.failure(Exception("Record not found."))
                if (entry.contributorId == validator.id) return@withContext Result.failure(Exception("Self-validation prohibited."))
                sentenceDao.update(entry.copy(status = status, updatedAt = System.currentTimeMillis()))
            }
            "SPEECH" -> {
                val entry = speechDao.getById(recordId) ?: return@withContext Result.failure(Exception("Record not found."))
                if (entry.contributorId == validator.id) return@withContext Result.failure(Exception("Self-validation prohibited."))
                speechDao.update(entry.copy(status = status, qualityScore = confidenceScore.toDouble()))
            }
            "STORY" -> {
                val entry = storyDao.getById(recordId) ?: return@withContext Result.failure(Exception("Record not found."))
                if (entry.contributorId == validator.id) return@withContext Result.failure(Exception("Self-validation prohibited."))
                storyDao.update(entry.copy(status = status))
            }
            "KNOWLEDGE" -> {
                val entry = knowledgeDao.getById(recordId) ?: return@withContext Result.failure(Exception("Record not found."))
                if (entry.contributorId == validator.id) return@withContext Result.failure(Exception("Self-validation prohibited."))
                knowledgeDao.update(entry.copy(status = status))
            }
            "IMAGE" -> {
                val entry = imageDao.getById(recordId) ?: return@withContext Result.failure(Exception("Record not found."))
                if (entry.contributorId == validator.id) return@withContext Result.failure(Exception("Self-validation prohibited."))
                imageDao.update(entry.copy(status = status))
            }
            else -> return@withContext Result.failure(Exception("Unsupported record type."))
        }

        val review = ValidationReview(
            recordType = normalizedType, recordId = recordId, validatorId = validator.id,
            validatorName = validator.displayName, decision = normalizedDecision,
            comments = comments.trim(), confidenceScore = confidenceScore, createdAt = System.currentTimeMillis()
        )
        validationDao.insertReview(review)
        logAudit(validator.id, validator.displayName, "VALIDATE_$normalizedDecision", normalizedType, recordId, "Review decision: $normalizedDecision (Confidence: $confidenceScore/5)")
        Result.success(Unit)
    }

    private suspend fun recordConsent(contributorId: String, subjectType: String, subjectId: String, consentType: String) {
        consentDao.insertConsent(ConsentRecord(
            contributorId = contributorId, subjectType = subjectType, subjectId = subjectId,
            consentType = consentType, isGranted = true, grantedAt = System.currentTimeMillis()
        ))
    }

    suspend fun withdrawConsent(subjectType: String, subjectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Please sign in first."))
        val existing = consentDao.getConsentForSubject(subjectType, subjectId)
            ?: return@withContext Result.failure(Exception("No consent record found."))
        if (existing.contributorId != user.id && user.role != UserRole.ADMIN && user.role != UserRole.SUPER_ADMIN) {
            return@withContext Result.failure(Exception("Unauthorized to withdraw consent."))
        }
        consentDao.updateConsent(existing.copy(isGranted = false, withdrawnAt = System.currentTimeMillis()))
        when (subjectType.uppercase()) {
            "LEXICON" -> lexiconDao.getById(subjectId)?.let { lexiconDao.update(it.copy(status = RecordStatus.ARCHIVED)) }
            "SENTENCE" -> sentenceDao.getById(subjectId)?.let { sentenceDao.update(it.copy(status = RecordStatus.ARCHIVED)) }
            "SPEECH" -> speechDao.getById(subjectId)?.let { speechDao.update(it.copy(status = RecordStatus.ARCHIVED)) }
            "STORY" -> storyDao.getById(subjectId)?.let { storyDao.update(it.copy(status = RecordStatus.ARCHIVED)) }
            "KNOWLEDGE" -> knowledgeDao.getById(subjectId)?.let { knowledgeDao.update(it.copy(status = RecordStatus.ARCHIVED)) }
            "IMAGE" -> imageDao.getById(subjectId)?.let { imageDao.update(it.copy(status = RecordStatus.ARCHIVED)) }
            else -> return@withContext Result.failure(Exception("Unsupported consent subject type."))
        }
        logAudit(user.id, user.displayName, "WITHDRAW_CONSENT", subjectType.uppercase(), subjectId, "Contributor withdrew consent; record archived.")
        Result.success(Unit)
    }

    suspend fun generateApiKey(keyName: String): Result<Pair<String, ApiKey>> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Sign in required."))
        if (user.role != UserRole.RESEARCHER && user.role != UserRole.ADMIN && user.role != UserRole.SUPER_ADMIN) {
            return@withContext Result.failure(Exception("Researcher or administrator role required for API access."))
        }
        val cleanName = keyName.trim()
        if (cleanName.isBlank()) return@withContext Result.failure(Exception("API key name cannot be empty."))
        val rawToken = "khowar_live_" + UUID.randomUUID().toString().replace("-", "")
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(rawToken.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val key = ApiKey(
            userId = user.id, keyName = cleanName, rawKeyDisplay = rawToken, hashedKey = hash,
            rateLimitPerHour = 2500, createdAt = System.currentTimeMillis()
        )
        metadataDao.insertApiKey(key)
        logAudit(user.id, user.displayName, "GENERATE_API_KEY", "API_KEY", key.id, "Generated research API key '$cleanName'")
        Result.success(Pair(rawToken, key))
    }

    suspend fun revokeApiKey(apiKey: ApiKey): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Sign in required."))
        if (apiKey.userId != user.id && user.role != UserRole.ADMIN && user.role != UserRole.SUPER_ADMIN) {
            return@withContext Result.failure(Exception("Unauthorized to revoke this API key."))
        }
        metadataDao.updateApiKey(apiKey.copy(isRevoked = true))
        logAudit(user.id, user.displayName, "REVOKE_API_KEY", "API_KEY", apiKey.id, "Revoked research API key")
        Result.success(Unit)
    }

    suspend fun createDatasetVersion(versionNumber: String, releaseName: String, description: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Admin role required."))
        if (user.role != UserRole.ADMIN && user.role != UserRole.SUPER_ADMIN) {
            return@withContext Result.failure(Exception("Admin role required."))
        }
        val cleanVersion = versionNumber.trim()
        val cleanName = releaseName.trim()
        if (cleanVersion.isBlank() || cleanName.isBlank()) {
            return@withContext Result.failure(Exception("Version number and release name are required."))
        }

        val words = lexiconDao.getAllApproved().first()
        val sentences = sentenceDao.getAllApproved().first()
        val speech = speechDao.getAllApproved().first()
        val durationSec = speechDao.totalApprovedDurationSeconds().first() ?: 0.0
        val allRecords = words.size + sentences.size + speech.size
        val validatedRecords = allRecords - listOf(RecordStatus.SUBMITTED, RecordStatus.UNDER_REVIEW).let { 0 }
        val speakerCount = speech.map { it.speakerPublicId }.distinct().size
        val dialectCount = (words.map { it.dialectId } + sentences.map { it.dialectId } + speech.map { it.dialectId }).distinct().size

        val version = DatasetVersion(
            versionNumber = cleanVersion,
            releaseName = cleanName,
            description = description.trim(),
            recordCount = allRecords,
            speechHours = durationSec / 3600.0,
            speakerCount = speakerCount,
            dialectCount = dialectCount,
            validatedRecordCount = validatedRecords,
            researchReadyRecordCount = 0,
            license = "CC BY-SA 4.0",
            status = "PUBLISHED",
            createdBy = user.displayName,
            createdAt = System.currentTimeMillis()
        )
        metadataDao.insertDatasetVersion(version)
        logAudit(user.id, user.displayName, "CREATE_DATASET_VERSION", "VERSION", version.id, "Published dataset release $cleanVersion")
        Result.success(Unit)
    }

    suspend fun submitReport(recordType: String, recordId: String, category: String, description: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Sign in required."))
        if (recordType.isBlank() || recordId.isBlank() || description.trim().isBlank()) {
            return@withContext Result.failure(Exception("Report type, record and description are required."))
        }
        val report = ModerationReport(
            reporterId = user.id, reporterName = user.displayName, recordType = recordType.trim().uppercase(), recordId = recordId,
            category = category.trim().uppercase(), description = description.trim(), createdAt = System.currentTimeMillis()
        )
        metadataDao.insertModerationReport(report)
        logAudit(user.id, user.displayName, "SUBMIT_REPORT", report.recordType, recordId, "Reported issue: ${report.category}")
        Result.success(Unit)
    }

    /**
     * Export only records that are currently APPROVED. This method is deliberately
     * local-data oriented; external publication should use a versioned release.
     */
    suspend fun generateExport(format: String): String = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: throw IllegalStateException("Sign in required to export dataset data.")
        if (user.role != UserRole.RESEARCHER && user.role != UserRole.ADMIN && user.role != UserRole.SUPER_ADMIN) {
            throw IllegalStateException("Researcher or administrator role required for dataset export.")
        }
        val words = lexiconDao.getAllApproved().first()
        val sentences = sentenceDao.getAllApproved().first()
        val speech = speechDao.getAllApproved().first()

        fun csv(value: String): String = "\"${value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")}\""
        fun json(value: String): String = buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }

        when (format.uppercase()) {
            "CSV" -> buildString {
                append("type,id,khowar_text,transliteration,english,urdu,dialect,region,license,published_at\n")
                words.forEach { w -> append("${csv("WORD")},${csv(w.id)},${csv(w.khowarWord)},${csv(w.transliteration)},${csv(w.englishMeaning)},${csv(w.urduMeaning)},${csv(w.dialectId)},${csv(w.regionId)},${csv(w.licenseId)},${w.createdAt}\n") }
                sentences.forEach { s -> append("${csv("SENTENCE")},${csv(s.id)},${csv(s.khowarText)},${csv(s.transliteration)},${csv(s.englishTranslation)},${csv(s.urduTranslation)},${csv(s.dialectId)},${csv(s.regionId)},${csv(s.licenseId)},${s.createdAt}\n") }
            }
            "JSONL" -> buildString {
                words.forEach { w ->
                    append("{\"type\":\"word\",\"id\":${json(w.id)},\"khowar\":${json(w.khowarWord)},\"transliteration\":${json(w.transliteration)},\"english\":${json(w.englishMeaning)},\"urdu\":${json(w.urduMeaning)},\"pos\":${json(w.partOfSpeech.name)},\"dialect\":${json(w.dialectId)},\"region\":${json(w.regionId)},\"license\":${json(w.licenseId)}}\n")
                }
                sentences.forEach { s ->
                    append("{\"type\":\"sentence\",\"id\":${json(s.id)},\"khowar\":${json(s.khowarText)},\"transliteration\":${json(s.transliteration)},\"english\":${json(s.englishTranslation)},\"urdu\":${json(s.urduTranslation)},\"dialect\":${json(s.dialectId)},\"region\":${json(s.regionId)},\"license\":${json(s.licenseId)}}\n")
                }
                speech.forEach { sp ->
                    append("{\"type\":\"speech\",\"id\":${json(sp.id)},\"speaker\":${json(sp.speakerPublicId)},\"duration_s\":${sp.durationSeconds},\"transcript\":${json(sp.transcriptKhowar)},\"english\":${json(sp.englishTranslation)},\"dialect\":${json(sp.dialectId)},\"region\":${json(sp.regionId)},\"license\":${json(sp.licenseId)}}\n")
                }
            }
            else -> buildString {
                append("{\n")
                append("  \"project\": \"Khowar Dataset\",\n")
                append("  \"tagline\": \"Preserving Khowar. Powering AI. Building the Future.\",\n")
                append("  \"exported_at\": ${System.currentTimeMillis()},\n")
                append("  \"total_records\": ${words.size + sentences.size + speech.size},\n")
                append("  \"lexicon\": [\n")
                words.forEachIndexed { i, w ->
                    append("    {\"id\":${json(w.id)},\"khowar\":${json(w.khowarWord)},\"transliteration\":${json(w.transliteration)},\"english\":${json(w.englishMeaning)},\"urdu\":${json(w.urduMeaning)},\"pos\":${json(w.partOfSpeech.name)},\"dialect\":${json(w.dialectId)},\"region\":${json(w.regionId)}}${if (i < words.size - 1) "," else ""}\n")
                }
                append("  ],\n  \"sentences\": [\n")
                sentences.forEachIndexed { i, s ->
                    append("    {\"id\":${json(s.id)},\"khowar\":${json(s.khowarText)},\"transliteration\":${json(s.transliteration)},\"english\":${json(s.englishTranslation)},\"urdu\":${json(s.urduTranslation)},\"dialect\":${json(s.dialectId)},\"region\":${json(s.regionId)}}${if (i < sentences.size - 1) "," else ""}\n")
                }
                append("  ]\n}\n")
            }
        }
    }

    private suspend fun logAudit(actorId: String, actorName: String, action: String, entityType: String, entityId: String, details: String) {
        metadataDao.insertAuditLog(
            AuditLog(
                actorId = actorId, actorName = actorName, action = action,
                entityType = entityType, entityId = entityId, details = details,
                createdAt = System.currentTimeMillis()
            )
        )
    }
}
