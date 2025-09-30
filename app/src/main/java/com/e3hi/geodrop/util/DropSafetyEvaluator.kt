package com.e3hi.geodrop.util

import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.VisionApiStatus

/**
 * Contract for pluggable NSFW evaluators. Implementations can call the Google
 * Vision API or provide a lightweight no-op used for previews/tests.
 */
interface DropSafetyEvaluator {
    suspend fun assess(
        text: String?,
        contentType: DropContentType,
        mediaMimeType: String?,
        mediaData: String?,
        mediaUrl: String?
    ): DropSafetyAssessment
}

/**
 * Convenience evaluator that always returns a safe assessment. Useful for UI
 * previews or when an API key is unavailable.
 */
object NoOpDropSafetyEvaluator : DropSafetyEvaluator {
    override suspend fun assess(
        text: String?,
        contentType: DropContentType,
        mediaMimeType: String?,
        mediaData: String?,
        mediaUrl: String?
    ): DropSafetyAssessment = DropSafetyAssessment(
        isNsfw = false,
        confidence = 0.0,
        reasons = emptyList(),
        evaluatorScore = null,
        classifierScore = null,
        visionStatus = VisionApiStatus.NOT_CONFIGURED
    )
}