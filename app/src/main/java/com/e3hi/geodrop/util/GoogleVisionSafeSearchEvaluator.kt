package com.e3hi.geodrop.util

import android.util.Log
import com.e3hi.geodrop.data.DropContentType
import com.e3hi.geodrop.data.VisionApiStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Drop safety evaluator backed exclusively by the Google Cloud Vision
 * SafeSearch API. If the call fails or the payload cannot be evaluated, the
 * content is treated as safe.
 */
class GoogleVisionSafeSearchEvaluator(
    private val apiKey: String,
    private val endpoint: String = "https://vision.googleapis.com/v1/images:annotate",
    private val minimumLikelihood: Likelihood = Likelihood.LIKELY,
    private val requestTimeoutMs: Int = 10_000,
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

        var directAttempted = false
        var directFailed = false
        var visionResult: VisionAssessment? = null

        if (apiKey.isNotBlank()) {
            directAttempted = true
            try {
                visionResult = withContext(Dispatchers.IO) {
                    requestSafeSearch(mediaData, mediaUrl)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Vision SafeSearch request failed", t)
                directFailed = true
            }
        }

        if (directAttempted && !directFailed) {
            return finalizeAssessment(visionResult)
        }

        val fallbackUnavailableStatus = if (directAttempted) {
            VisionApiStatus.ERROR
        } else {
            Log.i(TAG, "Vision API key missing; attempting callable fallback")
            VisionApiStatus.NOT_CONFIGURED
        }

        val callable = safeSearchCallable ?: return safeAssessment(fallbackUnavailableStatus)

        val callableResult = try {
            withContext(Dispatchers.IO) {
                requestSafeSearchViaCallable(mediaData, callable)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Vision SafeSearch callable fallback failed", t)
            return safeAssessment(VisionApiStatus.ERROR)
        }

        return finalizeAssessment(callableResult)
    }

    private fun safeAssessment(status: VisionApiStatus): DropSafetyAssessment = DropSafetyAssessment(
        isNsfw = false,
        reasons = emptyList(),
        visionStatus = status
    )

    private fun requestSafeSearch(
        mediaData: String?,
        mediaUrl: String?
    ): VisionAssessment? {
        val imagePayload = buildImagePayload(mediaData, mediaUrl) ?: return null
        val urlWithKey = buildEndpointUrl()
        val connection = (URL(urlWithKey).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = requestTimeoutMs
            readTimeout = requestTimeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        val requestBody = JSONObject().apply {
            put("requests", JSONArray().apply {
                put(JSONObject().apply {
                    put("image", imagePayload)
                    put("features", JSONArray().apply {
                        put(JSONObject().apply { put("type", "SAFE_SEARCH_DETECTION") })
                    })
                })
            })
        }

        return try {
            BufferedWriter(OutputStreamWriter(connection.outputStream, Charsets.UTF_8)).use { writer ->
                writer.write(requestBody.toString())
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseBody = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw IllegalStateException("Vision API error $responseCode: $responseBody")
            }
            if (responseBody.isBlank()) {
                return null
            }

            parseVisionResponse(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun requestSafeSearchViaCallable(
        mediaData: String?,
        callable: SafeSearchCallable
    ): VisionAssessment? {
        val payload = extractBase64Payload(mediaData) ?: return null
        val response = callable(payload) ?: return null
        return parseCallableResponse(response)
    }

    private fun buildImagePayload(mediaData: String?, mediaUrl: String?): JSONObject? {
        val payload = extractBase64Payload(mediaData)
        if (!payload.isNullOrBlank()) {
            return JSONObject().apply { put("content", payload) }
        }

        val sanitizedUrl = mediaUrl?.trim()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (!sanitizedUrl.isNullOrBlank()) {
            return JSONObject().apply {
                put("source", JSONObject().apply { put("imageUri", sanitizedUrl) })
            }
        }

        return null
    }

    private fun extractBase64Payload(mediaData: String?): String? {
        val trimmedData = mediaData?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val payload = trimmedData.substringAfter(',', trimmedData).trim()
        return payload.takeIf { it.isNotBlank() }
    }

    private fun buildEndpointUrl(): String {
        val separator = if (endpoint.contains('?')) '&' else '?'
        return "$endpoint${separator}key=$apiKey"
    }

    private fun parseVisionResponse(body: String): VisionAssessment? {
        val root = JSONObject(body)
        val responses = root.optJSONArray("responses") ?: return null
        if (responses.length() == 0) return null

        val first = responses.optJSONObject(0) ?: return null
        val error = first.optJSONObject("error")
        if (error != null) {
            val message = error.optString("message")
            throw IllegalStateException("Vision API error: $message")
        }

        val annotation = first.optJSONObject("safeSearchAnnotation") ?: return null
        val likelihoods = SafeSearchCategory.entries.associateWith { category ->
            Likelihood.fromResponse(annotation.optString(category.responseKey))
        }

        return buildAssessment(likelihoods)
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
        val resolved = result ?: return safeAssessment(VisionApiStatus.CLEARED)
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