package com.e3hi.geodrop.util

import com.e3hi.geodrop.data.DropContentType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}