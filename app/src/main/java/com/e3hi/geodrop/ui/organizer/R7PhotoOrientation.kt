package com.e3hi.geodrop.ui.organizer

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

internal data class R7ExifTransform(
    val rotationDegrees: Float,
    val flipHorizontal: Boolean
)

internal object R7PhotoOrientation {
    fun decode(contentResolver: ContentResolver, uri: Uri): Bitmap {
        val orientation = runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val decoded = contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            ?: error("Image unavailable")
        return apply(decoded, orientation)
    }

    fun transformFor(orientation: Int): R7ExifTransform = when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> R7ExifTransform(0f, true)
        ExifInterface.ORIENTATION_ROTATE_180 -> R7ExifTransform(180f, false)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> R7ExifTransform(180f, true)
        ExifInterface.ORIENTATION_TRANSPOSE -> R7ExifTransform(90f, true)
        ExifInterface.ORIENTATION_ROTATE_90 -> R7ExifTransform(90f, false)
        ExifInterface.ORIENTATION_TRANSVERSE -> R7ExifTransform(270f, true)
        ExifInterface.ORIENTATION_ROTATE_270 -> R7ExifTransform(270f, false)
        else -> R7ExifTransform(0f, false)
    }

    fun apply(source: Bitmap, orientation: Int): Bitmap {
        val transform = transformFor(orientation)
        if (transform.rotationDegrees == 0f && !transform.flipHorizontal) return source
        val matrix = Matrix().apply {
            if (transform.rotationDegrees != 0f) {
                postRotate(transform.rotationDegrees)
            }
            if (transform.flipHorizontal) {
                postScale(-1f, 1f)
            }
        }
        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        ).also { transformed ->
            if (transformed !== source) source.recycle()
        }
    }
}
