package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatMetricBox
import com.example.ui.i18n.Strings
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun StatsScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val stats by viewModel.statistics.collectAsState()
    val words by viewModel.approvedLexicon.collectAsState()
    val sentences by viewModel.approvedSentences.collectAsState()
    val speech by viewModel.approvedSpeech.collectAsState()

    // Real Dialect distribution calculation from Room DB
    val dialectCounts = remember(words, sentences, speech) {
        val map = mutableMapOf<String, Int>()
        words.forEach { map[it.dialectId] = (map[it.dialectId] ?: 0) + 1 }
        sentences.forEach { map[it.dialectId] = (map[it.dialectId] ?: 0) + 1 }
        speech.forEach { map[it.dialectId] = (map[it.dialectId] ?: 0) + 1 }
        map
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = TealAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Platform Analytics & Statistics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Live computed metrics from verified database entries (Zero synthetic or mock data)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatMetricBox(
                        label = Strings.get("stat_total_words", lang),
                        value = stats.totalWords.toString(),
                        icon = Icons.Default.MenuBook,
                        accent = TealAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricBox(
                        label = Strings.get("stat_total_sentences", lang),
                        value = stats.totalSentences.toString(),
                        icon = Icons.Default.FormatQuote,
                        accent = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatMetricBox(
                        label = Strings.get("stat_speech_hours", lang),
                        value = String.format("%.2f hrs", stats.totalSpeechHours),
                        icon = Icons.Default.Mic,
                        accent = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricBox(
                        label = Strings.get("stat_contributors", lang),
                        value = stats.totalContributors.toString(),
                        icon = Icons.Default.Groups,
                        accent = Color(0xFFC77DFF),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatMetricBox(
                        label = "Folklore & Stories",
                        value = stats.totalStories.toString(),
                        icon = Icons.Default.AutoStories,
                        accent = TealAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricBox(
                        label = "Cultural Knowledge",
                        value = stats.totalKnowledge.toString(),
                        icon = Icons.Default.Psychology,
                        accent = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Dialect Distribution Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dialectal Diversity Distribution",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Percentage representation across Khowar dialectal varieties",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (dialectCounts.isEmpty()) {
                        Text(
                            text = "No dialect records verified yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val total = dialectCounts.values.sum().coerceAtLeast(1)
                        dialectCounts.forEach { (dialect, count) ->
                            val fraction = count.toFloat() / total.toFloat()
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = dialect, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "$count entries (${(fraction * 100).toInt()}%)", fontSize = 11.sp, color = TealAccent)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = TealAccent,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Validation Pipeline Metrics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Validation & Quality Pipeline Status",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Human-Validated & Published:", fontSize = 12.sp)
                        Text("${stats.totalApprovedRecords} items", fontWeight = FontWeight.Bold, color = EmeraldGreen, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pending Peer-Review in Queue:", fontSize = 12.sp)
                        Text("${stats.pendingReviewCount} items", fontWeight = FontWeight.Bold, color = AmberAccent, fontSize = 12.sp)
                    }
                }
            }
        }

        if (stats.totalApprovedRecords == 0) {
            item {
                EmptyStateView(
                    title = "Database is Currently Fresh",
                    subtitle = "As contributors submit and validators verify entries, real graphs and metrics will update live.",
                    actionText = "Add First Contribution",
                    onAction = { viewModel.navigateTo(AppScreen.CONTRIBUTE) }
                )
            }
        }
    }
}
