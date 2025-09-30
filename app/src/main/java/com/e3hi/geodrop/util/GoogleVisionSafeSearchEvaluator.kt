package com.e3hi.geodrop.util

import android.util.Log
import com.e3hi.geodrop.data.DropContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.round

/**
 * Drop safety evaluator backed by the Google Cloud Vision SafeSearch API.
 * It inspects photo drops for adult / racy likelihoods and combines the
 * result with the existing heuristic classifier. If the Vision call fails,
 * the heuristic assessment is returned instead so the app never blocks the
 * user flow.
 */
class GoogleVisionSafeSearchEvaluator(
    private val apiKey: String,
    private val endpoint: String = "https://vision.googleapis.com/v1/images:annotate",
    private val minimumLikelihood: Likelihood = Likelihood.LIKELY,
    private val fallback: DropSafetyEvaluator = HeuristicDropSafetyEvaluator,
    private val requestTimeoutMs: Int = 10_000
) : DropSafetyEvaluator {

    override suspend fun assess(
        text: String?,
        contentType: DropContentType,
        mediaMimeType: String?,
        mediaData: String?,
        mediaUrl: String?
    ): DropSafetyAssessment {
        val heuristic = fallback.assess(text, contentType, mediaMimeType, mediaData, mediaUrl)

        val eligibleForVision = apiKey.isNotBlank() &&
                contentType == DropContentType.PHOTO &&
                (mediaMimeType?.startsWith("image/") == true || !mediaData.isNullOrBlank())

        if (!eligibleForVision) {
            return heuristic
        }

        return try {
            val visionResult = withContext(Dispatchers.IO) {
                requestSafeSearch(mediaData, mediaUrl)
            }

            if (visionResult == null) {
                heuristic
            } else {
                combineAssessments(heuristic, visionResult)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Vision SafeSearch request failed; using heuristic only", t)
            heuristic
        }
    }

    private fun combineAssessments(
        heuristic: DropSafetyAssessment,
        vision: VisionAssessment
    ): DropSafetyAssessment {
        val combinedReasons = mutableListOf<String>()
        if (vision.flagged && vision.reasons.isNotEmpty()) {
            combinedReasons += vision.reasons
        }
        if (heuristic.isNsfw && heuristic.reasons.isNotEmpty()) {
            combinedReasons += heuristic.reasons
        }

        val flagged = heuristic.isNsfw || vision.flagged
        val confidenceSources = buildList {
            if (vision.flagged) add(vision.confidence)
            if (heuristic.isNsfw) add(heuristic.confidence)
        }
        val resolvedConfidence = if (flagged) {
            confidenceSources.maxOrNull() ?: 0.0
        } else {
            0.0
        }

        return DropSafetyAssessment(
            isNsfw = flagged,
            confidence = resolvedConfidence,
            reasons = combinedReasons.distinct(),
            evaluatorScore = vision.confidence.takeIf { vision.flagged },
            classifierScore = heuristic.classifierScore
                ?: heuristic.confidence.takeIf { heuristic.isNsfw }
        )
    }

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

    private fun buildImagePayload(mediaData: String?, mediaUrl: String?): JSONObject? {
        val trimmedData = mediaData?.trim()?.takeIf { it.isNotEmpty() }
        if (!trimmedData.isNullOrBlank()) {
            val payload = trimmedData.substringAfter(',', trimmedData)
            if (payload.isNotBlank()) {
                return JSONObject().apply { put("content", payload) }
            }
        }

        val sanitizedUrl = mediaUrl?.trim()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (!sanitizedUrl.isNullOrBlank()) {
            return JSONObject().apply {
                put("source", JSONObject().apply { put("imageUri", sanitizedUrl) })
            }
        }

        return null
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

        val adultLikelihood = Likelihood.fromResponse(annotation.optString("adult"))
        val racyLikelihood = Likelihood.fromResponse(annotation.optString("racy"))

        val reasons = mutableListOf<String>()
        var confidence = 0.0

        if (adultLikelihood.isAtLeast(minimumLikelihood)) {
            reasons += "Google Vision flagged as adult (${adultLikelihood.readableLabel})"
            confidence = max(confidence, adultLikelihood.confidence)
        }

        if (racyLikelihood.isAtLeast(minimumLikelihood)) {
            reasons += "Google Vision flagged as racy (${racyLikelihood.readableLabel})"
            confidence = max(confidence, racyLikelihood.confidence)
        }

        val flagged = reasons.isNotEmpty()
        val roundedConfidence = if (flagged) round(confidence * 100) / 100.0 else 0.0

        return VisionAssessment(
            flagged = flagged,
            confidence = roundedConfidence,
            reasons = reasons
        )
    }

    private data class VisionAssessment(
        val flagged: Boolean,
        val confidence: Double,
        val reasons: List<String>
    )

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