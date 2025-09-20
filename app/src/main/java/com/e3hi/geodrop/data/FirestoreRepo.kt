package com.e3hi.geodrop.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Your existing Drop shape — matches other parts of the app that read:
 * text / lat / lng / createdBy / createdAt
 */


class FirestoreRepo(
    private val db: FirebaseFirestore = Firebase.firestore
) {
    private val drops = db.collection("drops")

    /**
     * NEW: Suspend API. Writes a drop and returns the new document id.
     */
    suspend fun addDrop(drop: Drop): String {
        val dropToSave = drop.prepareForSave()
        val ref: DocumentReference = drops.add(dropToSave).await()
        Log.d("GeoDrop", "Created drop ${ref.id}")
        return ref.id
    }

    /**
     * BACK-COMPAT: Callback-based write (if other places still use it).
     */
    fun createDrop(
        drop: Drop,
        onId: (String) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val dropToSave = drop.prepareForSave()

        drops.add(dropToSave)
            .addOnSuccessListener { ref ->
                Log.d("GeoDrop", "Created drop ${ref.id}")
                onId(ref.id)
            }
            .addOnFailureListener { e ->
                Log.e("GeoDrop", "Create drop FAILED", e)
                onError(e)
            }
    }

    suspend fun getDropsForUser(uid: String): List<Drop> {
        val snapshot = drops
            .whereEqualTo("createdBy", uid)
            .whereEqualTo("isDeleted", false)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val drop = doc.toObject(Drop::class.java)?.copy(id = doc.id)
                ?: Drop(id = doc.id)

            if (drop.isDeleted) null else drop
        }
    }

    suspend fun deleteDrop(dropId: String) {
        if (dropId.isBlank()) return

        val updates = mapOf(
            "isDeleted" to true,
            "deletedAt" to System.currentTimeMillis()
        )

        drops
            .document(dropId)
            .set(updates, SetOptions.merge())
            .await()

        Log.d("GeoDrop", "Marked drop $dropId as deleted")
    }


    private fun Drop.prepareForSave(): Map<String, Any?> {
        val withTimestamp = if (createdAt > 0L) this else copy(createdAt = System.currentTimeMillis())

        return hashMapOf(
            "text" to withTimestamp.text,
            "lat" to withTimestamp.lat,
            "lng" to withTimestamp.lng,
            "createdBy" to withTimestamp.createdBy,
            "createdAt" to withTimestamp.createdAt,
            "isDeleted" to false,
            "deletedAt" to null,
            "groupCode" to withTimestamp.groupCode?.takeIf { it.isNotBlank() }
        )
    }
}
