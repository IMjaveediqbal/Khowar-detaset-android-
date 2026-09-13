package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** One wire contract shared by queued submissions and downloaded records. */
object DatasetCodec {
    val collections = listOf("lexicon", "sentences", "speech", "stories", "knowledge", "images")
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    fun collection(type: String): String = when (type.uppercase()) {
        "WORD", "LEXICON" -> "lexicon"
        "SENTENCE", "SENTENCES" -> "sentences"
        "STORY", "STORIES" -> "stories"
        "IMAGE", "IMAGES" -> "images"
        "SPEECH" -> "speech"
        "KNOWLEDGE" -> "knowledge"
        else -> error("Unsupported record type")
    }
    fun encode(record: Any): String = moshi.adapter<Any>(record.javaClass).toJson(record)
    suspend fun delete(db: AppDatabase, collection: String, id: String) {
        when(collection) {
            "lexicon" -> db.lexiconDao().deleteById(id)
            "sentences" -> db.sentenceDao().deleteById(id)
            "speech" -> db.speechDao().deleteById(id)
            "stories" -> db.storyDao().deleteById(id)
            "knowledge" -> db.knowledgeDao().deleteById(id)
            "images" -> db.imageDao().deleteById(id)
        }
    }
    suspend fun save(db: AppDatabase, collection: String, data: Map<String, Any?>) {
        if(data["tombstone"] == true) { delete(db,collection,data["id"].toString()); return }
        val map = data.toMutableMap()
        // Never trust device-local file paths received from another device.
        if (collection == "speech") map["audioFilePath"] = data["mediaPath"]?.let { "cloud:$it" } ?: ""
        if (collection == "images") map["localUri"] = data["mediaPath"]?.let { "cloud:$it" } ?: ""
        val json = moshi.adapter(Map::class.java).toJson(map)
        when(collection) {
            "lexicon" -> db.lexiconDao().insert(moshi.adapter(LexiconEntry::class.java).fromJson(json)!!)
            "sentences" -> db.sentenceDao().insert(moshi.adapter(SentenceEntry::class.java).fromJson(json)!!)
            "speech" -> db.speechDao().insert(moshi.adapter(SpeechRecording::class.java).fromJson(json)!!)
            "stories" -> db.storyDao().insert(moshi.adapter(StoryEntry::class.java).fromJson(json)!!)
            "knowledge" -> db.knowledgeDao().insert(moshi.adapter(KnowledgeEntry::class.java).fromJson(json)!!)
            "images" -> db.imageDao().insert(moshi.adapter(ImageEntry::class.java).fromJson(json)!!)
        }
    }
}
