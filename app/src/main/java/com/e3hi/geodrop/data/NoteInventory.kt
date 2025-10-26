package com.e3hi.geodrop.data

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
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
    @Volatile
    private var activeUserKey: String = resolveUserKey(FirebaseAuth.getInstance().currentUser?.uid)

    /**
     * All [NoteInventory] instances read and write to the same SharedPreferences file, so changes
     * performed in one instance (for example from a broadcast receiver) should notify listeners
     * that were registered on another instance. Keep the listener registry in the companion object
     * so every instance shares the same callbacks.
     */
    private val listeners get() = sharedListeners

    @Synchronized
    fun setActiveUser(userId: String?) {
        val normalized = resolveUserKey(userId)
        if (activeUserKey == normalized) return

        activeUserKey = normalized
    }

    fun getCollectedNotes(): List<CollectedNote> {
        ensureStorageMigrated()
        val raw = prefs.getString(collectedKey(), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val notes = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(CollectedNote.fromJson(obj))
                }
            }.sortedByDescending { it.collectedAt }

            val now = System.currentTimeMillis()
            val (active, expired) = notes.partition { note -> !note.isExpired(now) }
            if (expired.isNotEmpty()) {
                persistCollected(active)
                expired.forEach { note ->
                    broadcastChange(changeType = CHANGE_REMOVED, dropId = note.id)
                }
                notifyListeners(ChangeOrigin.LOCAL)
            }

            active
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
        ensureStorageMigrated()
        val ignored = prefs.getStringSet(ignoredKey(), emptySet())?.toMutableSet() ?: mutableSetOf()
        if (ignored.add(id)) {
            prefs.edit { putStringSet(ignoredKey(), ignored) }
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

    fun updateLikeStatus(
        id: String,
        likeCount: Long,
        dislikeCount: Long,
        status: DropLikeStatus
    ) {
        val current = getCollectedNotes().toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx < 0) return

        val existing = current[idx]
        val updated = existing.copy(
            likeCount = likeCount,
            isLiked = status == DropLikeStatus.LIKED,
            dislikeCount = dislikeCount,
            isDisliked = status == DropLikeStatus.DISLIKED
        )
        if (updated == existing) return

        current[idx] = updated
        persistCollected(current)
        broadcastChange(changeType = CHANGE_COLLECTED, dropId = id)
        notifyListeners(ChangeOrigin.LOCAL)
    }

    fun isCollected(id: String): Boolean {
        ensureStorageMigrated()
        val stored = prefs.getStringSet(collectedIdsKey(), emptySet()) ?: emptySet()
        return stored.contains(id)
    }

    fun isIgnored(id: String): Boolean {
        ensureStorageMigrated()
        val stored = prefs.getStringSet(ignoredKey(), emptySet()) ?: emptySet()
        return stored.contains(id)
    }

    fun getIgnoredDropIds(): Set<String> {
        ensureStorageMigrated()
        val stored = prefs.getStringSet(ignoredKey(), emptySet()) ?: emptySet()
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
        prefs.edit { putStringSet(ignoredKey(), HashSet(snapshot.ignoredDropIds)) }

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
            putString(collectedKey(), array.toString())
            putStringSet(collectedIdsKey(), HashSet(ids))
        }
    }

    private fun removeIgnored(id: String) {
        val ignored = prefs.getStringSet(ignoredKey(), emptySet())?.toMutableSet() ?: mutableSetOf()
        if (ignored.remove(id)) {
            prefs.edit { putStringSet(ignoredKey(), ignored) }
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
        private const val KEY_COLLECTED_PREFIX = "collected_notes_v2_"
        private const val KEY_COLLECTED_IDS_PREFIX = "collected_note_ids_v2_"
        private const val KEY_IGNORED_PREFIX = "ignored_drop_ids_v2_"
        private const val DEFAULT_USER_KEY = "guest"

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

    private fun collectedKey(): String = collectedKey(activeUserKey)

    private fun collectedIdsKey(): String = collectedIdsKey(activeUserKey)

    private fun ignoredKey(): String = ignoredKey(activeUserKey)

    private fun collectedKey(userKey: String): String = "$KEY_COLLECTED_PREFIX$userKey"

    private fun collectedIdsKey(userKey: String): String = "$KEY_COLLECTED_IDS_PREFIX$userKey"

    private fun ignoredKey(userKey: String): String = "$KEY_IGNORED_PREFIX$userKey"

    private fun ensureStorageMigrated() {
        val legacyCollected = prefs.getString(KEY_COLLECTED, null)
        val legacyIds = prefs.getStringSet(KEY_COLLECTED_IDS, null)
        val legacyIgnored = prefs.getStringSet(KEY_IGNORED, null)
        if (legacyCollected == null && legacyIds == null && legacyIgnored == null) return

        val targetCollectedKey = collectedKey()
        val targetIdsKey = collectedIdsKey()
        val targetIgnoredKey = ignoredKey()
        if (prefs.contains(targetCollectedKey) || prefs.contains(targetIdsKey) ||
            prefs.contains(targetIgnoredKey)
        ) {
            // Data already migrated for this user; remove legacy leftovers to avoid reuse.
            prefs.edit {
                remove(KEY_COLLECTED)
                remove(KEY_COLLECTED_IDS)
                remove(KEY_IGNORED)
            }
            return
        }

        prefs.edit {
            legacyCollected?.let { putString(targetCollectedKey, it) }
            legacyIds?.let { putStringSet(targetIdsKey, HashSet(it)) }
            legacyIgnored?.let { putStringSet(targetIgnoredKey, HashSet(it)) }
            remove(KEY_COLLECTED)
            remove(KEY_COLLECTED_IDS)
            remove(KEY_IGNORED)
        }
    }

    private fun resolveUserKey(userId: String?): String {
        val raw = userId?.takeIf { it.isNotBlank() }
        return raw ?: DEFAULT_USER_KEY
    }
}