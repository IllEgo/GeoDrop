package com.kitheapp.ui.organizer

import org.junit.Assert.assertEquals
import org.junit.Test

class R7ExperienceDefaultsTest {
    @Test
    fun newExperienceStartsImmediatelyAndDefaultsToFourHours() {
        val now = 1_800_000_000_000L

        val window = r7DefaultExperienceWindow(now)

        assertEquals(now, window.first)
        assertEquals(now + 4 * 60 * 60 * 1_000L, window.second)
    }
}
