package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun ResearcherScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val exportText by viewModel.exportText.collectAsState()
    val generatedApiKey by viewModel.generatedApiKey.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var apiKeyName by remember { mutableStateOf("Khowar-Research-Project-Key") }
    var selectedExportFormat by remember { mutableStateOf("JSONL") }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = TealAccent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Researcher & Developer Hub", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Research-grade dataset governance, validation, statistics, exports and reproducible ML access.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Research-readiness layer: methodology is visible to collaborators instead of being hidden in code.
        item {
            ResearchCard("Research Readiness", "A transparent pipeline for turning community contributions into research-ready resources.") {
                PipelineStep("1", "RAW", "Community contribution; never silently overwritten")
                PipelineStep("2", "QUALITY CHECK", "Audio/text normalization, duplicate checks and metadata validation")
                PipelineStep("3", "COMMUNITY VERIFIED", "Independent native-speaker review with confidence score")
                PipelineStep("4", "EXPERT VERIFIED", "Research/linguistic review can be recorded separately")
                PipelineStep("5", "RESEARCH READY", "Eligible records are versioned into a release")
            }
        }

        item {
            ResearchCard("Dataset Quality & Coverage", "The app records the dimensions researchers need to assess representativeness.") {
                QualityRow("Speaker metadata", "Age group, gender, native-speaker status, region and dialect")
                QualityRow("Audio metadata", "Duration, sample rate, channels, format and recording environment")
                QualityRow("Quality score", "1–5 reviewer confidence is retained with each speech review")
                QualityRow("Validation history", "Decision, validator, comments, confidence and timestamp")
                QualityRow("Consent", "Versioned consent record can be associated with each contribution")
                QualityRow("Provenance", "Contributor, source, license and timestamps are retained")
            }
        }

        item {
            ResearchCard("Research Metrics", "Use these metrics in dataset reports and future papers; values should always come from verified records.") {
                MetricChip("Speaker diversity", "unique speakers / dialect / region")
                MetricChip("Speech coverage", "hours by dialect and quality band")
                MetricChip("Validation", "approval rate + mean confidence")
                MetricChip("Agreement", "inter-annotator agreement when multiple reviews exist")
                MetricChip("Release quality", "research-ready records / total records")
            }
        }

        item {
            ResearchCard("Benchmark Roadmap", "The platform can evolve from corpus collection into a reproducible Khowar benchmark.") {
                QualityRow("ASR", "Train/dev/test speech splits with speaker-disjoint evaluation")
                QualityRow("Speech-to-text", "Word error rate and character error rate")
                QualityRow("Lexical/NLP", "Classification, NER, language modelling and lexical evaluation")
                QualityRow("Cross-lingual", "Transfer from related/high-resource languages")
                QualityRow("Data efficiency", "Performance curves at increasing training-data sizes")
                QualityRow("Reproducibility", "Dataset version + preprocessing + split manifest + evaluation config")
            }
        }

        item {
            ResearchCard("Dataset Release Checklist", "Create a release only after these research-governance checks are satisfied.") {
                ChecklistRow("✓", "Consent status verified")
                ChecklistRow("✓", "Duplicate and normalization checks completed")
                ChecklistRow("✓", "Validation history retained")
                ChecklistRow("✓", "Speaker-disjoint evaluation splits planned")
                ChecklistRow("✓", "License and attribution documented")
                ChecklistRow("✓", "Known limitations documented")
                ChecklistRow("✓", "Version number and release notes assigned")
            }
        }

        item {
            ResearchCard("Dataset Export & Download Engine", "Export approved/verified resources in machine-readable formats.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("JSON", "JSONL", "CSV").forEach { fmt ->
                        FilterChip(selected = selectedExportFormat == fmt, onClick = { selectedExportFormat = fmt }, label = { Text(fmt, fontWeight = FontWeight.Bold) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.generateDatasetExport(selectedExportFormat) },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = Navy900),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Generate $selectedExportFormat Package", fontWeight = FontWeight.Bold)
                }
                exportText?.let { raw ->
                    Spacer(Modifier.height(12.dp))
                    Surface(color = Navy900, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Export Result (${raw.lines().size} lines)", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { clipboardManager.setText(AnnotatedString(raw)) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TealAccent, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp)); Text("Copy", color = TealAccent, fontSize = 11.sp)
                                }
                            }
                            Text(raw.take(500) + if (raw.length > 500) "\n... [truncated]" else "", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        item {
            ResearchCard("Research API Access", "Generate a project-specific bearer token for authorized research pipelines.") {
                OutlinedTextField(value = apiKeyName, onValueChange = { apiKeyName = it }, label = { Text("Application / Key Identifier") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { viewModel.generateApiKey(apiKeyName) }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Navy900), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Generate Research API Token", fontWeight = FontWeight.Bold)
                }
                generatedApiKey?.let { (rawSecret, entity) ->
                    Spacer(Modifier.height(12.dp))
                    Surface(color = Navy900, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Save this token now; it is shown only once.", color = AmberAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(rawSecret, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                                IconButton(onClick = { clipboardManager.setText(AnnotatedString(rawSecret)) }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TealAccent, modifier = Modifier.size(16.dp)) }
                            }
                            Text("Key ID: ${entity.id.take(8)} • Rate limit: ${entity.rateLimitPerHour}/hour", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            ResearchCard("API Reference", "Stable endpoints for authorized research applications.") {
                ApiRow("GET", "/api/v1/lexicon", "Verified lexicon with dialect/POS filters")
                ApiRow("GET", "/api/v1/sentences", "Parallel Khowar-English-Urdu sentences")
                ApiRow("GET", "/api/v1/speech", "Speech corpus metadata and transcripts")
                ApiRow("GET", "/api/v1/statistics", "Dataset statistics and coverage")
                Spacer(Modifier.height(10.dp))
                Surface(color = Navy900, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "import requests\n\nheaders = {\"Authorization\": \"Bearer khowar_live_YOUR_KEY\"}\nr = requests.get(\n    \"https://api.khowar-dataset.org/api/v1/lexicon\",\n    headers=headers,\n    params={\"dialect\": \"Central\", \"pos\": \"NOUN\"}\n)\ndata = r.json()",
                        fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResearchCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealAccent)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable private fun PipelineStep(number: String, title: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(50), color = TealAccent.copy(alpha = 0.16f)) { Text(number, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), fontWeight = FontWeight.Bold, color = TealAccent) }
        Spacer(Modifier.width(10.dp))
        Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(detail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun QualityRow(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(detail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable private fun MetricChip(title: String, detail: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(120.dp)); Text(detail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun ChecklistRow(icon: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, color = EmeraldGreen, fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); Text(text, fontSize = 11.sp) }
}

@Composable private fun ApiRow(method: String, path: String, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(method, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen, modifier = Modifier.width(36.dp))
        Column { Text(path, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealAccent); Text(detail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
