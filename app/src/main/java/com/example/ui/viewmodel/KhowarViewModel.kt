package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAssistanceService
import com.example.ai.AiSuggestionResult
import com.example.audio.AudioPlayerHelper
import com.example.audio.AudioRecorderHelper
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.DatasetStatistics
import com.example.data.repository.KhowarRepository
import com.example.data.repository.RbacRemoteService
import com.example.ui.i18n.AppLanguage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.tasks.await
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.data.remote.DatasetSyncWorker
import com.example.data.remote.DatasetCodec

enum class AppScreen {
    HOME,
    EXPLORE,
    CONTRIBUTE,
    VALIDATE,
    STATS,
    RESEARCH,
    ADMIN,
    ADMIN_LOGIN,
    DOCS,
    PROFILE
}

enum class ExploreTab {
    ALL,
    WORDS,
    SENTENCES,
    SPEECH,
    STORIES,
    KNOWLEDGE,
    IMAGES
}

enum class ContributeTab {
    WORD,
    SENTENCE,
    VOICE,
    STORY,
    KNOWLEDGE,
    IMAGE
}

class KhowarViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = KhowarRepository(database)
    private val rbacRemoteService by lazy { RbacRemoteService() }
    private val firebaseAuth: FirebaseAuth? = if (FirebaseApp.getApps(application).isNotEmpty()) FirebaseAuth.getInstance() else null
    private val errorHandler = CoroutineExceptionHandler { _, error -> _statusMessage.value = error.message ?: "Operation failed. Please retry." }

    val audioRecorder = AudioRecorderHelper(application)
    val audioPlayer = AudioPlayerHelper()

    // Navigation & App Settings
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.entries.firstOrNull { it.code == application.getSharedPreferences("settings", 0).getString("language", "en") } ?: AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(application.getSharedPreferences("settings", 0).getBoolean("dark", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // User Profile & Authentication
    val currentUser: StateFlow<User?> = repository.currentUser

    // Dataset Live Calculated Stats
    val statistics: StateFlow<DatasetStatistics> = repository.datasetStatistics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DatasetStatistics())

    // Explore Screen State
    private val _exploreTab = MutableStateFlow(ExploreTab.ALL)
    val exploreTab: StateFlow<ExploreTab> = _exploreTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDialectFilter = MutableStateFlow("All")
    val selectedDialectFilter: StateFlow<String> = _selectedDialectFilter.asStateFlow()

    private val _selectedPosFilter = MutableStateFlow<PartOfSpeech?>(null)
    val selectedPosFilter: StateFlow<PartOfSpeech?> = _selectedPosFilter.asStateFlow()

    private data class SearchOptions(val limit:Int,val query:String,val dialect:String)
    private fun pattern(text:String) = "%" + text.replace("\\","\\\\").replace("%","\\%").replace("_","\\_") + "%"
    private val visibleLimit = MutableStateFlow(200)
    private val searchOptions = combine(visibleLimit,_searchQuery,_selectedDialectFilter) { limit,query,dialect -> SearchOptions(limit,query,dialect) }

    fun loadMoreRecords() { visibleLimit.value += 200 }

    // Data streams
    val approvedLexicon = searchOptions.flatMapLatest { options -> val latin=com.example.data.KhowarNormalizer.normalizeTransliteration(options.query); database.lexiconDao().searchApproved(pattern(com.example.data.KhowarNormalizer.normalizeKhowarText(options.query)),pattern(latin),latin.isNotEmpty(),options.dialect,options.limit) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedSentences = searchOptions.flatMapLatest { options -> val latin=com.example.data.KhowarNormalizer.normalizeTransliteration(options.query); database.sentenceDao().searchApproved(pattern(com.example.data.KhowarNormalizer.normalizeKhowarText(options.query)),pattern(latin),latin.isNotEmpty(),options.dialect,options.limit) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedSpeech = searchOptions.flatMapLatest { options -> val latin=com.example.data.KhowarNormalizer.normalizeTransliteration(options.query); database.speechDao().searchApproved(pattern(com.example.data.KhowarNormalizer.normalizeKhowarText(options.query)),pattern(latin),latin.isNotEmpty(),options.dialect,options.limit) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedStories = searchOptions.flatMapLatest { options -> val latin=com.example.data.KhowarNormalizer.normalizeTransliteration(options.query); database.storyDao().searchApproved(pattern(com.example.data.KhowarNormalizer.normalizeKhowarText(options.query)),pattern(latin),latin.isNotEmpty(),options.dialect,options.limit) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedKnowledge = searchOptions.flatMapLatest { options -> val latin=com.example.data.KhowarNormalizer.normalizeTransliteration(options.query); database.knowledgeDao().searchApproved(pattern(com.example.data.KhowarNormalizer.normalizeKhowarText(options.query)),pattern(latin),latin.isNotEmpty(),options.dialect,options.limit) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedImages = searchOptions.flatMapLatest { options -> val latin=com.example.data.KhowarNormalizer.normalizeTransliteration(options.query); database.imageDao().searchApproved(pattern(com.example.data.KhowarNormalizer.normalizeKhowarText(options.query)),pattern(latin),latin.isNotEmpty(),options.dialect,options.limit) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Validation queues
    val lexiconQueue = combine(repository.lexiconReviewQueue, currentUser) { records, user -> if (user == null) emptyList() else records.filter { it.contributorId == user.id || com.example.data.model.RbacPolicy.can(user.role, com.example.data.model.Permission.VALIDATE_COMMUNITY) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sentenceQueue = combine(repository.sentenceReviewQueue, currentUser) { records, user -> if (user == null) emptyList() else records.filter { it.contributorId == user.id || com.example.data.model.RbacPolicy.can(user.role, com.example.data.model.Permission.VALIDATE_COMMUNITY) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val speechQueue = combine(repository.speechReviewQueue, currentUser) { records, user -> if (user == null) emptyList() else records.filter { it.contributorId == user.id || com.example.data.model.RbacPolicy.can(user.role, com.example.data.model.Permission.VALIDATE_COMMUNITY) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val storyQueue = combine(repository.storyReviewQueue, currentUser) { records, user -> if (user == null) emptyList() else records.filter { it.contributorId == user.id || com.example.data.model.RbacPolicy.can(user.role, com.example.data.model.Permission.VALIDATE_COMMUNITY) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val knowledgeQueue = combine(repository.knowledgeReviewQueue, currentUser) { records, user -> if (user == null) emptyList() else records.filter { it.contributorId == user.id || com.example.data.model.RbacPolicy.can(user.role, com.example.data.model.Permission.VALIDATE_COMMUNITY) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val imageQueue = combine(repository.imageReviewQueue, currentUser) { records, user -> if (user == null) emptyList() else records.filter { it.contributorId == user.id || com.example.data.model.RbacPolicy.can(user.role, com.example.data.model.Permission.VALIDATE_COMMUNITY) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Metadata
    val allDialects = repository.allDialects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allRegions = repository.allRegions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val datasetVersions = repository.datasetVersions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAuditLogs = repository.allAuditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allUsers = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Contribution Screen State
    private val _contributeTab = MutableStateFlow(ContributeTab.WORD)
    val contributeTab: StateFlow<ContributeTab> = _contributeTab.asStateFlow()

    // Duplicate suggestions
    private val _detectedDuplicates = MutableStateFlow<List<String>>(emptyList())
    val detectedDuplicates: StateFlow<List<String>> = _detectedDuplicates.asStateFlow()

    // AI suggestions
    private val _aiSuggestion = MutableStateFlow<AiSuggestionResult?>(null)
    val aiSuggestion: StateFlow<AiSuggestionResult?> = _aiSuggestion.asStateFlow()

    // Export Data Result
    private val _exportText = MutableStateFlow<String?>(null)
    val exportText: StateFlow<String?> = _exportText.asStateFlow()

    val exportFormat = MutableStateFlow("JSONL")
    fun showStatus(message: String) { _statusMessage.value = message }

    // Generated API Key
    private val _generatedApiKey = MutableStateFlow<Pair<String, ApiKey>?>(null)
    val generatedApiKey: StateFlow<Pair<String, ApiKey>?> = _generatedApiKey.asStateFlow()

    val syncOperations = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else database.cloudDao().observe(user.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        viewModelScope.launch(errorHandler) {
            val account = auth.currentUser
            repository.setCurrentUser(null)
            if (account == null || account.isAnonymous) repository.setCurrentUser(null)
            else {
                repository.purgeOtherPrivate(account.uid)
                val cached = database.userDao().getById(account.uid)
                if (cached != null) repository.setCurrentUser(cached.copy(role = UserRole.CONTRIBUTOR))
                refreshTrustedRole()
                DatasetSyncWorker.syncNow(getApplication())
            }
        }
    }
    init { firebaseAuth?.addAuthStateListener(authListener) }

    fun authenticate(email: String, password: String, create: Boolean, name: String) {
        viewModelScope.launch(errorHandler) {
            val auth = firebaseAuth ?: error("Firebase is not configured. Add google-services.json first.")
            require(email.isNotBlank() && if (create) password.length >= 8 else password.isNotEmpty()) { "Enter your email and password. New passwords need at least 8 characters." }
            if (create) {
                if (auth.currentUser?.isAnonymous == true) auth.currentUser!!.linkWithCredential(EmailAuthProvider.getCredential(email.trim(), password)).await()
                else auth.createUserWithEmailAndPassword(email.trim(), password).await()
                repository.registerOrLoginUser(name.ifBlank { "Contributor" }, "", "Chitral")
            } else auth.signInWithEmailAndPassword(email.trim(), password).await()
            refreshTrustedRole()
            _statusMessage.value = "Signed in successfully."
        }
    }

    fun authenticateAdmin(email: String, password: String) {
        viewModelScope.launch(errorHandler) {
            val auth = firebaseAuth ?: error("Firebase is not configured. Add google-services.json first.")
            require(email.isNotBlank() && password.isNotEmpty()) { "Enter the administrator email and password." }
            val account = auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await().user
                ?: error("Administrator sign-in failed.")
            val (uid, roleName) = rbacRemoteService.getMyRbac().getOrThrow()
            val role = runCatching { UserRole.valueOf(roleName) }.getOrDefault(UserRole.CONTRIBUTOR)
            if (uid != account.uid || role !in setOf(UserRole.ADMIN, UserRole.SUPER_ADMIN)) {
                auth.signOut()
                repository.setCurrentUser(null)
                error("This link is only for administrator accounts.")
            }
            loadTrustedUser(account, role)
            _currentScreen.value = AppScreen.ADMIN
            _statusMessage.value = "Administrator signed in successfully."
        }
    }
    fun signOut() {
        _exportText.value = null
        _generatedApiKey.value = null
        audioPlayer.stopAudio()
        firebaseAuth?.signOut()
        repository.setCurrentUser(null)
        _currentScreen.value = AppScreen.PROFILE
    }
    fun refreshTrustedRole() {
        viewModelScope.launch(errorHandler) {
            val account = firebaseAuth?.currentUser ?: return@launch
            if (account.isAnonymous) return@launch
            val (uid, roleName) = rbacRemoteService.getMyRbac().getOrThrow()
            require(uid == account.uid) { "Account changed. Please retry." }
            loadTrustedUser(account, UserRole.valueOf(roleName))
        }
    }

    private suspend fun loadTrustedUser(account: com.google.firebase.auth.FirebaseUser, role: UserRole) {
        val uid = account.uid
        val remote = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
        if (firebaseAuth?.currentUser?.uid != uid) return
        val cached = database.userDao().getById(uid)
        val user = (cached ?: User(
            id = uid,
            email = account.email.orEmpty(),
            displayName = remote.getString("displayName") ?: account.displayName ?: "Contributor",
            username = remote.getString("username").orEmpty(),
            region = remote.getString("region") ?: "Chitral"
        )).copy(
            email = account.email.orEmpty(),
            displayName = remote.getString("displayName") ?: cached?.displayName ?: account.displayName ?: "Contributor",
            username = remote.getString("username") ?: cached?.username.orEmpty(),
            region = remote.getString("region") ?: cached?.region ?: "Chitral",
            bio = remote.getString("bio") ?: cached?.bio.orEmpty(),
            role = role
        )
        database.userDao().insert(user)
        repository.setCurrentUser(user)
        DatasetSyncWorker.syncNow(getApplication())
    }
    fun retrySync() { DatasetSyncWorker.syncNow(getApplication()) }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        getApplication<Application>().getSharedPreferences("settings", 0).edit().putString("language", language.code).apply()
    }

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
        getApplication<Application>().getSharedPreferences("settings", 0).edit().putBoolean("dark", _isDarkTheme.value).apply()
    }

    fun setExploreTab(tab: ExploreTab) {
        _exploreTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDialectFilter(dialect: String) {
        _selectedDialectFilter.value = dialect
    }

    fun setPosFilter(pos: PartOfSpeech?) {
        _selectedPosFilter.value = pos
    }

    fun setContributeTab(tab: ContributeTab) {
        _contributeTab.value = tab
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Legacy UI hook retained for compatibility. Roles cannot be changed locally anymore.
     * The server must assign the role through Firebase custom claims.
     */
    fun switchUserRole(role: UserRole) {
        _statusMessage.value = "Role switching is disabled. Roles are assigned by the server. Requested: $role"
        refreshTrustedRole()
    }

    /** Assign a role through the trusted backend. The local Room role is only a cache. */
    fun updateUserRole(userId: String, role: UserRole, reason: String = "Role assignment approved by administrator") {
        viewModelScope.launch(errorHandler) {
            if (userId.isBlank()) {
                _statusMessage.value = "Role update failed: target user is required."
                return@launch
            }
            val res = rbacRemoteService.setUserRole(userId, role.name, reason)
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                val user = database.userDao().getById(userId)
                if (user != null) {
                    database.userDao().update(user.copy(role = role))
                    if (currentUser.value?.id == userId) {
                        repository.setCurrentUser(user.copy(role = role))
                    }
                }
                _statusMessage.value = "Role change accepted by the server for $userId."
                if (currentUser.value?.id == userId) refreshTrustedRole()
            }.onFailure {
                _statusMessage.value = "Role update rejected: ${it.message}"
            }
        }
    }

    fun submitReport(recordType: String, recordId: String, category: String, description: String) {
        viewModelScope.launch(errorHandler) {
            val res = repository.submitReport(recordType, recordId, category, description)
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Moderation report submitted for review."
            }.onFailure {
                _statusMessage.value = "Report error: ${it.message}"
            }
        }
    }

    fun submitImage(
        title: String,
        description: String,
        khowarLabel: String,
        englishLabel: String,
        culturalContext: String,
        localUri: String,
        photographerOrSource: String,
        regionId: String,
        licenseId: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(errorHandler) {
            val res = repository.submitImage(title, description, khowarLabel, englishLabel, culturalContext, localUri, photographerOrSource, regionId, licenseId)
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Image saved locally; upload queued."
                onSuccess()
            }.onFailure {
                _statusMessage.value = "Submission failed: ${it.message}"
            }
        }
    }

    /** Sign-in/registration compatibility hook; profile creation has no client role input. */
    fun loginOrRegister(name: String, username: String, region: String) {
        viewModelScope.launch(errorHandler) {
            val user = repository.registerOrLoginUser(name, username, region)
            _statusMessage.value = "Profile saved for ${user.displayName}."
            refreshTrustedRole()
        }
    }

    fun checkWordDuplicate(khowarWord: String, englishMeaning: String) {
        viewModelScope.launch(errorHandler) {
            if (khowarWord.length >= 2) {
                val duplicates = repository.checkLexiconDuplicate(khowarWord)
                _detectedDuplicates.value = duplicates.map { "${it.khowarWord} (${it.transliteration}) - ${it.englishMeaning}" }
                _aiSuggestion.value = AiAssistanceService.suggestTransliterationAndGrammar(khowarWord, englishMeaning)
            } else {
                _detectedDuplicates.value = emptyList()
                _aiSuggestion.value = null
            }
        }
    }

    fun submitWord(
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
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(errorHandler) {
            val res = repository.submitWord(
                khowarWord, transliteration, englishMeaning, urduMeaning, partOfSpeech,
                grammaticalCategory, definition, pronunciation, exampleKhowar, exampleEnglish,
                dialectId, regionId, source, licenseId, isAiAssisted,
                if (isAiAssisted) "local-heuristic-v1" else ""
            )
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Lexicon entry submitted successfully for human validation!"
                _detectedDuplicates.value = emptyList()
                _aiSuggestion.value = null
                onSuccess()
            }.onFailure {
                _statusMessage.value = "Submission failed: ${it.message}"
            }
        }
    }

    fun submitSentence(
        khowarText: String,
        transliteration: String,
        englishTranslation: String,
        urduTranslation: String,
        context: String,
        dialectId: String,
        regionId: String,
        source: String,
        licenseId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(errorHandler) {
            val res = repository.submitSentence(
                khowarText, transliteration, englishTranslation, urduTranslation,
                context, dialectId, regionId, source, licenseId
            )
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Sentence submitted for human validation!"
                onSuccess()
            }.onFailure {
                _statusMessage.value = "Submission error: ${it.message}"
            }
        }
    }

    fun submitSpeech(
        speakerAgeGroup: String,
        speakerGender: String,
        isNativeSpeaker: Boolean,
        audioFilePath: String,
        durationSeconds: Double,
        transcriptKhowar: String,
        transliteration: String,
        englishTranslation: String,
        urduTranslation: String,
        dialectId: String,
        regionId: String,
        recordingEnvironment: String,
        licenseId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(errorHandler) {
            val res = repository.submitSpeech(
                speakerAgeGroup, speakerGender, isNativeSpeaker, audioFilePath,
                durationSeconds, transcriptKhowar, transliteration, englishTranslation,
                urduTranslation, dialectId, regionId, recordingEnvironment, licenseId
            )
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Speech recording submitted to validation queue!"
                onSuccess()
            }.onFailure {
                _statusMessage.value = "Submission failed: ${it.message}"
            }
        }
    }

    fun submitStory(
        title: String,
        khowarText: String,
        transliteration: String,
        englishTranslation: String,
        urduTranslation: String,
        category: StoryCategory,
        authorOrSpeaker: String,
        dialectId: String,
        regionId: String,
        source: String,
        licenseId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(errorHandler) {
            val res = repository.submitStory(
                title, khowarText, transliteration, englishTranslation, urduTranslation,
                category, authorOrSpeaker, dialectId, regionId, source, licenseId
            )
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Cultural text submitted for verification!"
                onSuccess()
            }.onFailure {
                _statusMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun submitKnowledge(
        type: KnowledgeType,
        title: String,
        khowarContent: String,
        transliteration: String,
        englishContent: String,
        urduContent: String,
        explanation: String,
        source: String,
        dialectId: String,
        regionId: String,
        licenseId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch(errorHandler) {
            val res = repository.submitKnowledge(
                type, title, khowarContent, transliteration, englishContent,
                urduContent, explanation, source, dialectId, regionId, licenseId
            )
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Cultural knowledge entry submitted!"
                onSuccess()
            }.onFailure {
                _statusMessage.value = "Failed: ${it.message}"
            }
        }
    }

    fun submitValidationDecision(
        recordType: String,
        recordId: String,
        decision: String,
        comments: String,
        confidenceScore: Int,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch(errorHandler) {
            val res = repository.reviewRecord(recordType, recordId, decision, comments, confidenceScore)
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Record $decision successfully! Audit log recorded."
                onComplete()
            }.onFailure {
                _statusMessage.value = "Validation error: ${it.message}"
            }
        }
    }

    fun transitionDataStage(
        collection: String,
        recordId: String,
        targetStage: String,
        comments: String = "",
        confidenceScore: Int = 0,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(errorHandler) {
            val res = rbacRemoteService.transitionDataStage(
                collection = DatasetCodec.collection(collection),
                recordId = recordId,
                targetStage = targetStage,
                comments = comments,
                confidenceScore = confidenceScore
            )
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Dataset stage advanced to ${targetStage.uppercase()}."
                onComplete()
            }.onFailure {
                _statusMessage.value = "Stage transition rejected: ${it.message}"
            }
        }
    }

    fun generateApiKey(keyName: String) {
        viewModelScope.launch(errorHandler) {
            val res = repository.generateApiKey(keyName)
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _generatedApiKey.value = it
                _statusMessage.value = "New researcher API key generated."
            }.onFailure {
                _statusMessage.value = "Failed to generate key: ${it.message}"
            }
        }
    }

    fun generateDatasetExport(format: String) {
        viewModelScope.launch(errorHandler) {
            val output = repository.generateExport(format)
            exportFormat.value = format
            _exportText.value = output
            _statusMessage.value = "Generated $format dataset package (${output.lines().size} lines)"
        }
    }

    fun clearExportText() {
        _exportText.value = null
    }

    fun createDatasetRelease(version: String, name: String, desc: String, onSuccess: () -> Unit) {
        viewModelScope.launch(errorHandler) {
            val res = repository.createDatasetVersion(version, name, desc)
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Dataset release $version created as a draft."
                onSuccess()
            }.onFailure {
                _statusMessage.value = "Release error: ${it.message}"
            }
        }
    }

    fun withdrawConsent(subjectType: String, subjectId: String) {
        viewModelScope.launch(errorHandler) {
            val res = repository.withdrawConsent(subjectType, subjectId)
            res.onSuccess {
                DatasetSyncWorker.syncNow(getApplication())
                _statusMessage.value = "Consent withdrawn. Record unpublished and archived."
            }.onFailure {
                _statusMessage.value = "Consent withdrawal error: ${it.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.cancelRecording()
        audioPlayer.stopAudio()
        firebaseAuth?.removeAuthStateListener(authListener)
    }
}
