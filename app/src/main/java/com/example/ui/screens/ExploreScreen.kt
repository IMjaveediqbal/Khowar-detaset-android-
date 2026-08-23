package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import com.example.data.KhowarNormalizer
import com.example.data.model.*
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.i18n.Strings
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ExploreTab
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun ExploreScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val currentTab by viewModel.exploreTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDialect by viewModel.selectedDialectFilter.collectAsState()
    val dialects by viewModel.allDialects.collectAsState()

    val words by viewModel.approvedLexicon.collectAsState()
    val sentences by viewModel.approvedSentences.collectAsState()
    val speech by viewModel.approvedSpeech.collectAsState()
    val stories by viewModel.approvedStories.collectAsState()
    val knowledge by viewModel.approvedKnowledge.collectAsState()

    var selectedRecordDetails by remember { mutableStateOf<Any?>(null) }
    var reportDialogRecord by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Normalize search query for multilingual matching
    val normQuery = KhowarNormalizer.normalizeKhowarText(searchQuery).lowercase()
    val latinQuery = KhowarNormalizer.normalizeTransliteration(searchQuery)

    // Filtered lists
    val filteredWords = remember(words, searchQuery, selectedDialect) {
        words.filter { w ->
            (selectedDialect == "All" || w.dialectId.equals(selectedDialect, ignoreCase = true)) &&
            (normQuery.isEmpty() ||
             w.normalizedKhowarWord.contains(normQuery) ||
             w.transliteration.lowercase().contains(latinQuery) ||
             w.englishMeaning.lowercase().contains(latinQuery) ||
             w.urduMeaning.contains(searchQuery))
        }
    }

    val filteredSentences = remember(sentences, searchQuery, selectedDialect) {
        sentences.filter { s ->
            (selectedDialect == "All" || s.dialectId.equals(selectedDialect, ignoreCase = true)) &&
            (normQuery.isEmpty() ||
             s.normalizedText.contains(normQuery) ||
             s.transliteration.lowercase().contains(latinQuery) ||
             s.englishTranslation.lowercase().contains(latinQuery) ||
             s.urduTranslation.contains(searchQuery))
        }
    }

    val filteredSpeech = remember(speech, searchQuery, selectedDialect) {
        speech.filter { sp ->
            (selectedDialect == "All" || sp.dialectId.equals(selectedDialect, ignoreCase = true)) &&
            (normQuery.isEmpty() ||
             sp.normalizedTranscript.contains(normQuery) ||
             sp.transliteration.lowercase().contains(latinQuery) ||
             sp.englishTranslation.lowercase().contains(latinQuery))
        }
    }

    val filteredStories = remember(stories, searchQuery, selectedDialect) {
        stories.filter { st ->
            (selectedDialect == "All" || st.dialectId.equals(selectedDialect, ignoreCase = true)) &&
            (normQuery.isEmpty() ||
             st.title.lowercase().contains(latinQuery) ||
             st.khowarText.contains(searchQuery) ||
             st.englishTranslation.lowercase().contains(latinQuery))
        }
    }

    val filteredKnowledge = remember(knowledge, searchQuery, selectedDialect) {
        knowledge.filter { k ->
            (selectedDialect == "All" || k.dialectId.equals(selectedDialect, ignoreCase = true)) &&
            (normQuery.isEmpty() ||
             k.title.lowercase().contains(latinQuery) ||
             k.khowarContent.contains(searchQuery) ||
             k.englishContent.lowercase().contains(latinQuery))
        }
    }

    val totalRecords = filteredWords.size + filteredSentences.size + filteredSpeech.size + filteredStories.size + filteredKnowledge.size

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Filters Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search Khowar, Transliteration, English, Urdu...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TealAccent) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dialect Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedDialect == "All",
                            onClick = { viewModel.setDialectFilter("All") },
                            label = { Text("All Dialects") }
                        )
                    }
                    items(dialects) { dialect ->
                        FilterChip(
                            selected = selectedDialect == dialect.id,
                            onClick = { viewModel.setDialectFilter(dialect.id) },
                            label = { Text(dialect.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Sub-tabs (All, Words, Sentences, Speech, Stories, Knowledge)
                ScrollableTabRow(
                    selectedTabIndex = currentTab.ordinal,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = TealAccent
                ) {
                    ExploreTab.values().forEach { tab ->
                        Tab(
                            selected = currentTab == tab,
                            onClick = { viewModel.setExploreTab(tab) },
                            text = {
                                val count = when (tab) {
                                    ExploreTab.ALL -> totalRecords
                                    ExploreTab.WORDS -> filteredWords.size
                                    ExploreTab.SENTENCES -> filteredSentences.size
                                    ExploreTab.SPEECH -> filteredSpeech.size
                                    ExploreTab.STORIES -> filteredStories.size
                                    ExploreTab.KNOWLEDGE -> filteredKnowledge.size
                                    ExploreTab.IMAGES -> 0
                                }
                                Text("${tab.name} ($count)", fontSize = 12.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        )
                    }
                }
            }
        }

        // List Content
        if (totalRecords == 0) {
            EmptyStateView(
                title = Strings.get("empty_no_verified", lang),
                subtitle = "Be the first to contribute and grow the verified Khowar linguistic repository.",
                icon = Icons.Outlined.SearchOff,
                actionText = Strings.get("btn_contribute", lang),
                onAction = { viewModel.navigateTo(AppScreen.CONTRIBUTE) }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Lexicon entries
                if (currentTab == ExploreTab.ALL || currentTab == ExploreTab.WORDS) {
                    items(filteredWords) { word ->
                        LexiconItemCard(
                            entry = word,
                            onClick = { selectedRecordDetails = word }
                        )
                    }
                }

                // Sentence entries
                if (currentTab == ExploreTab.ALL || currentTab == ExploreTab.SENTENCES) {
                    items(filteredSentences) { sentence ->
                        SentenceItemCard(
                            entry = sentence,
                            onClick = { selectedRecordDetails = sentence }
                        )
                    }
                }

                // Speech entries
                if (currentTab == ExploreTab.ALL || currentTab == ExploreTab.SPEECH) {
                    items(filteredSpeech) { sp ->
                        SpeechItemCard(
                            entry = sp,
                            viewModel = viewModel,
                            onClick = { selectedRecordDetails = sp }
                        )
                    }
                }

                // Story entries
                if (currentTab == ExploreTab.ALL || currentTab == ExploreTab.STORIES) {
                    items(filteredStories) { story ->
                        StoryItemCard(
                            entry = story,
                            onClick = { selectedRecordDetails = story }
                        )
                    }
                }

                // Knowledge entries
                if (currentTab == ExploreTab.ALL || currentTab == ExploreTab.KNOWLEDGE) {
                    items(filteredKnowledge) { k ->
                        KnowledgeItemCard(
                            entry = k,
                            onClick = { selectedRecordDetails = k }
                        )
                    }
                }
            }
        }
    }

    // Detail Modal Dialog
    selectedRecordDetails?.let { record ->
        RecordDetailDialog(
            record = record,
            onDismiss = { selectedRecordDetails = null },
            onReport = { type, id ->
                reportDialogRecord = Pair(type, id)
                selectedRecordDetails = null
            }
        )
    }

    // Moderation Report Dialog
    reportDialogRecord?.let { (type, id) ->
        ReportSubmissionDialog(
            recordType = type,
            recordId = id,
            viewModel = viewModel,
            onDismiss = { reportDialogRecord = null }
        )
    }
}

@Composable
fun LexiconItemCard(entry: LexiconEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.khowarWord,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "/${entry.transliteration}/",
                        fontSize = 13.sp,
                        color = TealAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
                StatusBadge(status = entry.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "EN: ${entry.englishMeaning}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            if (entry.urduMeaning.isNotEmpty()) {
                Text(
                    text = "UR: ${entry.urduMeaning}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Navy800,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = entry.partOfSpeech.name,
                        fontSize = 9.sp,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Dialect: ${entry.dialectId}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (entry.isAiAssisted) {
                    Surface(
                        color = AmberAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "AI-Assisted Suggestion",
                            fontSize = 9.sp,
                            color = AmberAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SentenceItemCard(entry: SentenceEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("PARALLEL SENTENCE", fontSize = 9.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                StatusBadge(status = entry.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.khowarText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = entry.transliteration,
                fontSize = 12.sp,
                color = TealAccent
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "EN: ${entry.englishTranslation}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (entry.urduTranslation.isNotEmpty()) {
                Text(
                    text = "UR: ${entry.urduTranslation}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SpeechItemCard(entry: SpeechRecording, viewModel: KhowarViewModel, onClick: () -> Unit) {
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    var isCurrentPlayingThis by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SPEECH • ${entry.speakerPublicId}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                }
                StatusBadge(status = entry.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.transcriptKhowar,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "EN: ${entry.englishTranslation}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Audio Player Bar
            Surface(
                color = Navy800,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isCurrentPlayingThis && isPlaying) {
                                viewModel.audioPlayer.pauseAudio()
                            } else {
                                isCurrentPlayingThis = true
                                viewModel.audioPlayer.playAudio(entry.audioFilePath) {
                                    isCurrentPlayingThis = false
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isCurrentPlayingThis && isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                            contentDescription = "Play",
                            tint = TealAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = String.format("%.1fs • %s • %s", entry.durationSeconds, entry.format, entry.speakerAgeGroup),
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StoryItemCard(entry: StoryEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color(0xFFC77DFF).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = entry.category.name.replace("_", " "),
                        fontSize = 9.sp,
                        color = Color(0xFFC77DFF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                StatusBadge(status = entry.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.khowarText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
fun KnowledgeItemCard(entry: KnowledgeEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = AmberAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = entry.type.name.replace("_", " "),
                        fontSize = 9.sp,
                        color = AmberAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                StatusBadge(status = entry.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = entry.khowarContent,
                fontSize = 14.sp,
                color = TealAccent
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.explanation,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
fun RecordDetailDialog(
    record: Any,
    onDismiss: () -> Unit,
    onReport: (String, String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Record Metadata",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 10.dp))
                }

                when (record) {
                    is LexiconEntry -> {
                        item {
                            DetailField("Khowar Word", record.khowarWord, isPrimary = true)
                            DetailField("Transliteration", "/${record.transliteration}/")
                            DetailField("English Meaning", record.englishMeaning)
                            DetailField("Urdu Meaning", record.urduMeaning)
                            DetailField("Part of Speech", record.partOfSpeech.name)
                            DetailField("Dialect", record.dialectId)
                            DetailField("Region", record.regionId)
                            DetailField("Source / Provenance", record.source)
                            DetailField("License", record.licenseId)
                            DetailField("Status", record.status.name)
                            DetailField("Citation Key", "KhowarDataset:${record.id.take(8)}")
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { onReport("LEXICON", record.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Report Record (Inaccuracy/Copyright/Privacy)", fontSize = 11.sp)
                            }
                        }
                    }
                    is SentenceEntry -> {
                        item {
                            DetailField("Khowar Text", record.khowarText, isPrimary = true)
                            DetailField("Transliteration", record.transliteration)
                            DetailField("English Translation", record.englishTranslation)
                            DetailField("Urdu Translation", record.urduTranslation)
                            DetailField("Dialect", record.dialectId)
                            DetailField("Context", record.context)
                            DetailField("License", record.licenseId)
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { onReport("SENTENCE", record.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Report Issue", fontSize = 11.sp)
                            }
                        }
                    }
                    is SpeechRecording -> {
                        item {
                            DetailField("Speaker ID", record.speakerPublicId)
                            DetailField("Age Group / Gender", "${record.speakerAgeGroup} • ${record.speakerGender}")
                            DetailField("Duration", String.format("%.2f seconds", record.durationSeconds))
                            DetailField("Transcript", record.transcriptKhowar, isPrimary = true)
                            DetailField("English Translation", record.englishTranslation)
                            DetailField("Dialect", record.dialectId)
                            DetailField("License", record.licenseId)
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { onReport("SPEECH", record.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Report Issue", fontSize = 11.sp)
                            }
                        }
                    }
                    is StoryEntry -> {
                        item {
                            DetailField("Title", record.title, isPrimary = true)
                            DetailField("Category", record.category.name)
                            DetailField("Khowar Text", record.khowarText)
                            DetailField("English Translation", record.englishTranslation)
                            DetailField("Author / Speaker", record.authorOrSpeaker)
                            DetailField("Dialect", record.dialectId)
                        }
                    }
                    is KnowledgeEntry -> {
                        item {
                            DetailField("Title", record.title, isPrimary = true)
                            DetailField("Category", record.type.name)
                            DetailField("Khowar Content", record.khowarContent)
                            DetailField("Explanation", record.explanation)
                            DetailField("Dialect", record.dialectId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailField(label: String, value: String, isPrimary: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value.ifEmpty { "Not specified" },
            fontSize = if (isPrimary) 18.sp else 13.sp,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ReportSubmissionDialog(
    recordType: String,
    recordId: String,
    viewModel: KhowarViewModel,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf("INACCURATE_TRANSLATION") }
    var description by remember { mutableStateOf("") }
    val categories = listOf(
        "INACCURATE_TRANSLATION",
        "COPYRIGHT_VIOLATION",
        "PRIVACY_CONCERN",
        "OFFENSIVE_CONTENT",
        "DUPLICATE_ENTRY"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Submit Moderation Report",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Report Category:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                categories.forEach { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { category = cat }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = category == cat, onClick = { category = cat })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(cat.replace("_", " "), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details & Explanation") },
                    placeholder = { Text("Describe the issue for human moderators...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.submitReport(recordType, recordId, category, description)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                    ) {
                        Text("Submit Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
