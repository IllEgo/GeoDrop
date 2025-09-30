package com.e3hi.geodrop.util

import com.e3hi.geodrop.data.DropContentType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleVisionSafeSearchEvaluatorTest {

    @Test
    fun `blank api key returns safe assessment`() = runBlocking {
        val evaluator = GoogleVisionSafeSearchEvaluator(apiKey = "")

        val assessment = evaluator.assess(
            text = null,
            contentType = DropContentType.PHOTO,
            mediaMimeType = "image/jpeg",
            mediaData = "base64payload",
            mediaUrl = null
        )

        assertFalse(assessment.isNsfw)
        assertTrue(assessment.reasons.isEmpty())
    }

    @Test
    fun `non photo content bypasses vision`() = runBlocking {
        val evaluator = GoogleVisionSafeSearchEvaluator(apiKey = "test")

        val assessment = evaluator.assess(
            text = "hello",
            contentType = DropContentType.TEXT,
            mediaMimeType = null,
            mediaData = null,
            mediaUrl = null
        )

        assertFalse(assessment.isNsfw)
        assertTrue(assessment.reasons.isEmpty())
    }
}