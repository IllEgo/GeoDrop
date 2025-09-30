package com.e3hi.geodrop.util

import com.e3hi.geodrop.data.VisionApiStatus

/**
 * Result returned by the NSFW evaluator pipeline.
 */

data class DropSafetyAssessment(
    val isNsfw: Boolean,
    val confidence: Double,
    val reasons: List<String>,
    val evaluatorScore: Double? = null,
    val classifierScore: Double? = null,
    val visionStatus: VisionApiStatus = VisionApiStatus.NOT_CONFIGURED
)

class DropBlockedBySafetyException(val assessment: DropSafetyAssessment) : Exception(
    "NSFW content requires adult verification"
)