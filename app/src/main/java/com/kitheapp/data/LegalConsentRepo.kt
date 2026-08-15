package com.kitheapp.data

import com.kitheapp.BuildConfig
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.net.URI
import java.util.Locale

interface LegalConsentGateway {
    suspend fun fetchManifest(): LegalPolicyManifest
    suspend fun recordAcceptance(policyVersion: String)
}

class LegalConsentRepo(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(
        BuildConfig.FIREBASE_FUNCTIONS_REGION
    )
) : LegalConsentGateway {
    override suspend fun fetchManifest(): LegalPolicyManifest {
        val result = functions.getHttpsCallable("getLegalPolicyManifest")
            .call()
            .await()
        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException("Legal policy manifest is unavailable")
        return LegalPolicyManifest(
            version = data.requiredString("version"),
            terms = data.requiredHttpsUrl("terms"),
            privacy = data.requiredHttpsUrl("privacy"),
            communityGuidelines = data.requiredHttpsUrl("communityGuidelines"),
            promotionTerms = data.requiredHttpsUrl("promotionTerms"),
            retention = data.requiredHttpsUrl("retention"),
            processors = data.requiredHttpsUrl("processors"),
            minors = data.requiredHttpsUrl("minors"),
            support = data.requiredHttpsUrl("support")
        )
    }

    override suspend fun recordAcceptance(policyVersion: String) {
        functions.getHttpsCallable("recordLegalAcceptance")
            .call(
                mapOf(
                    "policyVersion" to policyVersion,
                    "platform" to "android",
                    "appVersion" to BuildConfig.VERSION_NAME,
                    "locale" to Locale.getDefault().toLanguageTag()
                )
            )
            .await()
    }

    private fun Map<*, *>.requiredString(key: String): String =
        this[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Legal policy manifest is missing $key")

    private fun Map<*, *>.requiredHttpsUrl(key: String): String {
        val value = requiredString(key)
        val uri = runCatching { URI(value) }.getOrNull()
        if (uri?.scheme?.lowercase(Locale.US) != "https" || uri.host.isNullOrBlank()) {
            throw IllegalStateException("Legal policy manifest has an invalid $key URL")
        }
        return value
    }
}

/** Local-only policy fixture selected by MainActivity only for a debug APK. */
object DebugDemoLegalConsentGateway : LegalConsentGateway {
    private const val DEBUG_POLICY_ROOT = "https://debug-device-demo.invalid"

    override suspend fun fetchManifest() = LegalPolicyManifest(
        version = "DEBUG-DEMO-NOT-PRODUCTION",
        terms = "$DEBUG_POLICY_ROOT/terms",
        privacy = "$DEBUG_POLICY_ROOT/privacy",
        communityGuidelines = "$DEBUG_POLICY_ROOT/community-guidelines",
        promotionTerms = "$DEBUG_POLICY_ROOT/promotion-terms",
        retention = "$DEBUG_POLICY_ROOT/retention",
        processors = "$DEBUG_POLICY_ROOT/processors",
        minors = "$DEBUG_POLICY_ROOT/minors",
        support = "$DEBUG_POLICY_ROOT/support"
    )

    override suspend fun recordAcceptance(policyVersion: String) = Unit
}

data class LegalPolicyManifest(
    val version: String,
    val terms: String,
    val privacy: String,
    val communityGuidelines: String,
    val promotionTerms: String,
    val retention: String,
    val processors: String,
    val minors: String,
    val support: String
)
