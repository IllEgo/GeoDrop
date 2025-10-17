package com.e3hi.geodrop.data

import android.util.Log
import com.e3hi.geodrop.util.GroupPreferences
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class UserDataSyncRepository(
    private val firestoreRepo: FirestoreRepo,
    private val groupPreferences: GroupPreferences,
    private val noteInventory: NoteInventory,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var currentUserId: String? = null
    private var groupRegistration: ListenerRegistration? = null
    private var inventoryRegistration: ListenerRegistration? = null
    private var initializationJob: Job? = null

    private val groupChangeListener = GroupPreferences.ChangeListener { groups, origin ->
        if (origin != GroupPreferences.ChangeOrigin.LOCAL) return@ChangeListener
        val uid = currentUserId ?: return@ChangeListener
        scope.launch(ioDispatcher) {
            runCatching { firestoreRepo.replaceUserGroups(uid, groups) }
                .onFailure { error ->
                    Log.e(TAG, "Failed to sync groups for $uid", error)
                }
        }
    }

    private val inventoryChangeListener = NoteInventory.ChangeListener { snapshot, origin ->
        if (origin != NoteInventory.ChangeOrigin.LOCAL) return@ChangeListener
        val uid = currentUserId ?: return@ChangeListener
        scope.launch(ioDispatcher) {
            runCatching { firestoreRepo.replaceUserInventory(uid, snapshot) }
                .onFailure { error ->
                    Log.e(TAG, "Failed to sync inventory for $uid", error)
                }
        }
    }

    fun start(userId: String) {
        if (userId.isBlank()) {
            stop()
            return
        }
        if (currentUserId == userId) return

        stop()
        currentUserId = userId
        noteInventory.setActiveUser(userId)
        groupPreferences.addChangeListener(groupChangeListener)
        noteInventory.addChangeListener(inventoryChangeListener)
        initializationJob = scope.launch(ioDispatcher) {
            initializeSync(userId)
        }
    }

    fun stop() {
        initializationJob?.cancel()
        initializationJob = null
        groupRegistration?.remove()
        groupRegistration = null
        inventoryRegistration?.remove()
        inventoryRegistration = null
        groupPreferences.removeChangeListener(groupChangeListener)
        noteInventory.removeChangeListener(inventoryChangeListener)
        noteInventory.setActiveUser(null)
        currentUserId = null
    }

    private suspend fun initializeSync(userId: String) {
        try {
            val localGroups = groupPreferences.getJoinedGroups()
            val remoteGroups = runCatching { firestoreRepo.fetchUserGroups(userId) }
                .onFailure { error ->
                    Log.e(TAG, "Failed to fetch remote groups for $userId", error)
                }
                .getOrDefault(emptyList())

            if (!coroutineContext.isActive) return

            if (remoteGroups.isEmpty() && localGroups.isNotEmpty()) {
                runCatching { firestoreRepo.replaceUserGroups(userId, localGroups) }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to migrate local groups for $userId", error)
                    }
            } else {
                withContext(Dispatchers.Main) {
                    groupPreferences.replaceAllFromSync(remoteGroups)
                }
            }

            if (!coroutineContext.isActive) return

            groupRegistration = firestoreRepo.listenForUserGroups(
                userId,
                onChanged = { codes ->
                    scope.launch {
                        groupPreferences.replaceAllFromSync(codes)
                    }
                },
                onError = { error ->
                    Log.e(TAG, "Group listener error for $userId", error)
                }
            )

            val localInventory = noteInventory.getSnapshot()
            val remoteInventoryResult = runCatching { firestoreRepo.fetchUserInventory(userId) }
                .onFailure { error ->
                    Log.e(TAG, "Failed to fetch remote inventory for $userId", error)
                }
            val remoteInventory = remoteInventoryResult.getOrNull()

            if (!coroutineContext.isActive) return

            val remoteHasData = remoteInventory?.let {
                it.collectedNotes.isNotEmpty() || it.ignoredDropIds.isNotEmpty()
            } == true
            val localHasData =
                localInventory.collectedNotes.isNotEmpty() || localInventory.ignoredDropIds.isNotEmpty()

            if (!remoteHasData && localHasData) {
                runCatching { firestoreRepo.replaceUserInventory(userId, localInventory) }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to migrate local inventory for $userId", error)
                    }
            } else if (remoteInventory != null) {
                withContext(Dispatchers.Main) {
                    noteInventory.replaceFromRemote(remoteInventory)
                }
            }

            if (!coroutineContext.isActive) return

            inventoryRegistration = firestoreRepo.listenForUserInventory(
                userId,
                onChanged = { snapshot ->
                    scope.launch {
                        noteInventory.replaceFromRemote(snapshot)
                    }
                },
                onError = { error ->
                    Log.e(TAG, "Inventory listener error for $userId", error)
                }
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e(TAG, "User data sync initialization failed for $userId", error)
        }
    }

    companion object {
        private const val TAG = "GeoDropSync"
    }
}