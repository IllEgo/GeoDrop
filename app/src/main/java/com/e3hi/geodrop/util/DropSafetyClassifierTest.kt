package com.e3hi.geodrop.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.Base64
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Performs a light-weight skin-tone based analysis on image attachments to flag
 * potentially explicit imagery. The implementation purposefully keeps the
 * processing inexpensive so it can run on-device without blocking the UI.
 */
object ImageNsfwAnalyzer {

    private const val MAX_DIMENSION = 128
    private const val MIN_BRIGHT_PIXELS = 150
    private const val SKIN_THRESHOLD = 0.32

    fun analyzeBase64(mediaData: String): ImageNsfwResult? {
        val payload = mediaData.substringAfterLast(",", mediaData)
        val decoded = try {
            Base64.getDecoder().decode(payload)
        } catch (ignored: IllegalArgumentException) {
            return null
        }

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        var bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size, options) ?: return null
        if (bitmap.width == 0 || bitmap.height == 0) {
            bitmap.recycle()
            return null
        }

        val maxDimension = max(bitmap.width, bitmap.height)
        if (maxDimension > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toFloat() / maxDimension.toFloat()
            val targetWidth = max(1, (bitmap.width * scale).roundToInt())
            val targetHeight = max(1, (bitmap.height * scale).roundToInt())
            val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            if (scaled != bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }

        val totalPixels = bitmap.width * bitmap.height
        val buffer = IntArray(totalPixels)
        bitmap.getPixels(buffer, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var brightPixels = 0
        var skinPixels = 0

        buffer.forEach { pixel ->
            val alpha = pixel ushr 24 and 0xFF
            if (alpha < 64) return@forEach

            val r = pixel shr 16 and 0xFF
            val g = pixel shr 8 and 0xFF
            val b = pixel and 0xFF

            val maxChannel = max(max(r, g), b)
            val minChannel = min(min(r, g), b)
            if (maxChannel < 40) return@forEach

            brightPixels += 1

            val cb = (-0.168736 * r) + (-0.331264 * g) + (0.5 * b) + 128.0
            val cr = (0.5 * r) + (-0.418688 * g) + (-0.081312 * b) + 128.0

            val skinLike = cb in 77.0..127.0 && cr in 133.0..173.0 && (maxChannel - minChannel) > 15
            if (skinLike) {
                skinPixels += 1
            }
        }

        bitmap.recycle()

        if (brightPixels < MIN_BRIGHT_PIXELS) return null

        val coverage = skinPixels.toDouble() / brightPixels.toDouble()
        val flagged = coverage >= SKIN_THRESHOLD
        val confidence = ((coverage - SKIN_THRESHOLD) / (1.0 - SKIN_THRESHOLD)).coerceIn(0.0, 1.0)
        val reason = "Image content flagged by skin tone analysis (~${(coverage * 100).roundToInt()}% coverage)"

        return ImageNsfwResult(
            flagged = flagged,
            confidence = confidence,
            coverageRatio = coverage,
            reason = reason
        )
    }
}

data class ImageNsfwResult(
    val flagged: Boolean,
    val confidence: Double,
    val coverageRatio: Double,
    val reason: String
)