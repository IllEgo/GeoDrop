package com.e3hi.geodrop.data

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import androidx.core.content.edit
import java.util.HashSet
import java.util.concurrent.CopyOnWriteArraySet
import org.json.JSONArray
import org.json.JSONException

/**
 * Local storage for notes collected by the user and drops they have chosen to ignore.
 */
class NoteInventory(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    /**
     * All [NoteInventory] instances read and write to the same SharedPreferences file, so changes
     * performed in one instance (for example from a broadcast receiver) should notify listeners
     * that were registered on another instance. Keep the listener registry in the companion object
     * so every instance shares the same callbacks.
     */
    private val listeners get() = sharedListeners

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
        notifyListeners(ChangeOrigin.LOCAL)
    }

    fun removeCollected(id: String) {
        val current = getCollectedNotes().filterNot { it.id == id }
        persistCollected(current)
        broadcastChange(changeType = CHANGE_REMOVED, dropId = id)
        notifyListeners(ChangeOrigin.LOCAL)
    }

    fun markIgnored(id: String) {
        val ignored = prefs.getStringSet(KEY_IGNORED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (ignored.add(id)) {
            prefs.edit { putStringSet(KEY_IGNORED, ignored) }
            broadcastChange(changeType = CHANGE_IGNORED, dropId = id)
            notifyListeners(ChangeOrigin.LOCAL)
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
        notifyListeners(ChangeOrigin.LOCAL)
    }

    fun updateLikeStatus(id: String, likeCount: Long, isLiked: Boolean) {
        val current = getCollectedNotes().toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return

        val existing = current[idx]
        val updated = existing.copy(likeCount = likeCount, isLiked = isLiked)
        if (updated == existing) return

        current[idx] = updated
        persistCollected(current)
        broadcastChange(changeType = CHANGE_COLLECTED, dropId = id)
        notifyListeners(ChangeOrigin.LOCAL)
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

    fun getSnapshot(): Snapshot = Snapshot(
        collectedNotes = getCollectedNotes(),
        ignoredDropIds = getIgnoredDropIds()
    )

    fun replaceFromRemote(snapshot: Snapshot) {
        val previousCollected = getCollectedNotes()
        val previousIgnored = getIgnoredDropIds()

        persistCollected(snapshot.collectedNotes)
        prefs.edit { putStringSet(KEY_IGNORED, HashSet(snapshot.ignoredDropIds)) }

        val previousCollectedMap = previousCollected.associateBy { it.id }
        val newCollectedMap = snapshot.collectedNotes.associateBy { it.id }

        val addedCollected = newCollectedMap.keys - previousCollectedMap.keys
        val removedCollected = previousCollectedMap.keys - newCollectedMap.keys

        val ignoredAdded = snapshot.ignoredDropIds - previousIgnored
        snapshot.collectedNotes.forEach { note ->
            val previous = previousCollectedMap[note.id]
            if (previous == null || previous != note) {
                broadcastChange(CHANGE_COLLECTED, note.id)
            }
        }
        removedCollected.forEach { id -> broadcastChange(CHANGE_REMOVED, id) }
        ignoredAdded.forEach { id -> broadcastChange(CHANGE_IGNORED, id) }

        notifyListeners(ChangeOrigin.REMOTE)
    }

    fun addChangeListener(listener: ChangeListener) {
        listeners.add(listener)
    }

    fun removeChangeListener(listener: ChangeListener) {
        listeners.remove(listener)
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

        private val sharedListeners = CopyOnWriteArraySet<ChangeListener>()
    }

    data class Snapshot(
        val collectedNotes: List<CollectedNote>,
        val ignoredDropIds: Set<String>
    )

    enum class ChangeOrigin {
        LOCAL,
        REMOTE
    }

    fun interface ChangeListener {
        fun onInventoryChanged(snapshot: Snapshot, origin: ChangeOrigin)
    }

    private fun notifyListeners(origin: ChangeOrigin) {
        val snapshot = getSnapshot()
        listeners.forEach { listener ->
            listener.onInventoryChanged(snapshot, origin)
        }
    }
}