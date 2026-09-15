package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.security.RbacPermission
import com.example.security.RbacPolicy
import com.example.security.StaffInvitationService
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun AdminScreen(viewModel: KhowarViewModel, modifier: Modifier = Modifier) {
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    val versions by viewModel.datasetVersions.collectAsState()
    var tab by remember { mutableStateOf("AUDIT") }
    var showRelease by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }

    if (!RbacPolicy.isAdministrative(currentUser?.role)) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateView("Administrator access required", "This workspace is protected by server-authoritative RBAC.")
        }
        return
    }

    Column(modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Dataset Governance & Admin", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("RBAC, staff provisioning, releases and audit logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(10.dp))
                TabRow(selectedTabIndex = listOf("AUDIT", "USERS", "RELEASES").indexOf(tab)) {
                    Tab(tab == "AUDIT", { tab = "AUDIT" }, text = { Text("Audit (${auditLogs.size})") })
                    Tab(tab == "USERS", { tab = "USERS" }, text = { Text("Users (${users.size})") })
                    Tab(tab == "RELEASES", { tab = "RELEASES" }, text = { Text("Releases (${versions.size})") })
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (tab) {
                "AUDIT" -> if (auditLogs.isEmpty()) item { EmptyStateView("No audit entries yet", "System actions are logged here.") } else items(auditLogs) { log ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(log.details, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("${log.action} • ${log.actorName}", fontSize = 10.sp, color = TealAccent) } }
                }
                "USERS" -> {
                    if (RbacPolicy.can(currentUser?.role, RbacPermission.MANAGE_USERS)) item {
                        Button(onClick = { showInvite = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900)) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Invite Staff Account", fontWeight = FontWeight.Bold)
                        }
                    }
                    items(users) { user ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(user.displayName, fontWeight = FontWeight.Bold); Text(user.email, fontSize = 11.sp); Text(user.role.name, fontSize = 10.sp, color = TealAccent) }
                                FilledTonalButton(onClick = { selectedUser = user }) { Text("Role", fontSize = 10.sp) }
                            }
                        }
                    }
                }
                "RELEASES" -> {
                    if (RbacPolicy.can(currentUser?.role, RbacPermission.RELEASE_DATASET)) item {
                        Button(onClick = { showRelease = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900)) {
                            Icon(Icons.Default.Publish, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("New Release")
                        }
                    }
                    if (versions.isEmpty()) item { EmptyStateView("No dataset releases yet", "Create the first formal dataset snapshot.") }
                    else items(versions) { v -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(v.versionNumber, fontWeight = FontWeight.Bold); Text(v.releaseName); Text("${v.recordCount} records", fontSize = 11.sp, color = TealAccent) } } }
                }
            }
        }
    }

    if (showInvite) StaffInvitationDialog { showInvite = false }
    if (showRelease) CreateReleaseDialog(viewModel) { showRelease = false }
    selectedUser?.let { target ->
        RoleChangeDialog(target, currentUser?.role, { role -> viewModel.updateUserRole(target.id, role); selectedUser = null }) { selectedUser = null }
    }
}

@Composable
private fun StaffInvitationDialog(onDismiss: () -> Unit) {
    val service = remember { StaffInvitationService(LocalContext.current) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.VALIDATOR) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Invite Staff Account", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("The staff member receives a password-reset email and uses the normal login. The administrator never sees a permanent password.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Staff name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(7.dp))
                UserRole.values().filter { it in setOf(UserRole.VALIDATOR, UserRole.EXPERT, UserRole.RESEARCHER, UserRole.MODERATOR, UserRole.DATA_STEWARD, UserRole.AUDITOR, UserRole.ADMIN) }.forEach { candidate ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { role = candidate }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(role == candidate, null, enabled = !busy)
                        Text(candidate.name, fontSize = 12.sp)
                    }
                }
                message?.let { Text(it, fontSize = 11.sp, color = if (it.startsWith("Invitation sent")) EmeraldGreen else MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") }
                    Button(enabled = !busy, onClick = { busy = true }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900)) { Text(if (busy) "Sending…" else "Send Invitation") }
                }
            }
        }
    }

    LaunchedEffect(busy) {
        if (!busy) return@LaunchedEffect
        service.invite(email, name, role).onSuccess { message = "Invitation sent to ${email.trim()}" }.onFailure { message = it.message ?: "Invitation failed." }
        busy = false
    }
}

@Composable
private fun RoleChangeDialog(targetUser: User, currentRole: UserRole?, onRoleSelected: (UserRole) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change role for ${targetUser.displayName}") },
        text = {
            Column {
                UserRole.values().filter { it != UserRole.VISITOR && (currentRole == UserRole.SUPER_ADMIN || it != UserRole.SUPER_ADMIN) }.forEach { role ->
                    Row(Modifier.fillMaxWidth().clickable { onRoleSelected(role) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(targetUser.role == role, null)
                        Text(role.name, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun CreateReleaseDialog(viewModel: KhowarViewModel, onDismiss: () -> Unit) {
    var tag by remember { mutableStateOf("v1.0.0") }
    var name by remember { mutableStateOf("Chitral Valley Initial Linguistic Corpus") }
    var desc by remember { mutableStateOf("Peer-validated snapshot of the Khowar corpus.") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Publish Dataset Release") }, text = {
        Column {
            OutlinedTextField(tag, { tag = it }, label = { Text("Version") }, singleLine = true)
            OutlinedTextField(name, { name = it }, label = { Text("Title") }, singleLine = true)
            OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, minLines = 2)
        }
    }, confirmButton = { Button(onClick = { viewModel.createDatasetRelease(tag, name, desc) { onDismiss() } }) { Text("Publish") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
