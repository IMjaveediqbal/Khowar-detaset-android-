package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Region::class, Dialect::class, License::class, LexiconEntry::class, SentenceEntry::class, SpeechRecording::class, StoryEntry::class, ImageEntry::class, KnowledgeEntry::class, ConsentRecord::class, ValidationReview::class, DatasetVersion::class, ApiKey::class, AuditLog::class, ModerationReport::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
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
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase = INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "khowar_dataset.db")
                .addCallback(AppDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }

        private class AppDatabaseCallback(private val scope: CoroutineScope) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database -> scope.launch(Dispatchers.IO) { populateSystemConfiguration(database) } }
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
