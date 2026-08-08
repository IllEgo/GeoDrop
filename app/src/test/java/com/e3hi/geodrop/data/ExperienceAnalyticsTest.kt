package com.e3hi.geodrop.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExperienceAnalyticsTest {

    @Test
    fun fromMap_readsServerCountersAndTimestamps() {
        val analytics = ExperienceAnalytics.fromMap(
            groupCode = "PILOT1",
            data = mapOf(
                "drops" to 4L,
                "collects" to 18L,
                "redemptions" to 7L,
                "updatedAt" to 1234L,
                "reconciledAt" to 1200L
            )
        )

        assertEquals("PILOT1", analytics.groupCode)
        assertEquals(4L, analytics.drops)
        assertEquals(18L, analytics.collects)
        assertEquals(7L, analytics.redemptions)
        assertEquals(1234L, analytics.updatedAtMillis)
        assertEquals(1200L, analytics.reconciledAtMillis)
    }

    @Test
    fun fromMap_defaultsMissingOrInvalidValuesSafely() {
        val analytics = ExperienceAnalytics.fromMap(
            groupCode = "EMPTY",
            data = mapOf("drops" to -2L, "collects" to "not-a-number")
        )

        assertEquals(0L, analytics.drops)
        assertEquals(0L, analytics.collects)
        assertEquals(0L, analytics.redemptions)
        assertNull(analytics.updatedAtMillis)
        assertNull(analytics.reconciledAtMillis)
    }
}
