package com.kitheapp.data

import android.net.Uri
import android.webkit.MimeTypeMap
import com.kitheapp.util.PilotFeatureFlags
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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
    ): MediaUploadResult {
        check(PilotFeatureFlags.creationEnabled && PilotFeatureFlags.mediaEnabled) {
            "Media uploads are disabled for this release."
        }
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

        val ownerId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Sign in before uploading media")

        val metadata = StorageMetadata.Builder()
            .setContentType(resolvedMime)
            .setCustomMetadata("ownerId", ownerId)
            .setCustomMetadata("accessLevel", "PRIVATE")
            .setCustomMetadata("safetyStatus", "PENDING")
            .build()

        ref.putBytes(data, metadata).await()
        val downloadUrl = rulesCheckedMediaUrl(ref.bucket, ref.path)
        return MediaUploadResult(
            downloadUrl = downloadUrl,
            storagePath = ref.path.trimStart('/')
        )
    }

    suspend fun deleteMedia(storagePath: String) {
        val normalizedPath = storagePath.trim().trimStart('/')
        if (normalizedPath.isEmpty()) return
        storage.reference.child(normalizedPath).delete().await()
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

        private fun rulesCheckedMediaUrl(bucket: String, storagePath: String): String {
            val encodedBucket = Uri.encode(bucket)
            val encodedPath = Uri.encode(storagePath.trimStart('/'))
            return "https://firebasestorage.googleapis.com/v0/b/$encodedBucket/o/$encodedPath?alt=media"
        }
    }
}

data class MediaUploadResult(
    val downloadUrl: String,
    val storagePath: String
)
