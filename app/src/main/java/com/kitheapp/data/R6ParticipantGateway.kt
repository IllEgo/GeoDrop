package com.kitheapp.data

import com.kitheapp.BuildConfig
import com.kitheapp.util.R6RolloutPolicy
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import java.time.Instant
import kotlinx.coroutines.tasks.await

interface R6ParticipantGateway {
    suspend fun loadDiscoveries(experienceCode: String): List<R6DropDiscovery>
    suspend fun loadCollection(userId: String): List<R6CollectionReceipt>
    suspend fun loadTrailProgress(userId: String, experienceCode: String): List<R6TrailProgress>
    suspend fun loadBlockedHostIds(userId: String): Set<String>
    suspend fun unlock(request: R6UnlockRequest): R6UnlockResult
    suspend fun submitReport(dropId: String, reason: String, narrative: String? = null)
    suspend fun blockHost(dropId: String)
}

class FirebaseR6ParticipantGateway(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = Firebase.functions(BuildConfig.FIREBASE_FUNCTIONS_REGION)
) : R6ParticipantGateway {

    override suspend fun loadDiscoveries(experienceCode: String): List<R6DropDiscovery> {
        val code = experienceCode.trim().uppercase()
        if (code.isBlank()) return emptyList()
        val result = firestore.collection("experienceDrops")
            .whereEqualTo("experienceCode", code)
            .whereEqualTo("state", "PUBLISHED")
            .whereEqualTo("moderationState", "SAFE")
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .get()
            .await()
        return result.documents.mapNotNull { document ->
            R6WireParser.discovery(document.id, document.data ?: return@mapNotNull null)
        }
    }

    override suspend fun loadCollection(userId: String): List<R6CollectionReceipt> {
        if (userId.isBlank()) return emptyList()
        val user = firestore.collection("users").document(userId)
        val (unlocks, rewards) = listOf(
            user.collection("unlocks").get(),
            user.collection("rewardReceipts").get()
        ).map { it.await() }
        val rewardByDrop = rewards.documents.mapNotNull { document ->
            R6WireParser.reward(document.data ?: return@mapNotNull null)?.let { document.id to it }
        }.toMap()
        return unlocks.documents.mapNotNull { document ->
            R6WireParser.receipt(document.data ?: return@mapNotNull null)
                ?.let { receipt -> receipt.copy(reward = rewardByDrop[receipt.dropId]) }
        }.sortedByDescending { it.unlockedAtMillis }
    }

    override suspend fun loadTrailProgress(
        userId: String,
        experienceCode: String
    ): List<R6TrailProgress> {
        if (userId.isBlank() || experienceCode.isBlank()) return emptyList()
        val result = firestore.collection("users").document(userId)
            .collection("trailProgress")
            .whereEqualTo("experienceCode", experienceCode.trim().uppercase())
            .get()
            .await()
        return result.documents.mapNotNull { document ->
            R6WireParser.trailProgress(document.id, document.data ?: return@mapNotNull null)
        }
    }

    override suspend fun loadBlockedHostIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        return firestore.collection("users").document(userId)
            .collection("blockedHosts")
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.getString("hostId")?.takeIf { it.isNotBlank() } ?: document.id
            }
            .toSet()
    }

    override suspend fun unlock(request: R6UnlockRequest): R6UnlockResult {
        val payload = mutableMapOf<String, Any>(
            "apiVersion" to R6RolloutPolicy.SUPPORTED_CONTRACT_VERSION,
            "dropId" to request.dropId,
            "location" to mapOf(
                "lat" to request.latitude,
                "lng" to request.longitude,
                "accuracyM" to request.accuracyM,
                "capturedAt" to Instant.ofEpochMilli(request.capturedAtMillis).toString()
            )
        )
        request.entrySessionId?.takeIf { it.isNotBlank() }?.let {
            payload["entrySessionId"] = it
        }
        val root = call("unlockDrop", payload)
        val receiptData = root["receipt"] as? Map<*, *>
            ?: throw R6ParticipantException(R6UnlockFailureReason.UNKNOWN, retryable = true)
        val receipt = R6WireParser.receipt(receiptData)
            ?: throw R6ParticipantException(R6UnlockFailureReason.UNKNOWN, retryable = true)
        val rewardData = root["reward"] as? Map<*, *>
        val reward = rewardData?.let(R6WireParser::reward)
        return R6UnlockResult(
            alreadyUnlocked = root["status"]?.toString() == "ALREADY_UNLOCKED",
            receipt = receipt.copy(reward = reward),
            rewardUnavailable = rewardData?.get("state")?.toString() == "UNAVAILABLE"
        )
    }

    override suspend fun submitReport(dropId: String, reason: String, narrative: String?) {
        val payload = mutableMapOf<String, Any>(
            "apiVersion" to R6RolloutPolicy.SUPPORTED_CONTRACT_VERSION,
            "dropId" to dropId,
            "reason" to reason
        )
        narrative?.trim()?.takeIf { it.isNotBlank() }?.let { payload["narrative"] = it }
        call("submitReport", payload)
    }

    override suspend fun blockHost(dropId: String) {
        call(
            "blockHost",
            mapOf(
                "apiVersion" to R6RolloutPolicy.SUPPORTED_CONTRACT_VERSION,
                "dropId" to dropId
            )
        )
    }

    private suspend fun call(callable: String, payload: Map<String, Any>): Map<*, *> = try {
        functions.getHttpsCallable(callable).call(payload).await().data as? Map<*, *>
            ?: throw R6ParticipantException(R6UnlockFailureReason.UNKNOWN, retryable = true)
    } catch (error: FirebaseFunctionsException) {
        throw R6WireParser.failure(error)
    } catch (error: R6ParticipantException) {
        throw error
    } catch (error: Exception) {
        throw R6ParticipantException(
            reason = R6UnlockFailureReason.OFFLINE,
            retryable = true,
            cause = error
        )
    }
}

internal object R6WireParser {
    fun discovery(id: String, raw: Map<*, *>): R6DropDiscovery? {
        val contentKind = enumValue<R6ContentKind>(raw["contentKind"]) ?: return null
        val dropKind = enumValue<R6DropKind>(raw["dropKind"]) ?: return null
        val expiryMode = enumValue<R6ExpiryMode>(raw["expiryMode"]) ?: return null
        return R6DropDiscovery(
            id = id.takeIf { it.isNotBlank() } ?: return null,
            experienceCode = text(raw["experienceCode"]) ?: return null,
            ownerId = text(raw["ownerId"]) ?: return null,
            hostLabel = text(raw["hostLabel"]) ?: "Host",
            lat = number(raw["lat"]) ?: return null,
            lng = number(raw["lng"]) ?: return null,
            radiusM = number(raw["radiusM"])?.toInt() ?: return null,
            contentKind = contentKind,
            dropKind = dropKind,
            payloadVersion = number(raw["payloadVersion"])?.toInt() ?: return null,
            trailId = text(raw["trailId"]),
            trailStepIndex = number(raw["trailStepIndex"])?.toInt(),
            trailTotalSteps = number(raw["trailTotalSteps"])?.toInt(),
            likeCount = number(raw["likeCount"])?.toInt()?.coerceAtLeast(0) ?: 0,
            publishedAtMillis = millis(raw["publishedAt"]) ?: 0L,
            editedAtMillis = millis(raw["editedAt"]),
            expiryMode = expiryMode,
            expiresAtMillis = millis(raw["expiresAt"])
        )
    }

    fun receipt(raw: Map<*, *>): R6CollectionReceipt? {
        val snapshot = raw["snapshot"] as? Map<*, *> ?: return null
        val parsedSnapshot = payload(snapshot) ?: return null
        val trail = (raw["trail"] as? Map<*, *>)?.let { trailRaw ->
            R6ReceiptTrail(
                trailId = text(trailRaw["trailId"]) ?: return@let null,
                stepIndex = number(trailRaw["stepIndex"])?.toInt() ?: return@let null,
                totalSteps = number(trailRaw["totalSteps"])?.toInt() ?: return@let null,
                completedAtUnlock = trailRaw["completedAtUnlock"] as? Boolean ?: false
            )
        }
        return R6CollectionReceipt(
            receiptId = text(raw["receiptId"]) ?: return null,
            dropId = text(raw["dropId"]) ?: return null,
            experienceCode = text(raw["experienceCode"]) ?: return null,
            unlockedAtMillis = millis(raw["unlockedAt"]) ?: return null,
            payloadVersion = number(raw["payloadVersion"])?.toInt() ?: return null,
            snapshot = parsedSnapshot,
            trail = trail,
            hasRewardReceipt = raw["hasRewardReceipt"] as? Boolean ?: false
        )
    }

    fun reward(raw: Map<*, *>): R6RewardReceipt? {
        return R6RewardReceipt(
            receiptId = text(raw["receiptId"]) ?: return null,
            dropId = text(raw["dropId"]) ?: return null,
            experienceCode = text(raw["experienceCode"]) ?: return null,
            code = text(raw["code"]) ?: return null,
            state = text(raw["state"]) ?: "ISSUED",
            issuedAtMillis = millis(raw["issuedAt"]) ?: return null,
            usedAtMillis = millis(raw["usedAt"])
        )
    }

    fun trailProgress(id: String, raw: Map<*, *>): R6TrailProgress? {
        return R6TrailProgress(
            experienceCode = text(raw["experienceCode"]) ?: return null,
            trailId = text(raw["trailId"]) ?: id.takeIf { it.isNotBlank() } ?: return null,
            currentStepIndex = number(raw["currentStepIndex"])?.toInt()?.coerceAtLeast(0) ?: 0,
            completedDropIds = (raw["completedDropIds"] as? List<*>)
                ?.mapNotNull(::text)
                .orEmpty(),
            completedAtMillis = millis(raw["completedAt"])
        )
    }

    fun failure(error: FirebaseFunctionsException): R6ParticipantException {
        val details = error.details as? Map<*, *>
        val reason = when (details?.get("reason")?.toString()) {
            "ACCOUNT_REQUIRED" -> R6UnlockFailureReason.ACCOUNT_REQUIRED
            "EXPERIENCE_NOT_JOINED" -> R6UnlockFailureReason.EXPERIENCE_NOT_JOINED
            "DROP_NOT_AVAILABLE" -> R6UnlockFailureReason.DROP_NOT_AVAILABLE
            "DROP_EXPIRED" -> R6UnlockFailureReason.DROP_EXPIRED
            "LOCATION_INVALID" -> R6UnlockFailureReason.LOCATION_INVALID
            "LOCATION_STALE" -> R6UnlockFailureReason.LOCATION_STALE
            "ACCURACY_INSUFFICIENT" -> R6UnlockFailureReason.ACCURACY_INSUFFICIENT
            "TOO_FAR" -> R6UnlockFailureReason.TOO_FAR
            "TRAIL_STEP_LOCKED" -> R6UnlockFailureReason.TRAIL_STEP_LOCKED
            "REWARD_UNAVAILABLE" -> R6UnlockFailureReason.REWARD_UNAVAILABLE
            "CONTRACT_VERSION_UNSUPPORTED" -> R6UnlockFailureReason.CONTRACT_VERSION_UNSUPPORTED
            "RATE_LIMITED" -> R6UnlockFailureReason.RATE_LIMITED
            else -> when (error.code) {
                FirebaseFunctionsException.Code.UNAVAILABLE,
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> R6UnlockFailureReason.OFFLINE
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> R6UnlockFailureReason.RATE_LIMITED
                else -> R6UnlockFailureReason.UNKNOWN
            }
        }
        val bucket = when (details?.get("distanceBucket")?.toString()) {
            "0_25" -> R6DistanceBucket.WITHIN_25_M
            "25_50" -> R6DistanceBucket.WITHIN_50_M
            "50_PLUS" -> R6DistanceBucket.OVER_50_M
            else -> null
        }
        return R6ParticipantException(
            reason = reason,
            retryable = details?.get("retryable") as? Boolean
                ?: (reason in setOf(
                    R6UnlockFailureReason.LOCATION_STALE,
                    R6UnlockFailureReason.ACCURACY_INSUFFICIENT,
                    R6UnlockFailureReason.TOO_FAR,
                    R6UnlockFailureReason.OFFLINE,
                    R6UnlockFailureReason.RATE_LIMITED,
                    R6UnlockFailureReason.UNKNOWN
                )),
            distanceBucket = bucket,
            cause = error
        )
    }

    private fun payload(raw: Map<*, *>): R6PayloadSnapshot? {
        val presentation = (raw["rewardPresentation"] as? Map<*, *>)
            ?.entries
            ?.mapNotNull { (key, value) ->
                val label = text(key) ?: return@mapNotNull null
                val content = text(value) ?: return@mapNotNull null
                label to content
            }
            ?.toMap()
            .orEmpty()
        return R6PayloadSnapshot(
            title = text(raw["title"]) ?: return null,
            body = text(raw["body"]),
            contentKind = enumValue<R6ContentKind>(raw["contentKind"]) ?: return null,
            hostLabel = text(raw["hostLabel"]) ?: "Host",
            mediaAssetId = text(raw["mediaAssetId"]),
            mediaMimeType = text(raw["mediaMimeType"]),
            mediaAltText = text(raw["mediaAltText"]),
            rewardPresentation = presentation,
            editedAtMillis = millis(raw["editedAt"])
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: Any?): T? =
        raw?.toString()?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

    private fun text(raw: Any?): String? = raw?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private fun number(raw: Any?): Double? = (raw as? Number)?.toDouble()?.takeIf { it.isFinite() }

    private fun millis(raw: Any?): Long? = when (raw) {
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
