package com.kitheapp.data

enum class R9ExperienceAvailability {
    UPCOMING,
    ACTIVE,
    ENDED,
    CANCELLED
}

data class R9JoinedExperience(
    val code: String,
    val name: String,
    val hostLabel: String,
    val startsAtMillis: Long,
    val endsAtMillis: Long,
    val timeZone: String,
    val availability: R9ExperienceAvailability,
    val isOwned: Boolean
)

data class R9BlockedHost(
    val hostId: String,
    val hostLabel: String,
    val blockedAtMillis: Long?
)

enum class R9ReportState {
    RECEIVED,
    ACTION_TAKEN,
    CLOSED,
    ESCALATED,
    UPHELD,
    OVERTURNED,
    UNKNOWN;

    companion object {
        fun fromRaw(raw: Any?): R9ReportState = when (raw?.toString()?.trim()?.uppercase()) {
            "RECEIVED", "PENDING", "QUEUED" -> RECEIVED
            "ACTION_TAKEN" -> ACTION_TAKEN
            "CLOSED" -> CLOSED
            "ESCALATED" -> ESCALATED
            "UPHELD" -> UPHELD
            "OVERTURNED" -> OVERTURNED
            else -> UNKNOWN
        }
    }
}

data class R9ReportStatus(
    val reportId: String,
    val dropId: String?,
    val state: R9ReportState,
    val updatedAtMillis: Long?
)

object R9AccountPolicy {
    fun availability(
        state: R7ExperienceState,
        startsAtMillis: Long,
        endsAtMillis: Long,
        nowMillis: Long
    ): R9ExperienceAvailability = when {
        state == R7ExperienceState.CANCELLED -> R9ExperienceAvailability.CANCELLED
        nowMillis < startsAtMillis -> R9ExperienceAvailability.UPCOMING
        nowMillis >= endsAtMillis -> R9ExperienceAvailability.ENDED
        else -> R9ExperienceAvailability.ACTIVE
    }

    fun sortHistory(items: List<R9JoinedExperience>): List<R9JoinedExperience> =
        items.sortedWith(
            compareBy<R9JoinedExperience> {
                when (it.availability) {
                    R9ExperienceAvailability.ACTIVE -> 0
                    R9ExperienceAvailability.UPCOMING -> 1
                    R9ExperienceAvailability.ENDED -> 2
                    R9ExperienceAvailability.CANCELLED -> 3
                }
            }.thenByDescending(R9JoinedExperience::startsAtMillis)
                .thenBy(R9JoinedExperience::name)
        )

    fun reportStatusLabel(state: R9ReportState): String = when (state) {
        R9ReportState.RECEIVED -> "Received"
        R9ReportState.ACTION_TAKEN -> "Action taken"
        R9ReportState.CLOSED -> "Review complete"
        R9ReportState.ESCALATED -> "Additional review"
        R9ReportState.UPHELD -> "Appeal upheld"
        R9ReportState.OVERTURNED -> "Appeal overturned"
        R9ReportState.UNKNOWN -> "Status unavailable"
    }
}
