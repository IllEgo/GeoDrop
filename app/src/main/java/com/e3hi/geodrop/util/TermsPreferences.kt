package com.e3hi.geodrop.util

import android.content.Context
import android.content.SharedPreferences

class TermsPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasAcceptedTerms(): Boolean = prefs.getBoolean(KEY_ACCEPTED, false)

    fun hasViewedFirstRunOnboarding(): Boolean = prefs.getBoolean(KEY_ONBOARDING_VIEWED, false)

    fun recordAcceptance(timestampMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putBoolean(KEY_ACCEPTED, true)
            .putLong(KEY_ACCEPTED_AT, timestampMillis)
            .apply()
    }

    fun recordOnboardingViewed() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_VIEWED, true)
            .apply()
    }

    fun clearAcceptance() {
        prefs.edit()
            .remove(KEY_ACCEPTED)
            .remove(KEY_ACCEPTED_AT)
            .remove(KEY_ONBOARDING_VIEWED)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "geodrop_terms_preferences"
        private const val KEY_ACCEPTED = "terms_privacy_accepted"
        private const val KEY_ACCEPTED_AT = "terms_privacy_accepted_at"
        private const val KEY_ONBOARDING_VIEWED = "first_run_onboarding_viewed"
    }
}