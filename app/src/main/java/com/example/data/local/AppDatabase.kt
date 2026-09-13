package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Region::class, Dialect::class, License::class, LexiconEntry::class, SentenceEntry::class, SpeechRecording::class, StoryEntry::class, ImageEntry::class, KnowledgeEntry::class, ConsentRecord::class, ValidationReview::class, DatasetVersion::class, ApiKey::class, AuditLog::class, ModerationReport::class, CloudOperation::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cloudDao(): CloudDao
    abstract fun lexiconDao(): LexiconDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun speechDao(): SpeechDao
    abstract fun storyDao(): StoryDao
    abstract fun imageDao(): ImageDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun userDao(): UserDao
    abstract fun validationDao(): ValidationDao
    abstract fun consentDao(): ConsentDao
    abstract fun metadataDao(): MetadataDao

    companion object {
        /**
         * Version 1 -> 2 preserves existing local contributions while adding
         * research-governance metadata introduced in schema version 2.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lexicon_entries ADD COLUMN dataStage TEXT NOT NULL DEFAULT 'RAW'")
                db.execSQL("ALTER TABLE lexicon_entries ADD COLUMN datasetVersion TEXT")

                db.execSQL("ALTER TABLE sentences ADD COLUMN dataStage TEXT NOT NULL DEFAULT 'RAW'")
                db.execSQL("ALTER TABLE sentences ADD COLUMN datasetVersion TEXT")

                db.execSQL("ALTER TABLE speech_recordings ADD COLUMN dataStage TEXT NOT NULL DEFAULT 'RAW'")
                db.execSQL("ALTER TABLE speech_recordings ADD COLUMN datasetVersion TEXT")

                db.execSQL("ALTER TABLE dataset_versions ADD COLUMN speakerCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE dataset_versions ADD COLUMN dialectCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE dataset_versions ADD COLUMN validatedRecordCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE dataset_versions ADD COLUMN researchReadyRecordCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS cloud_outbox (`key` TEXT NOT NULL, collection TEXT NOT NULL, recordId TEXT NOT NULL, ownerUid TEXT NOT NULL, payload TEXT NOT NULL, state TEXT NOT NULL, error TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(`key`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lexicon_entries_status_createdAt ON lexicon_entries(status, createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lexicon_entries_contributorId ON lexicon_entries(contributorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sentences_status_createdAt ON sentences(status, createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sentences_contributorId ON sentences(contributorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_speech_recordings_status_createdAt ON speech_recordings(status, createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_speech_recordings_contributorId ON speech_recordings(contributorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stories_status_createdAt ON stories(status, createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stories_contributorId ON stories(contributorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_images_status_createdAt ON images(status, createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_images_contributorId ON images(contributorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_status_createdAt ON knowledge(status, createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_knowledge_contributorId ON knowledge(contributorId)")
                // Old local-only credentials are not usable server credentials.
                db.execSQL("DELETE FROM api_keys")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase = INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "khowar_dataset.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(AppDatabaseCallback(scope))
                .build()
            INSTANCE = instance
            instance
        }

        private class AppDatabaseCallback(private val scope: CoroutineScope) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) { populateSystemConfiguration(database) }
                }
            }

            suspend fun populateSystemConfiguration(database: AppDatabase) {
                val metadataDao = database.metadataDao()
                metadataDao.insertLicenses(listOf(
                    License("CC-BY-SA-4.0", "Creative Commons Attribution-ShareAlike 4.0", "CC BY-SA 4.0", "Permits sharing and adapting with attribution and share-alike terms.", true, true, true),
                    License("CC-BY-4.0", "Creative Commons Attribution 4.0", "CC BY 4.0", "Permits sharing and adapting with attribution.", true, true, false),
                    License("CC-0", "Creative Commons Zero / Public Domain", "CC0", "Public-domain dedication where legally applicable.", true, false, false),
                    License("RESEARCH_ONLY", "Academic & Non-Commercial Research License", "Research Only", "Restricted to non-commercial research, academic and linguistic study.", false, true, false)
                ))
                metadataDao.insertDialects(listOf(
                    Dialect("Central", "Central Chitrali (Chitral Town & Lower)", "مرکزی چترالی", "Central/lower Chitral variety."),
                    Dialect("Upper", "Upper Chitral (Booni, Mastuj, Laspur)", "بالا چترالی", "Upper Chitral variety."),
                    Dialect("Lotkuh", "Lotkuh / Western Dialect", "لوٹکوہ ژبان", "Lotkuh/Western variety."),
                    Dialect("Torkhow", "Torkhow & Mulkhow", "تورکھو / موڑکھو", "Torkhow and Mulkhow varieties."),
                    Dialect("Ghizer", "Ghizer / Yasin (Gilgit-Baltistan)", "غذر / یاسین", "Khowar varieties represented in Ghizer/Yasin."),
                    Dialect("Other", "Other / Unspecified", "نامعلوم / دیگر", "Unspecified or diaspora variety.")
                ))
                metadataDao.insertRegions(listOf(
                    Region("Chitral Lower", "Lower Chitral District", "ضلع لوئر چترال", "Khyber Pakhtunkhwa", "Lower Chitral"),
                    Region("Chitral Upper", "Upper Chitral District", "ضلع اپر چترال", "Khyber Pakhtunkhwa", "Upper Chitral"),
                    Region("Ghizer", "Ghizer District", "ضلع غذر", "Gilgit-Baltistan", "Ghizer"),
                    Region("Peshawar", "Peshawar (Diaspora)", "پشاور", "Khyber Pakhtunkhwa", "Peshawar"),
                    Region("Islamabad", "Islamabad / Rawalpindi", "اسلام آباد", "Federal", "Islamabad"),
                    Region("Diaspora", "International / Overseas Diaspora", "بیرون ملک", "Diaspora", "Global")
                ))
            }
        }
    }
}
