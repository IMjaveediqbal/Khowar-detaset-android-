package com.example.data.research

import java.text.Normalizer
import java.util.Locale

/** Lightweight, dependency-free leakage checks for research manifests. */
object ResearchLeakage {
    data class DuplicateGroup(
        val canonicalValue: String,
        val recordIds: List<String>
    )

    data class SplitLeakage(
        val left: ResearchSplit.Split,
        val right: ResearchSplit.Split,
        val values: Set<String>
    )

    /** Canonical form for exact duplicate detection; does not attempt fuzzy matching. */
    fun canonicalText(value: String): String = Normalizer
        .normalize(value.trim(), Normalizer.Form.NFC)
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)

    fun exactTextDuplicates(records: Collection<Pair<String, String>>): List<DuplicateGroup> =
        records.groupBy { canonicalText(it.second) }
            .filter { it.key.isNotEmpty() && it.value.size > 1 }
            .map { (canonical, group) ->
                DuplicateGroup(canonical, group.map { it.first }.sorted())
            }
            .sortedBy { it.canonicalValue }

    /** Detect canonical text values appearing in multiple dataset splits. */
    fun crossSplitTextLeakage(
        records: Collection<Triple<String, ResearchSplit.Split, String>>
    ): List<SplitLeakage> {
        val grouped = records
            .mapNotNull { (id, split, text) ->
                val canonical = canonicalText(text)
                if (canonical.isEmpty()) null else Triple(id, split, canonical)
            }
            .groupBy { it.third }

        return grouped.mapNotNull { (value, group) ->
            val splits = group.map { it.second }.distinct().sortedBy { it.ordinal }
            if (splits.size < 2) return@mapNotNull null
            SplitLeakage(splits[0], splits[1], setOf(value))
        }.sortedBy { it.values.first() }
    }
}
