package com.e3hi.geodrop.util

import com.e3hi.geodrop.data.DropContentType
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

fun DropSafetyAssessment.visionStatusMessage(
    contentType: DropContentType
): String? {
    return when (visionStatus) {
        VisionApiStatus.NOT_CONFIGURED ->
            "Google Vision SafeSearch isn't configured, so this drop wasn't scanned."

        VisionApiStatus.NOT_ELIGIBLE -> when (contentType) {
            DropContentType.PHOTO ->
                "Google Vision SafeSearch skipped this photo because it couldn't be processed."

            else ->
                "Google Vision SafeSearch only scans photo drops, so this one was skipped."
        }

        VisionApiStatus.ERROR ->
            "Google Vision SafeSearch couldn't be reached, so the drop was saved without a scan."

        VisionApiStatus.CLEARED ->
            "Google Vision SafeSearch scanned the drop and cleared it."

        VisionApiStatus.FLAGGED -> {
            val reason = reasons.firstOrNull()?.takeIf { it.isNotBlank() }
            if (reason != null) {
                "Google Vision SafeSearch flagged this drop: $reason"
            } else {
                "Google Vision SafeSearch flagged this drop as potentially unsafe content."
            }
        }
    }
}