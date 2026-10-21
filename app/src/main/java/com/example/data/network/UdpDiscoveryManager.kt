package com.example.data.network

import android.content.Context
import android.net.wifi.WifiManager
import com.example.data.model.DiscoveryResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException

class UdpDiscoveryManager(
    private val context: Context,
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
) {
    private val discoveryAdapter = moshi.adapter(DiscoveryResult::class.java)

    companion object {
        const val DISCOVERY_PORT = 4210
        const val DISCOVERY_MESSAGE = "DISCOVER_NODEMCU"
        private const val PACKET_BUFFER_SIZE = 1024
        private const val RETRY_COUNT = 3
        private const val SOCKET_TIMEOUT_MS = 1500
    }

    /**
     * Broadcasts DISCOVER_NODEMCU on port 4210 and awaits JSON reply from NodeMCU
     */
    suspend fun discoverDevice(): Result<DiscoveryResult> = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock: WifiManager.MulticastLock? = try {
            wifiManager?.createMulticastLock("NodeMcuUdpDiscovery")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {
            null
        }

        var socket: DatagramSocket? = null
        try {
            // Bind to an ephemeral port on all interfaces
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                soTimeout = SOCKET_TIMEOUT_MS
                bind(InetSocketAddress(0))
            }

            val broadcastTargets = getBroadcastAddresses()
            val requestBytes = DISCOVERY_MESSAGE.toByteArray(Charsets.UTF_8)

            // Retry loop
            for (attempt in 1..RETRY_COUNT) {
                // Send broadcast packet to each target
                for (target in broadcastTargets) {
                    try {
                        val sendPacket = DatagramPacket(
                            requestBytes,
                            requestBytes.size,
                            target,
                            DISCOVERY_PORT
                        )
                        socket.send(sendPacket)
                    } catch (_: Exception) {
                        // Continue to next target
                    }
                }

                // Listen for reply until socket timeout
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < SOCKET_TIMEOUT_MS) {
                    val buffer = ByteArray(PACKET_BUFFER_SIZE)
                    val receivePacket = DatagramPacket(buffer, buffer.size)

                    try {
                        socket.receive(receivePacket)
                        val responseText = String(
                            receivePacket.data,
                            receivePacket.offset,
                            receivePacket.length,
                            Charsets.UTF_8
                        ).trim()

                        // Check if response contains JSON from NodeMCU
                        if (responseText.contains("NodeMCU") || responseText.contains("httpPort") || responseText.startsWith("{")) {
                            val senderHostAddress = receivePacket.address.hostAddress ?: ""
                            val parsedResult = try {
                                discoveryAdapter.fromJson(responseText)
                            } catch (_: Exception) {
                                null
                            }

                            val resolvedIp = if (!parsedResult?.ip.isNullOrBlank()) {
                                parsedResult!!.ip
                            } else {
                                senderHostAddress
                            }

                            if (resolvedIp.isNotBlank()) {
                                return@withContext Result.success(
                                    DiscoveryResult(
                                        device = parsedResult?.device ?: "NodeMCU ESP8266",
                                        ip = resolvedIp,
                                        httpPort = parsedResult?.httpPort ?: 80,
                                        udpPort = parsedResult?.udpPort ?: DISCOVERY_PORT,
                                        ledOn = parsedResult?.ledOn
                                    )
                                )
                            }
                        }
                    } catch (_: SocketTimeoutException) {
                        // Attempt timeout, continue to next attempt
                        break
                    } catch (e: Exception) {
                        // Continue trying until loop ends
                    }
                }
            }

            Result.failure(
                NoSuchElementException(
                    "No NodeMCU board responded to UDP broadcast on port $DISCOVERY_PORT. " +
                            "Ensure the board is powered on and connected to the same Wi-Fi router or hotspot, " +
                            "or enter its IP address manually below."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            socket?.close()
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Gathers global broadcast address and subnet-specific broadcast addresses
     */
    private fun getBroadcastAddresses(): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        try {
            // Standard universal broadcast
            addresses.add(InetAddress.getByName("255.255.255.255"))
        } catch (_: Exception) {}

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null && !addresses.contains(broadcast)) {
                        addresses.add(broadcast)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore interface enumeration errors and rely on 255.255.255.255
        }

        return addresses
    }
}
