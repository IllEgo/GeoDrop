package com.e3hi.geodrop.util

import android.content.Context
import android.content.SharedPreferences
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

    fun getJoinedGroups(): List<String> {
        val stored = prefs.getStringSet(KEY_GROUPS, emptySet()) ?: emptySet()
        return stored
            .mapNotNull { normalizeGroupCode(it) }
            .distinct()
            .sorted()
    }

    fun addGroup(code: String) {
        val normalized = normalizeGroupCode(code) ?: return
        val current = getJoinedGroups().toMutableSet()
        if (current.add(normalized)) {
            persistGroups(current)
            notifyListeners(current.sorted(), ChangeOrigin.LOCAL)
        }
    }

    fun removeGroup(code: String) {
        val normalized = normalizeGroupCode(code) ?: return
        val current = getJoinedGroups().toMutableSet()
        if (current.remove(normalized)) {
            persistGroups(current)
            notifyListeners(current.sorted(), ChangeOrigin.LOCAL)
        }
    }

    fun clear() {
        val hadGroups = prefs.contains(KEY_GROUPS)
        prefs.edit().remove(KEY_GROUPS).apply()
        if (hadGroups) {
            notifyListeners(emptyList(), ChangeOrigin.LOCAL)
        }
    }

    fun replaceAllFromSync(codes: Collection<String>) {
        val normalized = codes
            .mapNotNull { normalizeGroupCode(it) }
            .distinct()
            .sorted()
        val previous = getJoinedGroups()
        if (previous != normalized) {
            persistGroups(normalized.toSet())
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
        fun onGroupsChanged(groups: List<String>, origin: ChangeOrigin)
    }

    private fun persistGroups(groups: Set<String>) {
        prefs.edit().putStringSet(KEY_GROUPS, HashSet(groups)).apply()
    }

    private fun notifyListeners(groups: List<String>, origin: ChangeOrigin) {
        listeners.forEach { listener ->
            listener.onGroupsChanged(groups, origin)
        }
    }
}