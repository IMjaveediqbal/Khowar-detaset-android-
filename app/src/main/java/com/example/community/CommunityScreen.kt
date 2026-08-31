package com.example.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import com.example.ui.viewmodel.KhowarViewModel
import kotlinx.coroutines.launch

private val communityCategories = listOf(
    "All", "Contribution Help", "Grammar", "Pronunciation", "Dialects",
    "Culture", "Dataset Problems", "AI / NLP", "Ideas"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(viewModel: KhowarViewModel) {
    val profile by viewModel.currentUser.collectAsState()
    val service = remember { CommunityService() }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedPost by remember { mutableStateOf<CommunityPost?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    val posts by service.observePosts(selectedCategory).collectAsState(initial = emptyList())
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Khowar Community") },
                    navigationIcon = {
                        IconButton(onClick = { CommunityUiState.hide() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (profile != null) {
                            FilledTonalButton(onClick = { showCreate = true }) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.padding(2.dp))
                                Text("Discuss")
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Learn • Discuss • Help • Preserve", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A community where Khowar speakers solve language questions and improve the dataset together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(communityCategories) { category ->
                            AssistChip(onClick = { selectedCategory = category }, label = { Text(category) }, leadingIcon = if (category == "Contribution Help") ({ Icon(Icons.Default.HelpOutline, null) }) else null)
                        }
                    }
                }

                HorizontalDivider()

                if (profile == null) {
                    Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Join the community", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text("Create a profile to ask questions, help other contributors, vote, and report problems.")
                        }
                    }
                }

                if (posts.isEmpty()) {
                    Card(Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("No discussions yet.", style = MaterialTheme.typography.titleMedium)
                            Text("Be the first person to ask a Khowar language or contribution question.")
                        }
                    }
                } else {
                    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
                        items(posts, key = { it.id }) { post ->
                            CommunityPostCard(
                                post = post,
                                profile = profile,
                                onOpen = { selectedPost = post },
                                onVote = {
                                    if (profile != null) scope.launch { service.toggleVote(post.id, profile!!).onFailure { snackbar.showSnackbar(it.message ?: "Vote failed") } }
                                    else scope.launch { snackbar.showSnackbar("Create a profile to participate.") }
                                },
                                onReport = {
                                    if (profile != null) scope.launch { service.reportPost(post.id, profile!!, "Community report").onSuccess { snackbar.showSnackbar("Reported. Thank you.") }.onFailure { snackbar.showSnackbar(it.message ?: "Report failed") } }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate && profile != null) {
        CreateDiscussionDialog(profile!!, service) { message ->
            showCreate = false
            scope.launch { snackbar.showSnackbar(message) }
        }
    }

    selectedPost?.let { post ->
        DiscussionDialog(post, profile, service) { selectedPost = null }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    profile: User?,
    onOpen: () -> Unit,
    onVote: () -> Unit,
    onReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(post.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (post.solved) {
                    AssistChip(onClick = {}, enabled = false, label = { Text("Solved") }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(post.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(post.body, maxLines = 4, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Text("${post.authorName}  •  ${post.answerCount} replies  •  ${post.voteScore} helpful", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onVote) { Icon(Icons.Default.ThumbUp, null); Text("Helpful") }
                if (profile != null) IconButton(onClick = onReport) { Icon(Icons.Default.Flag, contentDescription = "Report") }
            }
        }
    }
}

@Composable
private fun CreateDiscussionDialog(profile: User, service: CommunityService, onDone: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Contribution Help") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onDone("") },
        confirmButton = {
            Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    val result = service.createPost(profile, title, body, category)
                    busy = false
                    onDone(result.fold({ "Discussion posted." }, { it.message ?: "Could not post discussion." }))
                }
            }) { Text(if (busy) "Posting…" else "Post") }
        },
        dismissButton = { OutlinedButton(enabled = !busy, onClick = { onDone("") }) { Text("Cancel") } },
        title = { Text("Start a discussion") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("Question or discussion") }, minLines = 4)
                Text("Category: $category", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

@Composable
private fun DiscussionDialog(post: CommunityPost, profile: User?, service: CommunityService, onClose: () -> Unit) {
    val comments by service.observeComments(post.id).collectAsState(initial = emptyList())
    var reply by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            if (profile != null) {
                Button(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        service.addComment(post.id, profile, reply)
                        reply = ""
                        busy = false
                    }
                }) { Text("Reply") }
            } else OutlinedButton(onClick = onClose) { Text("Close") }
        },
        dismissButton = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } },
        title = { Text(post.title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(post.body, style = MaterialTheme.typography.bodyLarge) }
                item { Text("${comments.size} replies", style = MaterialTheme.typography.labelLarge) }
                items(comments, key = { it.id }) { comment ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(comment.authorName, style = MaterialTheme.typography.labelMedium)
                            Text(comment.body)
                            if (comment.accepted) Text("Accepted answer", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (profile != null) item { OutlinedTextField(reply, { reply = it }, label = { Text("Your reply") }, minLines = 2) }
            }
        }
    )
}
