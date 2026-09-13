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
    @Query("SELECT * FROM lexicon_entries WHERE status = 'APPROVED' AND (:dialect = 'All' OR dialectId = :dialect) AND (normalizedKhowarWord LIKE :khowar ESCAPE '\\' OR urduMeaning LIKE :khowar ESCAPE '\\' OR (:hasLatin AND lower(transliteration) LIKE :latin ESCAPE '\\') OR (:hasLatin AND lower(englishMeaning) LIKE :latin ESCAPE '\\')) ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun searchApproved(khowar: String, latin: String, hasLatin: Boolean, dialect: String, limit: Int): Flow<List<LexiconEntry>>

    @Query("DELETE FROM lexicon_entries WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("UPDATE lexicon_entries SET status = 'ARCHIVED' WHERE contributorId = :uid AND (:id IS NULL OR id = :id)")
    suspend fun archiveOwned(uid: String, id: String?)
    @Query("DELETE FROM lexicon_entries WHERE status != 'APPROVED' AND contributorId != :uid AND NOT EXISTS (SELECT 1 FROM cloud_outbox WHERE collection = 'lexicon' AND recordId = lexicon_entries.id)")
    suspend fun purgeOtherPrivate(uid: String)

    @Query("SELECT * FROM lexicon_entries WHERE status = 'APPROVED' ORDER BY createdAt DESC LIMIT :limit")
    fun getAllApproved(limit: Int = 200): Flow<List<LexiconEntry>>

    @Query("SELECT * FROM lexicon_entries WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC")
    fun getReviewQueue(): Flow<List<LexiconEntry>>

    @Query("SELECT * FROM lexicon_entries WHERE contributorId = :userId ORDER BY createdAt DESC")
    fun getByContributor(userId: String): Flow<List<LexiconEntry>>

    @Query("SELECT * FROM lexicon_entries WHERE id = :id")
    suspend fun getById(id: String): LexiconEntry?

    @Query("SELECT * FROM lexicon_entries WHERE normalizedKhowarWord = :normalizedWord LIMIT 5")
    suspend fun findDuplicates(normalizedWord: String): List<LexiconEntry>

    @Query("SELECT COUNT(*) FROM lexicon_entries WHERE status = 'APPROVED'")
    fun countApproved(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LexiconEntry)

    @Update
    suspend fun update(entry: LexiconEntry)

    @Delete
    suspend fun delete(entry: LexiconEntry)
}

@Dao
interface SentenceDao {
    @Query("SELECT * FROM sentences WHERE status = 'APPROVED' AND (:dialect = 'All' OR dialectId = :dialect) AND (normalizedText LIKE :khowar ESCAPE '\\' OR urduTranslation LIKE :khowar ESCAPE '\\' OR (:hasLatin AND lower(transliteration) LIKE :latin ESCAPE '\\') OR (:hasLatin AND lower(englishTranslation) LIKE :latin ESCAPE '\\')) ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun searchApproved(khowar: String, latin: String, hasLatin: Boolean, dialect: String, limit: Int): Flow<List<SentenceEntry>>

    @Query("DELETE FROM sentences WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("UPDATE sentences SET status = 'ARCHIVED' WHERE contributorId = :uid AND (:id IS NULL OR id = :id)")
    suspend fun archiveOwned(uid: String, id: String?)
    @Query("DELETE FROM sentences WHERE status != 'APPROVED' AND contributorId != :uid AND NOT EXISTS (SELECT 1 FROM cloud_outbox WHERE collection = 'sentences' AND recordId = sentences.id)")
    suspend fun purgeOtherPrivate(uid: String)

    @Query("SELECT * FROM sentences WHERE status = 'APPROVED' ORDER BY createdAt DESC LIMIT :limit")
    fun getAllApproved(limit: Int = 200): Flow<List<SentenceEntry>>

    @Query("SELECT * FROM sentences WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC")
    fun getReviewQueue(): Flow<List<SentenceEntry>>

    @Query("SELECT * FROM sentences WHERE contributorId = :userId ORDER BY createdAt DESC")
    fun getByContributor(userId: String): Flow<List<SentenceEntry>>

    @Query("SELECT * FROM sentences WHERE id = :id")
    suspend fun getById(id: String): SentenceEntry?

    @Query("SELECT * FROM sentences WHERE normalizedText = :normalizedText LIMIT 5")
    suspend fun findDuplicates(normalizedText: String): List<SentenceEntry>

    @Query("SELECT COUNT(*) FROM sentences WHERE status = 'APPROVED'")
    fun countApproved(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SentenceEntry)

    @Update
    suspend fun update(entry: SentenceEntry)

    @Delete
    suspend fun delete(entry: SentenceEntry)
}

@Dao
interface SpeechDao {
    @Query("SELECT * FROM speech_recordings WHERE status = 'APPROVED' AND (:dialect = 'All' OR dialectId = :dialect) AND (normalizedTranscript LIKE :khowar ESCAPE '\\' OR urduTranslation LIKE :khowar ESCAPE '\\' OR (:hasLatin AND lower(transliteration) LIKE :latin ESCAPE '\\') OR (:hasLatin AND lower(englishTranslation) LIKE :latin ESCAPE '\\')) ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun searchApproved(khowar: String, latin: String, hasLatin: Boolean, dialect: String, limit: Int): Flow<List<SpeechRecording>>

    @Query("DELETE FROM speech_recordings WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("UPDATE speech_recordings SET status = 'ARCHIVED' WHERE contributorId = :uid AND (:id IS NULL OR id = :id)")
    suspend fun archiveOwned(uid: String, id: String?)
    @Query("DELETE FROM speech_recordings WHERE status != 'APPROVED' AND contributorId != :uid AND NOT EXISTS (SELECT 1 FROM cloud_outbox WHERE collection = 'speech' AND recordId = speech_recordings.id)")
    suspend fun purgeOtherPrivate(uid: String)

    @Query("SELECT * FROM speech_recordings WHERE status = 'APPROVED' ORDER BY createdAt DESC LIMIT :limit")
    fun getAllApproved(limit: Int = 200): Flow<List<SpeechRecording>>

    @Query("SELECT * FROM speech_recordings WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC")
    fun getReviewQueue(): Flow<List<SpeechRecording>>

    @Query("SELECT * FROM speech_recordings WHERE contributorId = :userId ORDER BY createdAt DESC")
    fun getByContributor(userId: String): Flow<List<SpeechRecording>>

    @Query("SELECT * FROM speech_recordings WHERE id = :id")
    suspend fun getById(id: String): SpeechRecording?

    @Query("SELECT SUM(durationSeconds) FROM speech_recordings WHERE status = 'APPROVED'")
    fun totalApprovedDurationSeconds(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM speech_recordings WHERE status = 'APPROVED'")
    fun countApproved(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SpeechRecording)

    @Update
    suspend fun update(entry: SpeechRecording)

    @Delete
    suspend fun delete(entry: SpeechRecording)
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE status = 'APPROVED' AND (:dialect = 'All' OR dialectId = :dialect) AND (khowarText LIKE :khowar ESCAPE '\\' OR urduTranslation LIKE :khowar ESCAPE '\\' OR (:hasLatin AND lower(title) LIKE :latin ESCAPE '\\') OR (:hasLatin AND lower(transliteration) LIKE :latin ESCAPE '\\') OR (:hasLatin AND lower(englishTranslation) LIKE :latin ESCAPE '\\')) ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun searchApproved(khowar: String, latin: String, hasLatin: Boolean, dialect: String, limit: Int): Flow<List<StoryEntry>>

    @Query("DELETE FROM stories WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("UPDATE stories SET status = 'ARCHIVED' WHERE contributorId = :uid AND (:id IS NULL OR id = :id)")
    suspend fun archiveOwned(uid: String, id: String?)
    @Query("DELETE FROM stories WHERE status != 'APPROVED' AND contributorId != :uid AND NOT EXISTS (SELECT 1 FROM cloud_outbox WHERE collection = 'stories' AND recordId = stories.id)")
    suspend fun purgeOtherPrivate(uid: String)

    @Query("SELECT * FROM stories WHERE status = 'APPROVED' ORDER BY createdAt DESC LIMIT :limit")
    fun getAllApproved(limit: Int = 200): Flow<List<StoryEntry>>

    @Query("SELECT * FROM stories WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC")
    fun getReviewQueue(): Flow<List<StoryEntry>>

    @Query("SELECT * FROM stories WHERE contributorId = :userId ORDER BY createdAt DESC")
    fun getByContributor(userId: String): Flow<List<StoryEntry>>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getById(id: String): StoryEntry?

    @Query("SELECT COUNT(*) FROM stories WHERE status = 'APPROVED'")
    fun countApproved(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: StoryEntry)

    @Update
    suspend fun update(entry: StoryEntry)

    @Delete
    suspend fun delete(entry: StoryEntry)
}

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE status = 'APPROVED' AND (:dialect = 'All' OR :dialect = 'Other') AND (khowarLabel LIKE :khowar ESCAPE '\\' OR (:hasLatin AND lower(title) LIKE :latin ESCAPE '\\') OR (:hasLatin AND lower(englishLabel) LIKE :latin ESCAPE '\\')) ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun searchApproved(khowar: String, latin: String, hasLatin: Boolean, dialect: String, limit: Int): Flow<List<ImageEntry>>

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("UPDATE images SET status = 'ARCHIVED' WHERE contributorId = :uid AND (:id IS NULL OR id = :id)")
    suspend fun archiveOwned(uid: String, id: String?)
    @Query("DELETE FROM images WHERE status != 'APPROVED' AND contributorId != :uid AND NOT EXISTS (SELECT 1 FROM cloud_outbox WHERE collection = 'images' AND recordId = images.id)")
    suspend fun purgeOtherPrivate(uid: String)

    @Query("SELECT * FROM images WHERE status = 'APPROVED' ORDER BY createdAt DESC LIMIT :limit")
    fun getAllApproved(limit: Int = 200): Flow<List<ImageEntry>>

    @Query("SELECT * FROM images WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC")
    fun getReviewQueue(): Flow<List<ImageEntry>>

    @Query("SELECT * FROM images WHERE contributorId = :userId ORDER BY createdAt DESC")
    fun getByContributor(userId: String): Flow<List<ImageEntry>>

    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getById(id: String): ImageEntry?

    @Query("SELECT COUNT(*) FROM images WHERE status = 'APPROVED'")
    fun countApproved(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ImageEntry)

    @Update
    suspend fun update(entry: ImageEntry)

    @Delete
    suspend fun delete(entry: ImageEntry)
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge WHERE status = 'APPROVED' AND (:dialect = 'All' OR dialectId = :dialect) AND (khowarContent LIKE :khowar ESCAPE '\\' OR urduContent LIKE :khowar ESCAPE '\\' OR (:hasLatin AND lower(title) LIKE :latin ESCAPE '\\') OR (:hasLatin AND lower(transliteration) LIKE :latin ESCAPE '\\') OR (:hasLatin AND lower(englishContent) LIKE :latin ESCAPE '\\')) ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun searchApproved(khowar: String, latin: String, hasLatin: Boolean, dialect: String, limit: Int): Flow<List<KnowledgeEntry>>

    @Query("DELETE FROM knowledge WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("UPDATE knowledge SET status = 'ARCHIVED' WHERE contributorId = :uid AND (:id IS NULL OR id = :id)")
    suspend fun archiveOwned(uid: String, id: String?)
    @Query("DELETE FROM knowledge WHERE status != 'APPROVED' AND contributorId != :uid AND NOT EXISTS (SELECT 1 FROM cloud_outbox WHERE collection = 'knowledge' AND recordId = knowledge.id)")
    suspend fun purgeOtherPrivate(uid: String)

    @Query("SELECT * FROM knowledge WHERE status = 'APPROVED' ORDER BY createdAt DESC LIMIT :limit")
    fun getAllApproved(limit: Int = 200): Flow<List<KnowledgeEntry>>

    @Query("SELECT * FROM knowledge WHERE status IN ('SUBMITTED', 'UNDER_REVIEW') ORDER BY createdAt ASC")
    fun getReviewQueue(): Flow<List<KnowledgeEntry>>

    @Query("SELECT * FROM knowledge WHERE contributorId = :userId ORDER BY createdAt DESC")
    fun getByContributor(userId: String): Flow<List<KnowledgeEntry>>

    @Query("SELECT * FROM knowledge WHERE id = :id")
    suspend fun getById(id: String): KnowledgeEntry?

    @Query("SELECT COUNT(*) FROM knowledge WHERE status = 'APPROVED'")
    fun countApproved(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: KnowledgeEntry)

    @Update
    suspend fun update(entry: KnowledgeEntry)

    @Delete
    suspend fun delete(entry: KnowledgeEntry)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT COUNT(DISTINCT contributorId) FROM (" +
            "SELECT contributorId FROM lexicon_entries WHERE status = 'APPROVED' " +
            "UNION SELECT contributorId FROM sentences WHERE status = 'APPROVED' " +
            "UNION SELECT contributorId FROM speech_recordings WHERE status = 'APPROVED' " +
            "UNION SELECT contributorId FROM stories WHERE status = 'APPROVED' " +
            "UNION SELECT contributorId FROM images WHERE status = 'APPROVED' " +
            "UNION SELECT contributorId FROM knowledge WHERE status = 'APPROVED')")
    fun countActiveContributors(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)
}

@Dao
interface ValidationDao {
    @Query("SELECT * FROM validation_reviews WHERE recordId = :recordId ORDER BY createdAt DESC")
    fun getReviewsForRecord(recordId: String): Flow<List<ValidationReview>>

    /** Prevents a validator from accidentally creating multiple reviews for the same record. */
    @Query("SELECT COUNT(*) > 0 FROM validation_reviews WHERE recordType = :recordType AND recordId = :recordId AND validatorId = :validatorId")
    suspend fun hasReviewed(recordType: String, recordId: String, validatorId: String): Boolean

    /** Number of records that have at least two independent validators. */
    @Query("SELECT COUNT(*) FROM (SELECT recordType, recordId FROM validation_reviews GROUP BY recordType, recordId HAVING COUNT(DISTINCT validatorId) >= 2)")
    fun countMultiReviewedRecords(): Flow<Int>

    /** Number of multi-reviewed records where all validators selected the same decision. */
    @Query("SELECT COUNT(*) FROM (SELECT recordType, recordId FROM validation_reviews GROUP BY recordType, recordId HAVING COUNT(DISTINCT validatorId) >= 2 AND COUNT(DISTINCT decision) = 1)")
    fun countUnanimousMultiReviewedRecords(): Flow<Int>

    @Query("SELECT COALESCE(SUM(n), 0) FROM (SELECT COUNT(*) n FROM validation_reviews GROUP BY recordType, recordId HAVING COUNT(DISTINCT validatorId) >= 2)")
    fun countReviewsOnMultiReviewedRecords(): Flow<Int>

    /** Total validation records, useful for reporting coverage alongside agreement. */
    @Query("SELECT COUNT(*) FROM validation_reviews")
    fun countAllReviews(): Flow<Int>

    /** Number of distinct validators participating in validation. */
    @Query("SELECT COUNT(DISTINCT validatorId) FROM validation_reviews")
    fun countDistinctValidators(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ValidationReview)
}

@Dao
interface ConsentDao {
    @Query("SELECT * FROM consents WHERE contributorId = :userId")
    fun getConsentsForUser(userId: String): Flow<List<ConsentRecord>>

    @Query("SELECT * FROM consents WHERE subjectType = :type AND subjectId = :subjectId LIMIT 1")
    suspend fun getConsentForSubject(type: String, subjectId: String): ConsentRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsent(consent: ConsentRecord)

    @Update
    suspend fun updateConsent(consent: ConsentRecord)
}

@Dao
interface MetadataDao {
    @Query("SELECT * FROM regions")
    fun getAllRegions(): Flow<List<Region>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegions(regions: List<Region>)

    @Query("SELECT * FROM dialects")
    fun getAllDialects(): Flow<List<Dialect>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDialects(dialects: List<Dialect>)

    @Query("SELECT * FROM licenses")
    fun getAllLicenses(): Flow<List<License>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLicenses(licenses: List<License>)

    @Query("SELECT * FROM dataset_versions ORDER BY createdAt DESC")
    fun getDatasetVersions(): Flow<List<DatasetVersion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDatasetVersion(version: DatasetVersion)

    @Query("SELECT * FROM api_keys WHERE userId = :userId AND isRevoked = 0 ORDER BY createdAt DESC")
    fun getApiKeysForUser(userId: String): Flow<List<ApiKey>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(key: ApiKey)

    @Update
    suspend fun updateApiKey(key: ApiKey)

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT 100")
    fun getAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Query("SELECT * FROM moderation_reports ORDER BY createdAt DESC")
    fun getModerationReports(): Flow<List<ModerationReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModerationReport(report: ModerationReport)

    @Update
    suspend fun updateModerationReport(report: ModerationReport)
}

@Dao
interface CloudDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(operation: CloudOperation)
    @Query("SELECT * FROM cloud_outbox WHERE ownerUid = :uid AND state IN ('PENDING','UPLOADING','FAILED') ORDER BY updatedAt")
    suspend fun pending(uid: String): List<CloudOperation>
    @Query("SELECT * FROM cloud_outbox WHERE `key` = :key")
    suspend fun get(key: String): CloudOperation?
    @Query("SELECT * FROM cloud_outbox WHERE ownerUid = :uid ORDER BY updatedAt DESC")
    fun observe(uid: String): Flow<List<CloudOperation>>
}
