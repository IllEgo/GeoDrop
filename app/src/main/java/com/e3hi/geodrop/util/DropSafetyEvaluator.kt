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
import kotlin.math.round

/**
 * Contract for pluggable NSFW evaluators. Implementations can call a
 * heuristic classifier, on-device ML model, or a cloud inference API.
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
 * Thin wrapper around the existing heuristic classifier so it can be used via
 * the evaluator interface.
 */
object HeuristicDropSafetyEvaluator : DropSafetyEvaluator {
    override suspend fun assess(
        text: String?,
        contentType: DropContentType,
        mediaMimeType: String?,
        mediaData: String?,
        mediaUrl: String?
    ): DropSafetyAssessment = DropSafetyClassifier.evaluate(
        text = text,
        contentType = contentType,
        mediaMimeType = mediaMimeType,
        mediaData = mediaData,
        mediaUrl = mediaUrl
    )
}

/**
 * Implementation that sends the drop payload to a remote ML model. On any
 * failure the evaluator falls back to the heuristic implementation so the app
 * never blocks on network errors.
 */
class AdvancedDropSafetyEvaluator(
    private val endpointUrl: String,
    private val apiKey: String? = null,
    private val timeoutMs: Int = 10_000,
    private val fallback: DropSafetyEvaluator = HeuristicDropSafetyEvaluator
) : DropSafetyEvaluator {

    override suspend fun assess(
        text: String?,
        contentType: DropContentType,
        mediaMimeType: String?,
        mediaData: String?,
        mediaUrl: String?
    ): DropSafetyAssessment {
        return try {
            val advanced = withContext(Dispatchers.IO) {
                requestAssessment(
                    text = text,
                    contentType = contentType,
                    mediaMimeType = mediaMimeType,
                    mediaData = mediaData,
                    mediaUrl = mediaUrl
                )
            }
            if (advanced.isNsfw && advanced.reasons.isEmpty()) {
                val heuristic = fallback.assess(text, contentType, mediaMimeType, mediaData, mediaUrl)
                advanced.copy(reasons = heuristic.reasons)
            } else {
                advanced
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Advanced NSFW check failed; falling back to heuristic", t)
            fallback.assess(text, contentType, mediaMimeType, mediaData, mediaUrl)
        }
    }

    private fun requestAssessment(
        text: String?,
        contentType: DropContentType,
        mediaMimeType: String?,
        mediaData: String?,
        mediaUrl: String?
    ): DropSafetyAssessment {
        val connection = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            apiKey?.let { setRequestProperty("Authorization", "Bearer $it") }
        }

        val payload = JSONObject().apply {
            put("contentType", contentType.name)
            text?.let { put("text", it) }
            mediaMimeType?.let { put("mediaMimeType", it) }
            mediaData?.let { put("mediaData", it) }
            mediaUrl?.let { put("mediaUrl", it) }
        }

        return try {
            BufferedWriter(OutputStreamWriter(connection.outputStream, Charsets.UTF_8)).use { writer ->
                writer.write(payload.toString())
            }

            val responseCode = connection.responseCode
            val responseBody = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299 || responseBody.isBlank()) {
                throw IllegalStateException("Unexpected response $responseCode: $responseBody")
            }

            val json = JSONObject(responseBody)
            val isNsfw = json.optBoolean("nsfw", false)
            val confidence = json.optDouble("confidence", if (isNsfw) 0.5 else 0.0)
                .coerceIn(0.0, 0.99)
            val reasons = json.optJSONArray("reasons")?.let { it.toStringList() } ?: emptyList()

            val roundedConfidence = if (isNsfw) round(confidence * 100) / 100.0 else 0.0
            val reportedReasons = if (isNsfw) reasons.filter { it.isNotBlank() }.distinct() else emptyList()

            DropSafetyAssessment(
                isNsfw = isNsfw,
                confidence = roundedConfidence,
                reasons = reportedReasons
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONArray.toStringList(): List<String> {
        val items = mutableListOf<String>()
        for (i in 0 until length()) {
            val value = optString(i)
            if (!value.isNullOrBlank()) {
                items += value
            }
        }
        return items
    }

    private companion object {
        private const val TAG = "AdvancedDropSafetyEval"
    }
}