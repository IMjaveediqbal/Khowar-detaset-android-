package com.example.data

import android.util.JsonWriter
import com.example.data.model.LexiconEntry
import com.example.data.model.SentenceEntry
import com.example.data.model.SpeechRecording
import java.io.StringWriter

/** Escapes all user-provided text safely when exporting dataset records as JSON/JSONL. */
object DatasetJsonSerializer {
    fun wordJson(entry: LexiconEntry): String = buildJson { json ->
        json.name("type").value("word")
        json.name("id").value(entry.id)
        json.name("khowar").value(entry.khowarWord)
        json.name("transliteration").value(entry.transliteration)
        json.name("english").value(entry.englishMeaning)
        json.name("urdu").value(entry.urduMeaning)
        json.name("pos").value(entry.partOfSpeech.name)
        json.name("dialect").value(entry.dialectId)
        json.name("region").value(entry.regionId)
        json.name("license").value(entry.licenseId)
    }

    fun sentenceJson(entry: SentenceEntry): String = buildJson { json ->
        json.name("type").value("sentence")
        json.name("id").value(entry.id)
        json.name("khowar").value(entry.khowarText)
        json.name("transliteration").value(entry.transliteration)
        json.name("english").value(entry.englishTranslation)
        json.name("urdu").value(entry.urduTranslation)
        json.name("dialect").value(entry.dialectId)
        json.name("region").value(entry.regionId)
        json.name("license").value(entry.licenseId)
    }

    fun speechJson(entry: SpeechRecording): String = buildJson { json ->
        json.name("type").value("speech")
        json.name("id").value(entry.id)
        json.name("speaker").value(entry.speakerPublicId)
        json.name("duration_s").value(entry.durationSeconds)
        json.name("transcript").value(entry.transcriptKhowar)
        json.name("english").value(entry.englishTranslation)
        json.name("urdu").value(entry.urduTranslation)
        json.name("dialect").value(entry.dialectId)
        json.name("region").value(entry.regionId)
        json.name("license").value(entry.licenseId)
    }

    private fun buildJson(write: (JsonWriter) -> Unit): String {
        val output = StringWriter()
        JsonWriter(output).use { writer ->
            writer.beginObject()
            write(writer)
            writer.endObject()
        }
        return output.toString()
    }
}
