package com.e3hi.geodrop.data

enum class R8RewardCodeState { AVAILABLE, ISSUED, USED }

data class R8RewardCodeEvent(
    val transition: String,
    val occurredAtMillis: Long,
    val reason: String?
)

data class R8RewardCode(
    val code: String,
    val state: R8RewardCodeState,
    val issuedAtMillis: Long?,
    val usedAtMillis: Long?,
    val version: Int,
    val history: List<R8RewardCodeEvent> = emptyList()
)

data class R8DropResult(
    val dropId: String,
    val unlocks: Long,
    val codesIssued: Long,
    val codesUsed: Long,
    val updatedAtMillis: Long?
)

data class R8ExperienceResults(
    val experienceCode: String,
    val joinedParticipants: Long,
    val publishedDrops: Long,
    val uniqueUnlockers: Long,
    val unlocks: Long,
    val mainTrailCompletions: Long,
    val codesIssued: Long,
    val codesUsed: Long,
    val updatedAtMillis: Long?,
    val reconciledAtMillis: Long?,
    val drops: List<R8DropResult> = emptyList()
)

enum class R8CorrectionReason {
    MARKED_BY_MISTAKE,
    BUSINESS_CORRECTION
}

interface R8OrganizerGateway {
    suspend fun loadRewardCodes(
        dropId: String,
        state: R8RewardCodeState? = null,
        searchCode: String? = null
    ): List<R8RewardCode>

    suspend fun markRewardCodeUsed(dropId: String, code: String): Boolean

    suspend fun correctRewardCodeUse(
        dropId: String,
        code: String,
        reason: R8CorrectionReason
    ): Boolean

    suspend fun loadResults(experienceCode: String): R8ExperienceResults
}

object R8RewardPolicy {
    private val codePattern = Regex("^[A-Z0-9][A-Z0-9-]{3,31}$")

    fun normalizeCode(raw: String): String = raw.trim().uppercase()

    fun validateSearchCode(raw: String): String? {
        val code = normalizeCode(raw)
        return if (code.isEmpty() || codePattern.matches(code)) null
        else "Enter the complete reward code using letters, numbers, or hyphens."
    }
}
