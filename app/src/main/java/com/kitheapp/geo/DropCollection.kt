package com.kitheapp.geo

import com.kitheapp.data.CollectedNote
import com.kitheapp.data.Drop
import com.kitheapp.data.DropContentType
import com.kitheapp.data.DropType

/** Pickup proximity budget, shared by every path that can collect a drop. */
const val PICKUP_RADIUS_METERS = 30.0f

/**
 * Everything needed to turn an unlocked drop into a collected note.
 *
 * Collection used to be expressed as a broadcast to [DropDecisionReceiver], which
 * meant the in-app pickup buttons inherited that receiver's notification feature
 * flag and silently discarded the note whenever notifications were disabled. The
 * request travels to [DropCollector] directly now; the notification path builds
 * the same request from its intent extras.
 */
data class DropCollectionRequest(
    val dropId: String,
    val text: String = "",
    val description: String? = null,
    val contentType: DropContentType = DropContentType.TEXT,
    val mediaUrl: String? = null,
    val mediaMimeType: String? = null,
    val mediaData: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val createdAt: Long? = null,
    val groupCode: String? = null,
    val dropperUsername: String? = null,
    val dropType: DropType = DropType.COMMUNITY,
    val businessId: String? = null,
    val businessName: String? = null,
    val redemptionLimit: Int? = null,
    val redemptionCount: Int = 0,
    val isNsfw: Boolean = false,
    val nsfwLabels: List<String> = emptyList(),
    val decayDays: Int? = null,
    val huntId: String? = null,
    val huntStepIndex: Int? = null,
    val huntTotalSteps: Int? = null
)

sealed class DropCollectionResult {
    /** The note is in the user's inventory. */
    object Collected : DropCollectionResult()

    object Expired : DropCollectionResult()

    /** A trustworthy fix put the user outside the pickup radius. */
    object OutOfRange : DropCollectionResult()

    /** No fix, a stale fix, or one too coarse to decide on. Fails closed. */
    object LocationUnavailable : DropCollectionResult()

    data class Failed(val error: Throwable) : DropCollectionResult()
}

private const val MILLIS_PER_DAY = 86_400_000L

fun DropCollectionRequest.expiresAtMillis(): Long? {
    val days = decayDays?.takeIf { it > 0 } ?: return null
    val created = createdAt?.takeIf { it > 0L } ?: return null
    return created + days * MILLIS_PER_DAY
}

fun DropCollectionRequest.isExpired(nowMillis: Long): Boolean {
    val expiresAt = expiresAtMillis() ?: return false
    return expiresAt <= nowMillis
}

/**
 * The proximity rule itself, kept free of Android types so it stays testable.
 *
 * Fails closed on a missing or coarse accuracy reading: a fix that cannot prove
 * where the user is must never unlock a drop.
 */
fun isWithinPickupRadius(distanceMeters: Float, accuracyMeters: Float?): Boolean {
    if (accuracyMeters == null || accuracyMeters <= 0f) return false
    if (accuracyMeters > PICKUP_RADIUS_METERS) return false
    return distanceMeters <= PICKUP_RADIUS_METERS + accuracyMeters
}

fun DropCollectionRequest.toCollectedNote(
    resolvedUsername: String?,
    collectedAt: Long
): CollectedNote = CollectedNote(
    id = dropId,
    text = text,
    description = description?.takeIf { it.isNotBlank() },
    contentType = contentType,
    mediaUrl = mediaUrl,
    mediaMimeType = mediaMimeType,
    mediaData = mediaData,
    lat = lat,
    lng = lng,
    groupCode = groupCode,
    dropCreatedAt = createdAt,
    dropperUsername = resolvedUsername,
    dropType = dropType,
    businessId = businessId,
    businessName = businessName,
    redemptionLimit = redemptionLimit,
    redemptionCount = redemptionCount,
    collectedAt = collectedAt,
    isNsfw = isNsfw,
    nsfwLabels = nsfwLabels,
    decayDays = decayDays,
    huntId = huntId,
    huntStepIndex = huntStepIndex,
    huntTotalSteps = huntTotalSteps
)

fun Drop.toCollectionRequest(): DropCollectionRequest = DropCollectionRequest(
    dropId = id,
    text = text,
    description = description,
    contentType = contentType,
    mediaUrl = mediaUrl,
    mediaMimeType = mediaMimeType,
    mediaData = mediaData,
    lat = lat,
    lng = lng,
    createdAt = createdAt.takeIf { it > 0L },
    groupCode = groupCode,
    dropperUsername = dropperUsername,
    dropType = dropType,
    businessId = businessId,
    businessName = businessName,
    redemptionLimit = redemptionLimit,
    redemptionCount = redemptionCount,
    isNsfw = isNsfw,
    nsfwLabels = nsfwLabels,
    decayDays = decayDays,
    huntId = huntId,
    huntStepIndex = huntStepIndex,
    huntTotalSteps = huntTotalSteps
)

/** One phrasing of each failure, so every pickup entry point says the same thing. */
fun pickupFailureMessage(result: DropCollectionResult): String = when (result) {
    DropCollectionResult.Collected -> "Drop added to your collection."
    DropCollectionResult.Expired -> "This drop has already expired."
    DropCollectionResult.OutOfRange ->
        "Move within ${PICKUP_RADIUS_METERS.toInt()} meters to pick up this drop."
    DropCollectionResult.LocationUnavailable ->
        "Couldn't confirm your location accurately enough. Step outside or try again."
    is DropCollectionResult.Failed -> "Couldn't pick up this drop. Try again."
}
