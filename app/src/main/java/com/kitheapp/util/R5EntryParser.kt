package com.kitheapp.util

import android.net.Uri
import com.kitheapp.data.R5EntryChannel
import com.kitheapp.data.R5EntryRequest
import java.security.SecureRandom
import java.util.Locale

object R5EntryParser {
    private val codePattern = Regex("^[A-Z0-9]{4,32}$")
    private val sessionPattern = Regex("^[A-Za-z0-9_-]{16,128}$")

    /** Spaces and visual dashes are presentation only; the backend document id is compact. */
    fun normalizeCode(raw: String?): String? {
        val normalized = raw
            ?.trim()
            ?.uppercase(Locale.US)
            ?.filterNot { it.isWhitespace() || it == '-' || it == '–' || it == '—' }
            .orEmpty()
        return normalized.takeIf(codePattern::matches)
    }

    fun displayCode(code: String): String {
        val normalized = normalizeCode(code) ?: return code
        return if (normalized.length == 8) {
            "${normalized.take(4)}-${normalized.drop(4)}"
        } else {
            normalized
        }
    }

    fun fromAppLink(uri: Uri?, configuredHost: String): R5EntryRequest? {
        if (uri == null || uri.scheme?.lowercase(Locale.US) != "https") return null
        if (!uri.host.equals(configuredHost, ignoreCase = true)) return null
        val segments = uri.pathSegments
        if (segments.size != 2 || segments[0] != "e") return null
        val code = normalizeCode(segments[1]) ?: return null
        return R5EntryRequest(
            code = code,
            entrySessionId = validSession(uri.getQueryParameter("entry_session_id"))
                ?: newEntrySessionId(),
            channel = parseChannel(uri.getQueryParameter("channel"), R5EntryChannel.LINK)
        )
    }

    fun fromInstallReferrer(rawReferrer: String?): R5EntryRequest? {
        val raw = rawReferrer?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val queryUri = runCatching { Uri.parse("https://install.invalid/?$raw") }.getOrNull()
            ?: return null
        val code = normalizeCode(queryUri.getQueryParameter("code")) ?: return null
        return R5EntryRequest(
            code = code,
            entrySessionId = validSession(queryUri.getQueryParameter("entry_session_id"))
                ?: newEntrySessionId(),
            channel = parseChannel(
                queryUri.getQueryParameter("channel"),
                R5EntryChannel.LINK
            )
        )
    }

    fun manual(rawCode: String, entrySessionId: String = newEntrySessionId()): R5EntryRequest? =
        normalizeCode(rawCode)?.let { code ->
            R5EntryRequest(
                code = code,
                entrySessionId = validSession(entrySessionId) ?: newEntrySessionId(),
                channel = R5EntryChannel.MANUAL
            )
        }

    fun newEntrySessionId(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun validSession(raw: String?): String? = raw?.takeIf(sessionPattern::matches)

    private fun parseChannel(raw: String?, fallback: R5EntryChannel): R5EntryChannel =
        R5EntryChannel.entries.firstOrNull { channel ->
            channel.name.equals(raw, ignoreCase = true)
        } ?: fallback
}
