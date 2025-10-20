package com.e3hi.geodrop.util

import android.content.Context
import android.content.SharedPreferences
import com.e3hi.geodrop.data.GroupMembership
import com.e3hi.geodrop.data.GroupRole
import java.util.HashSet
import java.util.Locale
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Stores the list of group access codes the user has joined locally on device.
 */
class GroupPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val listeners = CopyOnWriteArraySet<ChangeListener>()

    fun getMemberships(): List<GroupMembership> {
        val joined = prefs.getStringSet(KEY_GROUPS, emptySet()) ?: emptySet()
        val rawOwned = prefs.getStringSet(KEY_OWNED_GROUPS, null)
        val ownedSource = rawOwned ?: joined
        val normalizedOwned = ownedSource
            .mapNotNull { normalizeGroupCode(it) }
            .toSet()
        return joined
            .mapNotNull { stored ->
                val normalized = normalizeGroupCode(stored) ?: return@mapNotNull null
                val role = if (normalizedOwned.contains(normalized)) {
                    GroupRole.OWNER
                } else {
                    GroupRole.SUBSCRIBER
                }
                GroupMembership(code = normalized, ownerId = null, role = role)
            }
            .distinctBy { it.code }
            .sortedBy { it.code }
    }

    fun getJoinedGroups(): List<String> = getMemberships().map { it.code }

    fun getOwnedGroups(): List<String> {
        val joined = prefs.getStringSet(KEY_GROUPS, emptySet()) ?: emptySet()
        val rawOwned = prefs.getStringSet(KEY_OWNED_GROUPS, null)
        val ownedSource = rawOwned ?: joined
        return ownedSource
            .mapNotNull { normalizeGroupCode(it) }
            .distinct()
            .sorted()
    }

    fun isGroupOwned(code: String): Boolean {
        val normalized = normalizeGroupCode(code) ?: return false
        val joined = prefs.getStringSet(KEY_GROUPS, emptySet()) ?: emptySet()
        val rawOwned = prefs.getStringSet(KEY_OWNED_GROUPS, null)
        val ownedSource = rawOwned ?: joined
        return ownedSource.any { normalizeGroupCode(it) == normalized }
    }

    fun addGroup(membership: GroupMembership) {
        val normalized = normalizeGroupCode(membership.code) ?: return
        val currentJoined = getJoinedGroups().toMutableSet()
        val currentOwned = getOwnedGroups().toMutableSet()
        var changed = false
        if (currentJoined.add(normalized)) {
            changed = true
        }
        if (membership.role == GroupRole.OWNER) {
            if (currentOwned.add(normalized)) {
                changed = true
            }
        } else if (currentOwned.remove(normalized)) {
            changed = true
        }
        if (changed) {
            persistGroups(currentJoined, currentOwned)
            notifyListeners(getMemberships(), ChangeOrigin.LOCAL)
        }
    }

    fun removeGroup(code: String) {
        val normalized = normalizeGroupCode(code) ?: return
        val currentJoined = getJoinedGroups().toMutableSet()
        val currentOwned = getOwnedGroups().toMutableSet()
        var changed = currentJoined.remove(normalized)
        if (currentOwned.remove(normalized)) {
            changed = true
        }
        if (changed) {
            persistGroups(currentJoined, currentOwned)
            notifyListeners(getMemberships(), ChangeOrigin.LOCAL)
        }
    }

    fun clear() {
        val hadJoined = prefs.contains(KEY_GROUPS)
        val hadOwned = prefs.contains(KEY_OWNED_GROUPS)
        prefs.edit()
            .remove(KEY_GROUPS)
            .remove(KEY_OWNED_GROUPS)
            .apply()
        if (hadJoined || hadOwned) {
            notifyListeners(emptyList(), ChangeOrigin.LOCAL)
        }
    }

    fun replaceAllFromSync(memberships: Collection<GroupMembership>) {
        val normalized = memberships
            .mapNotNull { membership ->
                val normalizedCode = normalizeGroupCode(membership.code) ?: return@mapNotNull null
                membership.copy(code = normalizedCode)
            }
            .distinctBy { it.code }
            .sortedBy { it.code }

        val joined = normalized.map { it.code }.toSet()
        val owned = normalized.filter { it.role == GroupRole.OWNER }.map { it.code }.toSet()

        val previousJoined = getJoinedGroups().toSet()
        val previousOwned = getOwnedGroups().toSet()

        if (previousJoined != joined || previousOwned != owned) {
            persistGroups(joined, owned)
        }

        notifyListeners(normalized, ChangeOrigin.REMOTE)
    }

    fun addChangeListener(listener: ChangeListener) {
        listeners.add(listener)
    }

    fun removeChangeListener(listener: ChangeListener) {
        listeners.remove(listener)
    }

    companion object {
        private const val PREFS_NAME = "geodrop_groups"
        private const val KEY_GROUPS = "joined_group_codes"
        private const val KEY_OWNED_GROUPS = "owned_group_codes"

        fun normalizeGroupCode(input: String?): String? {
            if (input == null) return null
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return null
            return trimmed.uppercase(Locale.US)
        }
    }

    enum class ChangeOrigin {
        LOCAL,
        REMOTE
    }

    fun interface ChangeListener {
        fun onGroupsChanged(groups: List<GroupMembership>, origin: ChangeOrigin)
    }

    private fun persistGroups(joined: Set<String>, owned: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_GROUPS, HashSet(joined))
            .putStringSet(KEY_OWNED_GROUPS, HashSet(owned))
            .apply()
    }

    private fun notifyListeners(groups: List<GroupMembership>, origin: ChangeOrigin) {
        listeners.forEach { listener ->
            listener.onGroupsChanged(groups, origin)
        }
    }
}