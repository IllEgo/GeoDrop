package com.e3hi.geodrop.ui.participant

import org.junit.Assert.assertEquals
import org.junit.Test

class R6ParticipantContentTest {

    @Test
    fun `distance presentation uses broad privacy-safe bands`() {
        assertEquals("Distance unavailable", r6DistanceLabel(null))
        assertEquals("Distance unavailable", r6DistanceLabel(Double.NaN))
        assertEquals("Nearby", r6DistanceLabel(100.0))
        assertEquals("A short walk", r6DistanceLabel(800.0))
        assertEquals("Farther out", r6DistanceLabel(801.0))
    }
}
