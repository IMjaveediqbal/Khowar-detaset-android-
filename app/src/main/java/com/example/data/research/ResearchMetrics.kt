package com.example.data.research

import com.example.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ValidationMetrics(
    val totalReviews: Int = 0,
    val participatingValidators: Int = 0,
    val multiReviewedRecords: Int = 0,
    val unanimousMultiReviewedRecords: Int = 0,
    val percentAgreement: Double = 0.0,
    val averageReviewsPerMultiReviewedRecord: Double = 0.0
)

/**
 * Research reporting metrics derived only from stored validation records.
 * "Percent agreement" here means the share of records with >=2 distinct
 * validators whose recorded decisions are unanimous; it is not Cohen's kappa.
 */
class ResearchMetricsRepository(database: AppDatabase) {
    private val validationDao = database.validationDao()

    val validationMetrics: Flow<ValidationMetrics> = combine(
        validationDao.countAllReviews(),
        validationDao.countDistinctValidators(),
        validationDao.countMultiReviewedRecords(),
        validationDao.countUnanimousMultiReviewedRecords()
    ) { totalReviews, validators, multiReviewed, unanimous ->
        val agreement = if (multiReviewed > 0) {
            unanimous.toDouble() / multiReviewed.toDouble() * 100.0
        } else {
            0.0
        }
        val averageReviews = if (multiReviewed > 0) {
            totalReviews.toDouble() / multiReviewed.toDouble()
        } else {
            0.0
        }
        ValidationMetrics(
            totalReviews = totalReviews,
            participatingValidators = validators,
            multiReviewedRecords = multiReviewed,
            unanimousMultiReviewedRecords = unanimous,
            percentAgreement = agreement,
            averageReviewsPerMultiReviewedRecord = averageReviews
        )
    }
}
