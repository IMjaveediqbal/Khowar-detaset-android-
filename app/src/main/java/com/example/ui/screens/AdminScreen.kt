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
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.security.RbacPolicy
import com.example.security.RbacService
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.ui.viewmodel.KhowarViewModel
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(viewModel: KhowarViewModel, modifier: Modifier = Modifier) {
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    val versions by viewModel.datasetVersions.collectAsState()
    var showReleaseDialog by remember { mutableStateOf(false) }
    var selectedUserForRoleChange by remember { mutableStateOf<User?>(null) }
    var adminTab by remember { mutableStateOf("AUDIT") }
    val scope = rememberCoroutineScope()
    val rbac = remember { RbacService() }

    if (!RbacPolicy.isAdministrative(currentUser?.role)) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateView(title = "Administrator access required", subtitle = "This workspace is protected by server-authoritative RBAC.")
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dataset Governance & Admin", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("RBAC, user provisioning, dataset releases and audit logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (RbacPolicy.can(currentUser?.role, com.example.security.RbacPermission.RELEASE_DATASET)) {
                        Button(onClick = { showReleaseDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Release", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                TabRow(selectedTabIndex = when (adminTab) { "AUDIT" -> 0; "USERS" -> 1; "RELEASES" -> 2; else -> 0 }, containerColor = Color.Transparent, contentColor = TealAccent) {
                    Tab(selected = adminTab == "AUDIT", onClick = { adminTab = "AUDIT" }, text = { Text("Audit Ledger (${auditLogs.size})") })
                    Tab(selected = adminTab == "USERS", onClick = { adminTab = "USERS" }, text = { Text("Users (${users.size})") })
                    Tab(selected = adminTab == "RELEASES", onClick = { adminTab = "RELEASES" }, text = { Text("Releases (${versions.size})") })
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (adminTab) {
                "AUDIT" -> if (auditLogs.isEmpty()) item { EmptyStateView(title = "No audit entries yet", subtitle = "System actions are logged immutably here.") } else items(auditLogs) { log -> AuditLogItemCard(log) }
                "USERS" -> items(users) { u -> UserRowCard(user = u, onRoleChange = { selectedUserForRoleChange = u }) }
                "RELEASES" -> if (versions.isEmpty()) item { EmptyStateView(title = "No Dataset Releases Published Yet", subtitle = "Publish version v1.0.0 to create a formal citable snapshot of the dataset.", actionText = "Create Dataset Release", onAction = { showReleaseDialog = true }) } else items(versions) { v -> DatasetVersionCard(v) }
            }
        }
    }

    if (showReleaseDialog) CreateReleaseDialog(viewModel = viewModel, onDismiss = { showReleaseDialog = false })
    selectedUserForRoleChange?.let { targetUser ->
        RoleChangeDialog(
            targetUser = targetUser,
            currentRole = currentUser?.role,
            onRoleSelected = { role ->
                scope.launch {
                    rbac.setUserRole(targetEmail = targetUser.email, role = role)
                }
                selectedUserForRoleChange = null
            },
            onDismiss = { selectedUserForRoleChange = null }
        )
    }
}

@Composable
fun AuditLogItemCard(log: com.example.data.model.AuditLog) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp).clip(CircleShape).background(Navy800)) {
                Icon(imageVector = when { log.action.contains("APPROVED") -> Icons.Default.Check; log.action.contains("REJECT") -> Icons.Default.Close; else -> Icons.Default.History }, contentDescription = null, tint = TealAccent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.details, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Actor: ${log.actorName} • Entity: ${log.entityType}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(log.action.take(16), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
        }
    }
}

@Composable
fun UserRowCard(user: User, onRoleChange: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(user.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Region: ${user.region} • Language: ${user.preferredLanguage}", fontSize = 10.sp, color = TealAccent)
            }
            FilledTonalButton(onClick = onRoleChange, shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text(user.role.name, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun DatasetVersionCard(version: com.example.data.model.DatasetVersion) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NewReleases, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(version.versionNumber, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Surface(color = Navy800, shape = RoundedCornerShape(4.dp)) { Text(version.license, color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(version.releaseName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(version.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Snapshot: ${version.recordCount} total records, ${String.format("%.1f", version.speechHours)} speech hrs", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CreateReleaseDialog(viewModel: KhowarViewModel, onDismiss: () -> Unit) {
    var tag by remember { mutableStateOf("v1.0.0") }
    var name by remember { mutableStateOf("Chitral Valley Initial Linguistic Corpus") }
    var desc by remember { mutableStateOf("First peer-validated snapshot of Khowar lexicon, sentences, and oral audio corpus.") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Publish Dataset Release", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = tag, onValueChange = { tag = it }, label = { Text("Version Tag") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Release Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Release Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.createDatasetRelease(tag, name, desc) { onDismiss() } }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900)) { Text("Publish Release", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun RoleChangeDialog(targetUser: User, currentRole: UserRole?, onRoleSelected: (UserRole) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Change User Role: ${targetUser.displayName}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Changes are verified by Firebase Functions and recorded in the audit ledger.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                UserRole.values().filter { it != UserRole.VISITOR && (currentRole == UserRole.SUPER_ADMIN || it != UserRole.SUPER_ADMIN) }.forEach { role ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onRoleSelected(role) }.padding(vertical = 8.dp)) {
                        RadioButton(selected = targetUser.role == role, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(role.name, fontWeight = FontWeight.Medium)
                            Text(roleDescription(role), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

private fun roleDescription(role: UserRole): String = when (role) {
    UserRole.VISITOR -> "Public browsing only"
    UserRole.CONTRIBUTOR -> "Submit and manage contributions"
    UserRole.VALIDATOR -> "Peer validation of submissions"
    UserRole.EXPERT -> "Linguistic and cultural expert verification"
    UserRole.RESEARCHER -> "Research access, exports and API keys"
    UserRole.MODERATOR -> "Community moderation and reports"
    UserRole.ADMIN -> "Platform governance and administration"
    UserRole.SUPER_ADMIN -> "Full platform authority"
}
