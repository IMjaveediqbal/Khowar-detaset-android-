package com.example.data.remote

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Mirrors Room's submitted/approved records to the shared Firebase dataset. */
class RoomCloudSync(
    private val database: AppDatabase,
    private val firebase: FirebaseSyncService
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            database.lexiconDao().getReviewQueue().collectLatest { it.forEach(firebase::syncLexicon) }
        }
        scope.launch {
            database.lexiconDao().getAllApproved().collectLatest { it.forEach(firebase::syncLexicon) }
        }
        scope.launch {
            database.sentenceDao().getReviewQueue().collectLatest { it.forEach(firebase::syncSentence) }
        }
        scope.launch {
            database.sentenceDao().getAllApproved().collectLatest { it.forEach(firebase::syncSentence) }
        }
        scope.launch {
            database.speechDao().getReviewQueue().collectLatest { it.forEach(firebase::syncSpeech) }
        }
        scope.launch {
            database.speechDao().getAllApproved().collectLatest { it.forEach(firebase::syncSpeech) }
        }
        scope.launch {
            database.storyDao().getReviewQueue().collectLatest { it.forEach(firebase::syncStory) }
        }
        scope.launch {
            database.storyDao().getAllApproved().collectLatest { it.forEach(firebase::syncStory) }
        }
        scope.launch {
            database.knowledgeDao().getReviewQueue().collectLatest { it.forEach(firebase::syncKnowledge) }
        }
        scope.launch {
            database.knowledgeDao().getAllApproved().collectLatest { it.forEach(firebase::syncKnowledge) }
        }
        scope.launch {
            database.imageDao().getReviewQueue().collectLatest { it.forEach(firebase::syncImage) }
        }
        scope.launch {
            database.imageDao().getAllApproved().collectLatest { it.forEach(firebase::syncImage) }
        }
    }
}
