package com.e3hi.geodrop.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.e3hi.geodrop.util.GroupPreferences
import kotlinx.coroutines.tasks.await

/**
 * Your existing Drop shape — matches other parts of the app that read:
 * text / lat / lng / createdBy / createdAt
 */


class FirestoreRepo(
    private val db: FirebaseFirestore = Firebase.firestore
) {
    private val drops = db.collection("drops")

    /**
     * NEW: Suspend API. Writes a drop and returns the new document id.
     */
    suspend fun addDrop(drop: Drop): String {
        val dropToSave = drop.prepareForSave()
        val ref: DocumentReference = drops.add(dropToSave).await()
        Log.d("GeoDrop", "Created drop ${ref.id}")
        return ref.id
    }

    /**
     * BACK-COMPAT: Callback-based write (if other places still use it).
     */
    fun createDrop(
        drop: Drop,
        onId: (String) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val dropToSave = drop.prepareForSave()

        drops.add(dropToSave)
            .addOnSuccessListener { ref ->
                Log.d("GeoDrop", "Created drop ${ref.id}")
                onId(ref.id)
            }
            .addOnFailureListener { e ->
                Log.e("GeoDrop", "Create drop FAILED", e)
                onError(e)
            }
    }

    suspend fun getDropsForUser(uid: String): List<Drop> {
        val snapshot = drops
            .whereEqualTo("createdBy", uid)
            .whereEqualTo("isDeleted", false)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val drop = doc.toObject(Drop::class.java)?.copy(id = doc.id)
                ?: Drop(id = doc.id)

            if (drop.isDeleted) null else drop
        }
    }

    suspend fun getVisibleDropsForUser(
        userId: String?,
        allowedGroups: Set<String>
    ): List<Drop> {
        val normalizedGroups = allowedGroups
            .mapNotNull { GroupPreferences.normalizeGroupCode(it) }
            .toSet()

        val snapshot = drops
            .whereEqualTo("isDeleted", false)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val drop = doc.toObject(Drop::class.java)?.copy(id = doc.id)
                ?: Drop(id = doc.id)

            if (drop.isDeleted) return@mapNotNull null

            if (!drop.isVisibleTo(userId, normalizedGroups)) return@mapNotNull null

            drop
        }
    }

    suspend fun deleteDrop(dropId: String) {
        if (dropId.isBlank()) return

        val updates = mapOf(
            "isDeleted" to true,
            "deletedAt" to System.currentTimeMillis()
        )

        drops
            .document(dropId)
            .set(updates, SetOptions.merge())
            .await()

        Log.d("GeoDrop", "Marked drop $dropId as deleted")
    }

    suspend fun voteOnDrop(dropId: String, userId: String, vote: DropVoteType) {
        if (dropId.isBlank() || userId.isBlank()) return

        db.runTransaction { transaction ->
            val docRef = drops.document(dropId)
            val snapshot = transaction.get(docRef)
            if (!snapshot.exists()) {
                throw IllegalStateException("Drop $dropId does not exist")
            }

            val currentUpvotes = snapshot.getLong("upvoteCount") ?: 0L
            val currentDownvotes = snapshot.getLong("downvoteCount") ?: 0L
            @Suppress("UNCHECKED_CAST")
            val currentMap = snapshot.get("voteMap") as? Map<String, Long> ?: emptyMap()

            val previousVote = currentMap[userId]?.toInt() ?: 0
            val targetVote = vote.value
            if (previousVote == targetVote) {
                return@runTransaction
            }

            var updatedUpvotes = currentUpvotes
            var updatedDownvotes = currentDownvotes
            val updatedMap = currentMap.toMutableMap()

            when (previousVote) {
                1 -> updatedUpvotes = (updatedUpvotes - 1).coerceAtLeast(0)
                -1 -> updatedDownvotes = (updatedDownvotes - 1).coerceAtLeast(0)
            }

            when (vote) {
                DropVoteType.UPVOTE -> {
                    updatedUpvotes += 1
                    updatedMap[userId] = 1L
                }
                DropVoteType.DOWNVOTE -> {
                    updatedDownvotes += 1
                    updatedMap[userId] = -1L
                }
                DropVoteType.NONE -> updatedMap.remove(userId)
            }

            val updateData = hashMapOf<String, Any?>(
                "upvoteCount" to updatedUpvotes,
                "downvoteCount" to updatedDownvotes,
                "voteMap" to updatedMap
            )

            transaction.update(docRef, updateData)
        }.await()

        Log.d("GeoDrop", "Recorded ${vote.name.lowercase()} for drop $dropId by $userId")
    }

    suspend fun markDropCollected(dropId: String, userId: String) {
        if (dropId.isBlank() || userId.isBlank()) return

        val docRef = drops.document(dropId)
        val collectedField = FieldPath.of("collectedBy", userId)

        // Only update the collectedBy.{uid} entry so Firestore security rules that restrict
        // writes to that nested key continue to pass. Updating unrelated fields (like
        // collectorIds) triggers PERMISSION_DENIED errors for non-owners. We also skip writes if
        // the drop is already marked as collected for this user so that follow-up sync attempts
        // (for example, when opening the detail screen after collecting) remain idempotent and do
        // not violate stricter security rules.
        val updated = db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (!snapshot.exists()) {
                return@runTransaction false
            }

            val alreadyCollected = snapshot.get(collectedField) as? Boolean ?: false
            if (alreadyCollected) {
                return@runTransaction false
            }

            transaction.update(docRef, collectedField, true)
            true
        }.await()

        if (updated) {
            Log.d("GeoDrop", "Marked drop $dropId as collected by $userId")
        } else {
            Log.d("GeoDrop", "Drop $dropId already marked as collected by $userId")
        }
    }


    private fun Drop.prepareForSave(): Map<String, Any?> {
        val withTimestamp = if (createdAt > 0L) this else copy(createdAt = System.currentTimeMillis())
        val sanitized = withTimestamp.copy(
            text = withTimestamp.text.trim(),
            mediaUrl = withTimestamp.mediaUrl?.trim()?.takeIf { it.isNotEmpty() },
            mediaMimeType = withTimestamp.mediaMimeType?.trim()?.takeIf { it.isNotEmpty() },
            mediaData = withTimestamp.mediaData?.trim()?.takeIf { it.isNotEmpty() }
        )

        return hashMapOf(
            "text" to sanitized.text,
            "lat" to sanitized.lat,
            "lng" to sanitized.lng,
            "createdBy" to sanitized.createdBy,
            "createdAt" to sanitized.createdAt,
            "isDeleted" to false,
            "deletedAt" to null,
            "groupCode" to sanitized.groupCode?.takeIf { it.isNotBlank() },
            "contentType" to sanitized.contentType.name,
            "mediaUrl" to sanitized.mediaUrl,
            "mediaMimeType" to sanitized.mediaMimeType,
            "mediaData" to sanitized.mediaData,
            "upvoteCount" to sanitized.upvoteCount,
            "downvoteCount" to sanitized.downvoteCount,
            "voteMap" to sanitized.voteMap
        )
    }
}

private fun Drop.isVisibleTo(userId: String?, allowedGroups: Set<String>): Boolean {
    if (userId != null && createdBy == userId) return false
    val dropGroup = GroupPreferences.normalizeGroupCode(groupCode)
    return dropGroup == null || dropGroup in allowedGroups
}
