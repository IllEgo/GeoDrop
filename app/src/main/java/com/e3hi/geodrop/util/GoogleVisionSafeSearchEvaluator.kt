package com.e3hi.geodrop.util

import android.util.Log
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.VisionApiStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Drop safety evaluator backed by the authenticated Firebase callable. Vision
 * credentials remain server-side and are never embedded in the application.
 */
class GoogleVisionSafeSearchEvaluator(
    @Suppress("UNUSED_PARAMETER") apiKey: String = "",
    private val minimumLikelihood: Likelihood = Likelihood.VERY_LIKELY,
    private val safeSearchCallable: SafeSearchCallable? = null
) : DropSafetyEvaluator {

    override suspend fun assess(
        text: String?,
        contentType: DropContentType,
        mediaMimeType: String?,
        mediaData: String?,
        mediaUrl: String?
    ): DropSafetyAssessment {
        val eligibleForVision = contentType == DropContentType.PHOTO &&
                (mediaMimeType?.startsWith("image/") == true || !mediaData.isNullOrBlank() || !mediaUrl.isNullOrBlank())

        if (!eligibleForVision) {
            return safeAssessment(VisionApiStatus.NOT_ELIGIBLE)
        }

        val callable = safeSearchCallable ?: return safeAssessment(VisionApiStatus.NOT_CONFIGURED)

        val callableResult = try {
            withContext(Dispatchers.IO) {
                requestSafeSearchViaCallable(mediaData, callable)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Vision SafeSearch callable failed", t)
            return safeAssessment(VisionApiStatus.ERROR)
        }

        return finalizeAssessment(callableResult)
    }

    private fun safeAssessment(status: VisionApiStatus): DropSafetyAssessment = DropSafetyAssessment(
        isNsfw = false,
        reasons = emptyList(),
        visionStatus = status
    )

    private suspend fun requestSafeSearchViaCallable(
        mediaData: String?,
        callable: SafeSearchCallable
    ): VisionAssessment? {
        val payload = extractBase64Payload(mediaData) ?: return null
        val response = callable(payload) ?: return null
        return parseCallableResponse(response)
    }

    private fun extractBase64Payload(mediaData: String?): String? {
        val trimmedData = mediaData?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val payload = trimmedData.substringAfter(',', trimmedData).trim()
        return payload.takeIf { it.isNotBlank() }
    }

    private fun parseCallableResponse(body: Map<String, Any?>): VisionAssessment? {
        val likelihoods = SafeSearchCategory.entries.associateWith { category ->
            Likelihood.fromResponse(body[category.responseKey]?.toString())
        }

        return buildAssessment(likelihoods)
    }

    private fun buildAssessment(
        likelihoods: Map<SafeSearchCategory, Likelihood>
    ): VisionAssessment? {

        val reasons = SafeSearchCategory.entries.mapNotNull { category ->
            val likelihood = likelihoods[category] ?: Likelihood.UNKNOWN
            if (likelihood.isAtLeast(minimumLikelihood)) {
                "Google Vision flagged as ${category.reasonDescription} (${likelihood.readableLabel})"
            } else {
                null
            }
        }

        val flagged = reasons.isNotEmpty()

        return VisionAssessment(
            flagged = flagged,
            reasons = reasons
        )
    }

    private fun finalizeAssessment(result: VisionAssessment?): DropSafetyAssessment {
        val resolved = result ?: return safeAssessment(VisionApiStatus.ERROR)
        if (!resolved.flagged) {
            return safeAssessment(VisionApiStatus.CLEARED)
        }

        return DropSafetyAssessment(
            isNsfw = true,
            reasons = resolved.reasons,
            visionStatus = VisionApiStatus.FLAGGED
        )
    }

    private data class VisionAssessment(
        val flagged: Boolean,
        val reasons: List<String>
    )

    fun interface SafeSearchCallable {
        suspend operator fun invoke(base64Payload: String): Map<String, Any?>?
    }

    private enum class SafeSearchCategory(val responseKey: String, val reasonDescription: String) {
        ADULT("adult", "adult"),
        SPOOF("spoof", "spoofed"),
        MEDICAL("medical", "medical"),
        VIOLENCE("violence", "violent"),
        RACY("racy", "racy")
    }

    enum class Likelihood(val order: Int, val confidence: Double, val readableLabel: String) {
        UNKNOWN(0, 0.0, "Unknown"),
        VERY_UNLIKELY(1, 0.05, "Very unlikely"),
        UNLIKELY(2, 0.15, "Unlikely"),
        POSSIBLE(3, 0.4, "Possible"),
        LIKELY(4, 0.7, "Likely"),
        VERY_LIKELY(5, 0.9, "Very likely");

        fun isAtLeast(other: Likelihood): Boolean = order >= other.order

        companion object {
            fun fromResponse(value: String?): Likelihood {
                val normalized = value?.trim()?.uppercase() ?: return UNKNOWN
                return entries.firstOrNull { it.name == normalized } ?: UNKNOWN
            }
        }
    }

    private companion object {
        private const val TAG = "VisionSafetyEval"
    }
}
