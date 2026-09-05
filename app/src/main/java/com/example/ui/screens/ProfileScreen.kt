package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ImageEntry
import com.example.data.model.KnowledgeEntry
import com.example.data.model.LexiconEntry
import com.example.data.model.RecordStatus
import com.example.data.model.SentenceEntry
import com.example.data.model.SpeechRecording
import com.example.data.model.StoryEntry
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Navy800
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.KhowarViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ContributionTypeSummary(val label: String, val count: Int, val icon: ImageVector)
private data class Activity(val type: String, val title: String, val status: RecordStatus, val createdAt: Long)

@Composable
fun ProfileScreen(viewModel: KhowarViewModel, modifier: Modifier = Modifier) {
    val user by viewModel.currentUser.collectAsState()
    val userId = user?.id
    var showEditProfile by remember { mutableStateOf(false) }

    val wordsFlow = remember(userId) { userIdFlow(userId) { viewModel.repository.getUserContributionsLexicon(it) } }
    val sentencesFlow = remember(userId) { userIdFlow(userId) { viewModel.repository.getUserContributionsSentences(it) } }
    val speechFlow = remember(userId) { userIdFlow(userId) { viewModel.repository.getUserContributionsSpeech(it) } }
    val storiesFlow = remember(userId) { userIdFlow(userId) { viewModel.repository.getUserContributionsStories(it) } }
    val knowledgeFlow = remember(userId) { userIdFlow(userId) { viewModel.repository.getUserContributionsKnowledge(it) } }
    val imagesFlow = remember(userId) { userIdFlow(userId) { viewModel.repository.getUserContributionsImages(it) } }

    val words by wordsFlow.collectAsState(initial = emptyList())
    val sentences by sentencesFlow.collectAsState(initial = emptyList())
    val speech by speechFlow.collectAsState(initial = emptyList())
    val stories by storiesFlow.collectAsState(initial = emptyList())
    val knowledge by knowledgeFlow.collectAsState(initial = emptyList())
    val images by imagesFlow.collectAsState(initial = emptyList())

    val total = words.size + sentences.size + speech.size + stories.size + knowledge.size + images.size
    val approved = statusCount(words, RecordStatus.APPROVED) + statusCount(sentences, RecordStatus.APPROVED) +
        statusCount(speech, RecordStatus.APPROVED) + statusCount(stories, RecordStatus.APPROVED) +
        statusCount(knowledge, RecordStatus.APPROVED) + statusCount(images, RecordStatus.APPROVED)
    val pending = statusCount(words, RecordStatus.SUBMITTED, RecordStatus.UNDER_REVIEW) +
        statusCount(sentences, RecordStatus.SUBMITTED, RecordStatus.UNDER_REVIEW) +
        statusCount(speech, RecordStatus.SUBMITTED, RecordStatus.UNDER_REVIEW) +
        statusCount(stories, RecordStatus.SUBMITTED, RecordStatus.UNDER_REVIEW) +
        statusCount(knowledge, RecordStatus.SUBMITTED, RecordStatus.UNDER_REVIEW) +
        statusCount(images, RecordStatus.SUBMITTED, RecordStatus.UNDER_REVIEW)
    val correction = statusCount(words, RecordStatus.CHANGES_REQUESTED) + statusCount(sentences, RecordStatus.CHANGES_REQUESTED) +
        statusCount(speech, RecordStatus.CHANGES_REQUESTED) + statusCount(stories, RecordStatus.CHANGES_REQUESTED) +
        statusCount(knowledge, RecordStatus.CHANGES_REQUESTED) + statusCount(images, RecordStatus.CHANGES_REQUESTED)
    val rejected = statusCount(words, RecordStatus.REJECTED) + statusCount(sentences, RecordStatus.REJECTED) +
        statusCount(speech, RecordStatus.REJECTED) + statusCount(stories, RecordStatus.REJECTED) +
        statusCount(knowledge, RecordStatus.REJECTED) + statusCount(images, RecordStatus.REJECTED)

    val types = listOf(
        ContributionTypeSummary("Words", words.size, Icons.Default.TextFields),
        ContributionTypeSummary("Sentences", sentences.size, Icons.Default.MenuBook),
        ContributionTypeSummary("Voice", speech.size, Icons.Default.Mic),
        ContributionTypeSummary("Stories", stories.size, Icons.Default.RecordVoiceOver),
        ContributionTypeSummary("Knowledge", knowledge.size, Icons.Default.MenuBook),
        ContributionTypeSummary("Images", images.size, Icons.Default.Image)
    )

    val activity = buildList {
        words.forEach { add(Activity("Word", it.khowarWord, it.status, it.createdAt)) }
        sentences.forEach { add(Activity("Sentence", it.khowarText, it.status, it.createdAt)) }
        speech.forEach { add(Activity("Voice", it.transcriptKhowar, it.status, it.createdAt)) }
        stories.forEach { add(Activity("Story", it.title, it.status, it.createdAt)) }
        knowledge.forEach { add(Activity("Knowledge", it.title, it.status, it.createdAt)) }
        images.forEach { add(Activity("Image", it.title, it.status, it.createdAt)) }
    }.sortedByDescending { it.createdAt }.take(12)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ProfileHeader(user, total, approved) { showEditProfile = true } }
        item { OverviewCard(total, approved, pending, correction, rejected) }
        item { Text("Your contributions", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                types.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { ContributionCard(it, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent contributions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    if (activity.isEmpty()) {
                        Text("No contributions yet. Your words, sentences, voice, stories, knowledge, and images will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    } else {
                        activity.forEachIndexed { index, item ->
                            ActivityRow(item)
                            if (index < activity.lastIndex) Divider(modifier = Modifier.padding(start = 44.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.10f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Keep contributing to Khowar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Your profile keeps a complete record of what you contribute and how each item moves through validation.", color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { viewModel.navigateTo(AppScreen.CONTRIBUTE) }, modifier = Modifier.fillMaxWidth()) { Text("Make a contribution") }
                }
            }
        }
        item { ProfileDetails(user) }
        item {
            OutlinedButton(onClick = { showEditProfile = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit profile")
            }
        }
    }

    if (showEditProfile) EditProfileDialog(user, viewModel) { showEditProfile = false }
}

@Composable
private fun ProfileHeader(user: User?, total: Int, approved: Int, onEdit: () -> Unit) {
    val name = user?.displayName?.ifBlank { "Contributor" } ?: "Contributor"
    val initials = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "K" }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Navy800)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(66.dp).clip(CircleShape).background(TealAccent.copy(alpha = 0.14f)).border(2.dp, TealAccent, CircleShape), contentAlignment = Alignment.Center) {
                    Text(initials, color = TealAccent, fontSize = 21.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(user?.username?.let { "@$it" } ?: "Khowar Contributor", color = TealAccent, fontSize = 13.sp)
                    Text(user?.role?.name?.replace('_', ' ') ?: "CONTRIBUTOR", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.70f), fontSize = 11.sp)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = TealAccent) }
            }
            Spacer(Modifier.height(15.dp))
            Divider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f))
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                HeaderStat(total.toString(), "Contributions")
                HeaderStat(approved.toString(), "Approved")
                HeaderStat(user?.region ?: "Chitral", "Region")
            }
        }
    }
}

@Composable
private fun HeaderStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Text(label, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.68f), fontSize = 10.sp)
    }
}

@Composable
private fun OverviewCard(total: Int, approved: Int, pending: Int, correction: Int, rejected: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Contribution overview", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OverviewStat("Total", total, Modifier.weight(1f)); OverviewStat("Approved", approved, Modifier.weight(1f)); OverviewStat("Pending", pending, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OverviewStat("Correction", correction, Modifier.weight(1f)); OverviewStat("Rejected", rejected, Modifier.weight(1f)); Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverviewStat(label: String, value: Int, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ContributionCard(summary: ContributionTypeSummary, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(13.dp)) {
            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(TealAccent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(summary.icon, contentDescription = null, tint = TealAccent, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(summary.count.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(summary.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ActivityRow(item: Activity) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(TealAccent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Text(item.type.take(1), color = TealAccent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.type, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(item.title.ifBlank { "Untitled contribution" }, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatDate(item.createdAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusPill(item.status)
    }
}

@Composable
private fun StatusPill(status: RecordStatus) {
    val label = status.name.replace('_', ' ')
    val container = when (status) {
        RecordStatus.APPROVED -> EmeraldGreen.copy(alpha = 0.14f)
        RecordStatus.REJECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        else -> TealAccent.copy(alpha = 0.10f)
    }
    Surface(color = container, shape = RoundedCornerShape(999.dp)) { Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)) }
}

@Composable
private fun ProfileDetails(user: User?) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Profile details", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            DetailRow("Email", user?.email ?: "Not connected")
            DetailRow("Language", user?.preferredLanguage?.uppercase() ?: "EN")
            DetailRow("Region", user?.region ?: "Chitral")
            DetailRow("Bio", user?.bio?.ifBlank { "No bio added yet" } ?: "No bio added yet")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(205.dp))
    }
}

@Composable
private fun EditProfileDialog(user: User?, viewModel: KhowarViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var region by remember { mutableStateOf(user?.region ?: "Chitral") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Edit profile", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(region, { region = it }, label = { Text("Region / valley") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(bio, { bio = it }, label = { Text("Short bio") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(14.dp))
                Text("Profile saving will use the authenticated account when the Firebase profile sync is connected.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

private fun <T> userIdFlow(userId: String?, block: (String) -> Flow<List<T>>): Flow<List<T>> =
    if (userId.isNullOrBlank()) flowOf(emptyList()) else block(userId)

private fun statusCount(items: List<LexiconEntry>, vararg statuses: RecordStatus) = items.count { it.status in statuses }
private fun statusCount(items: List<SentenceEntry>, vararg statuses: RecordStatus) = items.count { it.status in statuses }
private fun statusCount(items: List<SpeechRecording>, vararg statuses: RecordStatus) = items.count { it.status in statuses }
private fun statusCount(items: List<StoryEntry>, vararg statuses: RecordStatus) = items.count { it.status in statuses }
private fun statusCount(items: List<KnowledgeEntry>, vararg statuses: RecordStatus) = items.count { it.status in statuses }
private fun statusCount(items: List<ImageEntry>, vararg statuses: RecordStatus) = items.count { it.status in statuses }
private fun formatDate(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
