package com.example.data.remote

import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Mirrors new Room review-queue submissions to Firebase. */
class RoomCloudSync(
    private val database: AppDatabase,
    private val firebase: FirebaseSyncService
) {
    fun start(scope: CoroutineScope) {
        scope.launch { database.lexiconDao().getReviewQueue().collectLatest { it.forEach(firebase::syncLexicon) } }
        scope.launch { database.sentenceDao().getReviewQueue().collectLatest { it.forEach(firebase::syncSentence) } }
        scope.launch { database.speechDao().getReviewQueue().collectLatest { it.forEach(firebase::syncSpeech) } }
        scope.launch { database.storyDao().getReviewQueue().collectLatest { it.forEach(firebase::syncStory) } }
        scope.launch { database.knowledgeDao().getReviewQueue().collectLatest { it.forEach(firebase::syncKnowledge) } }
        scope.launch { database.imageDao().getReviewQueue().collectLatest { it.forEach(firebase::syncImage) } }
    }
}
