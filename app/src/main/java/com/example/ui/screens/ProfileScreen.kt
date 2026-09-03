package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ManageAccounts
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
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.KhowarViewModel
import kotlinx.coroutines.flow.emptyList
import kotlinx.coroutines.flow.flatMapLatest

@Composable
fun ProfileScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val user = currentUser
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var profileTab by remember { mutableStateOf("OVERVIEW") }

    val myWordsFlow = remember(viewModel) {
        viewModel.currentUser.flatMapLatest { u ->
            if (u == null) emptyList() else viewModel.repository.getUserContributionsLexicon(u.id)
        }
    }
    val mySentencesFlow = remember(viewModel) {
        viewModel.currentUser.flatMapLatest { u ->
            if (u == null) emptyList() else viewModel.repository.getUserContributionsSentences(u.id)
        }
    }
    val mySpeechFlow = remember(viewModel) {
        viewModel.currentUser.flatMapLatest { u ->
            if (u == null) emptyList() else viewModel.repository.getUserContributionsSpeech(u.id)
        }
    }
    val myStoriesFlow = remember(viewModel) {
        viewModel.currentUser.flatMapLatest { u ->
            if (u == null) emptyList() else viewModel.repository.getUserContributionsStories(u.id)
        }
    }
    val myKnowledgeFlow = remember(viewModel) {
        viewModel.currentUser.flatMapLatest { u ->
            if (u == null) emptyList() else viewModel.repository.getUserContributionsKnowledge(u.id)
        }
    }
    val myImagesFlow = remember(viewModel) {
        viewModel.currentUser.flatMapLatest { u ->
            if (u == null) emptyList() else viewModel.repository.getUserContributionsImages(u.id)
        }
    }

    val myWords by myWordsFlow.collectAsState(initial = emptyList())
    val mySentences by mySentencesFlow.collectAsState(initial = emptyList())
    val mySpeech by mySpeechFlow.collectAsState(initial = emptyList())
    val myStories by myStoriesFlow.collectAsState(initial = emptyList())
    val myKnowledge by myKnowledgeFlow.collectAsState(initial = emptyList())
    val myImages by myImagesFlow.collectAsState(initial = emptyList())

    val myTotal = myWords.size + mySentences.size + mySpeech.size + myStories.size + myKnowledge.size + myImages.size
    val myApproved = listOf(
        myWords.count { it.status.name == "APPROVED" },
        mySentences.count { it.status.name == "APPROVED" },
        mySpeech.count { it.status.name == "APPROVED" },
        myStories.count { it.status.name == "APPROVED" },
        myKnowledge.count { it.status.name == "APPROVED" },
        myImages.count { it.status.name == "APPROVED" }
    ).sum()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(54.dp).clip(CircleShape).background(Navy800).border(2.dp, TealAccent, CircleShape)
                        ) { Icon(Icons.Default.AccountCircle, null, tint = TealAccent, modifier = Modifier.size(36.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user?.displayName ?: "Visitor", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(user?.username?.let { "@$it" } ?: "Sign in to contribute", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Contributor ID: ${user?.id?.take(8) ?: "—"}", fontSize = 10.sp, color = TealAccent)
                        }
                        if (user != null) IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.ManageAccounts, "Edit Profile", tint = TealAccent)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ProfileStat(myTotal.toString(), "Contributed")
                        ProfileStat(myApproved.toString(), "Approved")
                        ProfileStat(user?.region ?: "—", "Region")
                    }
                }
            }
        }

        item {
            TabRow(selectedTabIndex = if (profileTab == "OVERVIEW") 0 else 1, containerColor = Color.Transparent, contentColor = TealAccent) {
                Tab(profileTab == "OVERVIEW", { profileTab = "OVERVIEW" }, text = { Text("My Contributions") })
                Tab(profileTab == "COMMUNITY", { profileTab = "COMMUNITY" }, text = { Text("Community") })
            }
        }

        if (profileTab == "OVERVIEW") {
            if (user == null) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Your contribution profile", fontWeight = FontWeight.Bold, color = TealAccent)
                            Spacer(Modifier.height(6.dp))
                            Text("Sign in with a verified account to submit data and track every contribution on your profile.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                item { ContributionSummaryCard(myWords.size, mySentences.size, mySpeech.size, myStories.size, myKnowledge.size, myImages.size, myApproved) }
                item {
                    Text("Recent submissions", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                }
                val recent = buildList {
                    myWords.take(3).forEach { add("WORD" to "${it.khowarWord} — ${it.englishMeaning}" to it.status.name) }
                    mySentences.take(3).forEach { add("SENTENCE" to it.khowarText to it.status.name) }
                    myStories.take(2).forEach { add("STORY" to it.title to it.status.name) }
                    mySpeech.take(2).forEach { add("VOICE" to "${it.transcriptKhowar.take(50)}" to it.status.name) }
                }.take(8)
                if (recent.isEmpty()) item { Text("No contributions yet. Your first contribution will appear here.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(recent) { (type, title, status) -> ContributionRow(type, title, status) }
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Community contributions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                        Spacer(Modifier.height(5.dp))
                        Text("Approved contributions are visible to the community. This lets contributors receive recognition while keeping private account details protected.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            val community = buildList {
                viewModel.approvedLexicon.value.take(4).forEach { add("WORD" to "${it.khowarWord} — ${it.englishMeaning}" to it.contributorName) }
                viewModel.approvedSentences.value.take(3).forEach { add("SENTENCE" to it.khowarText to it.contributorName) }
                viewModel.approvedStories.value.take(2).forEach { add("STORY" to it.title to it.contributorName) }
                viewModel.approvedSpeech.value.take(2).forEach { add("VOICE" to it.transcriptKhowar.take(50) to it.contributorName) }
            }.take(10)
            items(community) { (type, title, contributor) ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(type, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Contributed by $contributor", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Privacy & data rights", fontWeight = FontWeight.Bold, color = CoralAccent)
                    Spacer(Modifier.height(6.dp))
                    Text("Your private account credentials are not part of the public dataset. Only approved contribution metadata/content intended for community display is shown publicly.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    if (user != null) Button(
                        onClick = { viewModel.withdrawConsent("ALL_USER_RECORDS", user.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Cancel, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Withdraw Consent & Archive My Contributions")
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) EditProfileDialog(user, viewModel) { showEditProfileDialog = false }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = TealAccent)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ContributionSummaryCard(words: Int, sentences: Int, speech: Int, stories: Int, knowledge: Int, images: Int, approved: Int) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Contribution record", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealAccent)
            Spacer(Modifier.height(10.dp))
            Text("Words: $words   •   Sentences: $sentences   •   Voice: $speech", fontSize = 12.sp)
            Text("Stories: $stories   •   Knowledge: $knowledge   •   Images: $images", fontSize = 12.sp)
            Spacer(Modifier.height(7.dp))
            Text("Approved: $approved", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
        }
    }
}

@Composable
private fun ContributionRow(type: String, title: String, status: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(type, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                Text(title, fontSize = 12.sp, maxLines = 2)
            }
            Text(status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (status == "APPROVED") EmeraldGreen else AmberAccent)
        }
    }
}

@Composable
fun EditProfileDialog(user: com.example.data.model.User?, viewModel: KhowarViewModel, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf(user?.email ?: "") }
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var region by remember { mutableStateOf(user?.region ?: "Chitral") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Edit Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(email, { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(region, { region = it }, label = { Text("Native Region / Valley") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { viewModel.loginOrRegister(email, name, username, user?.role ?: UserRole.CONTRIBUTOR, region); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = Navy900)) { Text("Save") }
                }
            }
        }
    }
}
