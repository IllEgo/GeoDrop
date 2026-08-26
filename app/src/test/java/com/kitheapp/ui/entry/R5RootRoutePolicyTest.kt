package com.kitheapp.ui.entry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class R5RootRoutePolicyTest {
    @Test
    fun `first launch without memberships stays on Experience entry`() {
        assertTrue(
            R5RootRoutePolicy.showEntry(
                hasActiveRequest = false,
                hasMemberships = false,
                manualEntryRequested = false,
                organizerAccessRequested = false
            )
        )
    }

    @Test
    fun `Organizer request bypasses membership-only entry gate`() {
        assertFalse(
            R5RootRoutePolicy.showEntry(
                hasActiveRequest = false,
                hasMemberships = false,
                manualEntryRequested = false,
                organizerAccessRequested = true
            )
        )
    }

    @Test
    fun `incoming Experience request still wins when Organizer route is inactive`() {
        assertTrue(
            R5RootRoutePolicy.showEntry(
                hasActiveRequest = true,
                hasMemberships = true,
                manualEntryRequested = false,
                organizerAccessRequested = false
            )
        )
    }
}
