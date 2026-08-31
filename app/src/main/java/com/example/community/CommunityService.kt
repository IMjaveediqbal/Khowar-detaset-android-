package com.example.community

import com.example.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Real-time community service. Dataset records remain a separate trusted domain. */
class CommunityService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val posts = db.collection("communityPosts")

    fun observePosts(category: String? = null, limit: Long = 50): Flow<List<CommunityPost>> = callbackFlow {
        var query: Query = posts.orderBy("createdAt", Query.Direction.DESCENDING).limit(limit)
        if (!category.isNullOrBlank() && category != "All") query = query.whereEqualTo("category", category)
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.mapNotNull { it.data?.asCommunityPost(it.id) }.orEmpty())
        }
        awaitClose { registration.remove() }
    }

    fun observeComments(postId: String): Flow<List<CommunityComment>> = callbackFlow {
        val registration = posts.document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.data?.asCommunityComment(it.id) }.orEmpty())
            }
        awaitClose { registration.remove() }
    }

    suspend fun createPost(
        profile: User,
        title: String,
        body: String,
        category: String,
        linkedRecordType: String? = null,
        linkedRecordId: String? = null
    ): Result<String> = runCatching {
        require(auth.currentUser != null) { "Sign in to the community first." }
        require(title.trim().length in 5..160) { "Title must be 5–160 characters." }
        require(body.trim().length in 10..5000) { "Discussion must be 10–5000 characters." }
        require(category.trim().isNotEmpty()) { "Choose a category." }

        val ref = posts.document()
        ref.set(
            mapOf(
                "ownerUid" to auth.currentUser!!.uid,
                "authorProfileId" to profile.id,
                "authorName" to profile.displayName,
                "title" to title.trim(),
                "body" to body.trim(),
                "category" to category.trim(),
                "solved" to false,
                "linkedRecordType" to linkedRecordType?.trim()?.takeIf { it.isNotEmpty() },
                "linkedRecordId" to linkedRecordId?.trim()?.takeIf { it.isNotEmpty() },
                "answerCount" to 0L,
                "voteScore" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        ref.id
    }

    suspend fun addComment(postId: String, profile: User, body: String): Result<String> = runCatching {
        require(auth.currentUser != null) { "Sign in to reply." }
        require(body.trim().length in 2..3000) { "Reply must be 2–3000 characters." }

        val postRef = posts.document(postId)
        val commentRef = postRef.collection("comments").document()
        db.runTransaction { transaction ->
            transaction.set(
                commentRef,
                mapOf(
                    "ownerUid" to auth.currentUser!!.uid,
                    "authorProfileId" to profile.id,
                    "authorName" to profile.displayName,
                    "body" to body.trim(),
                    "accepted" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            transaction.update(postRef, "answerCount", FieldValue.increment(1))
        }.await()
        commentRef.id
    }

    suspend fun markSolved(postId: String, profile: User): Result<Unit> = runCatching {
        require(auth.currentUser != null) { "Sign in first." }
        val ref = posts.document(postId)
        val snap = ref.get().await()
        require(snap.getString("authorProfileId") == profile.id) { "Only the discussion author can mark it solved." }
        ref.update("solved", true, "updatedAt", FieldValue.serverTimestamp()).await()
    }

    suspend fun reportPost(postId: String, profile: User, reason: String): Result<Unit> = runCatching {
        require(auth.currentUser != null) { "Sign in first." }
        require(reason.trim().length >= 5) { "Please provide a reason." }
        posts.document(postId).collection("reports").add(
            mapOf(
                "ownerUid" to auth.currentUser!!.uid,
                "reporterProfileId" to profile.id,
                "reason" to reason.trim(),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun toggleVote(postId: String, profile: User): Result<Unit> = runCatching {
        require(auth.currentUser != null) { "Sign in first." }
        val uid = auth.currentUser!!.uid
        val voteRef = posts.document(postId).collection("votes").document(uid)
        val postRef = posts.document(postId)
        db.runTransaction { tx ->
            val existing = tx.get(voteRef)
            if (existing.exists()) {
                tx.delete(voteRef)
                tx.update(postRef, "voteScore", FieldValue.increment(-1))
            } else {
                tx.set(voteRef, mapOf("profileId" to profile.id, "createdAt" to FieldValue.serverTimestamp()))
                tx.update(postRef, "voteScore", FieldValue.increment(1))
            }
        }.await()
    }

    fun isAuthenticated(): Boolean = auth.currentUser != null
}
