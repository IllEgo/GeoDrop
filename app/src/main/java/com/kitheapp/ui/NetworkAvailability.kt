package com.kitheapp.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberNetworkAvailable(): Boolean {
    val context = LocalContext.current
    val connectivityManager = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    var networkAvailable by remember(connectivityManager) {
        mutableStateOf(connectivityManager.hasValidatedNetwork())
    }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkAvailable = connectivityManager.hasValidatedNetwork()
            }

            override fun onLost(network: Network) {
                networkAvailable = false
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                networkAvailable = networkCapabilities.isValidatedInternet()
            }

            override fun onUnavailable() {
                networkAvailable = false
            }
        }

        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
            .onFailure { networkAvailable = connectivityManager.hasValidatedNetwork() }

        onDispose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    return networkAvailable
}

private fun ConnectivityManager.hasValidatedNetwork(): Boolean =
    activeNetwork
        ?.let(::getNetworkCapabilities)
        ?.isValidatedInternet() == true

private fun NetworkCapabilities.isValidatedInternet(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
