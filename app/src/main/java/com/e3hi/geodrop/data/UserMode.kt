package com.e3hi.geodrop.data

/**
 * Represents how a person is currently using GeoDrop. Modes determine whether the
 * session is strictly read-only or can fully participate in community actions.
 */
enum class UserMode {
    /** Browsing without any account — can only read drops. */
    GUEST,

    /** Temporary anonymous account that stays private but remains read-only. */
    ANONYMOUS_BROWSING,

    /** Standard signed-in account with full participation rights. */
    SIGNED_IN;

    val isReadOnly: Boolean
        get() = this != SIGNED_IN

    val canParticipate: Boolean
        get() = this == SIGNED_IN
}