package com.e3hi.geodrop.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class ExplorerAccountStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLastExplorerUid(): String? =
        prefs.getString(KEY_LAST_EXPLORER_UID, null)?.takeIf { it.isNotBlank() }

    fun setLastExplorerUid(uid: String) {
        prefs.edit { putString(KEY_LAST_EXPLORER_UID, uid) }
    }

    companion object {
        private const val PREFS_NAME = "geodrop_explorer_accounts"
        private const val KEY_LAST_EXPLORER_UID = "last_explorer_uid"
    }
}