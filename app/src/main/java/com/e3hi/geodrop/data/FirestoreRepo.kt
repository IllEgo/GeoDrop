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
    private val users = db.collection("users")

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

    suspend fun ensureUserProfile(userId: String, displayName: String? = null): UserProfile {
        if (userId.isBlank()) return UserProfile()

        val docRef = users.document(userId)
        val snapshot = docRef.get().await()

        val existingRole = if (snapshot.exists()) {
            UserRole.fromRaw(snapshot.getString("role"))
        } else {
            UserRole.EXPLORER
        }
        val existingBusinessName = snapshot.getString("businessName")?.takeIf { it.isNotBlank() }
        val existingBusinessCategories = snapshot.get("businessCategories")
            ?.let { raw ->
                (raw as? List<*>)
                    ?.mapNotNull { item -> BusinessCategory.fromId(item?.toString()) }
                    ?: emptyList()
            }
            ?: emptyList()
        val storedDisplayName = snapshot.getString("displayName")?.takeIf { it.isNotBlank() }
        val storedNsfwEnabled = snapshot.getBoolean("nsfwEnabled") == true
        val storedNsfwEnabledAt = snapshot.getLong("nsfwEnabledAt")?.takeIf { it > 0L }
        val resolvedDisplayName = storedDisplayName ?: displayName?.takeIf { it.isNotBlank() }

        val updates = hashMapOf<String, Any?>()
        if (!snapshot.exists()) {
            updates["role"] = existingRole.name
            updates["businessName"] = existingBusinessName
            updates["businessCategories"] = existingBusinessCategories.map { it.id }
            updates["displayName"] = resolvedDisplayName
            updates["nsfwEnabled"] = storedNsfwEnabled
            updates["nsfwEnabledAt"] = storedNsfwEnabledAt
        } else {
            if (snapshot.getString("role").isNullOrBlank()) {
                updates["role"] = existingRole.name
            }
            if (resolvedDisplayName != null && resolvedDisplayName != storedDisplayName) {
                updates["displayName"] = resolvedDisplayName
            }
            if (!snapshot.contains("businessCategories")) {
                updates["businessCategories"] = existingBusinessCategories.map { it.id }
            }
            if (!snapshot.contains("nsfwEnabled")) {
                updates["nsfwEnabled"] = storedNsfwEnabled
            }
            if (!snapshot.contains("nsfwEnabledAt")) {
                updates["nsfwEnabledAt"] = storedNsfwEnabledAt
            }
        }

        if (updates.isNotEmpty()) {
            docRef.set(updates, SetOptions.merge()).await()
        }

        return UserProfile(
            id = userId,
            displayName = resolvedDisplayName,
            role = existingRole,
            businessName = existingBusinessName,
            businessCategories = existingBusinessCategories,
            nsfwEnabled = storedNsfwEnabled,
            nsfwEnabledAt = storedNsfwEnabledAt
        )
    }

    suspend fun updateNsfwPreference(userId: String, enabled: Boolean): UserProfile {
        val profile = ensureUserProfile(userId)
        if (userId.isBlank()) return profile

        val timestamp = if (enabled) System.currentTimeMillis() else null
        val updates = hashMapOf<String, Any?>(
            "nsfwEnabled" to enabled,
            "nsfwEnabledAt" to timestamp
        )

        users.document(userId).set(updates, SetOptions.merge()).await()

        return profile.copy(nsfwEnabled = enabled, nsfwEnabledAt = timestamp)
    }

    suspend fun updateBusinessProfile(
        userId: String,
        businessName: String,
        categories: List<BusinessCategory>
    ): UserProfile {
        val profile = ensureUserProfile(userId)
        if (userId.isBlank()) return profile

        val sanitizedName = businessName.trim()
        if (sanitizedName.isEmpty()) {
            throw IllegalArgumentException("Business name cannot be empty")
        }

        if (categories.isEmpty()) {
            throw IllegalArgumentException("Select at least one business category")
        }

        val updates = hashMapOf<String, Any?>(
            "businessName" to sanitizedName,
            "businessCategories" to categories.map { it.id },
            "role" to UserRole.BUSINESS.name
        )
        users.document(userId).set(updates, SetOptions.merge()).await()

        return profile.copy(
            role = UserRole.BUSINESS,
            businessName = sanitizedName,
            businessCategories = categories
        )
    }

    suspend fun getDropsForUser(uid: String): List<Drop> {
        val snapshot = drops
            .whereEqualTo("createdBy", uid)
            .whereEqualTo("isDeleted", false)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val drop = doc.toDrop()

            if (drop.isDeleted) null else drop
        }
    }

    suspend fun getBusinessDrops(businessId: String): List<Drop> {
        if (businessId.isBlank()) return emptyList()

        val snapshot = drops
            .whereEqualTo("businessId", businessId)
            .whereEqualTo("isDeleted", false)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val drop = doc.toDrop()

            if (drop.isDeleted) null else drop
        }
    }

    suspend fun getVisibleDropsForUser(
        userId: String?,
        allowedGroups: Set<String>,
        allowNsfw: Boolean
    ): List<Drop> {
        val normalizedGroups = allowedGroups
            .mapNotNull { GroupPreferences.normalizeGroupCode(it) }
            .toSet()

        val snapshot = drops
            .whereEqualTo("isDeleted", false)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val drop = doc.toDrop()

            if (drop.isDeleted) return@mapNotNull null

            if (!drop.isVisibleTo(userId, normalizedGroups)) return@mapNotNull null
            if (drop.isNsfw && !allowNsfw && drop.createdBy != userId) return@mapNotNull null
            if (drop.isExpired()) return@mapNotNull null

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

    suspend fun redeemDrop(
        dropId: String,
        userId: String,
        providedCode: String
    ): RedemptionResult {
        if (dropId.isBlank() || userId.isBlank()) return RedemptionResult.Error("Missing identifiers")
        val trimmedCode = providedCode.trim()
        if (trimmedCode.isEmpty()) return RedemptionResult.InvalidCode

        return try {
            db.runTransaction { transaction ->
                val docRef = drops.document(dropId)
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    return@runTransaction RedemptionResult.NotEligible
                }

                val dropType = DropType.fromRaw(snapshot.getString("dropType"))
                if (dropType != DropType.RESTAURANT_COUPON) {
                    return@runTransaction RedemptionResult.NotEligible
                }

                val storedCode = snapshot.getString("redemptionCode")?.takeIf { it.isNotBlank() }
                if (storedCode.isNullOrBlank()) {
                    return@runTransaction RedemptionResult.NotEligible
                }

                if (storedCode != trimmedCode) {
                    return@runTransaction RedemptionResult.InvalidCode
                }

                @Suppress("UNCHECKED_CAST")
                val redeemedMap = snapshot.get("redeemedBy") as? Map<String, Long> ?: emptyMap()
                if (redeemedMap.containsKey(userId)) {
                    return@runTransaction RedemptionResult.AlreadyRedeemed
                }

                val limit = snapshot.getLong("redemptionLimit")?.toInt()
                val currentCount = snapshot.getLong("redemptionCount")?.toInt() ?: 0
                if (limit != null && currentCount >= limit) {
                    return@runTransaction RedemptionResult.OutOfRedemptions
                }

                val newCount = currentCount + 1
                val timestamp = System.currentTimeMillis()
                val updateData = hashMapOf<String, Any>(
                    "redemptionCount" to newCount,
                    "redeemedBy.$userId" to timestamp
                )
                transaction.update(docRef, updateData as Map<String, Any>)

                RedemptionResult.Success(
                    redemptionCount = newCount,
                    redemptionLimit = limit,
                    redeemedAt = timestamp
                )
            }.await()
        } catch (error: Exception) {
            Log.e("GeoDrop", "Redeem drop failed", error)
            RedemptionResult.Error(error.localizedMessage)
        }
    }


    private fun Drop.prepareForSave(): Map<String, Any?> {
        val withTimestamp = if (createdAt > 0L) this else copy(createdAt = System.currentTimeMillis())
        val sanitized = withTimestamp.copy(
            text = withTimestamp.text.trim(),
            mediaUrl = withTimestamp.mediaUrl?.trim()?.takeIf { it.isNotEmpty() },
            mediaMimeType = withTimestamp.mediaMimeType?.trim()?.takeIf { it.isNotEmpty() },
            mediaData = withTimestamp.mediaData?.trim()?.takeIf { it.isNotEmpty() },
            mediaStoragePath = withTimestamp.mediaStoragePath?.trim()?.takeIf { it.isNotEmpty() },
            nsfwLabels = withTimestamp.nsfwLabels.mapNotNull { it.trim().takeIf { label -> label.isNotEmpty() } },
            businessId = withTimestamp.businessId?.takeIf { it.isNotBlank() },
            businessName = withTimestamp.businessName?.trim()?.takeIf { it.isNotEmpty() },
            redemptionCode = withTimestamp.redemptionCode?.trim()?.takeIf { it.isNotEmpty() },
            redemptionLimit = withTimestamp.redemptionLimit?.takeIf { it > 0 },
            redemptionCount = withTimestamp.redemptionCount.coerceAtLeast(0),
            redeemedBy = withTimestamp.redeemedBy.filterKeys { it.isNotBlank() },
            decayDays = withTimestamp.decayDays?.takeIf { it > 0 }
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
            "dropType" to sanitized.dropType.name,
            "businessId" to sanitized.businessId,
            "businessName" to sanitized.businessName,
            "contentType" to sanitized.contentType.name,
            "mediaUrl" to sanitized.mediaUrl,
            "mediaMimeType" to sanitized.mediaMimeType,
            "mediaData" to sanitized.mediaData,
            "mediaStoragePath" to sanitized.mediaStoragePath,
            "isNsfw" to sanitized.isNsfw,
            "nsfw" to sanitized.isNsfw,
            "nsfwLabels" to sanitized.nsfwLabels,
            "upvoteCount" to sanitized.upvoteCount,
            "downvoteCount" to sanitized.downvoteCount,
            "voteMap" to sanitized.voteMap,
            "redemptionCode" to sanitized.redemptionCode,
            "redemptionLimit" to sanitized.redemptionLimit,
            "redemptionCount" to sanitized.redemptionCount,
            "redeemedBy" to sanitized.redeemedBy,
            "decayDays" to sanitized.decayDays
        )
    }
}

sealed class RedemptionResult {
    data class Success(
        val redemptionCount: Int,
        val redemptionLimit: Int?,
        val redeemedAt: Long
    ) : RedemptionResult()

    object InvalidCode : RedemptionResult()
    object AlreadyRedeemed : RedemptionResult()
    object OutOfRedemptions : RedemptionResult()
    object NotEligible : RedemptionResult()
    data class Error(val message: String?) : RedemptionResult()
}

private fun Drop.isVisibleTo(userId: String?, allowedGroups: Set<String>): Boolean {
    if (userId != null && createdBy == userId) return false
    val dropGroup = GroupPreferences.normalizeGroupCode(groupCode)
    return dropGroup == null || dropGroup in allowedGroups
}
