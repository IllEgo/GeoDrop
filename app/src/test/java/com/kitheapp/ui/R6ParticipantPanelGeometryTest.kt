package com.kitheapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class R6ParticipantPanelGeometryTest {

    @Test
    fun `anchored state NaN falls back to collapsed position`() {
        val translation = finitePanelTranslation(Float.NaN, 320f)

        assertEquals(320f, translation, 0f)
        assertTrue(translation.isFinite())
    }

    @Test
    fun `finite drag offset is preserved`() {
        assertEquals(96f, finitePanelTranslation(96f, 320f), 0f)
    }

    @Test
    fun `non-finite fallback never reaches graphics layer`() {
        val translation = finitePanelTranslation(Float.POSITIVE_INFINITY, Float.NaN)

        assertEquals(0f, translation, 0f)
        assertTrue(translation.isFinite())
    }
}
