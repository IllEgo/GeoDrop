package com.e3hi.geodrop.data

data class UserProfile(
    val id: String = "",
    val displayName: String? = null,
    val username: String? = null,
    val memberSince: Long? = null,
    val role: UserRole = UserRole.EXPLORER,
    val businessName: String? = null,
    val businessCategories: List<BusinessCategory> = emptyList(),
    val nsfwEnabled: Boolean = false,
    val nsfwEnabledAt: Long? = null
)

enum class UserRole {
    EXPLORER,
    BUSINESS;

    companion object {
        fun fromRaw(raw: String?): UserRole {
            if (raw.isNullOrBlank()) return EXPLORER
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: EXPLORER
        }
    }
}

fun UserProfile.isBusiness(): Boolean = role == UserRole.BUSINESS

// Mature content stays unavailable for the market pilot, including profiles
// that may still contain a legacy opt-in value.
fun UserProfile.canViewNsfw(): Boolean = false
