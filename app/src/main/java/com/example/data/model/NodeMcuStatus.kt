package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NodeMcuStatus(
    @Json(name = "device") val device: String? = "NodeMCU ESP8266",
    @Json(name = "connected") val connected: Boolean? = true,
    @Json(name = "ip") val ip: String? = null,
    @Json(name = "led") val led: String? = null,
    @Json(name = "ledOn") val ledOn: Boolean = false,
    @Json(name = "httpPort") val httpPort: Int = 80,
    @Json(name = "udpPort") val udpPort: Int = 4210,
    @Json(name = "rssi") val rssi: Int? = null,
    @Json(name = "uptime") val uptime: Long? = null,
    @Json(name = "ssid") val ssid: String? = null
)
