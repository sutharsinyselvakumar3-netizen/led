package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.Inet4Address
import java.net.NetworkInterface

data class NetworkStatus(
    val isWifiConnected: Boolean = false,
    val isCellularConnected: Boolean = false,
    val localIpAddress: String? = null,
    val networkDescription: String = "Disconnected"
)

class NetworkMonitor(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    val networkStatus: Flow<NetworkStatus> = callbackFlow {
        fun computeStatus(): NetworkStatus {
            val cm = connectivityManager ?: return NetworkStatus()
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)

            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val isEthernet = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true

            val localIp = getDeviceLocalIp()

            val desc = when {
                isWifi -> "Wi-Fi Connected (${localIp ?: "Active"})"
                isCellular -> "Cellular Active (Local IoT requires Wi-Fi/Hotspot)"
                isEthernet -> "Ethernet Connected"
                localIp != null -> "Local Network (${localIp})"
                else -> "No Active Network"
            }

            return NetworkStatus(
                isWifiConnected = isWifi || (localIp != null && !isCellular),
                isCellularConnected = isCellular,
                localIpAddress = localIp,
                networkDescription = desc
            )
        }

        trySend(computeStatus())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(computeStatus())
            }

            override fun onLost(network: Network) {
                trySend(computeStatus())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(computeStatus())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager?.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            try {
                connectivityManager?.registerDefaultNetworkCallback(callback)
            } catch (_: Exception) {}
        }

        awaitClose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }

    private fun getDeviceLocalIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
