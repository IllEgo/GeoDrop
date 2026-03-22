package com.e3hi.geodrop.data

data class HuntChain(
    val id: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val businessId: String? = null,
    val businessName: String? = null,
    val title: String = "",
    val description: String? = null,
    val dropIds: List<String> = emptyList(),
    val isActive: Boolean = true,
    val decayDays: Int? = null,
    val totalSteps: Int = 0
)

data class HuntProgress(
    val huntId: String = "",
    val currentStepIndex: Int = 0,
    val completedStepIds: List<String> = emptyList(),
    val completedAt: Long? = null,
    val startedAt: Long = 0L
) {
    val isComplete: Boolean get() = completedAt != null
}
