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
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.i18n.Strings
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun ProfileScreen(viewModel: KhowarViewModel, modifier: Modifier = Modifier) {
    val lang by viewModel.currentLanguage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val user = currentUser
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var profileTab by remember { mutableStateOf("OVERVIEW") }
    val myWords by viewModel.approvedLexicon.collectAsState()
    val mySentences by viewModel.approvedSentences.collectAsState()

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp).clip(CircleShape).background(Navy800).border(2.dp, TealAccent, CircleShape)) { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TealAccent, modifier = Modifier.size(36.dp)) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user?.displayName ?: "Visitor", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(user?.email ?: "guest@khowar-dataset.org", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(color = EmeraldGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top = 4.dp)) { Text("Role: ${user?.role?.name ?: "VISITOR"}", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                            }
                        }
                        IconButton(onClick = { showEditProfileDialog = true }) { Icon(Icons.Default.ManageAccounts, contentDescription = "Edit Profile", tint = TealAccent) }
                    }
                    Spacer(modifier = Modifier.height(14.dp)); Divider(); Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(user?.preferredLanguage?.uppercase() ?: "EN", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TealAccent); Text("Language", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(user?.role?.name ?: "MEMBER", fontSize = 13.sp, fontWeight = FontWeight.Black, color = EmeraldGreen); Text("Account Tier", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(user?.region ?: "Chitral", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberAccent); Text("Region", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
        item {
            TabRow(selectedTabIndex = if (profileTab == "OVERVIEW") 0 else 1, containerColor = Color.Transparent, contentColor = TealAccent) {
                Tab(selected = profileTab == "OVERVIEW", onClick = { profileTab = "OVERVIEW" }, text = { Text("Profile & Roles") })
                Tab(selected = profileTab == "CONSENT", onClick = { profileTab = "CONSENT" }, text = { Text("Privacy & Consent Settings") })
            }
        }
        if (profileTab == "OVERVIEW") {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Assigned Role & Permissions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                        Text("Roles are provisioned by administrators through the trusted RBAC backend. They cannot be self-assigned.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        UserRole.values().forEach { r ->
                            Surface(color = if (user?.role == r) TealAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                    RadioButton(selected = user?.role == r, onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(r.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(when (r) {
                                            UserRole.VISITOR -> "Can browse, search, and view public dataset records"
                                            UserRole.CONTRIBUTOR -> "Can submit and manage contributions"
                                            UserRole.VALIDATOR -> "Can peer-review and validate submissions"
                                            UserRole.EXPERT -> "Can perform linguistic and cultural expert verification"
                                            UserRole.RESEARCHER -> "Can access research data, API keys and exports"
                                            UserRole.MODERATOR -> "Can moderate community reports and content"
                                            UserRole.ADMIN -> "Can manage governance, users, releases and audits"
                                            UserRole.SUPER_ADMIN -> "Full platform governance and security authority"
                                        }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Consent Management & Right to be Forgotten", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CoralAccent)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("As a native speaker or contributor, you maintain ownership and dignity over your cultural and linguistic voice recordings. You can withdraw consent at any time.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = { user?.let { viewModel.withdrawConsent("ALL_USER_RECORDS", it.id) } }, colors = ButtonDefaults.buttonColors(containerColor = CoralAccent), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Withdraw All Consent & Archive My Contributions", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
    if (showEditProfileDialog) EditProfileDialog(user = user, viewModel = viewModel, onDismiss = { showEditProfileDialog = false })
}

@Composable
fun EditProfileDialog(user: com.example.data.model.User?, viewModel: KhowarViewModel, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf(user?.email ?: "") }
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var region by remember { mutableStateOf(user?.region ?: "Chitral") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Edit Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Native Region / Valley") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }; Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.loginOrRegister(email, name, username, user?.role ?: UserRole.CONTRIBUTOR, region); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = Navy900)) { Text("Save", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
