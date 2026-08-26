package com.kitheapp.util

enum class R5OrganizerAccessAction {
    /** The Organizer route was not requested; leave the participant shell alone. */
    NONE,

    /** Guest or signed-out: open the Account sign-in dialog, once. */
    PROMPT_SIGN_IN,

    /** Sign-in was already offered for this request; stay on Account. */
    AWAIT_SIGN_IN,

    /** Signed in, but the backend Organizer-approval state is still loading. */
    AWAIT_ORGANIZER_ACCESS,

    /** Signed-in approved Organizer: open Organizer tools, once. */
    OPEN_ORGANIZER_TOOLS,

    /** Signed in but unapproved, or tools were already opened for this request. */
    STAY_ON_ACCOUNT
}

/**
 * Pure ordering contract for the Organizer-access entry route.
 *
 * The route only bypasses the membership-required entry gate. Authentication and the
 * backend Organizer-approval gate are still what decide whether tools open, so an
 * unapproved user always lands on [STAY_ON_ACCOUNT] with their application/status options.
 */
object R5OrganizerAccessPolicy {
    fun nextAction(
        organizerAccessRequested: Boolean,
        isAnonymous: Boolean?,
        signInPrompted: Boolean,
        organizerAccessLoading: Boolean,
        organizerToolsAvailable: Boolean,
        organizerToolsAutoOpened: Boolean
    ): R5OrganizerAccessAction = when {
        !organizerAccessRequested -> R5OrganizerAccessAction.NONE
        // A null user counts as not signed in, matching `currentUser?.isAnonymous != false`.
        isAnonymous != false ->
            if (signInPrompted) {
                R5OrganizerAccessAction.AWAIT_SIGN_IN
            } else {
                R5OrganizerAccessAction.PROMPT_SIGN_IN
            }
        organizerAccessLoading -> R5OrganizerAccessAction.AWAIT_ORGANIZER_ACCESS
        organizerToolsAvailable && !organizerToolsAutoOpened ->
            R5OrganizerAccessAction.OPEN_ORGANIZER_TOOLS
        else -> R5OrganizerAccessAction.STAY_ON_ACCOUNT
    }
}
