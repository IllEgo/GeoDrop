package com.e3hi.geodrop.util

import android.content.Context
import android.content.SharedPreferences

class MessagingTokenStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun currentToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun lastSyncedToken(): String? = prefs.getString(KEY_SYNCED_TOKEN, null)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun markSynced(token: String) {
        prefs.edit().putString(KEY_SYNCED_TOKEN, token).apply()
    }

    fun clearSynced() {
        prefs.edit().remove(KEY_SYNCED_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "geodrop_messaging_tokens"
        private const val KEY_TOKEN = "fcm_token"
        private const val KEY_SYNCED_TOKEN = "synced_token"
    }
}