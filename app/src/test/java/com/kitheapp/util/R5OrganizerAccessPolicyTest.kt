package com.kitheapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class R5OrganizerAccessPolicyTest {
    @Test
    fun `participant route leaves the shell alone`() {
        assertEquals(
            R5OrganizerAccessAction.NONE,
            R5OrganizerAccessPolicy.nextAction(
                organizerAccessRequested = false,
                isAnonymous = false,
                signInPrompted = false,
                organizerAccessLoading = false,
                organizerToolsAvailable = true,
                organizerToolsAutoOpened = false
            )
        )
    }

    @Test
    fun `guest is taken to Account sign-in`() {
        assertEquals(
            R5OrganizerAccessAction.PROMPT_SIGN_IN,
            R5OrganizerAccessPolicy.nextAction(
                organizerAccessRequested = true,
                isAnonymous = true,
                signInPrompted = false,
                organizerAccessLoading = false,
                organizerToolsAvailable = false,
                organizerToolsAutoOpened = false
            )
        )
    }

    @Test
    fun `signed-out user with no account is treated as a guest`() {
        assertEquals(
            R5OrganizerAccessAction.PROMPT_SIGN_IN,
            R5OrganizerAccessPolicy.nextAction(
                organizerAccessRequested = true,
                isAnonymous = null,
                signInPrompted = false,
                organizerAccessLoading = false,
                organizerToolsAvailable = false,
                organizerToolsAutoOpened = false
            )
        )
    }

    @Test
    fun `sign-in is offered once, not on every recomposition`() {
        assertEquals(
            R5OrganizerAccessAction.AWAIT_SIGN_IN,
            R5OrganizerAccessPolicy.nextAction(
                organizerAccessRequested = true,
                isAnonymous = true,
                signInPrompted = true,
                organizerAccessLoading = false,
                organizerToolsAvailable = false,
                organizerToolsAutoOpened = false
            )
        )
    }

    @Test
    fun `approved Organizer opens tools automatically`() {
        assertEquals(
            R5OrganizerAccessAction.OPEN_ORGANIZER_TOOLS,
            R5OrganizerAccessPolicy.nextAction(
                organizerAccessRequested = true,
                isAnonymous = false,
                signInPrompted = true,
                organizerAccessLoading = false,
                organizerToolsAvailable = true,
                organizerToolsAutoOpened = false
            )
        )
    }

    @Test
    fun `tools open once so the operator can navigate away`() {
        assertEquals(
            R5OrganizerAccessAction.STAY_ON_ACCOUNT,
            R5OrganizerAccessPolicy.nextAction(
                organizerAccessRequested = true,
                isAnonymous = false,
                signInPrompted = true,
                organizerAccessLoading = false,
                organizerToolsAvailable = true,
                organizerToolsAutoOpened = true
            )
        )
    }

    @Test
    fun `unapproved user stays on Account with their status options`() {
        assertEquals(
            R5OrganizerAccessAction.STAY_ON_ACCOUNT,
            R5OrganizerAccessPolicy.nextAction(
                organizerAccessRequested = true,
                isAnonymous = false,
                signInPrompted = true,
                organizerAccessLoading = false,
                organizerToolsAvailable = false,
                organizerToolsAutoOpened = false
            )
        )
    }

    @Test
    fun `approval gate is never pre-empted while it is still loading`() {
        assertEquals(
            R5OrganizerAccessAction.AWAIT_ORGANIZER_ACCESS,
            R5OrganizerAccessPolicy.nextAction(
                organizerAccessRequested = true,
                isAnonymous = false,
                signInPrompted = true,
                organizerAccessLoading = true,
                organizerToolsAvailable = false,
                organizerToolsAutoOpened = false
            )
        )
    }

    @Test
    fun `a stale approved flag cannot open tools while access is reloading`() {
        assertEquals(
            R5OrganizerAccessAction.AWAIT_ORGANIZER_ACCESS,
            R5OrganizerAccessPolicy.nextAction(
                organizerAccessRequested = true,
                isAnonymous = false,
                signInPrompted = true,
                organizerAccessLoading = true,
                organizerToolsAvailable = true,
                organizerToolsAutoOpened = false
            )
        )
    }

    @Test
    fun `an anonymous user is never routed into Organizer tools`() {
        assertEquals(
            R5OrganizerAccessAction.PROMPT_SIGN_IN,
            R5OrganizerAccessPolicy.nextAction(
                organizerAccessRequested = true,
                isAnonymous = true,
                signInPrompted = false,
                organizerAccessLoading = false,
                organizerToolsAvailable = true,
                organizerToolsAutoOpened = false
            )
        )
    }
}
