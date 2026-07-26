package com.e3hi.geodrop.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextualPermissionPolicyTest {
    @Test
    fun `permission prompts are deferred until onboarding completes`() {
        val action = ContextualPermissionPolicy.nextAction(
            intent = ContextualPermissionIntent.NEARBY_DISCOVERY,
            onboardingComplete = false,
            foregroundLocation = PermissionGrantState.REQUESTABLE
        )

        assertEquals(ContextualPermissionAction.NONE, action)
    }

    @Test
    fun `nearby discovery is the foreground location request surface`() {
        val action = ContextualPermissionPolicy.nextAction(
            intent = ContextualPermissionIntent.NEARBY_DISCOVERY,
            onboardingComplete = true,
            foregroundLocation = PermissionGrantState.REQUESTABLE
        )

        assertEquals(ContextualPermissionAction.REQUEST_FOREGROUND_LOCATION, action)
    }

    @Test
    fun `alerts direct users to nearby before requesting location`() {
        val action = ContextualPermissionPolicy.nextAction(
            intent = ContextualPermissionIntent.ENABLE_NEARBY_ALERTS,
            onboardingComplete = true,
            foregroundLocation = PermissionGrantState.REQUESTABLE
        )

        assertEquals(ContextualPermissionAction.REQUIRE_NEARBY_LOCATION_FIRST, action)
    }

    @Test
    fun `alerts request notifications before background location`() {
        val action = ContextualPermissionPolicy.nextAction(
            intent = ContextualPermissionIntent.ENABLE_NEARBY_ALERTS,
            onboardingComplete = true,
            foregroundLocation = PermissionGrantState.GRANTED,
            notifications = PermissionGrantState.REQUESTABLE,
            backgroundLocation = PermissionGrantState.REQUESTABLE
        )

        assertEquals(ContextualPermissionAction.REQUEST_NOTIFICATIONS, action)
    }

    @Test
    fun `background location is preceded by explicit rationale`() {
        val action = ContextualPermissionPolicy.nextAction(
            intent = ContextualPermissionIntent.ENABLE_NEARBY_ALERTS,
            onboardingComplete = true,
            foregroundLocation = PermissionGrantState.GRANTED,
            notifications = PermissionGrantState.GRANTED,
            backgroundLocation = PermissionGrantState.REQUESTABLE
        )

        assertEquals(ContextualPermissionAction.SHOW_BACKGROUND_LOCATION_RATIONALE, action)
    }

    @Test
    fun `alerts enable only when required access is granted`() {
        val action = ContextualPermissionPolicy.nextAction(
            intent = ContextualPermissionIntent.ENABLE_NEARBY_ALERTS,
            onboardingComplete = true,
            foregroundLocation = PermissionGrantState.GRANTED,
            notifications = PermissionGrantState.GRANTED,
            backgroundLocation = PermissionGrantState.GRANTED
        )

        assertEquals(ContextualPermissionAction.ENABLE_NEARBY_ALERTS, action)
    }
}
