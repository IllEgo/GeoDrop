package com.kitheapp.util

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class PlayInstallReferrerSource(context: Context) {
    private val appContext = context.applicationContext

    suspend fun readOnce(): String? = suspendCancellableCoroutine { continuation ->
        val client = InstallReferrerClient.newBuilder(appContext).build()
        var finished = false

        fun finish(value: String?) {
            if (finished) return
            finished = true
            runCatching { client.endConnection() }
            if (continuation.isActive) continuation.resume(value)
        }

        continuation.invokeOnCancellation {
            finished = true
            runCatching { client.endConnection() }
        }

        runCatching {
            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (responseCode != InstallReferrerClient.InstallReferrerResponse.OK) {
                        finish(null)
                        return
                    }
                    finish(runCatching { client.installReferrer.installReferrer }.getOrNull())
                }

                override fun onInstallReferrerServiceDisconnected() {
                    finish(null)
                }
            })
        }.onFailure { finish(null) }
    }
}
