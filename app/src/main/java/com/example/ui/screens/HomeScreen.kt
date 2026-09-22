package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DatasetStatistics
import com.example.ui.components.*
import com.example.ui.i18n.Strings
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

@Composable
fun HomeScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val stats by viewModel.statistics.collectAsState()
    val auditLogs by viewModel.allAuditLogs.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Full Hero Section
        item {
            HeroSection(
                lang = lang,
                onExplore = { viewModel.navigateTo(AppScreen.EXPLORE) },
                onContribute = { viewModel.navigateTo(AppScreen.CONTRIBUTE) }
            )
        }

        // 2. Trust Badges
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TrustBadgesRow(lang = lang)
            }
        }

        // 3. Dataset At a Glance (Floating Real Calculated Stats Card)
        item {
            DatasetAtAGlanceCard(
                stats = stats,
                lang = lang,
                onViewFullStats = { viewModel.navigateTo(AppScreen.STATS) }
            )
        }

        // 4. Contribute to Khowar Dataset (6 Core Contribution Types)
        item {
            ContributionCategoriesSection(
                lang = lang,
                onSelectType = { type ->
                    viewModel.setContributeTab(type)
                    viewModel.navigateTo(AppScreen.CONTRIBUTE)
                }
            )
        }

        // 5. What We Collect Section
        item {
            WhatWeCollectSection(lang = lang, onExplore = { tab ->
                viewModel.setExploreTab(tab)
                viewModel.navigateTo(AppScreen.EXPLORE)
            })
        }

        // 6. Recent Real Activity / Audit Stream (Rule 2: No fake activity)
        item {
            RecentActivitySection(
                auditLogs = auditLogs,
                lang = lang
            )
        }

        // 7. Footer
        item {
            Spacer(modifier = Modifier.height(24.dp))
            AppFooter(
                lang = lang,
                onNavigate = { viewModel.navigateTo(it) }
            )
        }
    }
}

@Composable
fun HeroSection(
    lang: com.example.ui.i18n.AppLanguage,
    onExplore: () -> Unit,
    onContribute: () -> Unit
) {
    Surface(
        color = Navy900,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                color = TealAccent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    TealAccent.copy(alpha = 0.35f)
                )
            ) {
                Text(
                    text = "KHOWAR LANGUAGE PRESERVATION",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    color = TealAccent
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Khowar Dataset",
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = Strings.get("tagline", lang),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = EmeraldGreen
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = Strings.get("mission_desc", lang),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = Color(0xFFD3E0E6),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onContribute,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealAccent,
                        contentColor = Navy950
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        Strings.get("btn_contribute", lang),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onExplore,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        Strings.get("btn_explore", lang),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun DatasetAtAGlanceCard(
    stats: DatasetStatistics,
    lang: com.example.ui.i18n.AppLanguage,
    onViewFullStats: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = TealAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.get("stat_at_a_glance", lang),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = onViewFullStats) {
                    Text("Details →", fontSize = 12.sp, color = TealAccent, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = Strings.get("stat_real_calculated", lang),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Grid Stats Box
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatMetricBox(
                    label = Strings.get("stat_total_words", lang),
                    number = stats.totalWords.toString(),
                    icon = Icons.Default.MenuBook,
                    color = TealAccent,
                    modifier = Modifier.weight(1f)
                )
                StatMetricBox(
                    label = Strings.get("stat_total_sentences", lang),
                    number = stats.totalSentences.toString(),
                    icon = Icons.Default.FormatQuote,
                    color = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatMetricBox(
                    label = Strings.get("stat_speech_hours", lang),
                    number = String.format("%.2f hrs", stats.totalSpeechHours),
                    icon = Icons.Default.Mic,
                    color = AmberAccent,
                    modifier = Modifier.weight(1f)
                )
                StatMetricBox(
                    label = Strings.get("stat_contributors", lang),
                    number = stats.totalContributors.toString(),
                    icon = Icons.Default.Groups,
                    color = Color(0xFFC77DFF),
                    modifier = Modifier.weight(1f)
                )
            }

            if (stats.totalApprovedRecords == 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.get("empty_stats_growing", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetricBox(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ContributionCategoriesSection(
    lang: com.example.ui.i18n.AppLanguage,
    onSelectType: (ContributeTab) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Contribute to Khowar Dataset",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Six structured data collection pipelines for native speakers & linguists",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val types = listOf(
            Triple(ContributeTab.WORD, "Khowar Word", Icons.Default.MenuBook),
            Triple(ContributeTab.SENTENCE, "Sentence Pair", Icons.Default.FormatQuote),
            Triple(ContributeTab.VOICE, "Voice Recording", Icons.Default.Mic),
            Triple(ContributeTab.STORY, "Story & Folklore", Icons.Default.AutoStories),
            Triple(ContributeTab.KNOWLEDGE, "Cultural Knowledge", Icons.Default.Psychology),
            Triple(ContributeTab.IMAGE, "Visual Label", Icons.Default.Image)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            types.take(3).forEach { (tab, label, icon) ->
                ContributionCategoryCard(
                    title = label,
                    icon = icon,
                    onClick = { onSelectType(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            types.drop(3).forEach { (tab, label, icon) ->
                ContributionCategoryCard(
                    title = label,
                    icon = icon,
                    onClick = { onSelectType(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ContributionCategoryCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier.height(90.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Navy800)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun WhatWeCollectSection(
    lang: com.example.ui.i18n.AppLanguage,
    onExplore: (ExploreTab) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "What We Collect",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "From everyday vocabulary to speech, stories, dialects, and cultural knowledge, Khowar Dataset brings diverse language resources together in one trusted platform.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val domains = listOf(
                Pair("Lexicon & Morphosyntax", ExploreTab.WORDS),
                Pair("Parallel Sentences", ExploreTab.SENTENCES),
                Pair("Acoustic Speech Corpus", ExploreTab.SPEECH),
                Pair("Oral Folklore & Literature", ExploreTab.STORIES),
                Pair("Cultural Knowledge & Heritage", ExploreTab.KNOWLEDGE)
            )

            domains.forEach { (name, tab) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onExplore(tab) }
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun RecentActivitySection(
    auditLogs: List<com.example.data.model.AuditLog>,
    lang: com.example.ui.i18n.AppLanguage
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Recent Dataset Activity",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Real-time ledger of contributions, validations, and platform updates",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (auditLogs.isEmpty()) {
            EmptyStateView(
                title = Strings.get("empty_no_data", lang),
                subtitle = "Submissions and human validation events will appear here live."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                auditLogs.take(5).forEach { log ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Navy800)
                            ) {
                                Icon(
                                    imageVector = when {
                                        log.action.contains("VALIDATE_APPROVED") -> Icons.Default.Verified
                                        log.action.contains("VALIDATE_REJECT") -> Icons.Default.Cancel
                                        log.action.contains("SUBMIT") -> Icons.Default.CloudUpload
                                        log.action.contains("VERSION") -> Icons.Default.NewReleases
                                        else -> Icons.Default.History
                                    },
                                    contentDescription = null,
                                    tint = if (log.action.contains("APPROVED")) EmeraldGreen else TealAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.details,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "By ${log.actorName}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.entityType,
                                        fontSize = 10.sp,
                                        color = TealAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
