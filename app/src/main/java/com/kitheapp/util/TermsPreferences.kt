package com.kitheapp.util

import android.content.Context
import android.content.SharedPreferences

class TermsPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasAcceptedTerms(policyVersion: String): Boolean =
        prefs.getString(KEY_ACCEPTED_VERSION, null) == policyVersion

    fun acceptedPolicyVersion(): String? = prefs.getString(KEY_ACCEPTED_VERSION, null)

    fun hasViewedFirstRunOnboarding(): Boolean = prefs.getBoolean(KEY_ONBOARDING_VIEWED, false)

    fun recordAcceptance(
        policyVersion: String,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putString(KEY_ACCEPTED_VERSION, policyVersion)
            .putLong(KEY_ACCEPTED_AT, timestampMillis)
            .apply()
    }

    fun hasRecordedServerAcceptance(userId: String, policyVersion: String): Boolean =
        prefs.getString(serverAcceptanceKey(userId), null) == policyVersion

    fun recordServerAcceptance(userId: String, policyVersion: String) {
        prefs.edit()
            .putString(serverAcceptanceKey(userId), policyVersion)
            .apply()
    }

    fun recordOnboardingViewed() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_VIEWED, true)
            .apply()
    }

    fun clearAcceptance() {
        val editor = prefs.edit()
            .remove(KEY_LEGACY_ACCEPTED)
            .remove(KEY_ACCEPTED_VERSION)
            .remove(KEY_ACCEPTED_AT)
            .remove(KEY_ONBOARDING_VIEWED)
        prefs.all.keys
            .filter { it.startsWith(KEY_SERVER_ACCEPTED_VERSION_PREFIX) }
            .forEach(editor::remove)
        editor.apply()
    }

    private fun serverAcceptanceKey(userId: String): String =
        KEY_SERVER_ACCEPTED_VERSION_PREFIX + userId

    companion object {
        private const val PREFS_NAME = "geodrop_terms_preferences"
        private const val KEY_ACCEPTED_VERSION = "terms_privacy_accepted_version"
        private const val KEY_LEGACY_ACCEPTED = "terms_privacy_accepted"
        private const val KEY_ACCEPTED_AT = "terms_privacy_accepted_at"
        private const val KEY_ONBOARDING_VIEWED = "first_run_onboarding_viewed"
        private const val KEY_SERVER_ACCEPTED_VERSION_PREFIX =
            "terms_privacy_server_accepted_version_"
    }
}
