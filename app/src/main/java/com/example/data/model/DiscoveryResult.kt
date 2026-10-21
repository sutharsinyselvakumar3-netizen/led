package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiscoveryResult(
    @Json(name = "device") val device: String? = "NodeMCU ESP8266",
    @Json(name = "ip") val ip: String,
    @Json(name = "httpPort") val httpPort: Int = 80,
    @Json(name = "udpPort") val udpPort: Int = 4210,
    @Json(name = "ledOn") val ledOn: Boolean? = null
)
