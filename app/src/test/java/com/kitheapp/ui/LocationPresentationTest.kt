package com.kitheapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationPresentationTest {
    @Test
    fun `approximate area zoom never implies more precision than the fix`() {
        assertEquals(13f, approximateLocationZoom(null))
        assertEquals(15f, approximateLocationZoom(50.0))
        assertEquals(14f, approximateLocationZoom(300.0))
        assertEquals(13f, approximateLocationZoom(1_000.0))
        assertEquals(12f, approximateLocationZoom(2_000.0))
    }

    @Test
    fun `approximate area remains a blob even when reported accuracy is small or absent`() {
        assertEquals(1_000.0, approximateAreaRadiusMeters(null), 0.0)
        assertEquals(250.0, approximateAreaRadiusMeters(25.0), 0.0)
        assertEquals(2_000.0, approximateAreaRadiusMeters(2_000.0), 0.0)
    }

    @Test
    fun `browse distances use broad bands instead of exact values`() {
        assertEquals(BrowseDistanceBand.NEARBY, browseDistanceBand(0.0))
        assertEquals(BrowseDistanceBand.NEARBY, browseDistanceBand(300.0))
        assertEquals(BrowseDistanceBand.SHORT_WALK, browseDistanceBand(300.1))
        assertEquals(BrowseDistanceBand.SHORT_WALK, browseDistanceBand(1_000.0))
        assertEquals(BrowseDistanceBand.FARTHER_OUT, browseDistanceBand(1_000.1))
    }
}
