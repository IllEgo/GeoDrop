package com.e3hi.geodrop.util

import android.content.Context
import com.e3hi.geodrop.data.R5EntryChannel
import com.e3hi.geodrop.data.R5EntryRequest
import com.e3hi.geodrop.data.R5PendingUnlock

class R5EntryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "geodrop_r5_entry",
        Context.MODE_PRIVATE
    )

    fun pendingEntry(nowMillis: Long = System.currentTimeMillis()): R5EntryRequest? {
        val createdAt = prefs.getLong(KEY_PENDING_CREATED_AT, 0L)
        if (createdAt <= 0L || nowMillis - createdAt > ENTRY_TTL_MILLIS) {
            clearPendingEntry()
            return null
        }
        val code = R5EntryParser.normalizeCode(prefs.getString(KEY_PENDING_CODE, null))
            ?: return null
        val session = prefs.getString(KEY_PENDING_SESSION, null)
            ?.takeIf { it.length in 16..128 }
            ?: return null
        val channel = runCatching {
            R5EntryChannel.valueOf(prefs.getString(KEY_PENDING_CHANNEL, null).orEmpty())
        }.getOrDefault(R5EntryChannel.LINK)
        return R5EntryRequest(code, session, channel)
    }

    fun savePendingEntry(request: R5EntryRequest, nowMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString(KEY_PENDING_CODE, request.code)
            .putString(KEY_PENDING_SESSION, request.entrySessionId)
            .putString(KEY_PENDING_CHANNEL, request.channel.name)
            .putLong(KEY_PENDING_CREATED_AT, nowMillis)
            .apply()
    }

    fun completeEntry(request: R5EntryRequest) {
        prefs.edit()
            .putString(KEY_ACTIVE_SESSION, request.entrySessionId)
            .putString(KEY_ACTIVE_EXPERIENCE, request.code)
            .remove(KEY_PENDING_CODE)
            .remove(KEY_PENDING_SESSION)
            .remove(KEY_PENDING_CHANNEL)
            .remove(KEY_PENDING_CREATED_AT)
            .apply()
    }

    fun clearPendingEntry() {
        prefs.edit()
            .remove(KEY_PENDING_CODE)
            .remove(KEY_PENDING_SESSION)
            .remove(KEY_PENDING_CHANNEL)
            .remove(KEY_PENDING_CREATED_AT)
            .apply()
    }

    fun activeEntrySessionId(): String? = prefs.getString(KEY_ACTIVE_SESSION, null)

    fun activeExperienceCode(): String? = prefs.getString(KEY_ACTIVE_EXPERIENCE, null)

    fun installKey(): String {
        prefs.getString(KEY_INSTALL_KEY, null)?.let { return it }
        return R5EntryParser.newEntrySessionId().also { generated ->
            prefs.edit().putString(KEY_INSTALL_KEY, generated).apply()
        }
    }

    fun shouldReadInstallReferrer(): Boolean = !prefs.getBoolean(KEY_REFERRER_READ, false)

    fun markInstallReferrerRead() {
        prefs.edit().putBoolean(KEY_REFERRER_READ, true).apply()
    }

    fun savePendingUnlock(target: R5PendingUnlock) {
        prefs.edit()
            .putString(KEY_PENDING_UNLOCK_EXPERIENCE, target.experienceCode)
            .putString(KEY_PENDING_UNLOCK_DROP, target.dropId)
            .apply()
    }

    fun pendingUnlock(): R5PendingUnlock? {
        val dropId = prefs.getString(KEY_PENDING_UNLOCK_DROP, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return R5PendingUnlock(
            experienceCode = prefs.getString(KEY_PENDING_UNLOCK_EXPERIENCE, null),
            dropId = dropId
        )
    }

    fun clearPendingUnlock() {
        prefs.edit()
            .remove(KEY_PENDING_UNLOCK_EXPERIENCE)
            .remove(KEY_PENDING_UNLOCK_DROP)
            .apply()
    }

    fun notificationPrimerSeen(experienceCode: String): Boolean =
        prefs.getStringSet(KEY_NOTIFICATION_PRIMERS, emptySet())
            ?.contains(experienceCode) == true

    fun markNotificationPrimerSeen(experienceCode: String) {
        val current = prefs.getStringSet(KEY_NOTIFICATION_PRIMERS, emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()
        current += experienceCode
        prefs.edit().putStringSet(KEY_NOTIFICATION_PRIMERS, current).apply()
    }

    companion object {
        const val ENTRY_TTL_MILLIS = 24L * 60L * 60L * 1000L
        private const val KEY_PENDING_CODE = "pending_code"
        private const val KEY_PENDING_SESSION = "pending_session"
        private const val KEY_PENDING_CHANNEL = "pending_channel"
        private const val KEY_PENDING_CREATED_AT = "pending_created_at"
        private const val KEY_ACTIVE_SESSION = "active_session"
        private const val KEY_ACTIVE_EXPERIENCE = "active_experience"
        private const val KEY_INSTALL_KEY = "install_key"
        private const val KEY_REFERRER_READ = "referrer_read"
        private const val KEY_PENDING_UNLOCK_EXPERIENCE = "pending_unlock_experience"
        private const val KEY_PENDING_UNLOCK_DROP = "pending_unlock_drop"
        private const val KEY_NOTIFICATION_PRIMERS = "notification_primers"
    }
}
