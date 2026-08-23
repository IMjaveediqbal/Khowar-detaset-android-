package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.i18n.Strings
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun ValidateScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val lexiconQueue by viewModel.lexiconQueue.collectAsState()
    val sentenceQueue by viewModel.sentenceQueue.collectAsState()
    val speechQueue by viewModel.speechQueue.collectAsState()
    val storyQueue by viewModel.storyQueue.collectAsState()
    val knowledgeQueue by viewModel.knowledgeQueue.collectAsState()

    var selectedReviewRecord by remember { mutableStateOf<Any?>(null) }
    var reviewQueueType by remember { mutableStateOf("ALL") }

    val totalQueueCount = lexiconQueue.size + sentenceQueue.size + speechQueue.size + storyQueue.size + knowledgeQueue.size

    Column(modifier = modifier.fillMaxSize()) {
        // Queue Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Strings.get("nav_validate", lang),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Linguistic peer-review queue for native speakers and language validators",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = if (totalQueueCount > 0) AmberAccent.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$totalQueueCount Pending",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalQueueCount > 0) AmberAccent else EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Sub-tabs
                ScrollableTabRow(
                    selectedTabIndex = when (reviewQueueType) {
                        "ALL" -> 0
                        "WORDS" -> 1
                        "SENTENCES" -> 2
                        "SPEECH" -> 3
                        "STORIES" -> 4
                        else -> 0
                    },
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = TealAccent
                ) {
                    Tab(selected = reviewQueueType == "ALL", onClick = { reviewQueueType = "ALL" }, text = { Text("All ($totalQueueCount)") })
                    Tab(selected = reviewQueueType == "WORDS", onClick = { reviewQueueType = "WORDS" }, text = { Text("Words (${lexiconQueue.size})") })
                    Tab(selected = reviewQueueType == "SENTENCES", onClick = { reviewQueueType = "SENTENCES" }, text = { Text("Sentences (${sentenceQueue.size})") })
                    Tab(selected = reviewQueueType == "SPEECH", onClick = { reviewQueueType = "SPEECH" }, text = { Text("Speech (${speechQueue.size})") })
                    Tab(selected = reviewQueueType == "STORIES", onClick = { reviewQueueType = "STORIES" }, text = { Text("Stories (${storyQueue.size})") })
                }
            }
        }

        // List Content
        if (totalQueueCount == 0) {
            EmptyStateView(
                title = Strings.get("empty_queue_clean", lang),
                subtitle = "There are no submissions waiting in the validation queue. All community records are up to date.",
                icon = Icons.Outlined.CheckCircle,
                actionText = "Contribute New Data",
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
                // Lexicon items in queue
                if (reviewQueueType == "ALL" || reviewQueueType == "WORDS") {
                    items(lexiconQueue) { word ->
                        ValidationCard(
                            type = "LEXICON",
                            primaryText = word.khowarWord,
                            secondaryText = "/${word.transliteration}/ • ${word.englishMeaning}",
                            contributor = word.contributorName,
                            dialect = word.dialectId,
                            onClick = { selectedReviewRecord = word }
                        )
                    }
                }

                // Sentence items in queue
                if (reviewQueueType == "ALL" || reviewQueueType == "SENTENCES") {
                    items(sentenceQueue) { sen ->
                        ValidationCard(
                            type = "SENTENCE",
                            primaryText = sen.khowarText,
                            secondaryText = sen.englishTranslation,
                            contributor = sen.contributorName,
                            dialect = sen.dialectId,
                            onClick = { selectedReviewRecord = sen }
                        )
                    }
                }

                // Speech items in queue
                if (reviewQueueType == "ALL" || reviewQueueType == "SPEECH") {
                    items(speechQueue) { sp ->
                        ValidationCard(
                            type = "SPEECH",
                            primaryText = "Audio Clip (${String.format("%.1fs", sp.durationSeconds)})",
                            secondaryText = sp.transcriptKhowar,
                            contributor = sp.contributorName,
                            dialect = sp.dialectId,
                            onClick = { selectedReviewRecord = sp }
                        )
                    }
                }

                // Stories in queue
                if (reviewQueueType == "ALL" || reviewQueueType == "STORIES") {
                    items(storyQueue) { story ->
                        ValidationCard(
                            type = "STORY",
                            primaryText = story.title,
                            secondaryText = story.khowarText.take(60) + "...",
                            contributor = story.contributorName,
                            dialect = story.dialectId,
                            onClick = { selectedReviewRecord = story }
                        )
                    }
                }
            }
        }
    }

    // Review & Decision Dialog
    selectedReviewRecord?.let { record ->
        ValidationDecisionDialog(
            record = record,
            viewModel = viewModel,
            currentUser = currentUser,
            onDismiss = { selectedReviewRecord = null }
        )
    }
}

@Composable
fun ValidationCard(
    type: String,
    primaryText: String,
    secondaryText: String,
    contributor: String,
    dialect: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Navy800,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = type,
                            fontSize = 9.sp,
                            color = TealAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Dialect: $dialect",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = primaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = secondaryText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Submitted by: $contributor",
                    fontSize = 10.sp,
                    color = TealAccent
                )
            }

            Icon(
                imageVector = Icons.Default.RateReview,
                contentDescription = "Review",
                tint = EmeraldGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ValidationDecisionDialog(
    record: Any,
    viewModel: KhowarViewModel,
    currentUser: User?,
    onDismiss: () -> Unit
) {
    var comments by remember { mutableStateOf("") }
    var confidenceScore by remember { mutableStateOf(5) }
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()

    val (recordType, recordId, isSelf) = when (record) {
        is LexiconEntry -> Triple("LEXICON", record.id, record.contributorId == currentUser?.id)
        is SentenceEntry -> Triple("SENTENCE", record.id, record.contributorId == currentUser?.id)
        is SpeechRecording -> Triple("SPEECH", record.id, record.contributorId == currentUser?.id)
        is StoryEntry -> Triple("STORY", record.id, record.contributorId == currentUser?.id)
        is KnowledgeEntry -> Triple("KNOWLEDGE", record.id, record.contributorId == currentUser?.id)
        else -> Triple("RECORD", "", false)
    }

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
                            text = "Peer Validation Review",
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

                // Record details preview
                item {
                    when (record) {
                        is LexiconEntry -> {
                            Text(record.khowarWord, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Transliteration: /${record.transliteration}/", fontSize = 13.sp, color = TealAccent)
                            Text("English: ${record.englishMeaning}", fontSize = 13.sp)
                            Text("Urdu: ${record.urduMeaning}", fontSize = 13.sp)
                            Text("POS: ${record.partOfSpeech.name} • Dialect: ${record.dialectId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is SentenceEntry -> {
                            Text(record.khowarText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Transliteration: ${record.transliteration}", fontSize = 13.sp, color = TealAccent)
                            Text("English: ${record.englishTranslation}", fontSize = 13.sp)
                            Text("Urdu: ${record.urduTranslation}", fontSize = 13.sp)
                        }
                        is SpeechRecording -> {
                            Text("Transcript: ${record.transcriptKhowar}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Translation: ${record.englishTranslation}", fontSize = 13.sp)
                            Text("Speaker: ${record.speakerPublicId} (${record.speakerAgeGroup})", fontSize = 11.sp)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (isPlaying) viewModel.audioPlayer.pauseAudio()
                                    else viewModel.audioPlayer.playAudio(record.audioFilePath)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                            ) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = TealAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlaying) "Pause Audio" else "Listen to Audio Recording", color = Color.White)
                            }
                        }
                        is StoryEntry -> {
                            Text(record.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(record.khowarText, fontSize = 13.sp)
                            Text("Translation: ${record.englishTranslation}", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Confidence score rating
                item {
                    Text("Linguistic Accuracy Confidence (1 to 5):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        (1..5).forEach { score ->
                            FilledTonalButton(
                                onClick = { confidenceScore = score },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (confidenceScore == score) TealAccent else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (confidenceScore == score) Navy900 else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$score★", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        label = { Text("Validation Review Notes / Feedback") },
                        placeholder = { Text("e.g., Phonetically accurate, checked with regional dictionary...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isSelf) {
                        Surface(
                            color = CoralAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Rule: You cannot validate your own submission. Please switch to another validator profile to review.",
                                color = CoralAccent,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.submitValidationDecision(recordType, recordId, "APPROVED", comments, confidenceScore) {
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve & Publish", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.submitValidationDecision(recordType, recordId, "REJECTED", comments.ifEmpty { "Did not meet linguistic criteria." }, confidenceScore) {
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAccent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoralAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
