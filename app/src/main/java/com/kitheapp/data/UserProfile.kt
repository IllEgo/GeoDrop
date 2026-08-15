package com.kitheapp.data

data class UserProfile(
    val id: String = "",
    val displayName: String? = null,
    val username: String? = null,
    val memberSince: Long? = null,
    val role: UserRole = UserRole.EXPLORER,
    val businessName: String? = null,
    val businessCategories: List<BusinessCategory> = emptyList()
)

/**
 * The launch scope has exactly two account types. `role` on the profile document is
 * server-authored: a client may only ever seed [EXPLORER], and promotion to [BUSINESS]
 * happens in the `updateBusinessProfile` callable.
 */
enum class UserRole {
    EXPLORER,
    BUSINESS;

    companion object {
        /**
         * Canonical stored values are the uppercase enum names. firestore.rules compares
         * the stored string exactly (`role == 'BUSINESS'`), so accepting variants here
         * would let the client and the server disagree about who is a business. Anything
         * off-model resolves to [EXPLORER], the least-privileged type.
         */
        fun fromRaw(raw: String?): UserRole {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) return EXPLORER
            return entries.firstOrNull { it.name == value } ?: EXPLORER
        }
    }
}

fun UserProfile.isBusiness(): Boolean = role == UserRole.BUSINESS
