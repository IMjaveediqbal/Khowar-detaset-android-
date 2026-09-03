package com.example.data.research

import com.example.data.local.AppDatabase
import com.example.data.model.ValidationReview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.max

data class ValidationMetrics(
    val totalReviews: Int = 0,
    val participatingValidators: Int = 0,
    val multiReviewedRecords: Int = 0,
    val unanimousMultiReviewedRecords: Int = 0,
    val percentAgreement: Double = 0.0,
    val averageReviewsPerMultiReviewedRecord: Double = 0.0,
    val cohenKappa: Double? = null,
    val fleissKappa: Double? = null,
    val reliabilitySampleCount: Int = 0,
    val reliabilityNote: String = "Insufficient multi-rater data"
)

/**
 * Research reporting metrics derived only from stored validation records.
 * Percent agreement is retained for transparency, while Cohen/Fleiss kappa
 * provide chance-corrected agreement where their assumptions are satisfied.
 */
class ResearchMetricsRepository(database: AppDatabase) {
    private val validationDao = database.validationDao()

    val validationMetrics: Flow<ValidationMetrics> = combine(
        validationDao.countAllReviews(),
        validationDao.countDistinctValidators(),
        validationDao.countMultiReviewedRecords(),
        validationDao.countUnanimousMultiReviewedRecords(),
        validationDao.getAllReviews()
    ) { totalReviews, validators, multiReviewed, unanimous, reviews ->
        val agreement = if (multiReviewed > 0) {
            unanimous.toDouble() / multiReviewed.toDouble() * 100.0
        } else 0.0
        val averageReviews = if (multiReviewed > 0) {
            totalReviews.toDouble() / multiReviewed.toDouble()
        } else 0.0
        val cohen = averagePairwiseCohenKappa(reviews)
        val fleiss = fleissKappa(reviews)
        val sampleCount = reviews.groupBy { it.recordType to it.recordId }.count { it.value.map(ValidationReview::validatorId).distinct().size >= 2 }
        val note = when {
            fleiss != null -> "Fleiss kappa computed on records with a consistent number of independent ratings."
            cohen != null -> "Pairwise Cohen kappa averaged across independent validator pairs; Fleiss kappa not applicable because rating counts vary."
            else -> "At least two independent ratings per reliability unit are required."
        }
        ValidationMetrics(
            totalReviews = totalReviews,
            participatingValidators = validators,
            multiReviewedRecords = multiReviewed,
            unanimousMultiReviewedRecords = unanimous,
            percentAgreement = agreement,
            averageReviewsPerMultiReviewedRecord = averageReviews,
            cohenKappa = cohen,
            fleissKappa = fleiss,
            reliabilitySampleCount = sampleCount,
            reliabilityNote = note
        )
    }

    /** Average Cohen's kappa over all validator pairs with >=2 jointly rated records. */
    private fun averagePairwiseCohenKappa(reviews: List<ValidationReview>): Double? {
        val byRecord = reviews.groupBy { it.recordType to it.recordId }
        val validators = reviews.map(ValidationReview::validatorId).distinct()
        val kappas = mutableListOf<Double>()
        for (i in 0 until validators.size) {
            for (j in i + 1 until validators.size) {
                val paired = byRecord.values.mapNotNull { recordReviews ->
                    val a = recordReviews.firstOrNull { it.validatorId == validators[i] }
                    val b = recordReviews.firstOrNull { it.validatorId == validators[j] }
                    if (a != null && b != null) a.decision to b.decision else null
                }
                if (paired.size >= 2) {
                    kappaForTwoRaters(paired)?.let(kappas::add)
                }
            }
        }
        return kappas.takeIf { it.isNotEmpty() }?.average()
    }

    private fun kappaForTwoRaters(pairs: List<Pair<String, String>>): Double? {
        if (pairs.isEmpty()) return null
        val categories = pairs.flatMap { listOf(it.first, it.second) }.toSet()
        val n = pairs.size.toDouble()
        val observed = pairs.count { it.first == it.second }.toDouble() / n
        val expected = categories.sumOf { category ->
            val pa = pairs.count { it.first == category }.toDouble() / n
            val pb = pairs.count { it.second == category }.toDouble() / n
            pa * pb
        }
        return if (1.0 - expected < 1e-12) null else (observed - expected) / (1.0 - expected)
    }

    /**
     * Standard Fleiss kappa requires the same number of ratings per item.
     * We therefore return null rather than silently applying the formula to
     * mixed-rating records. Records with fewer than two independent ratings
     * are excluded from the reliability calculation.
     */
    private fun fleissKappa(reviews: List<ValidationReview>): Double? {
        val units = reviews.groupBy { it.recordType to it.recordId }
            .mapValues { (_, rs) -> rs.distinctBy(ValidationReview::validatorId) }
            .filterValues { it.size >= 2 }
            .values
        if (units.isEmpty()) return null
        val ratingCounts = units.map { it.size }.distinct()
        if (ratingCounts.size != 1) return null
        val n = ratingCounts.single()
        val categories = units.flatMap { unit -> unit.map(ValidationReview::decision) }.toSet()
        if (categories.isEmpty()) return null

        val itemAgreement = units.map { unit ->
            val counts = categories.associateWith { category -> unit.count { it.decision == category } }
            counts.values.sumOf { count -> count * (count - 1) }.toDouble() / (n * (n - 1)).toDouble()
        }.average()

        val totalRatings = units.size * n.toDouble()
        val categoryProportions = categories.map { category ->
            units.sumOf { unit -> unit.count { it.decision == category } }.toDouble() / totalRatings
        }
        val chanceAgreement = categoryProportions.sumOf { it * it }
        return if (1.0 - chanceAgreement < 1e-12) null else (itemAgreement - chanceAgreement) / (1.0 - chanceAgreement)
    }
}
