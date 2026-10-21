package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DeviceConfig
import com.example.data.model.DiscoveryResult
import com.example.data.model.NodeMcuStatus
import com.example.data.network.NetworkMonitor
import com.example.data.network.NetworkStatus
import com.example.data.network.NodeMcuApiClient
import com.example.data.network.UdpDiscoveryManager
import com.example.data.repository.DeviceSettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface DiscoveryUiState {
    data object Idle : DiscoveryUiState
    data object Searching : DiscoveryUiState
    data class Found(val result: DiscoveryResult) : DiscoveryUiState
    data class Failed(val error: String) : DiscoveryUiState
}

data class NodeMcuUiState(
    val config: DeviceConfig = DeviceConfig(),
    val status: NodeMcuStatus? = null,
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val lastCommandMessage: String? = null,
    val errorMessage: String? = null,
    val networkStatus: NetworkStatus = NetworkStatus(),
    val discoveryState: DiscoveryUiState = DiscoveryUiState.Idle,
    val isTestingConnection: Boolean = false,
    val testConnectionMessage: String? = null
)

class NodeMcuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceSettingsRepository(application)
    private val apiClient = NodeMcuApiClient()
    private val udpDiscovery = UdpDiscoveryManager(application)
    private val networkMonitor = NetworkMonitor(application)

    private val _uiState = MutableStateFlow(NodeMcuUiState())
    val uiState: StateFlow<NodeMcuUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        // Collect network changes
        viewModelScope.launch {
            networkMonitor.networkStatus.collect { netStatus ->
                _uiState.update { it.copy(networkStatus = netStatus) }
            }
        }

        // Collect persisted settings
        viewModelScope.launch {
            repository.config.collect { config ->
                _uiState.update { it.copy(config = config) }
                restartPolling(config)
                if (config.ipAddress.isNotBlank()) {
                    refreshStatus(showLoading = true)
                }
            }
        }
    }

    private fun restartPolling(config: DeviceConfig) {
        pollingJob?.cancel()
        if (config.autoRefresh && config.ipAddress.isNotBlank()) {
            pollingJob = viewModelScope.launch {
                while (isActive) {
                    delay(5000L)
                    if (_uiState.value.config.ipAddress.isNotBlank() && !_uiState.value.isLoading) {
                        refreshStatus(showLoading = false)
                    }
                }
            }
        }
    }

    /**
     * Fetch status from GET /status
     */
    fun refreshStatus(showLoading: Boolean = true) {
        val config = _uiState.value.config
        if (config.ipAddress.isBlank()) {
            _uiState.update {
                it.copy(
                    isConnected = false,
                    errorMessage = "No IP address configured. Open Settings to set or discover board IP."
                )
            }
            return
        }

        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val result = apiClient.getStatus(config.ipAddress, config.httpPort)
            val now = timeFormat.format(Date())

            result.onSuccess { newStatus ->
                _uiState.update {
                    it.copy(
                        status = newStatus,
                        isConnected = true,
                        isLoading = false,
                        errorMessage = null,
                        lastCommandMessage = "Status refreshed at $now"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isConnected = false,
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to connect to NodeMCU at ${config.ipAddress}:${config.httpPort}"
                    )
                }
            }
        }
    }

    /**
     * Controls physical built-in LED via GET /led/on or /led/off
     * Strict requirement: Update LED state ONLY upon verified board response!
     */
    fun setLed(turnOn: Boolean) {
        val config = _uiState.value.config
        if (config.ipAddress.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Please configure the NodeMCU IP address first in Settings.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = apiClient.setLed(config.ipAddress, config.httpPort, turnOn)
            val now = timeFormat.format(Date())
            val targetName = if (turnOn) "LED ON" else "LED OFF"

            result.onSuccess { updatedStatus ->
                // Verify the response confirms the desired state
                val actualState = updatedStatus.ledOn
                _uiState.update {
                    it.copy(
                        status = updatedStatus,
                        isConnected = true,
                        isLoading = false,
                        errorMessage = null,
                        lastCommandMessage = "Command $targetName executed successfully at $now (Physical LED: ${if (actualState) "ON" else "OFF"})"
                    )
                }
            }.onFailure { error ->
                // Never pretend LED changed
                _uiState.update {
                    it.copy(
                        isConnected = false,
                        isLoading = false,
                        errorMessage = "Command $targetName failed: ${error.localizedMessage ?: "Network error"}. Device marked DISCONNECTED."
                    )
                }
            }
        }
    }

    /**
     * Starts UDP Broadcast discovery on port 4210
     */
    fun startDiscovery() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    discoveryState = DiscoveryUiState.Searching,
                    errorMessage = null
                )
            }

            val result = udpDiscovery.discoverDevice()
            result.onSuccess { discoveryResult ->
                _uiState.update {
                    it.copy(discoveryState = DiscoveryUiState.Found(discoveryResult))
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(discoveryState = DiscoveryUiState.Failed(error.localizedMessage ?: "Discovery failed"))
                }
            }
        }
    }

    /**
     * Save discovered device configuration
     */
    fun applyDiscoveredDevice(result: DiscoveryResult) {
        val current = _uiState.value.config
        val updated = current.copy(
            ipAddress = result.ip,
            httpPort = result.httpPort
        )
        repository.saveConfig(updated)
        _uiState.update {
            it.copy(
                discoveryState = DiscoveryUiState.Idle,
                lastCommandMessage = "Discovered device saved: ${result.ip}:${result.httpPort}"
            )
        }
        refreshStatus(showLoading = true)
    }

    /**
     * Save manually entered settings
     */
    fun saveManualConfig(ip: String, port: Int, autoRefresh: Boolean) {
        val trimmedIp = ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val validPort = if (port in 1..65535) port else 80
        val updated = DeviceConfig(
            ipAddress = trimmedIp,
            httpPort = validPort,
            autoRefresh = autoRefresh
        )
        repository.saveConfig(updated)
        _uiState.update {
            it.copy(
                lastCommandMessage = "Saved configuration: $trimmedIp:$validPort",
                errorMessage = null
            )
        }
        if (trimmedIp.isNotBlank()) {
            refreshStatus(showLoading = true)
        }
    }

    /**
     * Clear saved device
     */
    fun clearSavedDevice() {
        repository.clearConfig()
        _uiState.update {
            it.copy(
                status = null,
                isConnected = false,
                lastCommandMessage = "Saved device configuration cleared",
                discoveryState = DiscoveryUiState.Idle,
                testConnectionMessage = null
            )
        }
    }

    /**
     * Test connection to arbitrary IP/port without saving
     */
    fun testConnection(ip: String, port: Int) {
        val trimmedIp = ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        if (trimmedIp.isBlank()) {
            _uiState.update { it.copy(testConnectionMessage = "Please enter an IP address to test.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTestingConnection = true,
                    testConnectionMessage = "Connecting to http://$trimmedIp:$port/status..."
                )
            }

            val result = apiClient.testConnection(trimmedIp, port)
            result.onSuccess { (status, latencyMs) ->
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionMessage = "Success! Connected in ${latencyMs}ms. Board reported: ${status.device ?: "ESP8266"} (LED: ${if (status.ledOn) "ON" else "OFF"})"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionMessage = "Failed: ${error.localizedMessage ?: "Could not connect"}"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetDiscovery() {
        _uiState.update { it.copy(discoveryState = DiscoveryUiState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
