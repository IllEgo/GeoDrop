package com.e3hi.geodrop.data

import android.util.Log
import com.e3hi.geodrop.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.e3hi.geodrop.util.GroupPreferences
import com.e3hi.geodrop.util.PilotFeatureFlags
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.tasks.await

/**
 * Your existing Drop shape — matches other parts of the app that read:
 * text / lat / lng / createdBy / createdAt
 */


class FirestoreRepo(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val drops = db.collection("drops")
    private val users = db.collection("users")
    private val usernames = db.collection("usernames")
    private val reports = db.collection("reports")
    private val functions = Firebase.functions(BuildConfig.FIREBASE_FUNCTIONS_REGION)
    private val claimExplorerUsernameCallableUnavailable = AtomicBoolean(false)

    private val huntChains = db.collection("huntChains")

    private fun userGroupsCollection(userId: String) =
        users.document(userId).collection("groups")

    private fun userInventoryCollection(userId: String) =
        users.document(userId).collection("inventory")

    private fun userBlockedCreatorsCollection(userId: String) =
        users.document(userId).collection("blockedCreators")

    private fun userNotificationTokensCollection(userId: String) =
        users.document(userId).collection("notificationTokens")

    private fun userHuntProgressCollection(userId: String) =
        users.document(userId).collection("huntProgress")

    /**
     * NEW: Suspend API. Writes a drop and returns the new document id.
     */
    suspend fun addDrop(drop: Drop): String {
        validateEnabledDropFeatures(drop)
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
        val validationError = runCatching { validateEnabledDropFeatures(drop) }.exceptionOrNull()
        if (validationError != null) {
            onError(validationError)
            return
        }
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

    suspend fun fetchUserGroupMemberships(userId: String): List<GroupMembership> {
        if (userId.isBlank()) return emptyList()

        val snapshot = userGroupsCollection(userId).get().await()
        return snapshot.documents
            .mapNotNull { doc ->
                val code = doc.getString("code") ?: doc.id
                val normalized = GroupPreferences.normalizeGroupCode(code) ?: return@mapNotNull null
                val storedRole = GroupRole.fromRaw(doc.getString("role"))
                val ownerId = doc.getString("ownerId")?.takeIf { it.isNotBlank() }
                val resolvedRole = when {
                    ownerId != null && ownerId == userId -> GroupRole.OWNER
                    ownerId != null && ownerId != userId -> storedRole
                    storedRole == GroupRole.OWNER -> GroupRole.OWNER
                    else -> GroupRole.OWNER
                }
                val resolvedOwnerId = ownerId ?: if (resolvedRole == GroupRole.OWNER) userId else ownerId
                GroupMembership(
                    code = normalized,
                    ownerId = resolvedOwnerId,
                    role = resolvedRole
                )
            }
            .distinctBy { it.code }
            .sortedBy { it.code }
    }

    fun listenForUserGroupMemberships(
        userId: String,
        onChanged: (List<GroupMembership>) -> Unit,
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

            val memberships = snapshot.documents
                .mapNotNull { doc ->
                    val code = doc.getString("code") ?: doc.id
                    val normalized = GroupPreferences.normalizeGroupCode(code) ?: return@mapNotNull null
                    val storedRole = GroupRole.fromRaw(doc.getString("role"))
                    val ownerId = doc.getString("ownerId")?.takeIf { it.isNotBlank() }
                    val resolvedRole = when {
                        ownerId != null && ownerId == userId -> GroupRole.OWNER
                        ownerId != null && ownerId != userId -> storedRole
                        storedRole == GroupRole.OWNER -> GroupRole.OWNER
                        else -> GroupRole.OWNER
                    }
                    val resolvedOwnerId = ownerId ?: if (resolvedRole == GroupRole.OWNER) userId else ownerId
                    GroupMembership(
                        code = normalized,
                        ownerId = resolvedOwnerId,
                        role = resolvedRole
                    )
                }
                .distinctBy { it.code }
                .sortedBy { it.code }
            onChanged(memberships)
        }
    }

    suspend fun joinGroup(
        userId: String,
        code: String,
        allowCreateIfMissing: Boolean
    ): GroupMembership {
        if (userId.isBlank()) throw IllegalArgumentException("User id required to join group")
        val normalized = GroupPreferences.normalizeGroupCode(code)
            ?: throw IllegalArgumentException("Invalid group code")
        val action = if (allowCreateIfMissing) "CREATE" else "JOIN"
        val result = try {
            functions.getHttpsCallable("manageGroup")
                .call(mapOf("action" to action, "code" to normalized))
                .await()
        } catch (error: FirebaseFunctionsException) {
            when (error.code) {
                FirebaseFunctionsException.Code.ALREADY_EXISTS ->
                    throw GroupAlreadyExistsException(normalized)
                FirebaseFunctionsException.Code.NOT_FOUND ->
                    throw GroupNotFoundException(normalized)
                else -> throw error
            }
        }
        val payload = result.data as? Map<*, *>
            ?: throw IllegalStateException("Group service returned an invalid response")
        val returnedCode = payload["code"]?.toString()
            ?.let(GroupPreferences::normalizeGroupCode)
            ?: normalized
        val ownerId = payload["ownerId"]?.toString()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Group service returned no owner")
        val role = GroupRole.fromRaw(payload["role"]?.toString())
        return GroupMembership(code = returnedCode, ownerId = ownerId, role = role)
    }

    suspend fun leaveGroup(userId: String, code: String) {
        if (userId.isBlank()) return
        val normalized = GroupPreferences.normalizeGroupCode(code) ?: return
        functions.getHttpsCallable("manageGroup")
            .call(mapOf("action" to "LEAVE", "code" to normalized))
            .await()
    }

    suspend fun isGroupOwner(userId: String, code: String): Boolean {
        if (userId.isBlank()) return false
        val normalized = GroupPreferences.normalizeGroupCode(code) ?: return false
        val snapshot = userGroupsCollection(userId).document(normalized).get().await()
        val ownerId = snapshot.getString("ownerId")?.takeIf { it.isNotBlank() }
        return snapshot.exists() && ownerId == userId &&
            GroupRole.fromRaw(snapshot.getString("role")) == GroupRole.OWNER
    }

    suspend fun fetchUserInventory(userId: String): NoteInventory.Snapshot {
        if (userId.isBlank()) return NoteInventory.Snapshot(emptyList(), emptySet())

        val snapshot = userInventoryCollection(userId).get().await()
        val collected = mutableListOf<CollectedNote>()
        val ignored = mutableSetOf<String>()

        snapshot.documents.forEach { doc ->
            when (doc.getString("state")?.uppercase(Locale.US) ?: STATE_COLLECTED) {
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
                when (doc.getString("state")?.uppercase(Locale.US) ?: STATE_COLLECTED) {
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

    suspend fun registerMessagingToken(userId: String, token: String, platform: String = "android") {
        val trimmedUserId = userId.trim()
        val trimmedToken = token.trim()
        if (trimmedUserId.isEmpty() || trimmedToken.isEmpty()) return

        val payload = hashMapOf<String, Any>(
            "token" to trimmedToken,
            "platform" to platform,
            "updatedAt" to System.currentTimeMillis()
        )

        userNotificationTokensCollection(trimmedUserId)
            .document(trimmedToken)
            .set(payload, SetOptions.merge())
            .await()

        Log.d("GeoDrop", "Registered messaging token for $trimmedUserId")
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
        // The stored `role` is the only source of truth for the account type (task 2.7).
        // Business metadata is deliberately NOT used to infer BUSINESS: firestore.rules
        // gates business drop creation on the stored role, so inferring here handed out
        // the business UI to accounts the server still treats as explorers. Profiles that
        // predate the field are normalized server-side by
        // `functions/scripts/normalize-account-roles.js`.
        val existingRole = UserRole.fromRaw(storedRoleRaw)
        val storedDisplayName = snapshot.getString("displayName")?.takeIf { it.isNotBlank() }
        val storedUsername = snapshot.getString("username")?.takeIf { it.isNotBlank() }
        val storedCreatedAtRaw = snapshot.get("createdAt")
        val storedCreatedAt = storedCreatedAtRaw.toMillisOrNull()
        val resolvedCreatedAt = storedCreatedAt ?: System.currentTimeMillis()
        val resolvedDisplayName = storedDisplayName ?: displayName?.takeIf { it.isNotBlank() }

        // Only client-authored fields are written here. `businessName`/`businessCategories`
        // are server-authored (see firestore.rules) and would be rejected. The NSFW
        // preference is no longer written at all (task 2.8): nothing reads it, and
        // firestore.rules still forces any surviving value to false. Legacy values on
        // existing documents are cleared server-side by
        // `functions/scripts/backfill-launch-fields.js`.
        val updates = hashMapOf<String, Any?>()
        if (!snapshot.exists()) {
            updates["role"] = UserRole.EXPLORER.name
            updates["displayName"] = resolvedDisplayName
            updates["createdAt"] = storedCreatedAtRaw ?: resolvedCreatedAt
        } else {
            if (snapshot.getString("role").isNullOrBlank()) {
                updates["role"] = existingRole.name
            }
            if (resolvedDisplayName != null && resolvedDisplayName != storedDisplayName) {
                updates["displayName"] = resolvedDisplayName
            }
            if (!snapshot.contains("createdAt")) {
                updates["createdAt"] = resolvedCreatedAt
            }
        }

        if (updates.isNotEmpty()) {
            docRef.set(updates, SetOptions.merge()).await()
        }

        return UserProfile(
            id = userId,
            displayName = resolvedDisplayName,
            username = storedUsername,
            memberSince = storedCreatedAt ?: resolvedCreatedAt,
            role = existingRole,
            businessName = existingBusinessName,
            businessCategories = existingBusinessCategories
        )
    }

    private fun Any?.toMillisOrNull(): Long? = when (this) {
        is Number -> this.toLong()
        is Timestamp -> this.toDate().time
        is Date -> this.time
        else -> null
    }?.takeIf { it > 0L }

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

        functions.getHttpsCallable("updateBusinessProfile")
            .call(
                mapOf(
                    "businessName" to sanitizedName,
                    "businessCategories" to categories.map { it.id }
                )
            )
            .await()

        return profile.copy(
            role = UserRole.BUSINESS,
            businessName = sanitizedName,
            businessCategories = categories
        )
    }

    suspend fun updateDisplayName(userId: String, displayName: String): UserProfile {
        val profile = ensureUserProfile(userId)
        if (userId.isBlank()) return profile

        val trimmed = displayName.trim()
        val updates = hashMapOf<String, Any?>("displayName" to trimmed.ifBlank { null })
        users.document(userId).set(updates, SetOptions.merge()).await()

        return profile.copy(displayName = trimmed.ifBlank { null })
    }

    suspend fun updateExplorerUsername(userId: String, desiredUsername: String): UserProfile {
        val profile = ensureUserProfile(userId)
        if (userId.isBlank()) return profile

        val sanitized = ExplorerUsername.sanitize(desiredUsername)
        val claimed = claimExplorerUsernameRemote(userId, sanitized)

        val updates = hashMapOf<String, Any?>("username" to claimed)
        users.document(userId).set(updates, SetOptions.merge()).await()

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

            when {
                drop.isDeleted -> null
                !drop.isEnabledForRelease() -> null
                drop.isBusinessDrop() && drop.isExpired() -> null
                else -> drop
            }
        }
    }

    suspend fun getBusinessDrops(businessId: String): List<Drop> {
        if (businessId.isBlank()) return emptyList()

        val snapshot = drops
            .whereEqualTo("businessId", businessId)
            .whereEqualTo("createdBy", businessId)
            .whereEqualTo("isDeleted", false)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val drop = doc.toDrop()

            when {
                drop.isDeleted -> null
                !drop.isEnabledForRelease() -> null
                drop.isBusinessDrop() && drop.isExpired() -> null
                else -> drop
            }
        }
    }

    suspend fun migrateExplorerAccount(previousUserId: String, newUserId: String) {
        if (previousUserId.isBlank() || newUserId.isBlank() || previousUserId == newUserId) return

        try {
            val previousProfile = users.document(previousUserId).get().await()
            if (previousProfile.exists()) {
                // Copy only what a client is allowed to author on a profile. Copying the
                // whole document would carry `role`, business metadata, moderation state,
                // and the previous `username` — all server-authored, and all rejected by
                // firestore.rules (task 2.7). The username moves below, through the
                // callable that owns it.
                previousProfile.getString("displayName")?.takeIf { it.isNotBlank() }?.let { name ->
                    users.document(newUserId).set(mapOf("displayName" to name), SetOptions.merge()).await()
                }
                val previousUsername = previousProfile.getString("username")?.takeIf { it.isNotBlank() }
                if (!previousUsername.isNullOrBlank()) {
                    val sanitized = runCatching { ExplorerUsername.sanitize(previousUsername) }.getOrNull()
                    if (!sanitized.isNullOrBlank()) {
                        try {
                            claimExplorerUsernameRemote(
                                userId = newUserId,
                                sanitizedUsername = sanitized,
                                transferFrom = previousUserId
                            )
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
        allowedGroups: Set<String>
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

        val lockedDropIds = if (!userId.isNullOrBlank()) {
            runCatching { fetchLockedHuntDropIds(userId, normalizedGroups) }
                .getOrElse { emptySet() }
        } else {
            // Guests see only step-0 drops for any hunt; fetch locked (step > 0) drops to hide
            runCatching {
                fetchVisibleActiveDropDocuments(null, emptySet())
                    .mapNotNull { doc ->
                    val stepIndex = (doc.get("huntStepIndex") as? Number)?.toInt()
                    val huntId = doc.getString("huntId")?.takeIf { it.isNotBlank() }
                    if (stepIndex != null && stepIndex > 0 && huntId != null) doc.id else null
                }.toSet()
            }.getOrElse { emptySet() }
        }

        val visibleDocuments = fetchVisibleActiveDropDocuments(
            userId,
            normalizedGroups
        )

        return visibleDocuments.mapNotNull { doc ->
            val drop = doc.toDrop()

            if (drop.isDeleted) return@mapNotNull null
            if (!drop.isEnabledForRelease()) return@mapNotNull null
            if (drop.id in lockedDropIds) return@mapNotNull null

            if (!drop.isVisibleTo(userId, normalizedGroups)) return@mapNotNull null
            if (drop.isNsfw && drop.createdBy != userId) return@mapNotNull null
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

    suspend fun unblockDropCreator(userId: String, creatorId: String) {
        if (userId.isBlank() || creatorId.isBlank()) return
        userBlockedCreatorsCollection(userId).document(creatorId.trim()).delete().await()
    }

    suspend fun getBlockedCreatorIds(userId: String): List<String> {
        if (userId.isBlank()) return emptyList()
        return userBlockedCreatorsCollection(userId).get().await()
            .documents
            .mapNotNull { doc ->
                (doc.getString("creatorId") ?: doc.id).takeIf { it.isNotBlank() }
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

    suspend fun setDropLike(dropId: String, userId: String, status: DropLikeStatus) {
        if (dropId.isBlank() || userId.isBlank()) return

        db.runTransaction { transaction ->
            val docRef = drops.document(dropId)
            val snapshot = transaction.get(docRef)
            if (!snapshot.exists()) {
                throw IllegalStateException("Drop $dropId does not exist")
            }

            val currentLikeCount = snapshot.getLong("likeCount") ?: 0L
            @Suppress("UNCHECKED_CAST")
            val likedMap = snapshot.get("likedBy") as? Map<String, Any?> ?: emptyMap()
            val currentStatus = if (likedMap[userId] == true) {
                DropLikeStatus.LIKED
            } else {
                DropLikeStatus.NONE
            }

            if (currentStatus == status) {
                return@runTransaction
            }

            var updatedLikeCount = currentLikeCount

            when (currentStatus) {
                DropLikeStatus.LIKED ->
                    updatedLikeCount = (updatedLikeCount - 1).coerceAtLeast(0)
                DropLikeStatus.NONE -> Unit
            }

            when (status) {
                DropLikeStatus.LIKED -> updatedLikeCount += 1
                DropLikeStatus.NONE -> Unit
            }

            // The payload carries only like fields. Dislikes were removed at task
            // 2.6 and firestore.rules now rejects any write that touches them.
            val updates = mutableMapOf<String, Any>(
                "likeCount" to updatedLikeCount
            )

            when (status) {
                DropLikeStatus.LIKED -> updates["likedBy.$userId"] = true
                DropLikeStatus.NONE -> updates["likedBy.$userId"] = FieldValue.delete()
            }

            transaction.update(docRef, updates)
        }.await()

        val action = when (status) {
            DropLikeStatus.LIKED -> "liked"
            DropLikeStatus.NONE -> "cleared reaction on"
        }
        Log.d("GeoDrop", "User $userId $action drop $dropId")
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

    suspend fun fetchDropperUsername(dropId: String): String? {
        if (dropId.isBlank()) return null

        return try {
            val dropSnapshot = drops.document(dropId).get().await()
            if (!dropSnapshot.exists()) return null

            val creatorId = dropSnapshot.getString("createdBy")?.takeIf { it.isNotBlank() }
            val storedUsername = when {
                dropSnapshot.contains("dropperUsername") ->
                    dropSnapshot.getString("dropperUsername")
                dropSnapshot.contains("createdByUsername") ->
                    dropSnapshot.getString("createdByUsername")
                else -> null
            }?.trim()?.takeIf { it.isNotEmpty() }

            if (!storedUsername.isNullOrEmpty()) {
                return storedUsername
            }

            val creator = creatorId?.let { users.document(it).get().await() }
            creator?.getString("username")?.trim()?.takeIf { it.isNotEmpty() }
        } catch (error: Exception) {
            Log.w("GeoDrop", "Failed to resolve dropper username for $dropId", error)
            null
        }
    }

    /**
     * Task 4.3 (ADR P6) — redemption is server-only. The code is issued by the
     * `redeemDrop` callable and returned to this caller alone; it is never stored on
     * the drop, which every reader can see. There is no code to type any more.
     */
    suspend fun redeemDrop(
        dropId: String,
        userId: String
    ): RedemptionResult {
        if (!PilotFeatureFlags.couponsEnabled) {
            return RedemptionResult.Error("Offers are disabled for this release.")
        }
        if (dropId.isBlank() || userId.isBlank()) return RedemptionResult.Error("Missing identifiers")

        return try {
            val response = functions
                .getHttpsCallable("redeemDrop")
                .call(mapOf("dropId" to dropId))
                .await()

            @Suppress("UNCHECKED_CAST")
            val payload = response.data as? Map<String, Any?>
                ?: return RedemptionResult.Error("Redemption failed. Try again.")
            val code = (payload["code"] as? String)?.takeIf { it.isNotBlank() }
                ?: return RedemptionResult.Error("Redemption failed. Try again.")

            RedemptionResult.Success(
                redemptionCount = (payload["redemptionCount"] as? Number)?.toInt() ?: 0,
                redemptionLimit = (payload["redemptionLimit"] as? Number)?.toInt(),
                redeemedAt = (payload["redeemedAt"] as? Number)?.toLong()
                    ?: System.currentTimeMillis(),
                code = code
            )
        } catch (error: FirebaseFunctionsException) {
            when (error.code) {
                FirebaseFunctionsException.Code.ALREADY_EXISTS -> RedemptionResult.AlreadyRedeemed
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> RedemptionResult.OutOfRedemptions
                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> RedemptionResult.NotEligible
                else -> RedemptionResult.Error(error.message)
            }
        } catch (error: Exception) {
            Log.e("GeoDrop", "Redeem drop failed", error)
            RedemptionResult.Error(error.localizedMessage)
        }
    }

    private suspend fun claimExplorerUsernameRemote(
        userId: String,
        sanitizedUsername: String,
        transferFrom: String? = null
    ): String {
        if (claimExplorerUsernameCallableUnavailable.get()) {
            return claimExplorerUsernameFallback(userId, sanitizedUsername, transferFrom)
        }

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

                FirebaseFunctionsException.Code.NOT_FOUND -> {
                    val alreadyMarkedMissing =
                        claimExplorerUsernameCallableUnavailable.getAndSet(true)
                    if (!alreadyMarkedMissing) {
                        Log.w(
                            "GeoDrop",
                            "claimExplorerUsername function missing; falling back to client transaction",
                            error
                        )
                    }
                    return claimExplorerUsernameFallback(userId, sanitizedUsername, transferFrom)
                }

                FirebaseFunctionsException.Code.PERMISSION_DENIED -> {
                    Log.w(
                        "GeoDrop",
                        "claimExplorerUsername permission denied; falling back to client transaction",
                        error
                    )
                    return claimExplorerUsernameFallback(userId, sanitizedUsername, transferFrom)
                }

                FirebaseFunctionsException.Code.UNAVAILABLE,
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> {
                    Log.w(
                        "GeoDrop",
                        "claimExplorerUsername function unavailable; falling back to client transaction",
                        error
                    )
                    return claimExplorerUsernameFallback(userId, sanitizedUsername, transferFrom)
                }

                else -> throw error
            }
        }

        val data = result.data as? Map<*, *>
        val claimed = data?.get("username") as? String
        return claimed?.takeIf { it.isNotBlank() } ?: sanitizedUsername
    }

    private suspend fun claimExplorerUsernameFallback(
        userId: String,
        sanitizedUsername: String,
        transferFrom: String?
    ): String {
        return try {
            db.runTransaction { transaction ->
                val userRef = users.document(userId)
                val usernameRef = usernames.document(sanitizedUsername)

                val userSnapshot = transaction.get(userRef)
                val usernameSnapshot = transaction.get(usernameRef)

                val currentUsername = userSnapshot.getString("username")?.takeIf { it.isNotBlank() }
                val existingOwner = usernameSnapshot.getString("userId")?.takeIf { it.isNotBlank() }

                if (existingOwner != null && existingOwner != userId) {
                    if (transferFrom.isNullOrBlank() || transferFrom != existingOwner) {
                        throw IllegalStateException(
                            "That username is already taken. Try another one."
                        )
                    }
                }

                transaction.set(usernameRef, mapOf("userId" to userId), SetOptions.merge())

                if (currentUsername != null && currentUsername != sanitizedUsername) {
                    transaction.delete(usernames.document(currentUsername))
                }

                transaction.set(userRef, mapOf("username" to sanitizedUsername), SetOptions.merge())

                sanitizedUsername
            }.await()
        } catch (error: FirebaseFirestoreException) {
            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.w(
                    "GeoDrop",
                    "Username reservation denied by security rules; falling back to direct user document update",
                    error
                )
                return claimExplorerUsernameWithoutReservation(userId, sanitizedUsername)
            }
            throw IllegalStateException(
                "Couldn't update your username. Try again.",
                error
            )
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: Exception) {
            throw IllegalStateException(
                "Couldn't update your username. Try again.",
                error
            )
        }
    }

    private suspend fun claimExplorerUsernameWithoutReservation(
        userId: String,
        sanitizedUsername: String
    ): String {
        val existing = users
            .whereEqualTo("username", sanitizedUsername)
            .get()
            .await()
            .documents
            .firstOrNull { it.id != userId }

        if (existing != null) {
            throw IllegalStateException("That username is already taken. Try another one.")
        }

        users.document(userId)
            .set(mapOf("username" to sanitizedUsername), SetOptions.merge())
            .await()

        Log.w(
            "GeoDrop",
            "Username claimed without reservation; uniqueness relies on users collection query"
        )

        return sanitizedUsername
    }


    // ── Scavenger Hunt ───────────────────────────────────────────────────────

    suspend fun createHuntChain(chain: HuntChain): String {
        check(PilotFeatureFlags.huntsEnabled) {
            "Scavenger hunts are disabled for this release."
        }
        val data = hashMapOf<String, Any?>(
            "createdBy" to chain.createdBy,
            "createdAt" to chain.createdAt,
            "businessId" to chain.businessId,
            "businessName" to chain.businessName?.trim()?.takeIf { it.isNotEmpty() },
            "title" to chain.title.trim(),
            "description" to chain.description?.trim()?.takeIf { it.isNotEmpty() },
            "dropIds" to chain.dropIds,
            "isActive" to chain.isActive,
            "decayDays" to chain.decayDays?.takeIf { it > 0 },
            "totalSteps" to chain.totalSteps
        )
        val ref = huntChains.add(data).await()
        Log.d("GeoDrop", "Created hunt chain ${ref.id}")
        return ref.id
    }

    suspend fun fetchHuntChain(huntId: String): HuntChain? {
        if (huntId.isBlank()) return null
        val doc = huntChains.document(huntId).get().await()
        if (!doc.exists()) return null
        return HuntChain(
            id = doc.id,
            createdBy = doc.getString("createdBy") ?: "",
            createdAt = doc.getLong("createdAt") ?: 0L,
            businessId = doc.getString("businessId")?.takeIf { it.isNotBlank() },
            businessName = doc.getString("businessName")?.takeIf { it.isNotBlank() },
            title = doc.getString("title") ?: "",
            description = doc.getString("description")?.takeIf { it.isNotBlank() },
            dropIds = (doc.get("dropIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            isActive = doc.getBoolean("isActive") ?: true,
            decayDays = (doc.get("decayDays") as? Number)?.toInt()?.takeIf { it > 0 },
            totalSteps = (doc.get("totalSteps") as? Number)?.toInt() ?: 0
        )
    }

    suspend fun fetchUserHuntProgress(userId: String, huntId: String): HuntProgress? {
        if (userId.isBlank() || huntId.isBlank()) return null
        val doc = userHuntProgressCollection(userId).document(huntId).get().await()
        if (!doc.exists()) return null
        return doc.toHuntProgress()
    }

    /**
     * Advances the user's progress in a hunt after collecting a step.
     * Idempotent — safe to call multiple times for the same step.
     */
    suspend fun advanceHuntProgress(
        userId: String,
        huntId: String,
        collectedDropId: String,
        nextStepIndex: Int,
        totalSteps: Int
    ) {
        if (!PilotFeatureFlags.huntsEnabled) return
        if (userId.isBlank() || huntId.isBlank()) return
        val progressRef = userHuntProgressCollection(userId).document(huntId)
        val now = System.currentTimeMillis()
        db.runTransaction { transaction ->
            val snapshot = transaction.get(progressRef)
            if (snapshot.exists()) {
                val current = snapshot.toHuntProgress()
                // Idempotent: don't regress progress
                if (current.currentStepIndex >= nextStepIndex) return@runTransaction
                val newCompleted = (current.completedStepIds + collectedDropId).distinct()
                val isComplete = nextStepIndex >= totalSteps
                transaction.update(
                    progressRef,
                    mapOf(
                        "currentStepIndex" to nextStepIndex,
                        "completedStepIds" to newCompleted,
                        "completedAt" to if (isComplete) now else null
                    )
                )
            } else {
                val isComplete = nextStepIndex >= totalSteps
                transaction.set(
                    progressRef,
                    mapOf(
                        "huntId" to huntId,
                        "currentStepIndex" to nextStepIndex,
                        "completedStepIds" to listOf(collectedDropId),
                        "completedAt" to if (isComplete) now else null,
                        "startedAt" to now
                    )
                )
            }
        }.await()
        Log.d("GeoDrop", "Advanced hunt $huntId progress for $userId to step $nextStepIndex")
    }

    /**
     * Returns the set of drop IDs that are currently locked for the given user
     * (i.e. the user has not yet unlocked those hunt steps).
     */
    suspend fun fetchLockedHuntDropIds(
        userId: String,
        allowedGroups: Set<String> = emptySet()
    ): Set<String> {
        if (!PilotFeatureFlags.huntsEnabled) return emptySet()
        if (userId.isBlank()) return emptySet()
        val progressSnapshot = userHuntProgressCollection(userId).get().await()
        val progressByHuntId = progressSnapshot.documents.mapNotNull { doc ->
            val huntId = doc.id
            val currentStep = (doc.get("currentStepIndex") as? Number)?.toInt() ?: 0
            huntId to currentStep
        }.toMap()

        if (progressByHuntId.isEmpty()) {
            // User has no progress at all — only step-0 drops are visible, all others are locked.
            // We need to find all drops with huntStepIndex > 0 to exclude them.
            val allHuntDrops = fetchVisibleActiveDropDocuments(
                userId,
                allowedGroups
            )
            return allHuntDrops.mapNotNull { doc ->
                val stepIndex = (doc.get("huntStepIndex") as? Number)?.toInt()
                val huntId = doc.getString("huntId")?.takeIf { it.isNotBlank() }
                if (stepIndex != null && stepIndex > 0 && huntId != null) doc.id else null
            }.toSet()
        }

        // For hunts the user has started, lock steps beyond currentStepIndex.
        // For hunts the user has NOT started, lock all steps > 0.
        val startedHuntIds = progressByHuntId.keys

        val allHuntDrops = fetchVisibleActiveDropDocuments(
            userId,
            allowedGroups
        )

        return allHuntDrops.mapNotNull { doc ->
            val stepIndex = (doc.get("huntStepIndex") as? Number)?.toInt() ?: return@mapNotNull null
            val huntId = doc.getString("huntId")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val currentStep = progressByHuntId[huntId]
            val isLocked = if (currentStep != null) {
                stepIndex > currentStep
            } else {
                // Hunt not started — only step 0 is visible
                stepIndex > 0
            }
            if (isLocked) doc.id else null
        }.toSet()
    }

    private suspend fun fetchVisibleActiveDropDocuments(
        userId: String?,
        allowedGroups: Set<String>
    ): List<DocumentSnapshot> {
        // Server-flagged content is never listed. The viewer preference that used to
        // gate this was removed at task 2.8 along with the NSFW pilot flag; NSFW is a
        // deferred feature and prohibited by policy, so the filter is unconditional.
        fun applySafetyFilter(query: com.google.firebase.firestore.Query): com.google.firebase.firestore.Query {
            return query.whereEqualTo("isNsfw", false)
        }

        val documentsById = linkedMapOf<String, DocumentSnapshot>()
        val publicQuery = applySafetyFilter(
            drops
                .whereEqualTo("isDeleted", false)
                .whereEqualTo("visibility", "PUBLIC")
        )
        publicQuery.get().await().documents.forEach { documentsById[it.id] = it }

        if (!userId.isNullOrBlank()) {
            allowedGroups
                .mapNotNull { GroupPreferences.normalizeGroupCode(it) }
                .distinct()
                .forEach { groupCode ->
                    val groupQuery = applySafetyFilter(
                        drops
                            .whereEqualTo("isDeleted", false)
                            .whereEqualTo("visibility", "GROUP")
                            .whereEqualTo("groupCode", groupCode)
                    )
                    groupQuery.get().await().documents.forEach { documentsById[it.id] = it }
                }
        }

        return documentsById.values.toList()
    }

    private fun DocumentSnapshot.toHuntProgress(): HuntProgress {
        val completedStepIds = (get("completedStepIds") as? List<*>)
            ?.mapNotNull { it as? String } ?: emptyList()
        return HuntProgress(
            huntId = id,
            currentStepIndex = (get("currentStepIndex") as? Number)?.toInt() ?: 0,
            completedStepIds = completedStepIds,
            completedAt = getLong("completedAt"),
            startedAt = getLong("startedAt") ?: 0L
        )
    }

    /** Sets the huntId field on a drop document after chain creation. */
    suspend fun updateDropHuntId(dropId: String, huntId: String) {
        check(PilotFeatureFlags.huntsEnabled) {
            "Scavenger hunts are disabled for this release."
        }
        if (dropId.isBlank() || huntId.isBlank()) return
        drops.document(dropId).update("huntId", huntId).await()
    }

    // ── End Scavenger Hunt ───────────────────────────────────────────────────

    private fun Drop.prepareForSave(): Map<String, Any?> {
        val withTimestamp = if (createdAt > 0L) this else copy(createdAt = System.currentTimeMillis())
        val sanitizedDescription = withTimestamp.description?.trim()?.takeIf { it.isNotEmpty() }
        val sanitized = withTimestamp.copy(
            text = withTimestamp.text.trim(),
            description = sanitizedDescription,
            mediaUrl = withTimestamp.mediaUrl?.trim()?.takeIf { it.isNotEmpty() },
            mediaMimeType = withTimestamp.mediaMimeType?.trim()?.takeIf { it.isNotEmpty() },
            mediaData = withTimestamp.mediaData?.trim()?.takeIf { it.isNotEmpty() },
            mediaStoragePath = withTimestamp.mediaStoragePath?.trim()?.takeIf { it.isNotEmpty() },
            nsfwLabels = withTimestamp.nsfwLabels.mapNotNull { it.trim().takeIf { label -> label.isNotEmpty() } },
            businessId = withTimestamp.businessId?.takeIf { it.isNotBlank() },
            businessName = withTimestamp.businessName?.trim()?.takeIf { it.isNotEmpty() },
            redemptionLimit = withTimestamp.redemptionLimit?.takeIf { it > 0 },
            redemptionCount = withTimestamp.redemptionCount.coerceAtLeast(0),
            redeemedBy = withTimestamp.redeemedBy.filterKeys { it.isNotBlank() },
            reportedBy = withTimestamp.reportedBy.filterKeys { it.isNotBlank() },
            decayDays = withTimestamp.decayDays?.takeIf { it > 0 },
            dropperUsername = withTimestamp.dropperUsername?.trim()?.takeIf { it.isNotEmpty() }
        )

        return hashMapOf<String, Any?>(
            "text" to sanitized.text,
            "description" to sanitized.description?.takeIf { it.isNotEmpty() },
            "lat" to sanitized.lat,
            "lng" to sanitized.lng,
            "createdBy" to sanitized.createdBy,
            "createdAt" to sanitized.createdAt,
            "isDeleted" to false,
            "deletedAt" to null,
            "visibility" to if (sanitized.groupCode.isNullOrBlank()) "PUBLIC" else "GROUP",
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
            "likeCount" to sanitized.likeCount,
            "likedBy" to sanitized.likedBy,
            "reportCount" to sanitized.reportCount,
            "reportedBy" to sanitized.reportedBy,
            "redemptionLimit" to sanitized.redemptionLimit,
            "redemptionCount" to sanitized.redemptionCount,
            "redeemedBy" to sanitized.redeemedBy,
            "decayDays" to sanitized.decayDays,
            "huntId" to sanitized.huntId?.takeIf { it.isNotBlank() },
            "huntStepIndex" to sanitized.huntStepIndex,
            "huntTotalSteps" to sanitized.huntTotalSteps
        ).apply {
            sanitized.dropperUsername?.takeIf { it.isNotBlank() }?.let { username ->
                this["dropperUsername"] = username
            }
        }
    }

    /**
     * Task 2.1 — every drop write must carry an authenticated, non-anonymous
     * creator. The Firestore rules enforce this server-side (task 1.2); this is
     * the client-side counterpart so no code path can even attempt the write.
     * Guest browsing and guest unlocking are unaffected — they do not write drops.
     */
    private fun requireIdentifiedCreator(drop: Drop) {
        val user = auth.currentUser
        check(user != null && !user.isAnonymous) {
            "Creating a drop requires a signed-in account."
        }
        check(drop.createdBy.isNotBlank() && drop.createdBy == user.uid) {
            "Drop creator must match the signed-in account."
        }
    }

    private fun validateEnabledDropFeatures(drop: Drop) {
        requireIdentifiedCreator(drop)
        check(PilotFeatureFlags.creationEnabled) {
            "Drop creation is disabled for this release."
        }
        check(PilotFeatureFlags.couponsEnabled || drop.dropType != DropType.RESTAURANT_COUPON) {
            "Offers are disabled for this release."
        }
        val hasMedia = drop.contentType != DropContentType.TEXT ||
            !drop.mediaUrl.isNullOrBlank() ||
            !drop.mediaMimeType.isNullOrBlank() ||
            !drop.mediaData.isNullOrBlank() ||
            !drop.mediaStoragePath.isNullOrBlank()
        check(PilotFeatureFlags.mediaEnabled || !hasMedia) {
            "Media drops are disabled for this release."
        }
        check(!drop.isNsfw) {
            "Mature content is prohibited."
        }
        val isHuntDrop = !drop.huntId.isNullOrBlank() ||
            drop.huntStepIndex != null || drop.huntTotalSteps != null
        check(PilotFeatureFlags.huntsEnabled || !isHuntDrop) {
            "Scavenger hunts are disabled for this release."
        }
    }

    private fun Drop.isEnabledForRelease(): Boolean {
        if (!PilotFeatureFlags.couponsEnabled && dropType == DropType.RESTAURANT_COUPON) {
            return false
        }
        if (!PilotFeatureFlags.mediaEnabled && contentType != DropContentType.TEXT) {
            return false
        }
        if (isNsfw) return false
        if (!PilotFeatureFlags.huntsEnabled && !huntId.isNullOrBlank()) return false
        return true
    }

    private fun DocumentSnapshot.toCollectedNoteOrNull(): CollectedNote? {
        val noteId = id.takeIf { it.isNotBlank() } ?: getString("id") ?: return null
        val collectedAt = getLong("collectedAt") ?: return null
        val nsfwLabels = (get("nsfwLabels") as? List<*>)
            ?.mapNotNull { value -> value?.toString()?.takeIf { it.isNotBlank() } }
            ?: emptyList()
        val likeCount = when (val raw = get("likeCount")) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull() ?: 0L
            else -> 0L
        }
        val isLiked = when (val raw = get("isLiked")) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> raw.equals("true", ignoreCase = true) || raw == "1"
            else -> false
        }
        return CollectedNote(
            id = noteId,
            text = getString("text") ?: "",
            description = getString("description")?.takeIf { it.isNotBlank() },
            contentType = DropContentType.fromRaw(getString("contentType")),
            mediaUrl = getString("mediaUrl")?.takeIf { it.isNotBlank() },
            mediaMimeType = getString("mediaMimeType")?.takeIf { it.isNotBlank() },
            mediaData = getString("mediaData")?.takeIf { it.isNotBlank() },
            lat = getDouble("lat"),
            lng = getDouble("lng"),
            groupCode = GroupPreferences.normalizeGroupCode(getString("groupCode")),
            dropCreatedAt = getLong("dropCreatedAt"),
            dropperUsername = when {
                contains("dropperUsername") -> getString("dropperUsername")
                contains("createdByUsername") -> getString("createdByUsername")
                else -> null
            }?.trim()?.takeIf { it.isNotEmpty() },
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
            likeCount = likeCount,
            isLiked = isLiked,
            collectedAt = collectedAt,
            isNsfw = (getBoolean("isNsfw") == true) || nsfwLabels.isNotEmpty(),
            nsfwLabels = nsfwLabels,
            huntId = getString("huntId")?.takeIf { it.isNotBlank() },
            huntStepIndex = (get("huntStepIndex") as? Number)?.toInt(),
            huntTotalSteps = (get("huntTotalSteps") as? Number)?.toInt()?.takeIf { it > 0 }
        )
    }

    private fun CollectedNote.toFirestoreData(): Map<String, Any?> {
        val data = mutableMapOf<String, Any?>(
            "id" to id,
            "text" to text,
            "description" to description,
            "contentType" to contentType.name,
            "collectedAt" to collectedAt,
            "dropType" to dropType.name,
            "redemptionCount" to redemptionCount,
            "isRedeemed" to isRedeemed,
            "likeCount" to likeCount,
            "isLiked" to isLiked,
            "isNsfw" to isNsfw,
            "nsfwLabels" to nsfwLabels
        )
        dropperUsername?.takeIf { it.isNotBlank() }?.let { data["dropperUsername"] = it }
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
        huntId?.takeIf { it.isNotBlank() }?.let { data["huntId"] = it }
        huntStepIndex?.let { data["huntStepIndex"] = it }
        huntTotalSteps?.takeIf { it > 0 }?.let { data["huntTotalSteps"] = it }
        return data
    }

    companion object {
        private const val STATE_COLLECTED = "COLLECTED"
        private const val STATE_IGNORED = "IGNORED"
    }
}

sealed class RedemptionResult {
    data class Success(
        val redemptionCount: Int,
        val redemptionLimit: Int?,
        val redeemedAt: Long,
        /** Issued by the server for this user alone (task 4.3). */
        val code: String? = null
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
