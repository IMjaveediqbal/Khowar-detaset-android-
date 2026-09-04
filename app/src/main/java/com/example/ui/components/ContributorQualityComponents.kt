package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TealAccent

/**
 * Quality-first contributor action model. Keep actions explicit so contributors
 * can choose the right linguistic data type instead of submitting everything
 * through one generic form.
 */
data class ContributorAction(
    val title: String,
    val description: String,
    val icon: ImageVector
)

val contributorActions = listOf(
    ContributorAction("Add Word", "Lexical entry, meaning, POS and dialect", Icons.Default.MenuBook),
    ContributorAction("Add Sentence", "Khowar + translation + context", Icons.Default.FormatQuote),
    ContributorAction("Record Voice", "Clear speech with transcript and speaker metadata", Icons.Default.Mic),
    ContributorAction("Add Story", "Folklore, oral history and narratives", Icons.Default.AutoStories),
    ContributorAction("Cultural Knowledge", "Proverbs, idioms, customs and terminology", Icons.Default.Psychology),
    ContributorAction("Visual Label", "Khowar labels and cultural context for images", Icons.Default.Image)
)

@Composable
fun ContributorQualityHeader(
    qualityScore: Int,
    pendingCount: Int,
    approvedCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Contributor Quality", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Build a clean dataset: quality matters more than quantity.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = TealAccent.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "$qualityScore/100",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = TealAccent,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                QualityMiniMetric("Pending", pendingCount, Modifier.weight(1f))
                QualityMiniMetric("Approved", approvedCount, Modifier.weight(1f))
                QualityMiniMetric("Quality", qualityScore, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QualityMiniMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value.toString(), fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ContributorActionGrid(
    onActionSelected: (ContributorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(contributorActions) { action ->
            Card(
                onClick = { onActionSelected(action) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(action.icon, contentDescription = null, tint = TealAccent)
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(action.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        action.description,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ContributionQualityChecklist(
    hasRequiredFields: Boolean,
    hasTranslation: Boolean,
    hasDialect: Boolean,
    hasContext: Boolean,
    hasConsent: Boolean,
    modifier: Modifier = Modifier
) {
    val checks = listOf(
        "Required fields complete" to hasRequiredFields,
        "Translation provided" to hasTranslation,
        "Dialect/region identified" to hasDialect,
        "Context or example provided" to hasContext,
        "Contributor consent confirmed" to hasConsent
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Pre-submission quality check", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            checks.forEach { (label, passed) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (passed) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 7.dp)
                    )
                    Text(label, fontSize = 10.sp)
                }
            }
        }
    }
}
