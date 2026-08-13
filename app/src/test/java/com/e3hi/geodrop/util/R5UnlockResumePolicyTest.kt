package com.e3hi.geodrop.util

import org.junit.Assert.assertEquals
import org.junit.Test

class R5UnlockResumePolicyTest {
    @Test
    fun `guest sees account gate before any location ask`() {
        assertEquals(
            R5UnlockResumeAction.SHOW_ACCOUNT_GATE,
            R5UnlockResumePolicy.nextAction(
                accountState = R5UnlockAccountState.GUEST,
                targetAvailable = true,
                preciseLocation = PermissionGrantState.REQUESTABLE
            )
        )
    }

    @Test
    fun `linked or merged account waits until exact target is loaded`() {
        assertEquals(
            R5UnlockResumeAction.WAIT_FOR_TARGET,
            R5UnlockResumePolicy.nextAction(
                accountState = R5UnlockAccountState.ACCOUNT,
                targetAvailable = false,
                preciseLocation = PermissionGrantState.GRANTED
            )
        )
    }

    @Test
    fun `coarse only account receives precise primer`() {
        assertEquals(
            R5UnlockResumeAction.SHOW_PRECISE_PRIMER,
            R5UnlockResumePolicy.nextAction(
                accountState = R5UnlockAccountState.ACCOUNT,
                targetAvailable = true,
                preciseLocation = PermissionGrantState.REQUESTABLE
            )
        )
    }

    @Test
    fun `blocked precise location offers settings recovery`() {
        assertEquals(
            R5UnlockResumeAction.OPEN_LOCATION_SETTINGS,
            R5UnlockResumePolicy.nextAction(
                accountState = R5UnlockAccountState.ACCOUNT,
                targetAvailable = true,
                preciseLocation = PermissionGrantState.BLOCKED
            )
        )
    }

    @Test
    fun `precise grant resumes exact unlock`() {
        assertEquals(
            R5UnlockResumeAction.RESUME_EXACT_UNLOCK,
            R5UnlockResumePolicy.nextAction(
                accountState = R5UnlockAccountState.ACCOUNT,
                targetAvailable = true,
                preciseLocation = PermissionGrantState.GRANTED
            )
        )
    }
}
