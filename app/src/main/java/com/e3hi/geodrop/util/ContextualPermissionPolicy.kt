package com.e3hi.geodrop.util

enum class PermissionGrantState {
    GRANTED,
    REQUESTABLE,
    BLOCKED
}

enum class ContextualPermissionIntent {
    NEARBY_DISCOVERY,
    ENABLE_NEARBY_ALERTS
}

enum class ContextualPermissionAction {
    NONE,
    REQUEST_FOREGROUND_LOCATION,
    OPEN_FOREGROUND_LOCATION_SETTINGS,
    REQUEST_NOTIFICATIONS,
    OPEN_NOTIFICATION_SETTINGS,
    ENABLE_NEARBY_ALERTS
}

/**
 * Pure policy for sequencing runtime permission prompts behind user intent.
 *
 * UI code remains responsible for rendering rationale/recovery copy and for
 * translating a system denial into REQUESTABLE or BLOCKED.
 */
object ContextualPermissionPolicy {
    fun nextAction(
        intent: ContextualPermissionIntent,
        onboardingComplete: Boolean,
        foregroundLocation: PermissionGrantState = PermissionGrantState.GRANTED,
        notificationsRequired: Boolean = true,
        notifications: PermissionGrantState = PermissionGrantState.REQUESTABLE
    ): ContextualPermissionAction {
        if (!onboardingComplete) return ContextualPermissionAction.NONE

        return when (intent) {
            ContextualPermissionIntent.NEARBY_DISCOVERY -> when (foregroundLocation) {
                PermissionGrantState.GRANTED -> ContextualPermissionAction.NONE
                PermissionGrantState.REQUESTABLE ->
                    ContextualPermissionAction.REQUEST_FOREGROUND_LOCATION
                PermissionGrantState.BLOCKED ->
                    ContextualPermissionAction.OPEN_FOREGROUND_LOCATION_SETTINGS
            }

            // Task 3.4 — alerts are membership-scoped and sent by the server, so this
            // intent needs no location grant of any kind, foreground or background.
            ContextualPermissionIntent.ENABLE_NEARBY_ALERTS -> when {
                notificationsRequired && notifications == PermissionGrantState.REQUESTABLE ->
                    ContextualPermissionAction.REQUEST_NOTIFICATIONS

                notificationsRequired && notifications == PermissionGrantState.BLOCKED ->
                    ContextualPermissionAction.OPEN_NOTIFICATION_SETTINGS

                else -> ContextualPermissionAction.ENABLE_NEARBY_ALERTS
            }
        }
    }
}
