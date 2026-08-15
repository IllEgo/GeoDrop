package com.kitheapp.util

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

    /**
     * Task 3.4 — alerts became membership-scoped, so they must not depend on any
     * location grant. This is the property that replaced the old
     * REQUIRE_NEARBY_LOCATION_FIRST and background-rationale steps.
     */
    @Test
    fun `alerts need no location grant at all`() {
        val action = ContextualPermissionPolicy.nextAction(
            intent = ContextualPermissionIntent.ENABLE_NEARBY_ALERTS,
            onboardingComplete = true,
            foregroundLocation = PermissionGrantState.BLOCKED,
            notifications = PermissionGrantState.GRANTED
        )

        assertEquals(ContextualPermissionAction.ENABLE_NEARBY_ALERTS, action)
    }

    @Test
    fun `alerts request notifications first`() {
        val action = ContextualPermissionPolicy.nextAction(
            intent = ContextualPermissionIntent.ENABLE_NEARBY_ALERTS,
            onboardingComplete = true,
            notifications = PermissionGrantState.REQUESTABLE
        )

        assertEquals(ContextualPermissionAction.REQUEST_NOTIFICATIONS, action)
    }

    @Test
    fun `alerts enable only when required access is granted`() {
        val action = ContextualPermissionPolicy.nextAction(
            intent = ContextualPermissionIntent.ENABLE_NEARBY_ALERTS,
            onboardingComplete = true,
            notifications = PermissionGrantState.GRANTED
        )

        assertEquals(ContextualPermissionAction.ENABLE_NEARBY_ALERTS, action)
    }
}
