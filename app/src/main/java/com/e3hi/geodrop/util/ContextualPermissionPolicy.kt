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
    REQUIRE_NEARBY_LOCATION_FIRST,
    REQUEST_NOTIFICATIONS,
    OPEN_NOTIFICATION_SETTINGS,
    SHOW_BACKGROUND_LOCATION_RATIONALE,
    OPEN_BACKGROUND_LOCATION_SETTINGS,
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
        foregroundLocation: PermissionGrantState,
        notificationsRequired: Boolean = true,
        notifications: PermissionGrantState = PermissionGrantState.REQUESTABLE,
        backgroundLocationRequired: Boolean = true,
        backgroundLocation: PermissionGrantState = PermissionGrantState.REQUESTABLE
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

            ContextualPermissionIntent.ENABLE_NEARBY_ALERTS -> when {
                foregroundLocation != PermissionGrantState.GRANTED ->
                    ContextualPermissionAction.REQUIRE_NEARBY_LOCATION_FIRST

                notificationsRequired && notifications == PermissionGrantState.REQUESTABLE ->
                    ContextualPermissionAction.REQUEST_NOTIFICATIONS

                notificationsRequired && notifications == PermissionGrantState.BLOCKED ->
                    ContextualPermissionAction.OPEN_NOTIFICATION_SETTINGS

                backgroundLocationRequired &&
                    backgroundLocation == PermissionGrantState.REQUESTABLE ->
                    ContextualPermissionAction.SHOW_BACKGROUND_LOCATION_RATIONALE

                backgroundLocationRequired &&
                    backgroundLocation == PermissionGrantState.BLOCKED ->
                    ContextualPermissionAction.OPEN_BACKGROUND_LOCATION_SETTINGS

                else -> ContextualPermissionAction.ENABLE_NEARBY_ALERTS
            }
        }
    }
}
