package com.example.data.research

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Deterministic, speaker-disjoint assignment for research datasets.
 *
 * Every non-empty speaker ID is assigned to exactly one split. Records from
 * the same speaker therefore cannot cross train/dev/test boundaries.
 * The assignment is stable for a fixed seed and algorithm version.
 */
object ResearchSplit {
    const val ALGORITHM_VERSION = "speaker-hash-v1"

    enum class Split { TRAIN, DEV, TEST }

    data class Ratios(
        val train: Int = 80,
        val dev: Int = 10,
        val test: Int = 10
    ) {
        init {
            require(train > 0 && dev > 0 && test > 0) { "All split ratios must be positive" }
            require(train + dev + test == 100) { "Split ratios must total 100" }
        }
    }

    data class Assignment(
        val speakerId: String,
        val split: Split,
        val seed: String,
        val algorithmVersion: String = ALGORITHM_VERSION
    )

    /** Assign a complete speaker to one split using a cryptographic hash. */
    fun assignSpeaker(
        speakerId: String,
        seed: String,
        ratios: Ratios = Ratios()
    ): Assignment {
        val canonicalSpeaker = speakerId.trim()
        require(canonicalSpeaker.isNotEmpty()) { "speakerId must not be blank" }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$seed:$canonicalSpeaker".toByteArray(StandardCharsets.UTF_8))

        // Use the first 8 bytes as an unsigned 64-bit value without relying on
        // platform-specific hashCode() behavior.
        var value = 0L
        for (index in 0 until 8) {
            value = (value shl 8) or (digest[index].toLong() and 0xffL)
        }
        val bucket = java.lang.Long.remainderUnsigned(value, 100L).toInt()

        val split = when {
            bucket < ratios.train -> Split.TRAIN
            bucket < ratios.train + ratios.dev -> Split.DEV
            else -> Split.TEST
        }
        return Assignment(canonicalSpeaker, split, seed)
    }

    /**
     * Assign all speakers and fail fast if an input ID is blank or duplicated
     * with conflicting canonical forms.
     */
    fun assignSpeakers(
        speakerIds: Collection<String>,
        seed: String,
        ratios: Ratios = Ratios()
    ): List<Assignment> {
        val canonical = speakerIds.map { it.trim() }
        require(canonical.none { it.isEmpty() }) { "speakerIds must not contain blank IDs" }
        require(canonical.distinct().size == canonical.size) { "speakerIds must be unique" }
        return canonical.sorted().map { assignSpeaker(it, seed, ratios) }
    }

    /** Verify the core integrity invariant: no speaker occurs in >1 split. */
    fun hasSpeakerLeakage(assignments: Collection<Assignment>): Boolean =
        assignments.groupBy { it.speakerId }
            .values
            .any { group -> group.map { it.split }.distinct().size > 1 }

    fun counts(assignments: Collection<Assignment>): Map<Split, Int> =
        Split.values().associateWith { split -> assignments.count { it.split == split } }
}
