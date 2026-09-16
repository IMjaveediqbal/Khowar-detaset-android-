package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.UserRole
import com.example.data.repository.RbacRemoteService
import com.example.ui.viewmodel.KhowarViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    var region by rememberSaveable(user?.id) { mutableStateOf(user?.region ?: "Chitral") }
    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var create by rememberSaveable { mutableStateOf(false) }
    var confirmWithdrawal by remember { mutableStateOf(false) }
    var mustChangePassword by remember(user?.id) { mutableStateOf(false) }
    var passwordChangeBusy by remember { mutableStateOf(false) }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user?.id) {
        val uid = user?.id ?: return@LaunchedEffect
        mustChangePassword = runCatching {
            FirebaseFirestore.getInstance().collection("users").document(uid).get().await().getBoolean("mustChangePassword") == true
        }.getOrDefault(false)
    }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (user == null) "Khowar Dataset Account" else "My Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("One account system for the whole platform.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (user == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (create) "Create an account" else "Sign in", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            if (create) "Create a new account and start collecting Khowar data."
                            else "Sign in with your email and password.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item { OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()) }
            if (create) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            }
            item {
                Button(onClick = { viewModel.authenticate(email, password, create, name); password = "" }, enabled = email.isNotBlank() && password.isNotEmpty() && (!create || (password.length >= 8 && name.trim().length >= 2)), modifier = Modifier.fillMaxWidth()) {
                    Text(if (create) "Create Account" else "Sign in")
                }
            }
            item { TextButton(onClick = { create = !create }, modifier = Modifier.fillMaxWidth()) { Text(if (create) "Already have an account? Sign in" else "Create a new account") } }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val initials = user!!.displayName.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "K" }
                            Surface(shape = CircleShape, tonalElevation = 3.dp) { Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Text(initials, fontWeight = FontWeight.Bold) } }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user!!.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(user!!.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                        RoleSummary(user!!.role)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Profile information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(name, { name = it }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(region, { region = it }, label = { Text("Region / community") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Text("Your role and login credentials are not editable from this profile. Role changes are administrator-controlled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { viewModel.loginOrRegister(user!!.email, name, username, user!!.role, region) }, enabled = name.trim().length >= 2, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Dataset activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            ActivityStat("Approved records", stats.totalApprovedRecords.toString(), Modifier.weight(1f))
                            ActivityStat("Speech hours", String.format("%.1f", stats.totalSpeechHours), Modifier.weight(1f))
                        }
                        Text("These are live platform totals, not a personal contribution count.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Set a new password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This project account was created by an administrator. You must replace the temporary password before continuing.")
                    OutlinedTextField(newPassword, { newPassword = it }, label = { Text("New password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !passwordChangeBusy)
                    OutlinedTextField(confirmPassword, { confirmPassword = it }, label = { Text("Confirm new password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), enabled = !passwordChangeBusy)
                    if (passwordChangeError != null) Text(passwordChangeError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Text("Use at least 12 characters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !passwordChangeBusy && newPassword.length >= 12 && newPassword == confirmPassword,
                    onClick = {
                        passwordChangeBusy = true
                        passwordChangeError = null
                        scope.launch {
                            val result = runCatching {
                                val account = FirebaseAuth.getInstance().currentUser ?: error("You are signed out.")
                                account.updatePassword(newPassword).await()
                                RbacRemoteService().completeManagedPasswordChange().getOrThrow()
                            }
                            result.onSuccess {
                                mustChangePassword = false
                                newPassword = ""
                                confirmPassword = ""
                            }.onFailure { error ->
                                passwordChangeError = error.message ?: "Password change failed. Please try again."
                            }
                            passwordChangeBusy = false
                        }
                    }
                ) { Text(if (passwordChangeBusy) "Saving…" else "Change password") }
            },
            dismissButton = { TextButton(onClick = { viewModel.signOut() }, enabled = !passwordChangeBusy) { Text("Sign out") } }
        )
    }
}

@Composable
private fun RoleSummary(role: UserRole) {
    val (title, description) = when (role) {
        UserRole.CONTRIBUTOR -> "Contributor" to "You can collect and submit Khowar data. Validation and publication happen separately."
        UserRole.VALIDATOR -> "Validator" to "Your project account includes community validation permissions."
        UserRole.EXPERT -> "Expert" to "Your project account includes linguistic and cultural verification permissions."
        UserRole.RESEARCHER -> "Researcher" to "Your project account includes approved research-data access."
        UserRole.MODERATOR -> "Moderator" to "Your project account includes community moderation permissions."
        UserRole.DATA_STEWARD -> "Data Steward" to "Your project account includes dataset quality and metadata stewardship."
        UserRole.AUDITOR -> "Auditor" to "Your project account includes audit-history access."
        UserRole.ADMIN -> "Administrator" to "Your project account includes platform governance permissions."
        UserRole.SUPER_ADMIN -> "Super Administrator" to "Your project account has the highest platform authority."
        UserRole.VISITOR -> "Visitor" to "Public browsing access."
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Account role: $title", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
