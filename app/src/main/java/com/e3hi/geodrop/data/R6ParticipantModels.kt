package com.e3hi.geodrop.data

enum class R6ContentKind { TEXT, PHOTO }

enum class R6DropKind { STANDARD, REWARD }

enum class R6ExpiryMode { NONE, CUSTOM, EXPERIENCE_END }

enum class R6DiscoveryState {
    LOCKED,
    NEAR,
    FOUND,
    EXPIRED,
    TRAIL_LOCKED
}

data class R6DropDiscovery(
    val id: String,
    val experienceCode: String,
    val ownerId: String,
    val hostLabel: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val contentKind: R6ContentKind,
    val dropKind: R6DropKind,
    val payloadVersion: Int,
    val trailId: String?,
    val trailStepIndex: Int?,
    val trailTotalSteps: Int?,
    val likeCount: Int,
    val publishedAtMillis: Long,
    val editedAtMillis: Long?,
    val expiryMode: R6ExpiryMode,
    val expiresAtMillis: Long?
) {
    fun participantLabel(): String = when {
        trailId != null -> trailStepIndex?.let { index ->
            trailTotalSteps?.let { total -> "Trail stop ${index + 1} of $total" }
                ?: "Trail stop ${index + 1}"
        } ?: "Trail stop"
        dropKind == R6DropKind.REWARD -> "Hidden reward"
        contentKind == R6ContentKind.PHOTO -> "Hidden photo"
        else -> "Hidden note"
    }
}

data class R6PayloadSnapshot(
    val title: String,
    val body: String?,
    val contentKind: R6ContentKind,
    val hostLabel: String,
    val mediaAssetId: String?,
    val mediaMimeType: String?,
    val mediaAltText: String?,
    val rewardPresentation: Map<String, String>,
    val editedAtMillis: Long?
)

data class R6ReceiptTrail(
    val trailId: String,
    val stepIndex: Int,
    val totalSteps: Int,
    val completedAtUnlock: Boolean
)

data class R6CollectionReceipt(
    val receiptId: String,
    val dropId: String,
    val experienceCode: String,
    val unlockedAtMillis: Long,
    val payloadVersion: Int,
    val snapshot: R6PayloadSnapshot,
    val trail: R6ReceiptTrail?,
    val hasRewardReceipt: Boolean,
    val reward: R6RewardReceipt? = null
)

data class R6RewardReceipt(
    val receiptId: String,
    val dropId: String,
    val experienceCode: String,
    val code: String,
    val state: String,
    val issuedAtMillis: Long,
    val usedAtMillis: Long?
)

data class R6TrailProgress(
    val experienceCode: String,
    val trailId: String,
    val currentStepIndex: Int,
    val completedDropIds: List<String>,
    val completedAtMillis: Long?
)

data class R6UnlockRequest(
    val dropId: String,
    val entrySessionId: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double,
    val capturedAtMillis: Long
)

data class R6UnlockResult(
    val alreadyUnlocked: Boolean,
    val receipt: R6CollectionReceipt,
    val rewardUnavailable: Boolean = false
)

enum class R6UnlockFailureReason {
    ACCOUNT_REQUIRED,
    EXPERIENCE_NOT_JOINED,
    DROP_NOT_AVAILABLE,
    DROP_EXPIRED,
    LOCATION_INVALID,
    LOCATION_STALE,
    ACCURACY_INSUFFICIENT,
    TOO_FAR,
    TRAIL_STEP_LOCKED,
    REWARD_UNAVAILABLE,
    CONTRACT_VERSION_UNSUPPORTED,
    OFFLINE,
    RATE_LIMITED,
    UNKNOWN
}

enum class R6DistanceBucket { WITHIN_25_M, WITHIN_50_M, OVER_50_M }

class R6ParticipantException(
    val reason: R6UnlockFailureReason,
    val retryable: Boolean,
    val distanceBucket: R6DistanceBucket? = null,
    cause: Throwable? = null
) : Exception(reason.name, cause)

object R6ParticipantPolicy {
    fun discoveryState(
        drop: R6DropDiscovery,
        unlockedDropIds: Set<String>,
        trailProgress: R6TrailProgress?,
        approximateDistanceM: Double?,
        approximateAccuracyM: Double?,
        nowMillis: Long,
        experienceEndsAtMillis: Long?
    ): R6DiscoveryState {
        val expired = when (drop.expiryMode) {
            R6ExpiryMode.NONE -> false
            R6ExpiryMode.CUSTOM -> drop.expiresAtMillis?.let { nowMillis >= it } ?: true
            R6ExpiryMode.EXPERIENCE_END -> experienceEndsAtMillis?.let { nowMillis >= it } ?: false
        }
        if (expired) return R6DiscoveryState.EXPIRED
        if (drop.id in unlockedDropIds) return R6DiscoveryState.FOUND

        val step = drop.trailStepIndex
        if (step != null && step > (trailProgress?.currentStepIndex ?: 0)) {
            return R6DiscoveryState.TRAIL_LOCKED
        }

        val distance = approximateDistanceM
        val accuracy = approximateAccuracyM
        return if (
            distance != null && distance.isFinite() &&
            accuracy != null && accuracy.isFinite() && accuracy > 0.0 &&
            distance <= drop.radiusM + accuracy
        ) {
            R6DiscoveryState.NEAR
        } else {
            R6DiscoveryState.LOCKED
        }
    }

    fun failureMessage(error: R6ParticipantException): String = when (error.reason) {
        R6UnlockFailureReason.ACCOUNT_REQUIRED ->
            "Create an account to unlock this drop."
        R6UnlockFailureReason.EXPERIENCE_NOT_JOINED ->
            "Join this Experience before unlocking its drops."
        R6UnlockFailureReason.DROP_NOT_AVAILABLE ->
            "This drop isn't available right now."
        R6UnlockFailureReason.DROP_EXPIRED ->
            "This one closed when the event ended."
        R6UnlockFailureReason.LOCATION_INVALID,
        R6UnlockFailureReason.LOCATION_STALE,
        R6UnlockFailureReason.ACCURACY_INSUFFICIENT ->
            "GeoDrop couldn't get a clear enough location. Step into the open and check again."
        R6UnlockFailureReason.TOO_FAR -> when (error.distanceBucket) {
            R6DistanceBucket.WITHIN_25_M -> "Not there yet — you're within about 25 m."
            R6DistanceBucket.WITHIN_50_M -> "Not there yet — you're within about 50 m."
            R6DistanceBucket.OVER_50_M, null -> "Not there yet — you're more than 50 m away."
        }
        R6UnlockFailureReason.TRAIL_STEP_LOCKED ->
            "Find the previous Trail stop first."
        R6UnlockFailureReason.REWARD_UNAVAILABLE ->
            "This reward has run out, but the drop remains part of the Experience."
        R6UnlockFailureReason.CONTRACT_VERSION_UNSUPPORTED ->
            "Update the app before continuing."
        R6UnlockFailureReason.OFFLINE ->
            "GeoDrop can't reach the internet. Move somewhere with signal and check again."
        R6UnlockFailureReason.RATE_LIMITED ->
            "Too many checks at once. Wait a moment and try again."
        R6UnlockFailureReason.UNKNOWN ->
            "GeoDrop couldn't check this drop. Try again."
    }
}
