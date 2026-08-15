package com.kitheapp.data

import org.junit.Assert.assertFalse
import org.junit.Test

class AccountLifecyclePolicyTest {
    @Test
    fun lifecycleFailuresNeverExposeRawMachineReasons() {
        val reasons = listOf(
            "REAUTHENTICATION_REQUIRED",
            "POLICY_VERSION_MISMATCH",
            "EXPLICIT_CONFIRMATION_REQUIRED",
            "SERVER_CONFIGURATION_REQUIRED",
            "SOMETHING_NEW"
        )

        reasons.forEach { reason ->
            val message = AccountLifecyclePolicy.failureMessage(reason)
            assertFalse(message.contains(reason))
            assertFalse(message.contains("error", ignoreCase = true))
        }
    }
}
