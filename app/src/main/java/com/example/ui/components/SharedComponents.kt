package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecordStatus
import com.example.data.model.UserRole
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.Strings
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun AppHeader(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var showLangMenu by remember { mutableStateOf(false) }
    var showRoleMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Brand Left
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.HOME) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Navy800)
                            .border(1.dp, TealAccent, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = "Khowar Peaks",
                            tint = TealAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "KHOWAR",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "DATASET",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = TealAccent,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = Strings.get("tagline", lang),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // Controls Right
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Language Selector
                    Box {
                        FilledTonalButton(
                            onClick = { showLangMenu = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Language", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(lang.nativeName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false }
                        ) {
                            AppLanguage.values().forEach { l ->
                                DropdownMenuItem(
                                    text = { Text("${l.nativeName} (${l.displayName})") },
                                    onClick = {
                                        viewModel.setLanguage(l)
                                        showLangMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Theme toggle
                    IconButton(
                        onClick = { viewModel.toggleDarkTheme() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Active Profile / Role Selector
                    Box {
                        AssistChip(
                            onClick = { showRoleMenu = true },
                            label = {
                                Text(
                                    text = currentUser?.role?.name ?: "LOGIN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealAccent
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "User",
                                    tint = TealAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        )
                        DropdownMenu(
                            expanded = showRoleMenu,
                            onDismissRequest = { showRoleMenu = false }
                        ) {
                            Text(
                                text = "  Switch Active Role (Demo / Test)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                            Divider()
                            UserRole.values().forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (currentUser?.role == role) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(role.name)
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchUserRole(role)
                                        showRoleMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigationBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    lang: AppLanguage,
    reviewQueueCount: Int
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        val items = listOf(
            Triple(AppScreen.HOME, Strings.get("nav_home", lang), Icons.Default.Home),
            Triple(AppScreen.EXPLORE, Strings.get("nav_explore", lang), Icons.Default.Search),
            Triple(AppScreen.CONTRIBUTE, Strings.get("nav_contribute", lang), Icons.Default.AddCircleOutline),
            Triple(AppScreen.VALIDATE, Strings.get("nav_validate", lang), Icons.Default.VerifiedUser),
            Triple(AppScreen.STATS, Strings.get("nav_stats", lang), Icons.Default.BarChart),
            Triple(AppScreen.RESEARCH, Strings.get("nav_research", lang), Icons.Default.Code),
            Triple(AppScreen.ADMIN, Strings.get("nav_admin", lang), Icons.Default.AdminPanelSettings)
        )

        items.forEach { (screen, label, icon) ->
            val selected = currentScreen == screen
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(screen) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (screen == AppScreen.VALIDATE && reviewQueueCount > 0) {
                                Badge(containerColor = AmberAccent) {
                                    Text(reviewQueueCount.toString(), color = Navy900, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) TealAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Navy700.copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
fun TrustBadgesRow(lang: AppLanguage) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        val badges = listOf(
            Pair(Icons.Default.LockOpen, Strings.get("badge_open", lang)),
            Pair(Icons.Default.Groups, Strings.get("badge_community", lang)),
            Pair(Icons.Default.Verified, Strings.get("badge_validated", lang)),
            Pair(Icons.Default.Shield, Strings.get("badge_privacy", lang)),
            Pair(Icons.Default.Copyright, Strings.get("badge_license", lang))
        )
        badges.forEach { (icon, text) ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TealAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = text,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = Navy900),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(actionText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: RecordStatus) {
    val (bgColor, textColor, label) = when (status) {
        RecordStatus.APPROVED -> Triple(EmeraldGreen.copy(alpha = 0.2f), EmeraldGreen, "HUMAN VALIDATED")
        RecordStatus.SUBMITTED -> Triple(AmberAccent.copy(alpha = 0.2f), AmberAccent, "UNDER REVIEW")
        RecordStatus.UNDER_REVIEW -> Triple(TealAccent.copy(alpha = 0.2f), TealAccent, "IN REVIEW")
        RecordStatus.REJECTED -> Triple(CoralAccent.copy(alpha = 0.2f), CoralAccent, "REJECTED")
        RecordStatus.CHANGES_REQUESTED -> Triple(Color(0xFF9D4EDD).copy(alpha = 0.2f), Color(0xFFC77DFF), "CHANGES REQUESTED")
        RecordStatus.DRAFT -> Triple(Color.Gray.copy(alpha = 0.2f), Color.LightGray, "DRAFT")
        RecordStatus.ARCHIVED -> Triple(Color.DarkGray.copy(alpha = 0.3f), Color.Gray, "ARCHIVED / WITHDRAWN")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun AppFooter(
    lang: AppLanguage,
    onNavigate: (AppScreen) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy900)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Terrain, contentDescription = null, tint = TealAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "KHOWAR DATASET PLATFORM",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Preserving Khowar. Powering AI. Building the Future.",
            color = Color.LightGray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Documentation",
                color = TealAccent,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onNavigate(AppScreen.DOCS) }
            )
            Text("•", color = Color.Gray)
            Text(
                text = "API Reference",
                color = TealAccent,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onNavigate(AppScreen.RESEARCH) }
            )
            Text("•", color = Color.Gray)
            Text(
                text = "CC BY-SA 4.0",
                color = EmeraldGreen,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "An open linguistic data infrastructure for researchers, native speakers, and language developers.",
            color = Color.Gray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatMetricBox(
    number: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = number,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
