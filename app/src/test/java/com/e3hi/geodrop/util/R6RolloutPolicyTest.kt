package com.e3hi.geodrop.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class R6RolloutPolicyTest {

    @Test
    fun `target backend stays closed when switch is false`() {
        assertFalse(R6RolloutPolicy.isTargetBackendUsable(false, 1))
    }

    @Test
    fun `unsupported minimum contract fails closed`() {
        assertFalse(R6RolloutPolicy.isTargetBackendUsable(true, Int.MAX_VALUE.toLong()))
        assertFalse(R6RolloutPolicy.isTargetBackendUsable(true, 0))
    }

    @Test
    fun `supported contract opens only with explicit switch`() {
        assertTrue(R6RolloutPolicy.isTargetBackendUsable(true, 1))
    }
}
