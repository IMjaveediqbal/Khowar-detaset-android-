package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.StatusBadge
import com.example.ui.i18n.Strings
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ContributeTab
import com.example.ui.viewmodel.KhowarViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

@Composable
fun ContributeScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val activeTab by viewModel.contributeTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val dialects by viewModel.allDialects.collectAsState()
    val regions by viewModel.allRegions.collectAsState()
    val formState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()

    if (currentUser == null) {
        Column(Modifier.padding(20.dp)) { Text("Sign in to save contributions under your account."); Button(onClick={viewModel.navigateTo(AppScreen.PROFILE)}) { Text("Sign in") } }
        return
    }
    Column(modifier = modifier.fillMaxSize()) {
        // Tab Row (Word, Sentence, Voice, Story, Knowledge, Image)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Contribute Data",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "All submissions undergo human validation prior to public inclusion.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                ScrollableTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = TealAccent
                ) {
                    ContributeTab.values().forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { viewModel.setContributeTab(tab) },
                            text = {
                                Text(
                                    text = tab.name.replace("_", " "),
                                    fontSize = 12.sp,
                                    fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }

        // Active Form
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {
                ContributeTab.WORD -> item {
                    formState.SaveableStateProvider(activeTab.name) { WordContributionForm(viewModel, dialects, regions, lang) }
                }
                ContributeTab.SENTENCE -> item {
                    formState.SaveableStateProvider(activeTab.name) { SentenceContributionForm(viewModel, dialects, regions, lang) }
                }
                ContributeTab.VOICE -> item {
                    formState.SaveableStateProvider(activeTab.name) { VoiceRecordingContributionForm(viewModel, dialects, regions, lang) }
                }
                ContributeTab.STORY -> item {
                    formState.SaveableStateProvider(activeTab.name) { StoryContributionForm(viewModel, dialects, regions, lang) }
                }
                ContributeTab.KNOWLEDGE -> item {
                    formState.SaveableStateProvider(activeTab.name) { KnowledgeContributionForm(viewModel, dialects, regions, lang) }
                }
                ContributeTab.IMAGE -> item {
                    formState.SaveableStateProvider(activeTab.name) { ImageContributionForm(viewModel, regions, lang) }
                }
            }
        }
    }
}

@Composable
fun WordContributionForm(
    viewModel: KhowarViewModel,
    dialects: List<Dialect>,
    regions: List<Region>,
    lang: com.example.ui.i18n.AppLanguage
) {
    var khowarWord by rememberSaveable { mutableStateOf("") }
    var transliteration by rememberSaveable { mutableStateOf("") }
    var englishMeaning by rememberSaveable { mutableStateOf("") }
    var urduMeaning by rememberSaveable { mutableStateOf("") }
    var partOfSpeech by rememberSaveable { mutableStateOf(PartOfSpeech.NOUN) }
    var grammaticalCategory by rememberSaveable { mutableStateOf("") }
    var definition by rememberSaveable { mutableStateOf("") }
    var pronunciation by rememberSaveable { mutableStateOf("") }
    var exampleKhowar by rememberSaveable { mutableStateOf("") }
    var exampleEnglish by rememberSaveable { mutableStateOf("") }
    var selectedDialect by rememberSaveable { mutableStateOf("Central") }
    var selectedRegion by rememberSaveable { mutableStateOf("Chitral Upper") }
    var source by rememberSaveable { mutableStateOf("Native Speaker Knowledge") }
    var isConsentChecked by rememberSaveable { mutableStateOf(false) }
    var isAiAssisted by rememberSaveable { mutableStateOf(false) }

    val duplicates by viewModel.detectedDuplicates.collectAsState()
    val aiSuggestion by viewModel.aiSuggestion.collectAsState()

    var showPosMenu by rememberSaveable { mutableStateOf(false) }
    var showDialectMenu by rememberSaveable { mutableStateOf(false) }
    var showRegionMenu by rememberSaveable { mutableStateOf(false) }

    // Trigger duplicate & AI assist check
    LaunchedEffect(khowarWord, englishMeaning) {
        viewModel.checkWordDuplicate(khowarWord, englishMeaning)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("1. Lexical Entry", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TealAccent)
            Spacer(modifier = Modifier.height(10.dp))

            // Khowar Word
            OutlinedTextField(
                value = khowarWord,
                onValueChange = { khowarWord = it },
                label = { Text("Khowar Word (Arabic/Perso-Arabic Script) *") },
                placeholder = { Text("مثال: ژور، انگار، بروش...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transliteration
            OutlinedTextField(
                value = transliteration,
                onValueChange = { transliteration = it },
                label = { Text("Latin Transliteration *") },
                placeholder = { Text("e.g., zhor, angar, brosh") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // AI Suggestion Banner
            aiSuggestion?.let { ai ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Navy800,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Linguistic Suggestion (Draft)", fontSize = 10.sp, color = AmberAccent, fontWeight = FontWeight.Bold)
                            }
                            Text("Phonetic hint: /${ai.transliteration}/ • Suggested POS: ${ai.suggestedPos}", fontSize = 11.sp, color = Color.White)
                        }
                        TextButton(
                            onClick = {
                                transliteration = ai.transliteration
                                partOfSpeech = ai.suggestedPos
                                isAiAssisted = true
                            }
                        ) {
                            Text("Apply", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Duplicate Warning Banner
            if (duplicates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = AmberAccent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Similar records already found in database:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                        }
                        duplicates.forEach { dup ->
                            Text("• $dup", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("You may still submit if this represents a distinct dialectal variant or homonym.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // English & Urdu Meanings
            OutlinedTextField(
                value = englishMeaning,
                onValueChange = { englishMeaning = it },
                label = { Text("English Meaning / Translation *") },
                placeholder = { Text("e.g., daughter, fire, light...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = urduMeaning,
                onValueChange = { urduMeaning = it },
                label = { Text("Urdu Meaning (اردو معنی)") },
                placeholder = { Text("مثال: بیٹی، آگ، روشنی...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("2. Linguistic & Dialect Metadata", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TealAccent)
            Spacer(modifier = Modifier.height(8.dp))

            // Part of Speech Selector
            Box {
                OutlinedTextField(
                    value = partOfSpeech.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Part of Speech *") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPosMenu = true }
                )
                DropdownMenu(expanded = showPosMenu, onDismissRequest = { showPosMenu = false }) {
                    PartOfSpeech.values().forEach { pos ->
                        DropdownMenuItem(
                            text = { Text(pos.name) },
                            onClick = {
                                partOfSpeech = pos
                                showPosMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dialect Selector
            Box {
                OutlinedTextField(
                    value = selectedDialect,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Dialect Variety *") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDialectMenu = true }
                )
                DropdownMenu(expanded = showDialectMenu, onDismissRequest = { showDialectMenu = false }) {
                    dialects.forEach { d ->
                        DropdownMenuItem(
                            text = { Text("${d.name} (${d.nameKhowar})") },
                            onClick = {
                                selectedDialect = d.id
                                showDialectMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Example sentence in Khowar & English
            OutlinedTextField(
                value = exampleKhowar,
                onValueChange = { exampleKhowar = it },
                label = { Text("Example Sentence (Khowar)") },
                placeholder = { Text("Example in native sentence...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = exampleEnglish,
                onValueChange = { exampleEnglish = it },
                label = { Text("Example Sentence (English)") },
                placeholder = { Text("Translation of example sentence...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Consent Agreement Box
            ConsentAgreementBox(
                isConsentChecked = isConsentChecked,
                onConsentChanged = { isConsentChecked = it },
                lang = lang
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.submitWord(
                        khowarWord, transliteration, englishMeaning, urduMeaning,
                        partOfSpeech, grammaticalCategory, definition, pronunciation,
                        exampleKhowar, exampleEnglish, selectedDialect, selectedRegion,
                        source, "CC-BY-SA-4.0", isAiAssisted
                    ) {
                        khowarWord = ""
                        transliteration = ""
                        englishMeaning = ""
                        urduMeaning = ""
                        exampleKhowar = ""
                        exampleEnglish = ""
                        isConsentChecked = false
                    }
                },
                enabled = khowarWord.isNotBlank() && (englishMeaning.isNotBlank() || urduMeaning.isNotBlank()) && isConsentChecked,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.get("btn_submit", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SentenceContributionForm(
    viewModel: KhowarViewModel,
    dialects: List<Dialect>,
    regions: List<Region>,
    lang: com.example.ui.i18n.AppLanguage
) {
    var khowarText by rememberSaveable { mutableStateOf("") }
    var transliteration by rememberSaveable { mutableStateOf("") }
    var englishTranslation by rememberSaveable { mutableStateOf("") }
    var urduTranslation by rememberSaveable { mutableStateOf("") }
    var context by rememberSaveable { mutableStateOf("Everyday Conversation") }
    var selectedDialect by rememberSaveable { mutableStateOf("Central") }
    var selectedRegion by rememberSaveable { mutableStateOf("Chitral Lower") }
    var source by rememberSaveable { mutableStateOf("Fieldwork / Native Speaker") }
    var isConsentChecked by rememberSaveable { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Parallel Sentence Submission", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TealAccent)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = khowarText,
                onValueChange = { khowarText = it },
                label = { Text("Khowar Sentence *") },
                placeholder = { Text("ہسی کیا کوراک شئے؟ (What is he doing?)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = transliteration,
                onValueChange = { transliteration = it },
                label = { Text("Latin Transliteration") },
                placeholder = { Text("Hesi kya korak sher?") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = englishTranslation,
                onValueChange = { englishTranslation = it },
                label = { Text("English Translation *") },
                placeholder = { Text("What is he/she doing?") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = urduTranslation,
                onValueChange = { urduTranslation = it },
                label = { Text("Urdu Translation (اردو ترجمہ)") },
                placeholder = { Text("وہ کیا کر رہا ہے؟") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = context,
                onValueChange = { context = it },
                label = { Text("Context / Domain") },
                placeholder = { Text("e.g., Greetings, Farming, Weather, Hospitality...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ConsentAgreementBox(
                isConsentChecked = isConsentChecked,
                onConsentChanged = { isConsentChecked = it },
                lang = lang
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.submitSentence(
                        khowarText, transliteration, englishTranslation, urduTranslation,
                        context, selectedDialect, selectedRegion, source, "CC-BY-SA-4.0"
                    ) {
                        khowarText = ""
                        transliteration = ""
                        englishTranslation = ""
                        urduTranslation = ""
                        isConsentChecked = false
                    }
                },
                enabled = khowarText.isNotBlank() && (englishTranslation.isNotBlank() || urduTranslation.isNotBlank()) && isConsentChecked,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(Strings.get("btn_submit", lang), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VoiceRecordingContributionForm(
    viewModel: KhowarViewModel,
    dialects: List<Dialect>,
    regions: List<Region>,
    lang: com.example.ui.i18n.AppLanguage
) {
    val isRecording by viewModel.audioRecorder.isRecording.collectAsState()
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    var recordedAudioPath by remember { mutableStateOf<String?>(null) }
    var recordedDurationSec by rememberSaveable { mutableStateOf(0.0) }
    var elapsedDisplay by rememberSaveable { mutableStateOf(0.0) }

    var transcriptKhowar by rememberSaveable { mutableStateOf("") }
    var englishTranslation by rememberSaveable { mutableStateOf("") }
    var speakerAgeGroup by rememberSaveable { mutableStateOf("Adult (26-50)") }
    var speakerGender by rememberSaveable { mutableStateOf("Unspecified") }
    var isNativeSpeaker by rememberSaveable { mutableStateOf(true) }
    var selectedDialect by rememberSaveable { mutableStateOf("Central") }
    var selectedRegion by rememberSaveable { mutableStateOf("Chitral") }
    var isConsentChecked by rememberSaveable { mutableStateOf(false) }

    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) recordedAudioPath = viewModel.audioRecorder.startRecording()
    }
    DisposableEffect(Unit) { onDispose { if (viewModel.audioRecorder.isRecording.value) viewModel.audioRecorder.cancelRecording() } }
    // Live timer when recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            val start = System.currentTimeMillis()
            while (isRecording) {
                elapsedDisplay = (System.currentTimeMillis() - start) / 1000.0
                delay(100)
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (permissionDenied) Text("Microphone permission is needed to record audio.", color=MaterialTheme.colorScheme.error)
            Text("Speech Corpus Voice Recording", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AmberAccent)
            Text(
                text = "Capture authentic spoken Khowar for ASR/TTS and phonological research.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Record Card
            Surface(
                color = Navy900,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isRecording) CoralAccent else TealAccent.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    if (isRecording) {
                        Text("RECORDING IN PROGRESS...", color = CoralAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(String.format("%.1fs", elapsedDisplay), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                    } else if (recordedAudioPath != null) {
                        Text("Audio Captured Successfully", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(String.format("%.1f seconds", recordedDurationSec), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text("Tap microphone below to record speech clip", fontSize = 12.sp, color = Color.LightGray)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRecording && recordedAudioPath == null) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                                shape = CircleShape,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                        } else if (isRecording) {
                            Button(
                                onClick = {
                                    recordedDurationSec = viewModel.audioRecorder.stopRecording()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                                shape = CircleShape,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Navy900, modifier = Modifier.size(30.dp))
                            }
                        } else {
                            // Play & Retake Buttons
                            IconButton(
                                onClick = {
                                    recordedAudioPath?.let { path ->
                                        if (isPlaying) viewModel.audioPlayer.pauseAudio()
                                        else viewModel.audioPlayer.playAudio(path)
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                    contentDescription = "Play",
                                    tint = TealAccent,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.audioPlayer.stopAudio()
                                    recordedAudioPath = null
                                    recordedDurationSec = 0.0
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAccent)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retake")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata fields
            OutlinedTextField(
                value = transcriptKhowar,
                onValueChange = { transcriptKhowar = it },
                label = { Text("Spoken Transcript (Khowar Script) *") },
                placeholder = { Text("Enter exact spoken words in Khowar script...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = englishTranslation,
                onValueChange = { englishTranslation = it },
                label = { Text("English Translation of Speech *") },
                placeholder = { Text("English meaning of the audio...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isNativeSpeaker, onCheckedChange = { isNativeSpeaker = it })
                Spacer(modifier = Modifier.width(4.dp))
                Text("Speaker is a native Khowar speaker", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            ConsentAgreementBox(
                isConsentChecked = isConsentChecked,
                onConsentChanged = { isConsentChecked = it },
                lang = lang
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val path = recordedAudioPath ?: return@Button
                    viewModel.submitSpeech(
                        speakerAgeGroup, speakerGender, isNativeSpeaker, path,
                        recordedDurationSec, transcriptKhowar, "", englishTranslation,
                        "", selectedDialect, selectedRegion, "Quiet mobile recording",
                        "CC-BY-SA-4.0"
                    ) {
                        recordedAudioPath = null
                        transcriptKhowar = ""
                        englishTranslation = ""
                        isConsentChecked = false
                    }
                },
                enabled = !isRecording && recordedDurationSec > 0 && recordedAudioPath != null && transcriptKhowar.isNotBlank() && isConsentChecked,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Submit Voice Recording to Validation Queue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StoryContributionForm(
    viewModel: KhowarViewModel,
    dialects: List<Dialect>,
    regions: List<Region>,
    lang: com.example.ui.i18n.AppLanguage
) {
    var title by rememberSaveable { mutableStateOf("") }
    var khowarText by rememberSaveable { mutableStateOf("") }
    var englishTranslation by rememberSaveable { mutableStateOf("") }
    var authorOrSpeaker by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(StoryCategory.FOLK_TALE) }
    var isConsentChecked by rememberSaveable { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Story, Folklore & Oral Literature", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TealAccent)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Story / Text Title *") },
                placeholder = { Text("e.g., The Story of Dok Yaftali, Nan Doshi...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = khowarText,
                onValueChange = { khowarText = it },
                label = { Text("Khowar Text / Narrative *") },
                placeholder = { Text("Enter the full Khowar text...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = englishTranslation,
                onValueChange = { englishTranslation = it },
                label = { Text("English Translation *") },
                placeholder = { Text("English translation or summary...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            ConsentAgreementBox(isConsentChecked = isConsentChecked, onConsentChanged = { isConsentChecked = it }, lang = lang)

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    viewModel.submitStory(title, khowarText, "", englishTranslation, "", category, authorOrSpeaker, "Central", "Chitral", "Oral Tradition", "CC-BY-SA-4.0") {
                        title = ""
                        khowarText = ""
                        englishTranslation = ""
                        isConsentChecked = false
                    }
                },
                enabled = title.isNotBlank() && khowarText.isNotBlank() && isConsentChecked,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.get("btn_submit", lang), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun KnowledgeContributionForm(
    viewModel: KhowarViewModel,
    dialects: List<Dialect>,
    regions: List<Region>,
    lang: com.example.ui.i18n.AppLanguage
) {
    var type by rememberSaveable { mutableStateOf(KnowledgeType.PROVERB) }
    var title by rememberSaveable { mutableStateOf("") }
    var khowarContent by rememberSaveable { mutableStateOf("") }
    var explanation by rememberSaveable { mutableStateOf("") }
    var isConsentChecked by rememberSaveable { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cultural Knowledge, Proverbs & Idioms", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AmberAccent)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Concept Name *") },
                placeholder = { Text("e.g., Traditional Proverb on Hospitality") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = khowarContent,
                onValueChange = { khowarContent = it },
                label = { Text("Khowar Content / Proverb / Idiom *") },
                placeholder = { Text("مثال: مروتو سورا ڈانگ...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = explanation,
                onValueChange = { explanation = it },
                label = { Text("Cultural Meaning & Linguistic Context *") },
                placeholder = { Text("Explain when and how this idiom or cultural concept is used...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            ConsentAgreementBox(isConsentChecked = isConsentChecked, onConsentChanged = { isConsentChecked = it }, lang = lang)

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    viewModel.submitKnowledge(type, title, khowarContent, "", explanation, "", explanation, "Community Elder", "Central", "Chitral", "CC-BY-SA-4.0") {
                        title = ""
                        khowarContent = ""
                        explanation = ""
                        isConsentChecked = false
                    }
                },
                enabled = title.isNotBlank() && khowarContent.isNotBlank() && isConsentChecked,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.get("btn_submit", lang), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ImageContributionForm(
    viewModel: KhowarViewModel,
    regions: List<Region>,
    lang: com.example.ui.i18n.AppLanguage
) {
    var title by rememberSaveable { mutableStateOf("") }
    var khowarLabel by rememberSaveable { mutableStateOf("") }
    var englishLabel by rememberSaveable { mutableStateOf("") }
    var culturalContext by rememberSaveable { mutableStateOf("") }
    var isConsentChecked by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imagePath by rememberSaveable { mutableStateOf("") }
    var imageError by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val mime = context.contentResolver.getType(uri).orEmpty()
                    require(mime.startsWith("image/")) { "Select an image." }
                    val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: error("Unsupported image type")
                    val folder = java.io.File(context.filesDir,"images").apply { mkdirs() }
                    val file = java.io.File(folder,"${java.util.UUID.randomUUID()}.$extension")
                    try {
                        context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { output ->
                            val buffer=ByteArray(8192); var total=0
                            while(true) { val count=input.read(buffer); if(count<0)break;total+=count;require(total<15*1024*1024){"Image must be smaller than 15 MB"};output.write(buffer,0,count) }
                        } }
                        require(file.length()>0){"Image is empty"}
                        file.absolutePath
                    } catch(e:Exception) { file.delete();throw e }
                }
            }.onSuccess { imagePath=it;imageError="" }.onFailure { imageError=it.message.orEmpty() }
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(onClick={picker.launch("image/*")}) { Text(if(imagePath.isEmpty()) "Select image" else "Change image") }
            if(imagePath.isNotEmpty()) coil.compose.AsyncImage(model=java.io.File(imagePath),contentDescription="Selected cultural object",modifier=Modifier.fillMaxWidth().height(180.dp))
            if(imageError.isNotEmpty()) Text(imageError,color=MaterialTheme.colorScheme.error)
            Text("Visual Artifact / Cultural Object Labeling", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TealAccent)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Item / Artifact Name *") },
                placeholder = { Text("e.g., Traditional Chitrali Chugha / Cap") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = khowarLabel,
                onValueChange = { khowarLabel = it },
                label = { Text("Khowar Label *") },
                placeholder = { Text("مثال: پکوڑ، چوغہ، رباب...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = englishLabel,
                onValueChange = { englishLabel = it },
                label = { Text("English Label *") },
                placeholder = { Text("e.g., Woolen Robe, Traditional Cap, Sitar...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = culturalContext,
                onValueChange = { culturalContext = it },
                label = { Text("Cultural Context") },
                placeholder = { Text("Usage, heritage, craftsmanship...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            ConsentAgreementBox(isConsentChecked = isConsentChecked, onConsentChanged = { isConsentChecked = it }, lang = lang)

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    viewModel.submitImage(title, culturalContext, khowarLabel, englishLabel, culturalContext, imagePath, "Contributor", "Chitral", "CC-BY-SA-4.0") {
                    title = ""
                    khowarLabel = ""
                    englishLabel = ""
                    culturalContext = ""
                    isConsentChecked = false
                    imagePath = ""
                    }
                },
                enabled = imagePath.isNotEmpty() && title.isNotBlank() && khowarLabel.isNotBlank() && isConsentChecked,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.get("btn_submit", lang), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConsentAgreementBox(
    isConsentChecked: Boolean,
    onConsentChanged: (Boolean) -> Unit,
    lang: com.example.ui.i18n.AppLanguage
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Strings.get("consent_title", lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Strings.get("consent_body", lang),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onConsentChanged(!isConsentChecked) }
            ) {
                Checkbox(checked = isConsentChecked, onCheckedChange = onConsentChanged)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = Strings.get("consent_checkbox", lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
