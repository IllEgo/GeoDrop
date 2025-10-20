package com.e3hi.geodrop.data

data class GroupMembership(
    val code: String,
    val ownerId: String?,
    val role: GroupRole
)

enum class GroupRole {
    OWNER,
    SUBSCRIBER;

    companion object {
        fun fromRaw(raw: String?): GroupRole {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SUBSCRIBER
        }
    }
}