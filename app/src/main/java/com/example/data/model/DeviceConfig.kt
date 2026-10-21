package com.example.data.model

data class DeviceConfig(
    val ipAddress: String = "",
    val httpPort: Int = 80,
    val autoRefresh: Boolean = true,
    val timeoutMs: Long = 3500L
)
