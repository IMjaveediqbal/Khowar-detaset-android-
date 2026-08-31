package com.example.community

import com.google.firebase.Timestamp
import java.util.Date

/** Firestore DTOs for the community layer. */
data class CommunityPost(
    val id: String,
    val ownerUid: String,
    val authorProfileId: String,
    val authorName: String,
    val title: String,
    val body: String,
    val category: String,
    val solved: Boolean,
    val linkedRecordType: String?,
    val linkedRecordId: String?,
    val answerCount: Long,
    val voteScore: Long,
    val createdAt: Date?,
    val updatedAt: Date?
)

data class CommunityComment(
    val id: String,
    val ownerUid: String,
    val authorProfileId: String,
    val authorName: String,
    val body: String,
    val accepted: Boolean,
    val createdAt: Date?
)

internal fun Map<String, Any?>.asCommunityPost(id: String): CommunityPost = CommunityPost(
    id = id,
    ownerUid = this["ownerUid"] as? String ?: "",
    authorProfileId = this["authorProfileId"] as? String ?: "",
    authorName = this["authorName"] as? String ?: "Community member",
    title = this["title"] as? String ?: "",
    body = this["body"] as? String ?: "",
    category = this["category"] as? String ?: "General",
    solved = this["solved"] as? Boolean ?: false,
    linkedRecordType = this["linkedRecordType"] as? String,
    linkedRecordId = this["linkedRecordId"] as? String,
    answerCount = (this["answerCount"] as? Number)?.toLong() ?: 0L,
    voteScore = (this["voteScore"] as? Number)?.toLong() ?: 0L,
    createdAt = (this["createdAt"] as? Timestamp)?.toDate(),
    updatedAt = (this["updatedAt"] as? Timestamp)?.toDate()
)

internal fun Map<String, Any?>.asCommunityComment(id: String): CommunityComment = CommunityComment(
    id = id,
    ownerUid = this["ownerUid"] as? String ?: "",
    authorProfileId = this["authorProfileId"] as? String ?: "",
    authorName = this["authorName"] as? String ?: "Community member",
    body = this["body"] as? String ?: "",
    accepted = this["accepted"] as? Boolean ?: false,
    createdAt = (this["createdAt"] as? Timestamp)?.toDate()
)
