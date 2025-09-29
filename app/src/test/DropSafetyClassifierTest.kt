package com.e3hi.geodrop.util

import android.graphics.Bitmap
import android.graphics.Color
import com.e3hi.geodrop.data.DropContentType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DropSafetyClassifierTest {

    @Test
    fun `photo attachments alone are not flagged as nsfw`() {
        val assessment = DropSafetyClassifier.evaluate(
            text = null,
            contentType = DropContentType.PHOTO,
            mediaMimeType = "image/jpeg",
            mediaData = "base64payload",
            mediaUrl = "https://example.com/photo.jpg"
        )

        assertFalse(assessment.isNsfw)
        assertTrue(assessment.reasons.isEmpty())
    }

    @Test
    fun `explicit keywords continue to trigger nsfw flag`() {
        val assessment = DropSafetyClassifier.evaluate(
            text = "This contains porn content",
            contentType = DropContentType.TEXT,
            mediaMimeType = null,
            mediaData = null,
            mediaUrl = null
        )

        assertTrue(assessment.isNsfw)
        assertTrue(assessment.reasons.isNotEmpty())
    }

    @Test
    fun `skin tone heavy images are flagged`() {
        val base64 = createSolidImageBase64(Color.rgb(222, 184, 135))

        val assessment = DropSafetyClassifier.evaluate(
            text = null,
            contentType = DropContentType.PHOTO,
            mediaMimeType = "image/png",
            mediaData = base64,
            mediaUrl = null
        )

        assertTrue(assessment.isNsfw)
        assertTrue(assessment.reasons.any { it.contains("skin tone", ignoreCase = true) })
    }

    @Test
    fun `non skin tone images are not flagged`() {
        val base64 = createSolidImageBase64(Color.rgb(25, 70, 180))

        val assessment = DropSafetyClassifier.evaluate(
            text = null,
            contentType = DropContentType.PHOTO,
            mediaMimeType = "image/png",
            mediaData = base64,
            mediaUrl = null
        )

        assertFalse(assessment.isNsfw)
    }

    private fun createSolidImageBase64(color: Int): String {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return Base64.getEncoder().encodeToString(stream.toByteArray())
    }
}