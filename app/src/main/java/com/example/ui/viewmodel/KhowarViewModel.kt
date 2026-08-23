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
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    EXPLORE,
    CONTRIBUTE,
    VALIDATE,
    STATS,
    RESEARCH,
    ADMIN,
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

    val audioRecorder = AudioRecorderHelper(application)
    val audioPlayer = AudioPlayerHelper()

    // Navigation & App Settings
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
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

    // Data streams
    val approvedLexicon = repository.approvedLexicon.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedSentences = repository.approvedSentences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedSpeech = repository.approvedSpeech.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedStories = repository.approvedStories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedKnowledge = repository.approvedKnowledge.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val approvedImages = repository.approvedImages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Validation queues
    val lexiconQueue = repository.lexiconReviewQueue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sentenceQueue = repository.sentenceReviewQueue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val speechQueue = repository.speechReviewQueue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val storyQueue = repository.storyReviewQueue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val knowledgeQueue = repository.knowledgeReviewQueue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val imageQueue = repository.imageReviewQueue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Generated API Key
    private val _generatedApiKey = MutableStateFlow<Pair<String, ApiKey>?>(null)
    val generatedApiKey: StateFlow<Pair<String, ApiKey>?> = _generatedApiKey.asStateFlow()

    init {
        // Auto-initialize demo session if empty so user can test all roles immediately
        viewModelScope.launch {
            repository.registerOrLoginUser(
                email = "researcher@khowar-dataset.org",
                displayName = "Prof. Chitrali Linguist",
                username = "chitrali_linguist",
                role = UserRole.VALIDATOR,
                region = "Chitral Upper"
            )
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
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

    fun switchUserRole(role: UserRole) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(role = role)
            database.userDao().update(updated)
            repository.setCurrentUser(updated)
            _statusMessage.value = "Active profile switched to role: $role"
        }
    }

    fun updateUserRole(userId: String, role: UserRole) {
        viewModelScope.launch {
            val user = database.userDao().getById(userId)
            if (user != null) {
                val updated = user.copy(role = role)
                database.userDao().update(updated)
                if (currentUser.value?.id == userId) {
                    repository.setCurrentUser(updated)
                }
                _statusMessage.value = "Updated role for ${user.displayName} to $role"
            }
        }
    }

    fun submitReport(recordType: String, recordId: String, category: String, description: String) {
        viewModelScope.launch {
            val res = repository.submitReport(recordType, recordId, category, description)
            res.onSuccess {
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
        licenseId: String
    ) {
        viewModelScope.launch {
            val res = repository.submitImage(title, description, khowarLabel, englishLabel, culturalContext, localUri, photographerOrSource, regionId, licenseId)
            res.onSuccess {
                _statusMessage.value = "Visual object label submitted for validation!"
            }.onFailure {
                _statusMessage.value = "Submission failed: ${it.message}"
            }
        }
    }

    fun loginOrRegister(email: String, name: String, username: String, role: UserRole, region: String) {
        viewModelScope.launch {
            val user = repository.registerOrLoginUser(email, name, username, role, region)
            _statusMessage.value = "Signed in as ${user.displayName} (${user.role})"
        }
    }

    fun checkWordDuplicate(khowarWord: String, englishMeaning: String) {
        viewModelScope.launch {
            if (khowarWord.length >= 2) {
                val duplicates = repository.checkLexiconDuplicate(khowarWord)
                _detectedDuplicates.value = duplicates.map { "${it.khowarWord} (${it.transliteration}) - ${it.englishMeaning}" }
                
                // AI Linguistic Assist
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
        viewModelScope.launch {
            val res = repository.submitWord(
                khowarWord, transliteration, englishMeaning, urduMeaning, partOfSpeech,
                grammaticalCategory, definition, pronunciation, exampleKhowar, exampleEnglish,
                dialectId, regionId, source, licenseId, isAiAssisted,
                if (isAiAssisted) "LinguisticAI-Khowar-Assist-v1" else ""
            )
            res.onSuccess {
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
        viewModelScope.launch {
            val res = repository.submitSentence(
                khowarText, transliteration, englishTranslation, urduTranslation,
                context, dialectId, regionId, source, licenseId
            )
            res.onSuccess {
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
        viewModelScope.launch {
            val res = repository.submitSpeech(
                speakerAgeGroup, speakerGender, isNativeSpeaker, audioFilePath,
                durationSeconds, transcriptKhowar, transliteration, englishTranslation,
                urduTranslation, dialectId, regionId, recordingEnvironment, licenseId
            )
            res.onSuccess {
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
        viewModelScope.launch {
            val res = repository.submitStory(
                title, khowarText, transliteration, englishTranslation, urduTranslation,
                category, authorOrSpeaker, dialectId, regionId, source, licenseId
            )
            res.onSuccess {
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
        viewModelScope.launch {
            val res = repository.submitKnowledge(
                type, title, khowarContent, transliteration, englishContent,
                urduContent, explanation, source, dialectId, regionId, licenseId
            )
            res.onSuccess {
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
        viewModelScope.launch {
            val res = repository.reviewRecord(recordType, recordId, decision, comments, confidenceScore)
            res.onSuccess {
                _statusMessage.value = "Record $decision successfully! Audit log recorded."
                onComplete()
            }.onFailure {
                _statusMessage.value = "Validation error: ${it.message}"
            }
        }
    }

    fun generateApiKey(keyName: String) {
        viewModelScope.launch {
            val res = repository.generateApiKey(keyName)
            res.onSuccess {
                _generatedApiKey.value = it
                _statusMessage.value = "New researcher API key generated."
            }.onFailure {
                _statusMessage.value = "Failed to generate key: ${it.message}"
            }
        }
    }

    fun generateDatasetExport(format: String) {
        viewModelScope.launch {
            val output = repository.generateExport(format)
            _exportText.value = output
            _statusMessage.value = "Generated $format dataset package (${output.lines().size} lines)"
        }
    }

    fun clearExportText() {
        _exportText.value = null
    }

    fun createDatasetRelease(version: String, name: String, desc: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = repository.createDatasetVersion(version, name, desc)
            res.onSuccess {
                _statusMessage.value = "Dataset release $version published!"
                onSuccess()
            }.onFailure {
                _statusMessage.value = "Release error: ${it.message}"
            }
        }
    }

    fun withdrawConsent(subjectType: String, subjectId: String) {
        viewModelScope.launch {
            val res = repository.withdrawConsent(subjectType, subjectId)
            res.onSuccess {
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
    }
}
