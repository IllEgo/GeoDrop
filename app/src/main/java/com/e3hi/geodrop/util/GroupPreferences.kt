package com.e3hi.geodrop.util

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

/**
 * Stores the list of group access codes the user has joined locally on device.
 */
class GroupPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
            prefs.edit().putStringSet(KEY_GROUPS, current).apply()
        }
    }

    fun removeGroup(code: String) {
        val normalized = normalizeGroupCode(code) ?: return
        val current = getJoinedGroups().toMutableSet()
        if (current.remove(normalized)) {
            prefs.edit().putStringSet(KEY_GROUPS, current).apply()
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_GROUPS).apply()
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
}