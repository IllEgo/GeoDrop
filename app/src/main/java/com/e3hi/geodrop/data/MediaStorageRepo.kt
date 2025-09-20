package com.e3hi.geodrop.data

import android.webkit.MimeTypeMap
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MediaStorageRepo(
    private val storage: FirebaseStorage = defaultStorage()
) {
    suspend fun uploadMedia(
        contentType: DropContentType,
        data: ByteArray,
        mimeType: String?
    ): String {
        val (folder, defaultMime, defaultExtension) = when (contentType) {
            DropContentType.TEXT -> Triple("other", "text/plain", "txt")
            DropContentType.PHOTO -> Triple("photos", "image/jpeg", "jpg")
            DropContentType.AUDIO -> Triple("audio", "audio/mpeg", "m4a")
        }

        val resolvedMime = mimeType?.takeIf { it.isNotBlank() } ?: defaultMime
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.takeIf { it.isNotBlank() }
            ?: defaultExtension
        val fileName = "${System.currentTimeMillis()}-${UUID.randomUUID()}.$extension"
        val ref = storage.reference.child("drops/$folder/$fileName")

        val metadata = StorageMetadata.Builder()
            .setContentType(resolvedMime)
            .build()

        ref.putBytes(data, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    companion object {
        private fun defaultStorage(): FirebaseStorage {
            val app = FirebaseApp.getInstance()
            val bucket = app.options.storageBucket?.takeIf { it.isNotBlank() }

            if (bucket.isNullOrBlank()) {
                return Firebase.storage(app)
            }

            val normalizedBucket = normalizeBucket(bucket)

            return runCatching { Firebase.storage(app, normalizedBucket) }
                .getOrElse { Firebase.storage(app) }
        }

        private fun normalizeBucket(rawBucket: String): String {
            val withoutScheme = rawBucket.removePrefix("gs://")

            val canonicalBucket = withoutScheme.removeSuffix("/")

            return "gs://$canonicalBucket"
        }
    }
}