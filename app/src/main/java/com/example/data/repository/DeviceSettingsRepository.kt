package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DeviceConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceSettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nodemcu_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<DeviceConfig> = _config.asStateFlow()

    private fun loadConfig(): DeviceConfig {
        val ip = prefs.getString(KEY_IP, "") ?: ""
        val port = prefs.getInt(KEY_PORT, 80)
        val autoRefresh = prefs.getBoolean(KEY_AUTO_REFRESH, true)
        val timeoutMs = prefs.getLong(KEY_TIMEOUT, 3500L)
        return DeviceConfig(
            ipAddress = ip,
            httpPort = port,
            autoRefresh = autoRefresh,
            timeoutMs = timeoutMs
        )
    }

    fun saveConfig(config: DeviceConfig) {
        prefs.edit()
            .putString(KEY_IP, config.ipAddress.trim())
            .putInt(KEY_PORT, config.httpPort)
            .putBoolean(KEY_AUTO_REFRESH, config.autoRefresh)
            .putLong(KEY_TIMEOUT, config.timeoutMs)
            .apply()
        _config.value = config.copy(ipAddress = config.ipAddress.trim())
    }

    fun clearConfig() {
        prefs.edit()
            .remove(KEY_IP)
            .putInt(KEY_PORT, 80)
            .putBoolean(KEY_AUTO_REFRESH, true)
            .apply()
        _config.value = DeviceConfig(
            ipAddress = "",
            httpPort = 80,
            autoRefresh = true,
            timeoutMs = 3500L
        )
    }

    companion object {
        private const val KEY_IP = "pref_device_ip"
        private const val KEY_PORT = "pref_http_port"
        private const val KEY_AUTO_REFRESH = "pref_auto_refresh"
        private const val KEY_TIMEOUT = "pref_timeout_ms"
    }
}
