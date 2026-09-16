package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.repository.RbacRemoteService
import com.example.ui.viewmodel.KhowarViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(viewModel: KhowarViewModel, modifier: Modifier = Modifier) {
    val user by viewModel.currentUser.collectAsState()
    val operations by viewModel.syncOperations.collectAsState()
    val stats by viewModel.statistics.collectAsState()
    var email by rememberSaveable(user?.id) { mutableStateOf(user?.email.orEmpty()) }
    var name by rememberSaveable(user?.id) { mutableStateOf(user?.displayName.orEmpty()) }
    var username by rememberSaveable(user?.id) { mutableStateOf(user?.username.orEmpty()) }
    var region by rememberSaveable(user?.id) { mutableStateOf(user?.region.orEmpty()) }
    var bio by rememberSaveable(user?.id) { mutableStateOf(user?.bio.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var create by rememberSaveable { mutableStateOf(false) }
    var confirmWithdrawal by remember { mutableStateOf(false) }
    var mustChangePassword by remember(user?.id) { mutableStateOf(false) }
    var passwordChangeBusy by remember { mutableStateOf(false) }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }
    var profileBusy by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user?.id) {
        val uid = user?.id ?: return@LaunchedEffect
        val remote = runCatching { FirebaseFirestore.getInstance().collection("users").document(uid).get().await() }.getOrNull() ?: return@LaunchedEffect
        if (!remote.exists()) return@LaunchedEffect
        val remoteName = remote.getString("displayName").orEmpty()
        val remoteUsername = remote.getString("username").orEmpty()
        val remoteRegion = remote.getString("region").orEmpty()
        val remoteBio = remote.getString("bio").orEmpty()
        name = remoteName.ifBlank { name }
        username = remoteUsername
        region = remoteRegion
        bio = remoteBio
        val cached = user!!
        viewModel.repository.setCurrentUser(cached.copy(
            displayName = remoteName.ifBlank { cached.displayName },
            username = remoteUsername,
            region = remoteRegion,
            bio = remoteBio
        ))
        mustChangePassword = remote.getBoolean("mustChangePassword") == true
    }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (user == null) "Khowar Dataset Account" else "My Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (user == null) "Create your account, then complete the information used beside your contributions."
                    else "Keep your profile accurate and useful for the Khowar community.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (user == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (create) "Create your account" else "Sign in", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(if (create) "Your account is automatically set up for contribution. No role selection is needed." else "Sign in to manage your profile and contributions.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()) }
            if (create) item { OutlinedTextField(name, { name = it.take(100) }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(onClick = { viewModel.authenticate(email.trim(), password, create, name.trim()); password = "" }, enabled = email.trim().isNotEmpty() && password.isNotEmpty() && (!create || (password.length >= 8 && name.trim().length >= 2)), modifier = Modifier.fillMaxWidth()) {
                    Text(if (create) "Create Account" else "Sign in")
                }
            }
            item { TextButton(onClick = { create = !create }, modifier = Modifier.fillMaxWidth()) { Text(if (create) "Already have an account? Sign in" else "Create a new account") } }
        } else {
            val initials = user!!.displayName.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "K" }
            val profileNeedsCompletion = user!!.username.isBlank() || user!!.region.isBlank() || user!!.displayName.isBlank()
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, tonalElevation = 4.dp) { Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { Text(initials, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) } }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user!!.displayName.ifBlank { "Complete your profile" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (user!!.username.isNotBlank()) Text("@${user!!.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Text(user!!.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                        if (profileNeedsCompletion) {
                            Text("Complete your profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Add a username and region so your contributions have useful community context.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(user!!.region, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            if (user!!.bio.isNotBlank()) Text(user!!.bio, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Edit profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(username, { username = it.take(32) }, label = { Text("Username") }, supportingText = { Text("2–32 letters, numbers, _, -, or .") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(name, { name = it.take(100) }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(region, { region = it.take(100) }, label = { Text("Region / community") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(bio, { bio = it.take(300) }, label = { Text("About you (optional)") }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
                        Text("Your email and internal access permissions are managed separately.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (profileError != null) Text(profileError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            profileBusy = true
                            profileError = null
                            scope.launch {
                                val result = runCatching {
                                    val account = FirebaseAuth.getInstance().currentUser ?: error("You are signed out.")
                                    val cleanName = name.trim()
                                    val cleanUsername = username.trim()
                                    val cleanRegion = region.trim()
                                    val cleanBio = bio.trim()
                                    require(cleanName.length in 2..100) { "Display name must be 2–100 characters." }
                                    require(cleanUsername.matches(Regex("[A-Za-z0-9_.-]{2,32}"))) { "Username must use 2–32 letters, numbers, _, -, or ." }
                                    require(cleanRegion.isNotEmpty()) { "Enter your region or community." }
                                    FirebaseFunctions.getInstance().getHttpsCallable("saveProfileDetails").call(mapOf("displayName" to cleanName, "username" to cleanUsername, "region" to cleanRegion, "bio" to cleanBio)).await()
                                    account.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(cleanName).build()).await()
                                    viewModel.repository.setCurrentUser(user!!.copy(displayName = cleanName, username = cleanUsername, region = cleanRegion, bio = cleanBio))
                                }
                                result.onFailure { profileError = it.message ?: "Profile could not be saved." }.onSuccess { viewModel.showStatus("Profile saved successfully.") }
                                profileBusy = false
                            }
                        }, enabled = !profileBusy && name.trim().length in 2..100 && username.trim().matches(Regex("[A-Za-z0-9_.-]{2,32}")) && region.trim().isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                            Text(if (profileBusy) "Saving…" else "Save profile")
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Platform activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            ActivityStat("Approved records", stats.totalApprovedRecords.toString(), Modifier.weight(1f))
                            ActivityStat("Speech hours", String.format("%.1f", stats.totalSpeechHours), Modifier.weight(1f))
                        }
                        Text("These are live totals across the platform.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (operations.isEmpty()) Icon(Icons.Default.CloudDone, contentDescription = null) else Icon(Icons.Default.CloudOff, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Upload & sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(if (operations.isEmpty()) "No pending local uploads." else "${operations.size} upload operation(s) are being tracked on this device.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { viewModel.retrySync() }) { Text("Sync / retry uploads") }
                    }
                }
            }
            items(operations.take(10), key = { it.key }) { operation ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text("${operation.collection} · ${operation.state}", style = MaterialTheme.typography.labelMedium)
                        if (operation.error.isNotBlank()) Text(operation.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { OutlinedButton(onClick = { confirmWithdrawal = true }, modifier = Modifier.fillMaxWidth()) { Text("Withdraw consent for all my contributions") } }
            item { TextButton(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") } }
        }
    }

    if (confirmWithdrawal) {
        AlertDialog(onDismissRequest = { confirmWithdrawal = false }, title = { Text("Withdraw your contributions?") }, text = { Text("This archives your cloud records and excludes them from future exports. Previously downloaded copies cannot be recalled. An internet connection is required.") }, confirmButton = { TextButton(onClick = { confirmWithdrawal = false; viewModel.withdrawConsent("ALL_USER_RECORDS", user!!.id) }) { Text("Withdraw") } }, dismissButton = { TextButton(onClick = { confirmWithdrawal = false }) { Text("Cancel") } })
    }

    if (user != null && mustChangePassword) {
        AlertDialog(onDismissRequest = { }, title = { Text("Set a new password") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("This project account was created by an administrator. You must replace the temporary password before continuing.")
                OutlinedTextField(newPassword, { newPassword = it }, label = { Text("New password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !passwordChangeBusy)
                OutlinedTextField(confirmPassword, { confirmPassword = it }, label = { Text("Confirm new password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !passwordChangeBusy)
                if (passwordChangeError != null) Text(passwordChangeError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Use at least 12 characters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, confirmButton = { TextButton(enabled = !passwordChangeBusy && newPassword.length >= 12 && newPassword == confirmPassword, onClick = {
            passwordChangeBusy = true
            passwordChangeError = null
            scope.launch {
                val result = runCatching {
                    val account = FirebaseAuth.getInstance().currentUser ?: error("You are signed out.")
                    account.updatePassword(newPassword).await()
                    RbacRemoteService().completeManagedPasswordChange().getOrThrow()
                }
                result.onSuccess { mustChangePassword = false; newPassword = ""; confirmPassword = "" }
                    .onFailure { error -> passwordChangeError = error.message ?: "Password change failed. Please try again." }
                passwordChangeBusy = false
            }
        }) { Text(if (passwordChangeBusy) "Saving…" else "Change password") } }, dismissButton = { TextButton(onClick = { viewModel.signOut() }, enabled = !passwordChangeBusy) { Text("Sign out") } })
    }
}

@Composable
private fun ActivityStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
