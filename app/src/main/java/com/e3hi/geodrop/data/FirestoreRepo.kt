package com.e3hi.geodrop.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
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
        val ref: DocumentReference = drops.add(drop).await()
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
        drops.add(drop)
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
            .get()
            .await()

        return snapshot.documents.map { doc ->
            doc.toObject(Drop::class.java)?.copy(id = doc.id) ?: Drop(id = doc.id)
        }
    }
}
