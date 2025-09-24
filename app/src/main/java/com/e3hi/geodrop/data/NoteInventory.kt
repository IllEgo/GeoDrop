package com.e3hi.geodrop.data

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONException

/**
 * Local storage for notes collected by the user and drops they have chosen to ignore.
 */
class NoteInventory(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    fun getCollectedNotes(): List<CollectedNote> {
        val raw = prefs.getString(KEY_COLLECTED, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(CollectedNote.fromJson(obj))
                }
            }.sortedByDescending { it.collectedAt }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun saveCollected(note: CollectedNote) {
        val current = getCollectedNotes().toMutableList()
        val idx = current.indexOfFirst { it.id == note.id }
        if (idx >= 0) {
            current[idx] = note
        } else {
            current.add(0, note)
        }
        persistCollected(current)
        removeIgnored(note.id)
        broadcastChange(changeType = CHANGE_COLLECTED, dropId = note.id)
    }

    fun removeCollected(id: String) {
        val current = getCollectedNotes().filterNot { it.id == id }
        persistCollected(current)
        broadcastChange(changeType = CHANGE_REMOVED, dropId = id)
    }

    fun markIgnored(id: String) {
        val ignored = prefs.getStringSet(KEY_IGNORED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (ignored.add(id)) {
            prefs.edit { putStringSet(KEY_IGNORED, ignored) }
            broadcastChange(changeType = CHANGE_IGNORED, dropId = id)
        }
    }

    fun updateRedemptionStatus(
        id: String,
        redemptionCount: Int?,
        redeemedAt: Long?,
        isRedeemed: Boolean
    ) {
        val current = getCollectedNotes().toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return

        val existing = current[idx]
        val updated = existing.copy(
            redemptionCount = redemptionCount ?: existing.redemptionCount,
            redeemedAt = redeemedAt ?: existing.redeemedAt,
            isRedeemed = isRedeemed
        )
        current[idx] = updated
        persistCollected(current)
        broadcastChange(changeType = CHANGE_COLLECTED, dropId = id)
    }

    fun isCollected(id: String): Boolean {
        val stored = prefs.getStringSet(KEY_COLLECTED_IDS, emptySet()) ?: emptySet()
        return stored.contains(id)
    }

    fun isIgnored(id: String): Boolean {
        val stored = prefs.getStringSet(KEY_IGNORED, emptySet()) ?: emptySet()
        return stored.contains(id)
    }

    fun getIgnoredDropIds(): Set<String> {
        val stored = prefs.getStringSet(KEY_IGNORED, emptySet()) ?: emptySet()
        return stored.toSet()
    }

    private fun persistCollected(notes: List<CollectedNote>) {
        val array = JSONArray()
        val ids = mutableSetOf<String>()
        notes.forEach { note ->
            array.put(note.toJson())
            ids.add(note.id)
        }
        prefs.edit {
            putString(KEY_COLLECTED, array.toString())
            putStringSet(KEY_COLLECTED_IDS, ids)
        }
    }

    private fun removeIgnored(id: String) {
        val ignored = prefs.getStringSet(KEY_IGNORED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (ignored.remove(id)) {
            prefs.edit { putStringSet(KEY_IGNORED, ignored) }
        }
    }

    private fun broadcastChange(changeType: String, dropId: String) {
        val intent = Intent(ACTION_INVENTORY_CHANGED).apply {
            putExtra(EXTRA_CHANGE_TYPE, changeType)
            putExtra(EXTRA_DROP_ID, dropId)
            `package` = appContext.packageName
        }
        appContext.sendBroadcast(intent)
    }

    companion object {
        const val ACTION_INVENTORY_CHANGED = "com.e3hi.geodrop.action.INVENTORY_CHANGED"
        const val EXTRA_CHANGE_TYPE = "changeType"
        const val EXTRA_DROP_ID = "dropId"
        const val CHANGE_COLLECTED = "collected"
        const val CHANGE_REMOVED = "removed"
        const val CHANGE_IGNORED = "ignored"

        private const val PREFS_NAME = "geodrop_note_inventory"
        private const val KEY_COLLECTED = "collected_notes"
        private const val KEY_COLLECTED_IDS = "collected_note_ids"
        private const val KEY_IGNORED = "ignored_drop_ids"
    }
}