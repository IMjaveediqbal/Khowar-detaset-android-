package com.example.data.remote

import android.content.Context
import com.example.data.local.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Converts Room review-queue changes into durable outbox operations. */
class RoomCloudSync(
    private val context: Context,
    private val database: AppDatabase
) {
    private val scheduler = SyncScheduler(context, database)
    private val auth = FirebaseAuth.getInstance()

    fun start(scope: CoroutineScope) {
        scope.launch { database.lexiconDao().getReviewQueue().collectLatest { entries -> entries.forEach { enqueue("LEXICON", it.id) } } }
        scope.launch { database.sentenceDao().getReviewQueue().collectLatest { entries -> entries.forEach { enqueue("SENTENCE", it.id) } } }
        scope.launch { database.speechDao().getReviewQueue().collectLatest { entries -> entries.forEach { enqueue("SPEECH", it.id) } } }
        scope.launch { database.storyDao().getReviewQueue().collectLatest { entries -> entries.forEach { enqueue("STORY", it.id) } } }
        scope.launch { database.knowledgeDao().getReviewQueue().collectLatest { entries -> entries.forEach { enqueue("KNOWLEDGE", it.id) } } }
        scope.launch { database.imageDao().getReviewQueue().collectLatest { entries -> entries.forEach { enqueue("IMAGE", it.id) } } }

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null && user.isEmailVerified) {
                scope.launch {
                    database.lexiconDao().getReviewQueue().first().forEach { enqueue("LEXICON", it.id) }
                    database.sentenceDao().getReviewQueue().first().forEach { enqueue("SENTENCE", it.id) }
                    database.speechDao().getReviewQueue().first().forEach { enqueue("SPEECH", it.id) }
                    database.storyDao().getReviewQueue().first().forEach { enqueue("STORY", it.id) }
                    database.knowledgeDao().getReviewQueue().first().forEach { enqueue("KNOWLEDGE", it.id) }
                    database.imageDao().getReviewQueue().first().forEach { enqueue("IMAGE", it.id) }
                }
            }
        }
    }

    private suspend fun enqueue(recordType: String, recordId: String) {
        val user = auth.currentUser ?: return
        if (!user.isEmailVerified) return
        runCatching { scheduler.enqueue(recordType, recordId) }
    }
}
