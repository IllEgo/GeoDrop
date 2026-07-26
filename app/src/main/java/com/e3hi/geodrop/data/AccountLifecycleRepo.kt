package com.e3hi.geodrop.data

import com.e3hi.geodrop.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class AccountLifecycleRepo(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(
        BuildConfig.FIREBASE_FUNCTIONS_REGION
    )
) {
    suspend fun requestExport(): AccountExportResult {
        refreshIdToken()
        val result = functions.getHttpsCallable("requestAccountExport")
            .call(mapOf("policyVersion" to POLICY_VERSION))
            .await()
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
        val result = functions.getHttpsCallable("deleteAccount")
            .call(
                mapOf(
                    "policyVersion" to POLICY_VERSION,
                    "confirmation" to confirmation
                )
            )
            .await()
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

    private fun Map<*, *>.string(key: String): String =
        this[key]?.toString()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Account lifecycle response is missing $key")

    private fun Map<*, *>.long(key: String): Long =
        (this[key] as? Number)?.toLong() ?: 0L

    companion object {
        const val POLICY_VERSION = "pilot-2026-07-21-draft"
    }
}

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

