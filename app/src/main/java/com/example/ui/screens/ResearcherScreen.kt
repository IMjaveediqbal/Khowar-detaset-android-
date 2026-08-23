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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmptyStateView
import com.example.ui.i18n.Strings
import com.example.ui.theme.*
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun ResearcherScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val exportText by viewModel.exportText.collectAsState()
    val generatedApiKey by viewModel.generatedApiKey.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var apiKeyName by remember { mutableStateOf("Chitral-AI-Project-Key") }
    var selectedExportFormat by remember { mutableStateOf("JSON") }

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
                    Icon(Icons.Default.Code, contentDescription = null, tint = TealAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Researcher & Developer Hub",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Open API access, raw dataset exports, and reproducible ML training artifacts under CC BY-SA 4.0",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Dataset Exporter Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dataset Export & Download Engine",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent
                    )
                    Text(
                        text = "Export all approved and verified linguistic entries in standard machine-readable formats.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("JSON", "JSONL", "CSV").forEach { fmt ->
                            FilterChip(
                                selected = selectedExportFormat == fmt,
                                onClick = { selectedExportFormat = fmt },
                                label = { Text(fmt, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.generateDatasetExport(selectedExportFormat) },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = Navy900),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate $selectedExportFormat Package", fontWeight = FontWeight.Bold)
                    }

                    // Export Result Box
                    exportText?.let { raw ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Navy900,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Export Result (${raw.lines().size} lines)", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    TextButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(raw))
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TealAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", color = TealAccent, fontSize = 11.sp)
                                    }
                                }
                                Text(
                                    text = raw.take(400) + if (raw.length > 400) "\n... [truncated]" else "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. API Key Management Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "API Key Management",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                    Text(
                        text = "Generate bearer tokens for programmatic data ingestion into PyTorch, Hugging Face, or research pipelines.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKeyName,
                        onValueChange = { apiKeyName = it },
                        label = { Text("Application / Key Identifier") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.generateApiKey(apiKeyName) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Live API Token", fontWeight = FontWeight.Bold)
                    }

                    // Newly Generated Key Display
                    generatedApiKey?.let { (rawSecret, entity) ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Navy900,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("⚠️ Save this API token. It is shown only once:", color = AmberAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(rawSecret, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(rawSecret)) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TealAccent, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text("Key ID: ${entity.id.take(8)} • Rate Limit: ${entity.rateLimitPerHour} req/hour", fontSize = 10.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }

        // 3. Interactive API Documentation & Code Snippets
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RESTful API Endpoints",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val endpoints = listOf(
                        Triple("GET", "/api/v1/lexicon", "Fetch verified lexicon entries with dialect and POS filters"),
                        Triple("GET", "/api/v1/sentences", "Query parallel sentences with bilingual translations"),
                        Triple("GET", "/api/v1/speech", "Stream acoustic speech corpus audio files & transcripts"),
                        Triple("GET", "/api/v1/statistics", "Live platform statistics and dialect distribution")
                    )

                    endpoints.forEach { (method, path, desc) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = EmeraldGreen.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(method, color = EmeraldGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(path, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Python SDK Example:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Navy900,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = """
import requests

headers = {"Authorization": "Bearer khowar_live_YOUR_KEY"}
response = requests.get(
    "https://api.khowar-dataset.org/api/v1/lexicon",
    headers=headers,
    params={"dialect": "Central", "pos": "NOUN"}
)
dataset = response.json()
print(f"Loaded {len(dataset)} entries")
                            """.trimIndent(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFFA7F3D0),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
