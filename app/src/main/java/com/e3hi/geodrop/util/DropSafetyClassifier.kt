package com.e3hi.geodrop.util

import com.e3hi.geodrop.data.VisionApiStatus

/**
 * Result returned by the NSFW evaluator pipeline.
 */

data class DropSafetyAssessment(
    val isNsfw: Boolean,
    val reasons: List<String>,
    val visionStatus: VisionApiStatus = VisionApiStatus.NOT_CONFIGURED
)

class DropBlockedBySafetyException(val assessment: DropSafetyAssessment) : Exception(
    "NSFW content requires adult verification"
)