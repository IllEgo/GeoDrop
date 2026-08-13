package com.e3hi.geodrop.util

import android.net.Uri
import com.e3hi.geodrop.data.R5EntryChannel
import com.e3hi.geodrop.data.R5EntryRequest

/** Canonical payloads used by host share actions, QR images, and Play recovery. */
object R5EntryLinks {
    fun appLink(host: String, request: R5EntryRequest): Uri {
        require(validHost(host)) { "An owned HTTPS host is required." }
        return Uri.Builder()
            .scheme("https")
            .authority(host.lowercase())
            .appendPath("e")
            .appendPath(request.code)
            .appendQueryParameter("entry_session_id", request.entrySessionId)
            .appendQueryParameter("channel", request.channel.name)
            .build()
    }

    fun qrPayload(host: String, code: String): Uri = appLink(
        host = host,
        request = R5EntryRequest(
            code = requireNotNull(R5EntryParser.normalizeCode(code)) {
                "A valid Experience code is required."
            },
            entrySessionId = R5EntryParser.newEntrySessionId(),
            channel = R5EntryChannel.QR
        )
    )

    fun playListingUrl(
        packageName: String,
        request: R5EntryRequest
    ): Uri {
        val referrer = Uri.Builder()
            .appendQueryParameter("code", request.code)
            .appendQueryParameter("entry_session_id", request.entrySessionId)
            .appendQueryParameter("channel", request.channel.name)
            .build()
            .encodedQuery
            .orEmpty()
        return Uri.Builder()
            .scheme("https")
            .authority("play.google.com")
            .appendPath("store")
            .appendPath("apps")
            .appendPath("details")
            .appendQueryParameter("id", packageName)
            .appendQueryParameter("referrer", referrer)
            .build()
    }

    private fun validHost(host: String): Boolean =
        host.matches(Regex("^[a-zA-Z0-9.-]+$")) &&
            !host.contains("..") &&
            !host.equals("geodrop.app", ignoreCase = true)
}
