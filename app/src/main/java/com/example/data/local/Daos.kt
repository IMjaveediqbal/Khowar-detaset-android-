package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LexiconDao {
    @Query("SELECT * FROM lexicon_entries WHERE status = 'APPROVED' ORDER BY createdAt DESC") fun getAllApproved(): Flow<List<LexiconEntry>>
    @Query("SELECT * FROM lexicon_entries WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC") fun getReviewQueue(): Flow<List<LexiconEntry>>
    @Query("SELECT * FROM lexicon_entries WHERE contributorId = :userId ORDER BY createdAt DESC") fun getByContributor(userId: String): Flow<List<LexiconEntry>>
    @Query("SELECT * FROM lexicon_entries WHERE id = :id") suspend fun getById(id: String): LexiconEntry?
    @Query("SELECT * FROM lexicon_entries WHERE normalizedKhowarWord = :normalizedWord LIMIT 5") suspend fun findDuplicates(normalizedWord: String): List<LexiconEntry>
    @Query("SELECT COUNT(*) FROM lexicon_entries WHERE status = 'APPROVED'") fun countApproved(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entry: LexiconEntry)
    @Update suspend fun update(entry: LexiconEntry)
    @Delete suspend fun delete(entry: LexiconEntry)
}

@Dao
interface SentenceDao {
    @Query("SELECT * FROM sentences WHERE status = 'APPROVED' ORDER BY createdAt DESC") fun getAllApproved(): Flow<List<SentenceEntry>>
    @Query("SELECT * FROM sentences WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC") fun getReviewQueue(): Flow<List<SentenceEntry>>
    @Query("SELECT * FROM sentences WHERE contributorId = :userId ORDER BY createdAt DESC") fun getByContributor(userId: String): Flow<List<SentenceEntry>>
    @Query("SELECT * FROM sentences WHERE id = :id") suspend fun getById(id: String): SentenceEntry?
    @Query("SELECT * FROM sentences WHERE normalizedText = :normalizedText LIMIT 5") suspend fun findDuplicates(normalizedText: String): List<SentenceEntry>
    @Query("SELECT COUNT(*) FROM sentences WHERE status = 'APPROVED'") fun countApproved(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entry: SentenceEntry)
    @Update suspend fun update(entry: SentenceEntry)
    @Delete suspend fun delete(entry: SentenceEntry)
}

@Dao
interface SpeechDao {
    @Query("SELECT * FROM speech_recordings WHERE status = 'APPROVED' ORDER BY createdAt DESC") fun getAllApproved(): Flow<List<SpeechRecording>>
    @Query("SELECT * FROM speech_recordings WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC") fun getReviewQueue(): Flow<List<SpeechRecording>>
    @Query("SELECT * FROM speech_recordings WHERE contributorId = :userId ORDER BY createdAt DESC") fun getByContributor(userId: String): Flow<List<SpeechRecording>>
    @Query("SELECT * FROM speech_recordings WHERE id = :id") suspend fun getById(id: String): SpeechRecording?
    @Query("SELECT SUM(durationSeconds) FROM speech_recordings WHERE status = 'APPROVED'") fun totalApprovedDurationSeconds(): Flow<Double?>
    @Query("SELECT COUNT(*) FROM speech_recordings WHERE status = 'APPROVED'") fun countApproved(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entry: SpeechRecording)
    @Update suspend fun update(entry: SpeechRecording)
    @Delete suspend fun delete(entry: SpeechRecording)
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE status = 'APPROVED' ORDER BY createdAt DESC") fun getAllApproved(): Flow<List<StoryEntry>>
    @Query("SELECT * FROM stories WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC") fun getReviewQueue(): Flow<List<StoryEntry>>
    @Query("SELECT * FROM stories WHERE contributorId = :userId ORDER BY createdAt DESC") fun getByContributor(userId: String): Flow<List<StoryEntry>>
    @Query("SELECT * FROM stories WHERE id = :id") suspend fun getById(id: String): StoryEntry?
    @Query("SELECT COUNT(*) FROM stories WHERE status = 'APPROVED'") fun countApproved(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entry: StoryEntry)
    @Update suspend fun update(entry: StoryEntry)
    @Delete suspend fun delete(entry: StoryEntry)
}

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE status = 'APPROVED' ORDER BY createdAt DESC") fun getAllApproved(): Flow<List<ImageEntry>>
    @Query("SELECT * FROM images WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC") fun getReviewQueue(): Flow<List<ImageEntry>>
    @Query("SELECT * FROM images WHERE contributorId = :userId ORDER BY createdAt DESC") fun getByContributor(userId: String): Flow<List<ImageEntry>>
    @Query("SELECT * FROM images WHERE id = :id") suspend fun getById(id: String): ImageEntry?
    @Query("SELECT COUNT(*) FROM images WHERE status = 'APPROVED'") fun countApproved(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entry: ImageEntry)
    @Update suspend fun update(entry: ImageEntry)
    @Delete suspend fun delete(entry: ImageEntry)
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge WHERE status = 'APPROVED' ORDER BY createdAt DESC") fun getAllApproved(): Flow<List<KnowledgeEntry>>
    @Query("SELECT * FROM knowledge WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC") fun getReviewQueue(): Flow<List<KnowledgeEntry>>
    @Query("SELECT * FROM knowledge WHERE contributorId = :userId ORDER BY createdAt DESC") fun getByContributor(userId: String): Flow<List<KnowledgeEntry>>
    @Query("SELECT * FROM knowledge WHERE id = :id") suspend fun getById(id: String): KnowledgeEntry?
    @Query("SELECT COUNT(*) FROM knowledge WHERE status = 'APPROVED'") fun countApproved(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entry: KnowledgeEntry)
    @Update suspend fun update(entry: KnowledgeEntry)
    @Delete suspend fun delete(entry: KnowledgeEntry)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC") fun getAllUsers(): Flow<List<User>>
    @Query("SELECT * FROM users WHERE id = :id") fun getUserById(id: String): Flow<User?>
    @Query("SELECT * FROM users WHERE id = :id") suspend fun getById(id: String): User?
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1") suspend fun getUserByEmail(email: String): User?
    @Query("SELECT * FROM users WHERE firebaseUid = :firebaseUid LIMIT 1") suspend fun getUserByFirebaseUid(firebaseUid: String): User?
    @Query("SELECT COUNT(DISTINCT contributorId) FROM (SELECT contributorId FROM lexicon_entries WHERE status = 'APPROVED' UNION SELECT contributorId FROM sentences WHERE status = 'APPROVED' UNION SELECT contributorId FROM speech_recordings WHERE status = 'APPROVED' UNION SELECT contributorId FROM stories WHERE status = 'APPROVED' UNION SELECT contributorId FROM images WHERE status = 'APPROVED' UNION SELECT contributorId FROM knowledge WHERE status = 'APPROVED')") fun countActiveContributors(): Flow<Int>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(user: User)
    @Update suspend fun update(user: User)
}

@Dao
interface SyncOperationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(operation: SyncOperation): Long
    @Update suspend fun update(operation: SyncOperation)
    @Query("SELECT * FROM sync_operations WHERE state IN ('PENDING', 'FAILED') AND nextAttemptAt <= :now ORDER BY createdAt ASC LIMIT :limit") suspend fun getDue(now: Long, limit: Int): List<SyncOperation>
    @Query("SELECT * FROM sync_operations WHERE id = :id LIMIT 1") suspend fun getById(id: String): SyncOperation?
    @Query("UPDATE sync_operations SET state = 'UPLOADING', updatedAt = :now WHERE id = :id AND state IN ('PENDING', 'FAILED')") suspend fun markUploading(id: String, now: Long): Int
    @Query("UPDATE sync_operations SET state = 'COMPLETED', lastError = NULL, updatedAt = :now WHERE id = :id") suspend fun markCompleted(id: String, now: Long)
    @Query("UPDATE sync_operations SET state = 'FAILED', attempts = attempts + 1, lastError = :error, nextAttemptAt = :nextAttemptAt, updatedAt = :now WHERE id = :id") suspend fun markFailed(id: String, error: String, nextAttemptAt: Long, now: Long)
    @Query("UPDATE sync_operations SET state = 'PENDING', updatedAt = :now WHERE state = 'UPLOADING' AND updatedAt < :staleBefore") suspend fun recoverStale(staleBefore: Long, now: Long): Int
    @Query("SELECT COUNT(*) FROM sync_operations WHERE state IN ('PENDING', 'UPLOADING', 'FAILED')") fun countPending(): Flow<Int>
}

@Dao
interface ValidationDao {
    @Query("SELECT * FROM validation_reviews WHERE recordId = :recordId ORDER BY createdAt DESC") fun getReviewsForRecord(recordId: String): Flow<List<ValidationReview>>
    @Query("SELECT COUNT(*) > 0 FROM validation_reviews WHERE recordType = :recordType AND recordId = :recordId AND validatorId = :validatorId") suspend fun hasReviewed(recordType: String, recordId: String, validatorId: String): Boolean
    @Query("SELECT COUNT(*) FROM (SELECT recordType, recordId FROM validation_reviews GROUP BY recordType, recordId HAVING COUNT(DISTINCT validatorId) >= 2)") fun countMultiReviewedRecords(): Flow<Int>
    @Query("SELECT COUNT(*) FROM (SELECT recordType, recordId FROM validation_reviews GROUP BY recordType, recordId HAVING COUNT(DISTINCT validatorId) >= 2 AND COUNT(DISTINCT decision) = 1)") fun countUnanimousMultiReviewedRecords(): Flow<Int>
    @Query("SELECT COUNT(*) FROM validation_reviews") fun countAllReviews(): Flow<Int>
    @Query("SELECT COUNT(DISTINCT validatorId) FROM validation_reviews") fun countDistinctValidators(): Flow<Int>
    @Query("SELECT * FROM validation_reviews ORDER BY recordType, recordId, createdAt ASC") fun getAllReviews(): Flow<List<ValidationReview>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReview(review: ValidationReview)
}

@Dao
interface ConsentDao {
    @Query("SELECT * FROM consents WHERE contributorId = :userId") fun getConsentsForUser(userId: String): Flow<List<ConsentRecord>>
    @Query("SELECT * FROM consents WHERE subjectType = :type AND subjectId = :subjectId LIMIT 1") suspend fun getConsentForSubject(type: String, subjectId: String): ConsentRecord?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertConsent(consent: ConsentRecord)
    @Update suspend fun updateConsent(consent: ConsentRecord)
}

@Dao
interface MetadataDao {
    @Query("SELECT * FROM regions") fun getAllRegions(): Flow<List<Region>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRegions(regions: List<Region>)
    @Query("SELECT * FROM dialects") fun getAllDialects(): Flow<List<Dialect>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDialects(dialects: List<Dialect>)
    @Query("SELECT * FROM licenses") fun getAllLicenses(): Flow<List<License>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLicenses(licenses: List<License>)
    @Query("SELECT * FROM dataset_versions ORDER BY createdAt DESC") fun getDatasetVersions(): Flow<List<DatasetVersion>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDatasetVersion(version: DatasetVersion)
    @Query("SELECT * FROM api_keys WHERE userId = :userId AND isRevoked = 0 ORDER BY createdAt DESC") fun getApiKeysForUser(userId: String): Flow<List<ApiKey>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertApiKey(key: ApiKey)
    @Update suspend fun updateApiKey(key: ApiKey)
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT 100") fun getAuditLogs(): Flow<List<AuditLog>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAuditLog(log: AuditLog)
    @Query("SELECT * FROM moderation_reports ORDER BY createdAt DESC") fun getModerationReports(): Flow<List<ModerationReport>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertModerationReport(report: ModerationReport)
    @Update suspend fun updateModerationReport(report: ModerationReport)
}