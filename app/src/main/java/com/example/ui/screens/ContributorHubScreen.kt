package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ContributorActionGrid
import com.example.ui.components.ContributorQualityHeader
import com.example.ui.viewmodel.ContributeTab
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun ContributorHubScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    var showForms by remember { mutableStateOf(false) }

    if (showForms) {
        ContributeScreen(viewModel = viewModel, modifier = modifier)
        return
    }

    val lexiconQueue by viewModel.lexiconQueue.collectAsState()
    val sentenceQueue by viewModel.sentenceQueue.collectAsState()
    val speechQueue by viewModel.speechQueue.collectAsState()
    val storyQueue by viewModel.storyQueue.collectAsState()
    val pendingCount = lexiconQueue.size + sentenceQueue.size + speechQueue.size + storyQueue.size

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ContributorQualityHeader(
                qualityScore = 0,
                pendingCount = pendingCount,
                approvedCount = 0
            )
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Choose what you want to contribute",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Each contribution type has its own structured fields, metadata and validation checks.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ContributorActionGrid(
                onActionSelected = { action ->
                    val tab = when (action.title) {
                        "Add Word" -> ContributeTab.WORD
                        "Add Sentence" -> ContributeTab.SENTENCE
                        "Record Voice" -> ContributeTab.VOICE
                        "Add Story" -> ContributeTab.STORY
                        "Cultural Knowledge" -> ContributeTab.KNOWLEDGE
                        "Visual Label" -> ContributeTab.IMAGE
                        else -> ContributeTab.WORD
                    }
                    viewModel.setContributeTab(tab)
                    showForms = true
                },
                modifier = Modifier.height(430.dp)
            )
        }

        item {
            Text(
                text = "Quality-first workflow",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Complete required fields → check dialect and context → confirm consent → submit → human validation → verified dataset.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
