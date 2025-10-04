package com.e3hi.geodrop.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.e3hi.geodrop.util.GroupPreferences
import java.util.Locale
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
    private val reports = db.collection("reports")
    private val functions = Firebase.functions("us-central1")

    private fun userGroupsCollection(userId: String) =
        users.document(userId).collection("groups")

    private fun userInventoryCollection(userId: String) =
        users.document(userId).collection("inventory")

    private fun userBlockedCreatorsCollection(userId: String) =
        users.document(userId).collection("blockedCreators")

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

    suspend fun fetchUserGroups(userId: String): List<String> {
        if (userId.isBlank()) return emptyList()

        val snapshot = userGroupsCollection(userId).get().await()
        return snapshot.documents
            .mapNotNull { doc ->
                val code = doc.getString("code") ?: doc.id
                GroupPreferences.normalizeGroupCode(code)
            }
            .distinct()
            .sorted()
    }

    suspend fun replaceUserGroups(userId: String, codes: Collection<String>) {
        if (userId.isBlank()) return

        val normalized = codes
            .mapNotNull { GroupPreferences.normalizeGroupCode(it) }
            .distinct()

        val collection = userGroupsCollection(userId)
        val existing = collection.get().await()

        db.runBatch { batch ->
            existing.documents.forEach { doc -> batch.delete(doc.reference) }
            normalized.forEach { code ->
                val data = hashMapOf(
                    "code" to code,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(collection.document(code), data)
            }
        }.await()
    }

    fun listenForUserGroups(
        userId: String,
        onChanged: (List<String>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration? {
        if (userId.isBlank()) return null

        return userGroupsCollection(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                onChanged(emptyList())
                return@addSnapshotListener
            }

            val codes = snapshot.documents
                .mapNotNull { doc ->
                    val code = doc.getString("code") ?: doc.id
                    GroupPreferences.normalizeGroupCode(code)
                }
                .distinct()
                .sorted()
            onChanged(codes)
        }
    }

    suspend fun fetchUserInventory(userId: String): NoteInventory.Snapshot {
        if (userId.isBlank()) return NoteInventory.Snapshot(emptyList(), emptySet())

        val snapshot = userInventoryCollection(userId).get().await()
        val collected = mutableListOf<CollectedNote>()
        val ignored = mutableSetOf<String>()

        snapshot.documents.forEach { doc ->
            when (doc.getString("state")?.lowercase(Locale.US) ?: STATE_COLLECTED) {
                STATE_IGNORED -> {
                    val id = doc.id.takeIf { it.isNotBlank() }
                        ?: doc.getString("id")
                    if (!id.isNullOrBlank()) {
                        ignored += id
                    }
                }
                else -> {
                    doc.toCollectedNoteOrNull()?.let { collected += it }
                }
            }
        }

        collected.sortByDescending { it.collectedAt }
        return NoteInventory.Snapshot(collected, ignored)
    }

    suspend fun replaceUserInventory(userId: String, snapshot: NoteInventory.Snapshot) {
        if (userId.isBlank()) return

        val collection = userInventoryCollection(userId)
        val existing = collection.get().await()

        db.runBatch { batch ->
            existing.documents.forEach { doc -> batch.delete(doc.reference) }

            snapshot.collectedNotes.forEach { note ->
                val data = note.toFirestoreData().toMutableMap()
                data["state"] = STATE_COLLECTED
                data["updatedAt"] = System.currentTimeMillis()
                batch.set(collection.document(note.id), data)
            }

            snapshot.ignoredDropIds.forEach { rawId ->
                val id = rawId.trim()
                if (id.isNotEmpty()) {
                    val data = hashMapOf(
                        "state" to STATE_IGNORED,
                        "id" to id,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(collection.document(id), data)
                }
            }
        }.await()
    }

    fun listenForUserInventory(
        userId: String,
        onChanged: (NoteInventory.Snapshot) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration? {
        if (userId.isBlank()) return null

        return userInventoryCollection(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                onChanged(NoteInventory.Snapshot(emptyList(), emptySet()))
                return@addSnapshotListener
            }

            val collected = mutableListOf<CollectedNote>()
            val ignored = mutableSetOf<String>()

            snapshot.documents.forEach { doc ->
                when (doc.getString("state")?.lowercase(Locale.US) ?: STATE_COLLECTED) {
                    STATE_IGNORED -> {
                        val id = doc.id.takeIf { it.isNotBlank() }
                            ?: doc.getString("id")
                        if (!id.isNullOrBlank()) {
                            ignored += id
                        }
                    }
                    else -> doc.toCollectedNoteOrNull()?.let { collected += it }
                }
            }

            collected.sortByDescending { it.collectedAt }
            onChanged(NoteInventory.Snapshot(collected, ignored))
        }
    }

    suspend fun ensureUserProfile(userId: String, displayName: String? = null): UserProfile {
        if (userId.isBlank()) return UserProfile()

        val docRef = users.document(userId)
        val snapshot = docRef.get().await()

        val storedRoleRaw = snapshot.getString("role")
        val existingBusinessName = snapshot.getString("businessName")?.takeIf { it.isNotBlank() }
        val existingBusinessCategories = snapshot.get("businessCategories")
            ?.let { raw ->
                (raw as? List<*>)
                    ?.mapNotNull { item -> BusinessCategory.fromId(item?.toString()) }
                    ?: emptyList()
            }
            ?: emptyList()
        val hasBusinessMetadata = !existingBusinessName.isNullOrBlank() || existingBusinessCategories.isNotEmpty()
        // Older business accounts may not have an explicit role stored yet, so infer it from
        // business metadata to keep their access.
        val existingRole = when {
            !storedRoleRaw.isNullOrBlank() -> {
                val parsed = UserRole.fromRaw(storedRoleRaw)
                if (parsed == UserRole.EXPLORER && hasBusinessMetadata) {
                    UserRole.BUSINESS
                } else {
                    parsed
                }
            }
            hasBusinessMetadata -> UserRole.BUSINESS
            else -> UserRole.EXPLORER
        }
        val storedDisplayName = snapshot.getString("displayName")?.takeIf { it.isNotBlank() }
        val storedUsername = snapshot.getString("username")?.takeIf { it.isNotBlank() }
        val storedNsfwEnabled = snapshot.getBoolean("nsfwEnabled") == true
        val storedNsfwEnabledAt = snapshot.getLong("nsfwEnabledAt")?.takeIf { it > 0L }
        val resolvedDisplayName = storedDisplayName ?: displayName?.takeIf { it.isNotBlank() }

        val updates = hashMapOf<String, Any?>()
        if (!snapshot.exists()) {
            updates["role"] = existingRole.name
            updates["businessName"] = existingBusinessName
            updates["businessCategories"] = existingBusinessCategories.map { it.id }
            updates["displayName"] = resolvedDisplayName
            storedUsername?.let { updates["username"] = it }
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
            username = storedUsername,
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

    suspend fun updateExplorerUsername(userId: String, desiredUsername: String): UserProfile {
        val profile = ensureUserProfile(userId)
        if (userId.isBlank()) return profile

        val sanitized = ExplorerUsername.sanitize(desiredUsername)
        val claimed = claimExplorerUsernameRemote(sanitized)

        return profile.copy(username = claimed)
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

    suspend fun migrateExplorerAccount(previousUserId: String, newUserId: String) {
        if (previousUserId.isBlank() || newUserId.isBlank() || previousUserId == newUserId) return

        try {
            val previousProfile = users.document(previousUserId).get().await()
            if (previousProfile.exists()) {
                val data = previousProfile.data ?: emptyMap<String, Any?>()
                users.document(newUserId).set(data, SetOptions.merge()).await()
                val previousUsername = previousProfile.getString("username")?.takeIf { it.isNotBlank() }
                if (!previousUsername.isNullOrBlank()) {
                    val sanitized = runCatching { ExplorerUsername.sanitize(previousUsername) }.getOrNull()
                    if (!sanitized.isNullOrBlank()) {
                        try {
                            claimExplorerUsernameRemote(sanitized, transferFrom = previousUserId)
                        } catch (error: Exception) {
                            Log.w(
                                "GeoDrop",
                                "Failed to transfer username $previousUsername from $previousUserId",
                                error
                            )
                        }
                    }
                }
            }

            val existingDrops = drops
                .whereEqualTo("createdBy", previousUserId)
                .get()
                .await()

            existingDrops.documents.forEach { doc ->
                doc.reference.update("createdBy", newUserId).await()
            }
        } catch (error: Exception) {
            Log.e("GeoDrop", "Failed to migrate explorer account", error)
            throw error
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

        val blockedCreators = if (userId.isNullOrBlank()) {
            emptySet()
        } else {
            val snapshot = try {
                userBlockedCreatorsCollection(userId).get().await()
            } catch (error: FirebaseFirestoreException) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(
                        "GeoDrop",
                        "Missing permission to load blocked creators for $userId; continuing without filters.",
                        error
                    )
                    null
                } else {
                    throw error
                }
            }

            snapshot?.documents?.mapNotNull { doc ->
                val fromId = doc.id.takeIf { it.isNotBlank() }
                val explicit = doc.getString("creatorId")?.takeIf { it.isNotBlank() }
                fromId ?: explicit
            }?.toSet() ?: emptySet()
        }

        val snapshot = drops
            .whereEqualTo("isDeleted", false)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val drop = doc.toDrop()

            if (drop.isDeleted) return@mapNotNull null

            if (!drop.isVisibleTo(userId, normalizedGroups)) return@mapNotNull null
            if (drop.isNsfw && !allowNsfw && drop.createdBy != userId) return@mapNotNull null
            if (!userId.isNullOrBlank() && drop.reportedBy.containsKey(userId)) return@mapNotNull null
            if (!drop.createdBy.isNullOrBlank() && drop.createdBy in blockedCreators) return@mapNotNull null
            if (drop.isExpired()) return@mapNotNull null

            drop
        }
    }

    suspend fun submitDropReport(
        dropId: String,
        reporterId: String,
        reasonCodes: Collection<String>,
        additionalContext: Map<String, Any?> = emptyMap()
    ) {
        if (dropId.isBlank() || reporterId.isBlank()) return

        val sanitizedReasons = reasonCodes
            .mapNotNull { code ->
                val trimmed = code.trim()
                trimmed.takeIf { it.isNotEmpty() }
            }
            .distinct()
            .ifEmpty { listOf("unspecified") }

        val now = System.currentTimeMillis()
        val dropRef = drops.document(dropId)
        val dropSnapshot = runCatching { dropRef.get().await() }.getOrNull()
        val dropExists = dropSnapshot?.exists() == true
        val dropMetadata = dropSnapshot
            ?.takeIf { it.exists() }
            ?.toDrop()
            ?.toModerationSnapshot()

        val reportData = hashMapOf<String, Any?>(
            "dropId" to dropId,
            "reportedBy" to reporterId,
            "reportedAt" to now,
            "reasonCodes" to sanitizedReasons,
            "status" to "pending"
        )

        if (additionalContext.isNotEmpty()) {
            reportData["context"] = additionalContext
        }
        if (dropMetadata != null) {
            reportData["dropSnapshot"] = dropMetadata
        }

        reports.add(reportData).await()

        if (!dropExists) return

        db.runTransaction { transaction ->
            val snapshot = transaction.get(dropRef)
            if (!snapshot.exists()) {
                return@runTransaction
            }

            @Suppress("UNCHECKED_CAST")
            val current = snapshot.get("reportedBy") as? Map<String, Any?>
            val alreadyReported = current?.containsKey(reporterId) == true
            val updates = hashMapOf<String, Any?>(
                "reportedBy.$reporterId" to now
            )
            if (!alreadyReported) {
                val currentCount = snapshot.getLong("reportCount") ?: 0L
                updates["reportCount"] = currentCount + 1
            }

            transaction.set(dropRef, updates, SetOptions.merge())
        }.await()
    }

    suspend fun blockDropCreator(userId: String, creatorId: String) {
        if (userId.isBlank()) return
        val sanitized = creatorId.trim()
        if (sanitized.isEmpty()) return

        val data = hashMapOf<String, Any?>(
            "creatorId" to sanitized,
            "blockedAt" to System.currentTimeMillis()
        )

        userBlockedCreatorsCollection(userId)
            .document(sanitized)
            .set(data, SetOptions.merge())
            .await()
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


    private suspend fun claimExplorerUsernameRemote(
        sanitizedUsername: String,
        transferFrom: String? = null
    ): String {
        val payload = hashMapOf<String, Any?>("desiredUsername" to sanitizedUsername)
        if (!transferFrom.isNullOrBlank()) {
            payload["allowTransferFrom"] = transferFrom
        }

        val callable = functions.getHttpsCallable("claimExplorerUsername")

        val result = try {
            callable.call(payload).await()
        } catch (error: FirebaseFunctionsException) {
            when (error.code) {
                FirebaseFunctionsException.Code.ALREADY_EXISTS -> {
                    throw IllegalStateException(
                        "That username is already taken. Try another one.",
                        error
                    )
                }

                FirebaseFunctionsException.Code.INVALID_ARGUMENT -> {
                    val reason = (error.details as? Map<*, *>)
                        ?.get("reason")
                        ?.toString()
                    val validationError = when (reason) {
                        "TOO_SHORT" -> ExplorerUsername.ValidationError.TOO_SHORT
                        "TOO_LONG" -> ExplorerUsername.ValidationError.TOO_LONG
                        "INVALID_CHARACTERS" -> ExplorerUsername.ValidationError.INVALID_CHARACTERS
                        else -> null
                    }
                    if (validationError != null) {
                        throw ExplorerUsername.InvalidUsernameException(validationError)
                    }
                    throw error
                }

                FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                    throw IllegalStateException(
                        "Sign in again to update your username.",
                        error
                    )
                }

                else -> throw error
            }
        }

        val data = result.data as? Map<*, *>
        val claimed = data?.get("username") as? String
        return claimed?.takeIf { it.isNotBlank() } ?: sanitizedUsername
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
            reportedBy = withTimestamp.reportedBy.filterKeys { it.isNotBlank() },
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
            "reportCount" to sanitized.reportCount,
            "reportedBy" to sanitized.reportedBy,
            "redemptionCode" to sanitized.redemptionCode,
            "redemptionLimit" to sanitized.redemptionLimit,
            "redemptionCount" to sanitized.redemptionCount,
            "redeemedBy" to sanitized.redeemedBy,
            "decayDays" to sanitized.decayDays
        )
    }

    private fun DocumentSnapshot.toCollectedNoteOrNull(): CollectedNote? {
        val noteId = id.takeIf { it.isNotBlank() } ?: getString("id") ?: return null
        val collectedAt = getLong("collectedAt") ?: return null
        val nsfwLabels = (get("nsfwLabels") as? List<*>)
            ?.mapNotNull { value -> value?.toString()?.takeIf { it.isNotBlank() } }
            ?: emptyList()

        return CollectedNote(
            id = noteId,
            text = getString("text") ?: "",
            contentType = DropContentType.fromRaw(getString("contentType")),
            mediaUrl = getString("mediaUrl")?.takeIf { it.isNotBlank() },
            mediaMimeType = getString("mediaMimeType")?.takeIf { it.isNotBlank() },
            mediaData = getString("mediaData")?.takeIf { it.isNotBlank() },
            lat = getDouble("lat"),
            lng = getDouble("lng"),
            groupCode = GroupPreferences.normalizeGroupCode(getString("groupCode")),
            dropCreatedAt = getLong("dropCreatedAt"),
            decayDays = when (val raw = get("decayDays")) {
                is Number -> raw.toInt().takeIf { it > 0 }
                is String -> raw.toIntOrNull()?.takeIf { it > 0 }
                else -> null
            },
            dropType = DropType.fromRaw(getString("dropType")),
            businessId = getString("businessId")?.takeIf { it.isNotBlank() },
            businessName = getString("businessName")?.takeIf { it.isNotBlank() },
            redemptionLimit = when (val raw = get("redemptionLimit")) {
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull()
                else -> null
            },
            redemptionCount = when (val raw = get("redemptionCount")) {
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull() ?: 0
                else -> 0
            },
            isRedeemed = getBoolean("isRedeemed") == true,
            redeemedAt = getLong("redeemedAt"),
            collectedAt = collectedAt,
            isNsfw = (getBoolean("isNsfw") == true) || nsfwLabels.isNotEmpty(),
            nsfwLabels = nsfwLabels
        )
    }

    private fun CollectedNote.toFirestoreData(): Map<String, Any?> {
        val data = mutableMapOf<String, Any?>(
            "id" to id,
            "text" to text,
            "contentType" to contentType.name,
            "collectedAt" to collectedAt,
            "dropType" to dropType.name,
            "redemptionCount" to redemptionCount,
            "isRedeemed" to isRedeemed,
            "isNsfw" to isNsfw,
            "nsfwLabels" to nsfwLabels
        )
        mediaUrl?.takeIf { it.isNotBlank() }?.let { data["mediaUrl"] = it }
        mediaMimeType?.takeIf { it.isNotBlank() }?.let { data["mediaMimeType"] = it }
        mediaData?.takeIf { it.isNotBlank() }?.let { data["mediaData"] = it }
        lat?.let { data["lat"] = it }
        lng?.let { data["lng"] = it }
        groupCode?.let { code ->
            GroupPreferences.normalizeGroupCode(code)?.let { normalized -> data["groupCode"] = normalized }
        }
        dropCreatedAt?.let { data["dropCreatedAt"] = it }
        decayDays?.let { data["decayDays"] = it }
        businessId?.takeIf { it.isNotBlank() }?.let { data["businessId"] = it }
        businessName?.takeIf { it.isNotBlank() }?.let { data["businessName"] = it }
        redemptionLimit?.let { data["redemptionLimit"] = it }
        redeemedAt?.let { data["redeemedAt"] = it }
        return data
    }

    private fun Drop.toModerationSnapshot(): Map<String, Any?> {
        val data = mutableMapOf<String, Any?>()
        if (id.isNotBlank()) data["id"] = id
        if (text.isNotBlank()) data["text"] = text
        data["contentType"] = contentType.name
        data["createdAt"] = createdAt
        if (createdBy.isNotBlank()) data["createdBy"] = createdBy
        lat.takeIf { it != 0.0 }?.let { data["lat"] = it }
        lng.takeIf { it != 0.0 }?.let { data["lng"] = it }
        groupCode?.takeIf { it.isNotBlank() }?.let { data["groupCode"] = it }
        data["dropType"] = dropType.name
        businessId?.takeIf { it.isNotBlank() }?.let { data["businessId"] = it }
        businessName?.takeIf { it.isNotBlank() }?.let { data["businessName"] = it }
        if (!mediaUrl.isNullOrBlank()) data["mediaUrl"] = mediaUrl
        if (!mediaMimeType.isNullOrBlank()) data["mediaMimeType"] = mediaMimeType
        if (!mediaStoragePath.isNullOrBlank()) data["mediaStoragePath"] = mediaStoragePath
        data["isNsfw"] = isNsfw
        if (nsfwLabels.isNotEmpty()) data["nsfwLabels"] = nsfwLabels
        data["upvoteCount"] = upvoteCount
        data["downvoteCount"] = downvoteCount
        if (decayDays != null) data["decayDays"] = decayDays
        if (reportCount > 0) data["reportCount"] = reportCount
        if (reportedBy.isNotEmpty()) data["reportedBy"] = reportedBy.keys
        if (redemptionLimit != null) data["redemptionLimit"] = redemptionLimit
        if (redemptionCount > 0) data["redemptionCount"] = redemptionCount
        if (redeemedBy.isNotEmpty()) data["redeemedBy"] = redeemedBy
        return data
    }

    private companion object {
        private const val STATE_COLLECTED = "collected"
        private const val STATE_IGNORED = "ignored"
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
    if (userId != null && reportedBy.containsKey(userId)) return false
    return dropGroup == null || dropGroup in allowedGroups
}
