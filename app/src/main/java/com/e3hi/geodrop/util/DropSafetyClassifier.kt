package com.e3hi.geodrop.util

import com.e3hi.geodrop.data.DropContentType
import java.util.Locale
import kotlin.math.round

/**
 * Lightweight heuristic "AI" classifier that flags potentially NSFW drops.
 * This is intentionally conservative and errs on the side of caution by
 * flagging content whenever a risky keyword or MIME type is detected.
 */
object DropSafetyClassifier {
    private val keywordSignals: Map<String, String> = mapOf(
        "porn" to "Contains explicit adult reference",
        "porno" to "Contains explicit adult reference",
        "sex" to "Contains sexual content",
        "xxx" to "Contains explicit content shorthand",
        "nude" to "References nudity",
        "nudity" to "References nudity",
        "nsfw" to "Marked as NSFW",
        "explicit" to "Marked as explicit",
        "fetish" to "References fetish content",
        "erotic" to "References erotic content"
    )

    private val riskyMimePrefixes: Map<String, Pair<Double, String>> = mapOf(
        "image/" to (0.2 to "Contains an image attachment"),
        "video/" to (0.25 to "Contains a video attachment"),
        "audio/" to (0.15 to "Contains an audio attachment")
    )

    fun evaluate(
        text: String?,
        contentType: DropContentType,
        mediaMimeType: String?,
        mediaData: String?,
        mediaUrl: String?
    ): DropSafetyAssessment {
        val lowered = text?.lowercase(Locale.US).orEmpty()
        val strongSignals = mutableListOf<String>()
        val weakSignals = mutableListOf<String>()
        var confidence = 0.05

        keywordSignals.forEach { (keyword, reason) ->
            if (lowered.contains(keyword)) {
                strongSignals += reason
                confidence += 0.25
            }
        }

        if (mediaUrl?.lowercase(Locale.US)?.contains("nsfw") == true) {
            strongSignals += "Media URL indicates NSFW content"
            confidence += 0.2
        }

        riskyMimePrefixes.forEach { (prefix, pair) ->
            val (weight, reason) = pair
            if (mediaMimeType?.lowercase(Locale.US)?.startsWith(prefix) == true) {
                weakSignals += reason
                confidence += weight
            }
        }

        if (contentType != DropContentType.TEXT && !mediaData.isNullOrBlank()) {
            weakSignals += "Contains embedded media data"
            confidence += 0.1
        }

        if (contentType != DropContentType.TEXT && !mediaUrl.isNullOrBlank()) {
            weakSignals += "Contains linked media attachment"
            confidence += 0.15
        }

        val normalizedConfidence = confidence.coerceIn(0.0, 0.99)
        val threshold = if (strongSignals.isNotEmpty()) 0.35 else 0.65
        val isNsfw = strongSignals.isNotEmpty() || normalizedConfidence >= threshold
        val reasons = if (isNsfw) (strongSignals + weakSignals).distinct() else emptyList()

        val reportedConfidence = if (isNsfw) normalizedConfidence else 0.0

        return DropSafetyAssessment(
            isNsfw = isNsfw,
            confidence = round(reportedConfidence * 100) / 100.0,
            reasons = reasons.distinct()
        )
    }
}

data class DropSafetyAssessment(
    val isNsfw: Boolean,
    val confidence: Double,
    val reasons: List<String>
)

class DropBlockedBySafetyException(val assessment: DropSafetyAssessment) : Exception(
    "NSFW content requires adult verification"
)