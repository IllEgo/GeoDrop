package com.e3hi.geodrop.util

/**
 * Result returned by the NSFW evaluator pipeline.
 */
data class DropSafetyAssessment(
    val isNsfw: Boolean,
    val confidence: Double,
    val reasons: List<String>,
    val evaluatorScore: Double? = null,
    val classifierScore: Double? = null
)

class DropBlockedBySafetyException(val assessment: DropSafetyAssessment) : Exception(
    "NSFW content requires adult verification"
)