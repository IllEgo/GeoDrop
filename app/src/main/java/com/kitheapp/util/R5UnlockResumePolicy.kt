package com.kitheapp.util

enum class R5UnlockAccountState {
    GUEST,
    ACCOUNT
}

enum class R5UnlockResumeAction {
    SHOW_ACCOUNT_GATE,
    WAIT_FOR_TARGET,
    SHOW_PRECISE_PRIMER,
    OPEN_LOCATION_SETTINGS,
    RESUME_EXACT_UNLOCK,
    NONE
}

/** Pure ordering contract for the R5 account/permission/exact-target handoff. */
object R5UnlockResumePolicy {
    fun nextAction(
        accountState: R5UnlockAccountState,
        targetAvailable: Boolean,
        preciseLocation: PermissionGrantState,
        primerDismissedForTarget: Boolean = false
    ): R5UnlockResumeAction = when {
        accountState == R5UnlockAccountState.GUEST ->
            R5UnlockResumeAction.SHOW_ACCOUNT_GATE
        !targetAvailable -> R5UnlockResumeAction.WAIT_FOR_TARGET
        preciseLocation == PermissionGrantState.GRANTED ->
            R5UnlockResumeAction.RESUME_EXACT_UNLOCK
        primerDismissedForTarget -> R5UnlockResumeAction.NONE
        preciseLocation == PermissionGrantState.BLOCKED ->
            R5UnlockResumeAction.OPEN_LOCATION_SETTINGS
        else -> R5UnlockResumeAction.SHOW_PRECISE_PRIMER
    }
}
