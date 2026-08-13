package com.e3hi.geodrop.data

import com.e3hi.geodrop.BuildConfig
import com.e3hi.geodrop.util.R6RolloutPolicy
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.tasks.await

interface R7OrganizerGateway : R8OrganizerGateway {
    suspend fun loadAccessState(userId: String): R7OrganizerAccessState
    suspend fun createApplicationLink(): R7OrganizerApplicationLink
    suspend fun loadExperiences(userId: String): List<R7Experience>
    suspend fun createExperience(draft: R7ExperienceDraft): R7Experience
    suspend fun updateExperience(code: String, draft: R7ExperienceDraft): R7Experience
    suspend fun loadDrops(userId: String, experienceCode: String): List<R7OrganizerDropSummary>
    suspend fun loadDrop(dropId: String): R7OrganizerDrop
    suspend fun saveDrop(userId: String, draft: R7DropDraft): R7SaveDropResult
    suspend fun deleteDrop(dropId: String)
}

class R7OrganizerException(
    val userMessage: String,
    val retryable: Boolean,
    cause: Throwable? = null
) : Exception(userMessage, cause)

class FirebaseR7OrganizerGateway(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = Firebase.functions(BuildConfig.FIREBASE_FUNCTIONS_REGION),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : R7OrganizerGateway {

    override suspend fun loadRewardCodes(
        dropId: String,
        state: R8RewardCodeState?,
        searchCode: String?
    ): List<R8RewardCode> {
        val normalizedSearch = searchCode?.let(R8RewardPolicy::normalizeCode)
            ?.takeIf(String::isNotEmpty)
        normalizedSearch?.let { search ->
            R8RewardPolicy.validateSearchCode(search)?.let {
                throw R7OrganizerException(it, retryable = false)
            }
        }
        val root = call(
            "listRewardCodes",
            buildMap {
                put("apiVersion", apiVersion())
                put("dropId", dropId)
                put("limit", 100)
                state?.let { put("state", it.name) }
                normalizedSearch?.let { put("searchCode", it) }
            }
        )
        return (root["codes"] as? List<*>)?.mapNotNull { raw ->
            R8WireParser.rewardCode(raw as? Map<*, *> ?: return@mapNotNull null)
        }.orEmpty()
    }

    override suspend fun markRewardCodeUsed(dropId: String, code: String): Boolean {
        val root = call(
            "markRewardCodeUsed",
            mapOf(
                "apiVersion" to apiVersion(),
                "dropId" to dropId,
                "code" to R8RewardPolicy.normalizeCode(code)
            )
        )
        return root["changed"] as? Boolean ?: false
    }

    override suspend fun correctRewardCodeUse(
        dropId: String,
        code: String,
        reason: R8CorrectionReason
    ): Boolean {
        val root = call(
            "correctRewardCodeUse",
            mapOf(
                "apiVersion" to apiVersion(),
                "dropId" to dropId,
                "code" to R8RewardPolicy.normalizeCode(code),
                "reason" to reason.name
            )
        )
        return root["changed"] as? Boolean ?: false
    }

    override suspend fun loadResults(experienceCode: String): R8ExperienceResults {
        val analytics = firestore.collection("groups")
            .document(experienceCode.trim().uppercase())
            .collection("analytics")
            .get()
            .await()
        val summary = analytics.documents.firstOrNull { it.id == "summary" }?.data
        val drops = analytics.documents
            .filter { it.id.startsWith("drop_") }
            .mapNotNull { document -> R8WireParser.dropResult(document.data ?: return@mapNotNull null) }
            .sortedByDescending(R8DropResult::unlocks)
        return R8WireParser.results(experienceCode, summary, drops)
    }

    override suspend fun loadAccessState(userId: String): R7OrganizerAccessState {
        if (userId.isBlank()) return R7OrganizerAccessState()
        val profile = firestore.collection("users").document(userId).get().await()
        return R7OrganizerAccessState(
            status = R7OrganizerAccessStatus.fromRaw(profile.get("organizerAccessStatus")),
            submittedAtMillis = R7WireParser.millis(profile.get("organizerAccessSubmittedAt")),
            reviewedAtMillis = R7WireParser.millis(profile.get("organizerAccessReviewedAt"))
        )
    }

    override suspend fun createApplicationLink(): R7OrganizerApplicationLink {
        val root = call("createOrganizerApplicationLink", mapOf("apiVersion" to apiVersion()))
        return R7OrganizerApplicationLink(
            url = R7WireParser.text(root["url"])
                ?: throw R7OrganizerException("The application link was unavailable.", true),
            expiresAtMillis = R7WireParser.millis(root["expiresAt"])
                ?: throw R7OrganizerException("The application link was unavailable.", true)
        )
    }

    override suspend fun loadExperiences(userId: String): List<R7Experience> {
        if (userId.isBlank()) return emptyList()
        val memberships = firestore.collection("users").document(userId)
            .collection("groups")
            .whereEqualTo("role", "OWNER")
            .get()
            .await()
        return memberships.documents.mapNotNull { membership ->
            val code = membership.getString("code")?.trim()?.takeIf(String::isNotEmpty)
                ?: membership.id.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val group = firestore.collection("groups").document(code).get().await()
            R7WireParser.experience(group.data ?: return@mapNotNull null, code)
        }.sortedByDescending { it.startsAtMillis }
    }

    override suspend fun createExperience(draft: R7ExperienceDraft): R7Experience {
        R7OrganizerPolicy.validateExperience(draft)?.let {
            throw R7OrganizerException(it, retryable = false)
        }
        val root = call("createExperience", experiencePayload(draft, includeState = false))
        val raw = root["experience"] as? Map<*, *>
            ?: throw R7OrganizerException("The Experience was created, but could not be loaded.", true)
        return R7WireParser.experience(raw)
            ?: throw R7OrganizerException("The Experience was created, but could not be loaded.", true)
    }

    override suspend fun updateExperience(
        code: String,
        draft: R7ExperienceDraft
    ): R7Experience {
        R7OrganizerPolicy.validateExperience(draft)?.let {
            throw R7OrganizerException(it, retryable = false)
        }
        val payload = experiencePayload(draft, includeState = true).toMutableMap().apply {
            put("code", code.trim().uppercase())
        }
        val root = call("updateExperience", payload)
        val raw = root["experience"] as? Map<*, *>
            ?: throw R7OrganizerException("The Experience was saved, but could not be loaded.", true)
        return R7WireParser.experience(raw)
            ?: throw R7OrganizerException("The Experience was saved, but could not be loaded.", true)
    }

    override suspend fun loadDrops(
        userId: String,
        experienceCode: String
    ): List<R7OrganizerDropSummary> {
        if (userId.isBlank() || experienceCode.isBlank()) return emptyList()
        val snapshots = firestore.collection("experienceDrops")
            .whereEqualTo("ownerId", userId)
            .get()
            .await()
            .documents
            .filter { it.getString("experienceCode") == experienceCode && it.getString("state") != "DELETED" }

        return snapshots.mapNotNull { snapshot ->
            val detail = runCatching { loadDrop(snapshot.id) }.getOrNull()
            detail?.summary ?: R7WireParser.dropSummary(
                id = snapshot.id,
                discovery = snapshot.data ?: return@mapNotNull null,
                title = null
            )
        }.sortedByDescending { it.publishedAtMillis }
    }

    override suspend fun loadDrop(dropId: String): R7OrganizerDrop {
        val root = call(
            "getOrganizerDrop",
            mapOf("apiVersion" to apiVersion(), "dropId" to dropId)
        )
        val discovery = root["discovery"] as? Map<*, *>
            ?: throw R7OrganizerException("This drop could not be loaded.", true)
        val payload = root["payload"] as? Map<*, *>
            ?: throw R7OrganizerException("This drop could not be loaded.", true)
        val summary = R7WireParser.dropSummary(
            id = dropId,
            discovery = discovery,
            title = R7WireParser.text(payload["title"])
        ) ?: throw R7OrganizerException("This drop could not be loaded.", true)
        return R7OrganizerDrop(
            summary = summary,
            body = R7WireParser.text(payload["body"]),
            mediaAltText = R7WireParser.text(payload["mediaAltText"]),
            rewardPresentation = (payload["rewardPresentation"] as? Map<*, *>)
                ?.mapNotNull { (key, value) ->
                    val text = R7WireParser.text(value) ?: return@mapNotNull null
                    key.toString() to text
                }?.toMap().orEmpty(),
            inventoryLimit = (root["reward"] as? Map<*, *>)
                ?.let { R7WireParser.number(it["inventoryLimit"])?.toInt() }
        )
    }

    override suspend fun saveDrop(userId: String, draft: R7DropDraft): R7SaveDropResult {
        R7OrganizerPolicy.validateDrop(draft)?.let {
            throw R7OrganizerException(it, retryable = false)
        }
        var stagedPath: String? = null
        val stagingUploadId = if (draft.contentKind == R7DropContentKind.PHOTO) {
            val bytes = draft.photoBytes
                ?: throw R7OrganizerException("Choose a photo.", retryable = false)
            val uploadId = UUID.randomUUID().toString()
            val path = "drop-upload-staging/$userId/$uploadId"
            val metadata = StorageMetadata.Builder()
                .setContentType(draft.photoMimeType ?: "image/jpeg")
                .setCustomMetadata("ownerId", userId)
                .setCustomMetadata("purpose", "DROP_STAGING")
                .build()
            storage.reference.child(path).putBytes(bytes, metadata).await()
            stagedPath = path
            uploadId
        } else {
            null
        }

        val payload = mutableMapOf<String, Any?>(
            "apiVersion" to apiVersion(),
            "experienceCode" to draft.experienceCode.trim().uppercase(),
            "location" to mapOf("lat" to draft.lat, "lng" to draft.lng),
            "radiusM" to draft.radiusM,
            "expiryMode" to draft.expiryMode.name,
            "content" to mapOf(
                "contentKind" to draft.contentKind.name,
                "title" to draft.title.trim(),
                "body" to draft.body?.trim()?.takeIf(String::isNotEmpty),
                "mediaAltText" to draft.mediaAltText?.trim()?.takeIf(String::isNotEmpty),
                "rewardPresentation" to if (draft.dropKind == R6DropKind.REWARD) {
                    mapOf(
                        "rewardLabel" to draft.rewardLabel?.trim()?.takeIf(String::isNotEmpty),
                        "businessLabel" to draft.businessLabel?.trim()?.takeIf(String::isNotEmpty),
                        "instructions" to draft.rewardInstructions?.trim()?.takeIf(String::isNotEmpty),
                        "terms" to draft.rewardTerms?.trim()?.takeIf(String::isNotEmpty)
                    )
                } else null
            ),
            "dropKind" to draft.dropKind.name
        )
        if (draft.dropKind == R6DropKind.REWARD) {
            payload["inventoryLimit"] = draft.inventoryLimit
        }
        draft.dropId?.let { payload["dropId"] = it }
        stagingUploadId?.let { payload["stagingUploadId"] = it }
        if (draft.expiryMode == R7ExpiryMode.CUSTOM) {
            payload["expiresAt"] = Instant.ofEpochMilli(draft.expiresAtMillis!!).toString()
        }

        return try {
            val root = call("saveDrop", payload)
            R7SaveDropResult(
                dropId = R7WireParser.text(root["dropId"])
                    ?: throw R7OrganizerException("The drop was saved, but could not be loaded.", true),
                payloadVersion = R7WireParser.number(root["payloadVersion"])?.toInt() ?: 1
            )
        } catch (error: Exception) {
            stagedPath?.let { path -> runCatching { storage.reference.child(path).delete().await() } }
            throw error
        }
    }

    override suspend fun deleteDrop(dropId: String) {
        call("deleteDrop", mapOf("apiVersion" to apiVersion(), "dropId" to dropId))
    }

    private fun experiencePayload(
        draft: R7ExperienceDraft,
        includeState: Boolean
    ): MutableMap<String, Any?> = mutableMapOf<String, Any?>(
        "apiVersion" to apiVersion(),
        "name" to draft.name.trim(),
        "description" to draft.description?.trim()?.takeIf(String::isNotEmpty),
        "startsAt" to Instant.ofEpochMilli(draft.startsAtMillis).toString(),
        "endsAt" to Instant.ofEpochMilli(draft.endsAtMillis).toString(),
        "timeZone" to draft.timeZone.trim(),
        "defaultRadiusM" to draft.defaultRadiusM
    ).apply {
        if (includeState) put("state", draft.state.name)
    }

    private suspend fun call(callable: String, payload: Map<String, Any?>): Map<*, *> = try {
        functions.getHttpsCallable(callable).call(payload).await().data as? Map<*, *>
            ?: throw R7OrganizerException("The server returned an unexpected response.", true)
    } catch (error: FirebaseFunctionsException) {
        val details = error.details as? Map<*, *>
        val reason = details?.get("reason")?.toString()
        val message = when (reason) {
            "ORGANIZER_APPROVAL_REQUIRED" -> "Organizer approval is required."
            "SERVER_CONFIGURATION_REQUIRED" -> "Organizer applications are not configured yet."
            "EXPERIENCE_NOT_FOUND" -> "This Experience could not be found."
            "DROP_NOT_AVAILABLE" -> "This drop is no longer available."
            "DROP_EDIT_CONFLICT" -> "This drop changed elsewhere. Reload it and try again."
            "REWARD_INVENTORY_IMMUTABLE", "DROP_KIND_IMMUTABLE" ->
                "Reward type and inventory cannot change after publishing."
            "REWARD_CODE_NOT_FOUND" -> "That code was not found for this reward."
            "REWARD_NOT_ISSUED" -> "That code has not been issued to a guest."
            "REWARD_NOT_USED" -> "That code is not marked used."
            "INVALID_REWARD_CODE" -> "Enter the complete reward code."
            "UPLOAD_NOT_FOUND", "UPLOAD_INVALID", "UPLOAD_REQUIRED" ->
                "The photo could not be uploaded. Choose it again and retry."
            else -> error.message?.takeIf(String::isNotBlank)
                ?: "Couldn't complete that action. Try again."
        }
        throw R7OrganizerException(
            userMessage = message,
            retryable = error.code in setOf(
                FirebaseFunctionsException.Code.UNAVAILABLE,
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
                FirebaseFunctionsException.Code.ABORTED
            ),
            cause = error
        )
    } catch (error: R7OrganizerException) {
        throw error
    } catch (error: Exception) {
        throw R7OrganizerException("You're offline. Reconnect and try again.", true, error)
    }

    private fun apiVersion(): Int = R6RolloutPolicy.SUPPORTED_CONTRACT_VERSION
}

internal object R8WireParser {
    fun rewardCode(raw: Map<*, *>): R8RewardCode? {
        val code = R7WireParser.text(raw["code"]) ?: return null
        val state = R7WireParser.enumValue<R8RewardCodeState>(raw["state"]) ?: return null
        return R8RewardCode(
            code = code,
            state = state,
            issuedAtMillis = R7WireParser.millis(raw["issuedAt"]),
            usedAtMillis = R7WireParser.millis(raw["usedAt"]),
            version = R7WireParser.number(raw["version"])?.toInt() ?: 1,
            history = (raw["history"] as? List<*>)?.mapNotNull { event ->
                val value = event as? Map<*, *> ?: return@mapNotNull null
                R8RewardCodeEvent(
                    transition = R7WireParser.text(value["transition"]) ?: return@mapNotNull null,
                    occurredAtMillis = R7WireParser.millis(value["occurredAt"])
                        ?: return@mapNotNull null,
                    reason = R7WireParser.text(value["reason"])
                )
            }.orEmpty()
        )
    }

    fun dropResult(raw: Map<*, *>): R8DropResult? {
        val dropId = R7WireParser.text(raw["dropId"]) ?: return null
        return R8DropResult(
            dropId = dropId,
            unlocks = count(raw, "unlocks"),
            codesIssued = count(raw, "codesIssued"),
            codesUsed = count(raw, "codesUsed"),
            updatedAtMillis = R7WireParser.millis(raw["updatedAt"])
        )
    }

    fun results(
        experienceCode: String,
        raw: Map<String, Any?>?,
        drops: List<R8DropResult>
    ) = R8ExperienceResults(
        experienceCode = experienceCode.trim().uppercase(),
        joinedParticipants = count(raw, "joinedParticipants"),
        publishedDrops = count(raw, "publishedDrops"),
        uniqueUnlockers = count(raw, "uniqueUnlockers"),
        unlocks = count(raw, "unlocks"),
        mainTrailCompletions = count(raw, "mainTrailCompletions"),
        codesIssued = count(raw, "codesIssued"),
        codesUsed = count(raw, "codesUsed"),
        updatedAtMillis = R7WireParser.millis(raw?.get("updatedAt")),
        reconciledAtMillis = R7WireParser.millis(raw?.get("reconciledAt")),
        drops = drops
    )

    private fun count(raw: Map<*, *>?, field: String): Long =
        ((raw?.get(field) as? Number)?.toLong() ?: 0L).coerceAtLeast(0L)
}

internal object R7WireParser {
    fun experience(raw: Map<*, *>, fallbackCode: String? = null): R7Experience? {
        val code = text(raw["code"]) ?: fallbackCode ?: return null
        return R7Experience(
            code = code,
            name = text(raw["name"]) ?: return null,
            description = text(raw["description"]),
            startsAtMillis = millis(raw["startsAt"]) ?: return null,
            endsAtMillis = millis(raw["endsAt"]) ?: return null,
            timeZone = text(raw["timeZone"]) ?: ZoneId.systemDefault().id,
            state = enumValue<R7ExperienceState>(raw["state"]) ?: R7ExperienceState.PUBLISHED,
            defaultRadiusM = number(raw["defaultRadiusM"])?.toInt() ?: 25,
            dropCount = number(raw["availableDropCount"])?.toInt()?.coerceAtLeast(0) ?: 0
        )
    }

    fun dropSummary(
        id: String,
        discovery: Map<*, *>,
        title: String?
    ): R7OrganizerDropSummary? {
        val contentKind = enumValue<R7DropContentKind>(discovery["contentKind"]) ?: return null
        return R7OrganizerDropSummary(
            id = id.takeIf(String::isNotBlank) ?: return null,
            experienceCode = text(discovery["experienceCode"]) ?: return null,
            title = title ?: when (contentKind) {
                R7DropContentKind.PHOTO -> "Photo drop"
                R7DropContentKind.TEXT -> "Text drop"
            },
            contentKind = contentKind,
            dropKind = enumValue<R6DropKind>(discovery["dropKind"]) ?: R6DropKind.STANDARD,
            moderationState = text(discovery["moderationState"]) ?: "SAFE",
            lat = number(discovery["lat"]) ?: return null,
            lng = number(discovery["lng"]) ?: return null,
            radiusM = number(discovery["radiusM"])?.toInt() ?: 25,
            expiryMode = enumValue<R7ExpiryMode>(discovery["expiryMode"]) ?: R7ExpiryMode.NONE,
            expiresAtMillis = millis(discovery["expiresAt"]),
            publishedAtMillis = millis(discovery["publishedAt"]) ?: 0L,
            editedAtMillis = millis(discovery["editedAt"])
        )
    }

    inline fun <reified T : Enum<T>> enumValue(raw: Any?): T? =
        raw?.toString()?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

    fun text(raw: Any?): String? = raw?.toString()?.trim()?.takeIf(String::isNotEmpty)
    fun number(raw: Any?): Double? = (raw as? Number)?.toDouble()?.takeIf(Double::isFinite)

    fun millis(raw: Any?): Long? = when (raw) {
        is Timestamp -> raw.toDate().time
        is java.util.Date -> raw.time
        is Number -> raw.toLong()
        is String -> runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
        is Map<*, *> -> {
            val seconds = (raw["seconds"] ?: raw["_seconds"]) as? Number
            val nanos = (raw["nanoseconds"] ?: raw["_nanoseconds"]) as? Number
            seconds?.let { it.toLong() * 1_000L + (nanos?.toLong() ?: 0L) / 1_000_000L }
        }
        else -> null
    }
}
