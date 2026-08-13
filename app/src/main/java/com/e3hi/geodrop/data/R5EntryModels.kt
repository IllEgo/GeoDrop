package com.e3hi.geodrop.data

enum class R5EntryChannel {
    QR,
    LINK,
    MANUAL
}

data class R5EntryRequest(
    val code: String,
    val entrySessionId: String,
    val channel: R5EntryChannel
)

enum class R5ExperienceAvailability {
    UPCOMING,
    ACTIVE,
    ENDED,
    CANCELLED
}

enum class R5ExperienceMembership {
    NONE,
    MEMBER,
    OWNER
}

data class R5ExperiencePreview(
    val code: String,
    val name: String,
    val description: String?,
    val hostLabel: String,
    val startsAt: String?,
    val endsAt: String?,
    val timeZone: String,
    val availability: R5ExperienceAvailability,
    val availableDropCount: Int,
    val membership: R5ExperienceMembership
)

enum class R5EntryFailureReason {
    INVALID_CODE,
    EXPERIENCE_NOT_FOUND,
    EXPERIENCE_CANCELLED,
    EXPERIENCE_ENDED,
    RATE_LIMITED,
    OFFLINE,
    UNAVAILABLE,
    UNKNOWN
}

class R5EntryException(
    val reason: R5EntryFailureReason,
    val retryable: Boolean,
    cause: Throwable? = null
) : Exception(reason.name, cause)

data class R5PendingUnlock(
    val experienceCode: String?,
    val dropId: String
)
