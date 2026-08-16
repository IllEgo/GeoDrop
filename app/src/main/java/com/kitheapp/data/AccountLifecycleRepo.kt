package com.kitheapp.data

import com.kitheapp.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

class AccountLifecycleRepo(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(
        BuildConfig.FIREBASE_FUNCTIONS_REGION
    )
) {
    suspend fun requestExport(): AccountExportResult {
        refreshIdToken()
        val result = lifecycleCall {
            functions.getHttpsCallable("requestAccountExport")
                .call(mapOf("policyVersion" to POLICY_VERSION))
                .await()
        }
        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Account export returned an invalid response")
        return AccountExportResult(
            requestId = data.string("requestId"),
            downloadUrl = data.string("downloadUrl"),
            expiresAt = data.string("expiresAt"),
            policyVersion = data.string("policyVersion")
        )
    }

    suspend fun deleteAccount(confirmation: String): AccountDeletionReceipt {
        refreshIdToken()
        val result = lifecycleCall {
            functions.getHttpsCallable("deleteAccount")
                .call(
                    mapOf(
                        "policyVersion" to POLICY_VERSION,
                        "confirmation" to confirmation
                    )
                )
                .await()
        }
        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Account deletion returned an invalid response")
        val counts = data["counts"] as? Map<*, *> ?: emptyMap<Any, Any>()
        return AccountDeletionReceipt(
            receiptId = data.string("receiptId"),
            status = data.string("status"),
            policyVersion = data.string("policyVersion"),
            completedAt = data.string("completedAt"),
            deletedDrops = counts.long("drops"),
            deletedMediaObjects = counts.long("mediaObjects")
        )
    }

    private suspend fun refreshIdToken() {
        val user = auth.currentUser ?: throw IllegalStateException("Sign in to manage account data")
        user.getIdToken(true).await()
    }

    private suspend fun <T> lifecycleCall(block: suspend () -> T): T = try {
        block()
    } catch (error: FirebaseFunctionsException) {
        val reason = (error.details as? Map<*, *>)?.get("reason")?.toString()
        throw AccountLifecycleException(
            userMessage = AccountLifecyclePolicy.failureMessage(reason),
            retryable = error.code in setOf(
                FirebaseFunctionsException.Code.UNAVAILABLE,
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
                FirebaseFunctionsException.Code.ABORTED
            ),
            cause = error
        )
    }

    private fun Map<*, *>.string(key: String): String =
        this[key]?.toString()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Account lifecycle response is missing $key")

    private fun Map<*, *>.long(key: String): Long =
        (this[key] as? Number)?.toLong() ?: 0L

    companion object {
        // Must equal ACCOUNT_LIFECYCLE_POLICY_VERSION in functions/src/accountLifecycle.ts.
        // This is the deletion/retention policy, not the legal bundle version.
        const val POLICY_VERSION = "pilot-redesign-r2-2026-08-09-draft"
    }
}

object AccountLifecyclePolicy {
    fun failureMessage(reason: String?): String = when (reason) {
        "REAUTHENTICATION_REQUIRED" ->
            "Your sign-in is too old for this action. Verify your identity and try again."
        "POLICY_VERSION_MISMATCH" ->
            "The account policy changed. Close this screen, review the latest policy, and retry."
        "EXPLICIT_CONFIRMATION_REQUIRED" ->
            "Type DELETE exactly before deleting your account."
        "SERVER_CONFIGURATION_REQUIRED" ->
            "Account data tools aren't available right now. Try again later."
        else -> "Kithe couldn't complete this account action. Try again."
    }
}

class AccountLifecycleException(
    val userMessage: String,
    val retryable: Boolean,
    cause: Throwable? = null
) : Exception(userMessage, cause)

data class AccountExportResult(
    val requestId: String,
    val downloadUrl: String,
    val expiresAt: String,
    val policyVersion: String
)

data class AccountDeletionReceipt(
    val receiptId: String,
    val status: String,
    val policyVersion: String,
    val completedAt: String,
    val deletedDrops: Long,
    val deletedMediaObjects: Long
)

