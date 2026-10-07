package com.bgradio.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(context: Context) {

    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private fun caps(): NetworkCapabilities? = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }

    fun isOnline(): Boolean = caps()?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    /** True on Wi-Fi or Ethernet — used by the "Wi-Fi only" setting. */
    fun isOnWifi(): Boolean = caps()?.let {
        it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } == true

    /** Emits true whenever a network with internet becomes available. */
    val online: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isOnline())
            }

            override fun onCapabilitiesChanged(network: Network, nc: NetworkCapabilities) {
                trySend(nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) || isOnline())
            }
        }
        trySend(isOnline())
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
            // Some devices limit the number of callbacks; the player still works without it.
        }
        awaitClose { runCatching { cm?.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged()
}
