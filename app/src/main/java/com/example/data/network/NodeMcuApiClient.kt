package com.example.data.network

import com.example.data.model.NodeMcuStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class NodeMcuApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3500, TimeUnit.MILLISECONDS)
        .readTimeout(3500, TimeUnit.MILLISECONDS)
        .writeTimeout(3500, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build(),
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
) {
    private val statusAdapter = moshi.adapter(NodeMcuStatus::class.java)

    /**
     * Builds standard base URL for the NodeMCU board
     */
    fun buildUrl(ip: String, port: Int, path: String): String {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val formattedPath = if (path.startsWith("/")) path else "/$path"
        return "http://$cleanIp:$port$formattedPath"
    }

    /**
     * Fetch status from GET /status
     */
    suspend fun getStatus(ip: String, port: Int): Result<NodeMcuStatus> = withContext(Dispatchers.IO) {
        executeRequest(ip, port, "/status")
    }

    /**
     * Fetch device info from GET /
     */
    suspend fun getInfo(ip: String, port: Int): Result<NodeMcuStatus> = withContext(Dispatchers.IO) {
        executeRequest(ip, port, "/")
    }

    /**
     * Controls LED: GET /led/on or GET /led/off
     * Expects device to reply with current updated NodeMcuStatus JSON
     */
    suspend fun setLed(ip: String, port: Int, turnOn: Boolean): Result<NodeMcuStatus> = withContext(Dispatchers.IO) {
        val path = if (turnOn) "/led/on" else "/led/off"
        executeRequest(ip, port, path)
    }

    /**
     * Tests connection and computes latency in milliseconds
     */
    suspend fun testConnection(ip: String, port: Int): Result<Pair<NodeMcuStatus, Long>> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val result = executeRequest(ip, port, "/status")
        val elapsed = System.currentTimeMillis() - startTime
        result.map { it to elapsed }
    }

    private fun executeRequest(ip: String, port: Int, path: String): Result<NodeMcuStatus> {
        val cleanIp = ip.trim()
        if (cleanIp.isBlank()) {
            return Result.failure(IllegalArgumentException("IP address is empty. Configure IP in Settings."))
        }

        val urlString = buildUrl(cleanIp, port, path)

        return try {
            val request = Request.Builder()
                .url(urlString)
                .header("Accept", "application/json")
                .header("User-Agent", "NodeMcuLedController-Android")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(
                        IOException("HTTP ${response.code}: ${response.message.ifBlank { "Request failed" }}")
                    )
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    return Result.failure(IOException("Empty response received from board"))
                }

                try {
                    val status = statusAdapter.fromJson(bodyString)
                        ?: return Result.failure(IOException("Failed to parse device JSON response"))
                    Result.success(status)
                } catch (e: Exception) {
                    Result.failure(IOException("Malformed JSON received: ${e.localizedMessage ?: "Parse error"}"))
                }
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(IOException("Connection timed out. Verify NodeMCU is powered and on the same Wi-Fi."))
        } catch (e: IOException) {
            Result.failure(IOException(e.localizedMessage ?: "Network error connecting to NodeMCU"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
