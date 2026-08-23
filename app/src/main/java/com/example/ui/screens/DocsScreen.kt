package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.ui.i18n.Strings
import com.example.ui.theme.*
import com.example.ui.viewmodel.KhowarViewModel

@Composable
fun DocsScreen(
    viewModel: KhowarViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var activeDocSection by remember { mutableStateOf("MISSION") }

    val bibtexCitation = """
@misc{khowar_dataset_2026,
  author = {Khowar Linguistic Initiative & Community Contributors},
  title = {Khowar Dataset: An Open Multilingual Speech, Lexicon, and Cultural Corpus for Khowar},
  year = {2026},
  publisher = {Khowar Dataset Platform},
  howpublished = {\url{https://khowar-dataset.org}},
  license = {CC BY-SA 4.0}
}
    """.trimIndent()

    val apaCitation = "Khowar Linguistic Initiative. (2026). Khowar Dataset: Open Multilingual Speech and Lexicon Corpus (Version 1.0.0) [Data set]. CC BY-SA 4.0. https://khowar-dataset.org"

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
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = TealAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Documentation & Governance",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Linguistic guidelines, validation protocols, ethical consent rules, and research citations",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section Selection Chips
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    Pair("MISSION", "Platform Overview"),
                    Pair("GUIDELINES", "Linguistic Guide"),
                    Pair("ETHICS", "Consent & Privacy"),
                    Pair("CITATION", "Academic Citation")
                ).forEach { (id, label) ->
                    FilterChip(
                        selected = activeDocSection == id,
                        onClick = { activeDocSection = id },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        when (activeDocSection) {
            "MISSION" -> {
                item {
                    DocContentCard(
                        title = "Preserving Khowar. Powering AI. Building the Future.",
                        content = """
Khowar (کھوار), also known as Chitrali, is an Indo-Aryan (Dardic) language spoken primarily in the Chitral district of Khyber Pakhtunkhwa, the Ghizer district of Gilgit-Baltistan, and diaspora communities across the globe.

Despite being spoken by over 400,000 native speakers with a rich millennia-old poetic and oral storytelling tradition, Khowar remains critically under-resourced in modern Natural Language Processing (NLP), Automatic Speech Recognition (ASR), and Machine Translation technologies.

Khowar Dataset is an open, community-driven linguistic data repository created to:
1. Preserve lexical, phonological, and cultural heritage digitally.
2. Build high-quality parallel text and acoustic speech corpora for machine learning and speech technology.
3. Provide open, peer-validated datasets to academic linguists, computational researchers, and native educators worldwide under Creative Commons (CC BY-SA 4.0).
                        """.trimIndent()
                    )
                }
            }
            "GUIDELINES" -> {
                item {
                    DocContentCard(
                        title = "Linguistic Orthography & Standard Rules",
                        content = """
Orthographic Standard:
Khowar is traditionally written in the Perso-Arabic Nastaliq script with specific extended Arabic characters representing retroflex and affricate phonemes unique to Dardic phonology:
• Retroflex Affricates: څ (ts), ځ (dz), ݯ (tsh), ݰ (sh-retroflex), ݱ (zh-retroflex)
• Vowel Extensions: Dedicated diacritics for short and long vowels.

Transliteration Guidelines:
When contributing Roman transliterations, use clean standard Latin characters (e.g., 'zh' for ژ, 'gh' for غ, 'kh' for خ, 'q' for ق).

Peer-Validation Rules:
1. Every record submitted by the community enters the Human Validation Queue.
2. Certified Validators and Linguists review entries for phonetic correctness, dialect attribution, and semantic translation accuracy.
3. Authors cannot validate their own submissions. A minimum of one independent validator approval is mandatory for public inclusion.
                        """.trimIndent()
                    )
                }
            }
            "ETHICS" -> {
                item {
                    DocContentCard(
                        title = "Ethical Standards & Right to Withdraw Consent",
                        content = """
Community Ownership & Dignity:
Linguistic data belongs first and foremost to the community of native speakers who preserve it. We adhere strictly to ethical AI research standards:

1. Free & Explicit Consent: All voice recordings and cultural narratives are collected only with the explicit consent of the speaker or author.
2. Right to Withdraw Consent: Any speaker or contributor may request the withdrawal of their data at any time through their Profile Settings. Withdrawn entries are immediately unpublished and archived.
3. Open Sharing under CC BY-SA 4.0: Verified public records are shared freely for research, education, and language technology under Creative Commons Attribution-ShareAlike 4.0.
4. Privacy & Anonymization: Audio files are assigned pseudonymous Speaker IDs. No sensitive personal identification numbers or biometric profiles are exposed.
                        """.trimIndent()
                    )
                }
            }
            "CITATION" -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Citing Khowar Dataset in Academic Publications", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TealAccent)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("APA Format:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(color = Navy900, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(apaCitation, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(apaCitation)) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TealAccent, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("BibTeX Entry:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(color = Navy900, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(bibtexCitation, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFA7F3D0), modifier = Modifier.weight(1f))
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(bibtexCitation)) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TealAccent, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocContentCard(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealAccent)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}
