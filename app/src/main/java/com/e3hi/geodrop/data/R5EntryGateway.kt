package com.e3hi.geodrop.data

import com.e3hi.geodrop.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.tasks.await

interface R5EntryGateway {
    suspend fun ensureGuestSession(entrySessionId: String)
    suspend fun resolve(request: R5EntryRequest): R5ExperiencePreview
    suspend fun join(request: R5EntryRequest): R5ExperiencePreview
    suspend fun recordAuthCompletion(
        entrySessionId: String,
        upgradePath: String?,
        pendingUnlockResumed: Boolean
    )
    suspend fun recordClientEvent(
        eventName: String,
        entrySessionId: String?,
        experienceCode: String? = null,
        dropId: String? = null,
        installKey: String? = null,
        params: Map<String, Any> = emptyMap()
    )
}

class FirebaseR5EntryGateway(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = Firebase.functions(BuildConfig.FIREBASE_FUNCTIONS_REGION)
) : R5EntryGateway {

    override suspend fun ensureGuestSession(entrySessionId: String) {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
        // Entry must remain useful when optional instrumentation is unavailable.
        runCatching {
            recordAuthCompletion(
                entrySessionId = entrySessionId,
                upgradePath = null,
                pendingUnlockResumed = false
            )
        }
    }

    override suspend fun resolve(request: R5EntryRequest): R5ExperiencePreview =
        callPreview(
            callable = "resolveExperience",
            payload = mapOf(
                "apiVersion" to 1,
                "code" to request.code,
                "entrySessionId" to request.entrySessionId,
                "channel" to request.channel.name
            )
        )

    override suspend fun join(request: R5EntryRequest): R5ExperiencePreview =
        callPreview(
            callable = "joinExperience",
            payload = mapOf(
                "apiVersion" to 1,
                "code" to request.code,
                "entrySessionId" to request.entrySessionId
            )
        )

    override suspend fun recordAuthCompletion(
        entrySessionId: String,
        upgradePath: String?,
        pendingUnlockResumed: Boolean
    ) {
        val payload = mutableMapOf<String, Any>(
            "apiVersion" to 1,
            "entrySessionId" to entrySessionId,
            "pendingUnlockResumed" to pendingUnlockResumed,
            "platform" to "ANDROID",
            "appVersion" to BuildConfig.VERSION_NAME
        )
        upgradePath?.let { payload["upgradePath"] = it }
        call("recordAuthCompletion", payload)
    }

    override suspend fun recordClientEvent(
        eventName: String,
        entrySessionId: String?,
        experienceCode: String?,
        dropId: String?,
        installKey: String?,
        params: Map<String, Any>
    ) {
        val payload = mutableMapOf<String, Any>(
            "apiVersion" to 1,
            "eventId" to UUID.randomUUID().toString().replace("-", ""),
            "eventName" to eventName,
            "occurredAt" to Timestamp(Date()),
            "platform" to "ANDROID",
            "appVersion" to BuildConfig.VERSION_NAME,
            "params" to params
        )
        entrySessionId?.let { payload["entrySessionId"] = it }
        experienceCode?.let { payload["experienceCode"] = it }
        dropId?.let { payload["dropId"] = it }
        installKey?.let { payload["installKey"] = it }
        call("recordClientEvent", payload)
    }

    private suspend fun callPreview(
        callable: String,
        payload: Map<String, Any>
    ): R5ExperiencePreview {
        val root = call(callable, payload)
        val experience = root["experience"] as? Map<*, *>
            ?: throw R5EntryException(R5EntryFailureReason.UNAVAILABLE, retryable = true)
        return parsePreview(experience)
    }

    private suspend fun call(callable: String, payload: Map<String, Any>): Map<*, *> = try {
        functions.getHttpsCallable(callable).call(payload).await().data as? Map<*, *>
            ?: throw R5EntryException(R5EntryFailureReason.UNAVAILABLE, retryable = true)
    } catch (error: FirebaseFunctionsException) {
        throw mapFailure(error)
    } catch (error: R5EntryException) {
        throw error
    } catch (error: Exception) {
        throw R5EntryException(
            reason = R5EntryFailureReason.OFFLINE,
            retryable = true,
            cause = error
        )
    }

    private fun parsePreview(data: Map<*, *>): R5ExperiencePreview = R5ExperiencePreview(
        code = data["code"]?.toString().orEmpty(),
        name = data["name"]?.toString()?.takeIf { it.isNotBlank() }
            ?: data["code"]?.toString().orEmpty(),
        description = data["description"]?.toString()?.takeIf { it.isNotBlank() },
        hostLabel = data["hostLabel"]?.toString()?.takeIf { it.isNotBlank() } ?: "Host",
        startsAt = data["startsAt"]?.toString()?.takeIf { it.isNotBlank() },
        endsAt = data["endsAt"]?.toString()?.takeIf { it.isNotBlank() },
        timeZone = data["timeZone"]?.toString().orEmpty(),
        availability = runCatching {
            R5ExperienceAvailability.valueOf(data["availability"]?.toString().orEmpty())
        }.getOrDefault(R5ExperienceAvailability.CANCELLED),
        availableDropCount = (data["availableDropCount"] as? Number)?.toInt() ?: 0,
        membership = runCatching {
            R5ExperienceMembership.valueOf(data["membership"]?.toString().orEmpty())
        }.getOrDefault(R5ExperienceMembership.NONE)
    )

    private fun mapFailure(error: FirebaseFunctionsException): R5EntryException {
        val details = error.details as? Map<*, *>
        val reason = when (details?.get("reason")?.toString()) {
            "INVALID_CODE" -> R5EntryFailureReason.INVALID_CODE
            "EXPERIENCE_NOT_FOUND" -> R5EntryFailureReason.EXPERIENCE_NOT_FOUND
            "EXPERIENCE_CANCELLED" -> R5EntryFailureReason.EXPERIENCE_CANCELLED
            "EXPERIENCE_ENDED" -> R5EntryFailureReason.EXPERIENCE_ENDED
            "RATE_LIMITED" -> R5EntryFailureReason.RATE_LIMITED
            else -> R5EntryFailureReason.UNKNOWN
        }
        return R5EntryException(
            reason = reason,
            retryable = details?.get("retryable") as? Boolean ?: false,
            cause = error
        )
    }
}
