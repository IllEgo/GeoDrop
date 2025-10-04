package com.e3hi.geodrop.data

data class UserProfile(
    val id: String = "",
    val displayName: String? = null,
    val username: String? = null,
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

fun UserProfile.canViewNsfw(): Boolean = nsfwEnabled