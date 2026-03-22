package com.e3hi.geodrop.data

data class HuntStepDraft(
    val stepIndex: Int = 0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val noteText: String = "",
    val description: String = "",
    val dropType: DropType = DropType.COMMUNITY,
    val dropAnonymously: Boolean = false,
    val redemptionCode: String = "",
    val redemptionLimit: Int? = null
)

data class HuntBuilderState(
    val title: String = "",
    val description: String = "",
    val decayDays: Int? = null,
    val steps: List<HuntStepDraft> = listOf(HuntStepDraft(stepIndex = 0)),
    val currentStepEditing: Int = 0
) {
    val isEditingStep: Boolean get() = currentStepEditing < steps.size

    fun addStep(lat: Double = 0.0, lng: Double = 0.0): HuntBuilderState {
        val newStep = HuntStepDraft(stepIndex = steps.size, lat = lat, lng = lng)
        return copy(
            steps = steps + newStep,
            currentStepEditing = steps.size
        )
    }

    fun updateStep(index: Int, block: (HuntStepDraft) -> HuntStepDraft): HuntBuilderState {
        if (index !in steps.indices) return this
        return copy(steps = steps.toMutableList().also { it[index] = block(it[index]) })
    }

    fun removeStep(index: Int): HuntBuilderState {
        if (steps.size <= 1 || index !in steps.indices) return this
        val newSteps = steps.toMutableList().also { it.removeAt(index) }
            .mapIndexed { i, s -> s.copy(stepIndex = i) }
        return copy(
            steps = newSteps,
            currentStepEditing = currentStepEditing.coerceAtMost(newSteps.lastIndex)
        )
    }
}
