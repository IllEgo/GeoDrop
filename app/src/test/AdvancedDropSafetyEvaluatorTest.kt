package com.e3hi.geodrop.util

import com.e3hi.geodrop.data.DropContentType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdvancedDropSafetyEvaluatorTest {

    @Test
    fun `fallback preserves evaluator score when classifier flags content`() = runBlocking {
        val evaluator = AdvancedDropSafetyEvaluator(
            endpointUrl = "http://127.0.0.1:9",
            timeoutMs = 100,
            fallback = HeuristicDropSafetyEvaluator
        )

        val assessment = evaluator.assess(
            text = "This contains porn content",
            contentType = DropContentType.TEXT,
            mediaMimeType = null,
            mediaData = null,
            mediaUrl = null
        )

        assertTrue(assessment.isNsfw)
        assertNotNull(assessment.evaluatorScore)
        assertEquals(assessment.classifierScore, assessment.evaluatorScore)
    }

    @Test
    fun `fallback does not report evaluator score for safe content`() = runBlocking {
        val evaluator = AdvancedDropSafetyEvaluator(
            endpointUrl = "http://127.0.0.1:9",
            timeoutMs = 100,
            fallback = HeuristicDropSafetyEvaluator
        )

        val assessment = evaluator.assess(
            text = "This is a wholesome note",
            contentType = DropContentType.TEXT,
            mediaMimeType = null,
            mediaData = null,
            mediaUrl = null
        )

        assertFalse(assessment.isNsfw)
        assertNull(assessment.evaluatorScore)
    }
}