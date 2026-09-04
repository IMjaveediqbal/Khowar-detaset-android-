package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.KhowarViewModel

/**
 * Validator landing layer. The existing ValidateScreen remains the detailed
 * review form; this screen makes the research-grade workflow explicit and
 * prevents non-validator accounts from being presented with review controls.
 */
@Composable
fun ValidatorWorkflowScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val lexicon by viewModel.lexiconQueue.collectAsState()
    val sentences by viewModel.sentenceQueue.collectAsState()
    val speech by viewModel.speechQueue.collectAsState()
    val stories by viewModel.storyQueue.collectAsState()
    val knowledge by viewModel.knowledgeQueue.collectAsState()

    val canValidate = currentUser?.role in setOf(
        UserRole.VALIDATOR, UserRole.MODERATOR, UserRole.ADMIN, UserRole.SUPER_ADMIN
    )
    val total = lexicon.size + sentences.size + speech.size + stories.size + knowledge.size

    if (!canValidate) {
        AccessDeniedCard(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WorkflowHeader(total)
        }
        item {
            WorkflowStages()
        }
        item {
            Text("Review queue", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(
                "Open a submission for linguistic review. Never approve your own contribution.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (total == 0) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = EmeraldGreen.copy(alpha = 0.10f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen)
                        Spacer(Modifier.padding(horizontal = 5.dp))
                        Text("No records are waiting for validation.", fontSize = 12.sp)
                    }
                }
            }
        } else {
            if (lexicon.isNotEmpty()) {
                items(lexicon) { item ->
                    WorkflowQueueCard("WORD", item.khowarWord, item.contributorName, item.dialectId) {
                        // Detailed review is opened through the existing validation screen.
                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.VALIDATE)
                    }
                }
            }
            if (sentences.isNotEmpty()) {
                items(sentences) { item ->
                    WorkflowQueueCard("SENTENCE", item.khowarText, item.contributorName, item.dialectId) {
                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.VALIDATE)
                    }
                }
            }
            if (speech.isNotEmpty()) {
                items(speech) { item ->
                    WorkflowQueueCard("SPEECH", item.transcriptKhowar, item.contributorName, item.dialectId) {
                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.VALIDATE)
                    }
                }
            }
            if (stories.isNotEmpty()) {
                items(stories) { item ->
                    WorkflowQueueCard("STORY", item.title, item.contributorName, item.dialectId) {
                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.VALIDATE)
                    }
                }
            }
            if (knowledge.isNotEmpty()) {
                items(knowledge) { item ->
                    WorkflowQueueCard("KNOWLEDGE", item.title, item.contributorName, item.dialectId) {
                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.VALIDATE)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowHeader(total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FactCheck, null, tint = TealAccent)
            Spacer(Modifier.padding(horizontal = 5.dp))
            Column(Modifier.weight(1f)) {
                Text("Validator Workspace", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Research-grade linguistic quality control", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = TealAccent.copy(alpha = 0.14f), shape = MaterialTheme.shapes.small) {
                Text("$total pending", modifier = Modifier.padding(9.dp), color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun WorkflowStages() {
    val stages = listOf(
        "1  Submitted" to Icons.Default.RateReview,
        "2  Validator review" to Icons.Default.FactCheck,
        "3  Community verified" to Icons.Default.CheckCircle,
        "4  Expert verified" to Icons.Default.Verified,
        "5  Research-ready" to Icons.Default.Lock
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Quality pipeline", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            stages.forEach { (label, icon) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, modifier = Modifier.padding(end = 8.dp), tint = TealAccent)
                    Text(label, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun WorkflowQueueCard(type: String, title: String, contributor: String, dialect: String, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(type, color = TealAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("$dialect • submitted by $contributor", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccessDeniedCard(modifier: Modifier) {
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error)
        Text("Validator access required", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Only Validator, Moderator, Admin and Super Admin accounts can review dataset submissions.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
