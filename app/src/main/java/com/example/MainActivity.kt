package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NodeMcuViewModel
import com.example.ui.screens.FirmwareScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    Home,
    Settings,
    Firmware
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NodeMcuApp()
            }
        }
    }
}

@Composable
fun NodeMcuApp(
    viewModel: NodeMcuViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }

    // Hardware and gesture back navigation
    if (currentScreen != AppScreen.Home) {
        BackHandler {
            viewModel.resetDiscovery()
            currentScreen = AppScreen.Home
        }
    }

    Crossfade(
        targetState = currentScreen,
        label = "ScreenTransition",
        modifier = Modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppScreen.Home -> {
                HomeScreen(
                    uiState = uiState,
                    onTurnLedOn = { viewModel.setLed(turnOn = true) },
                    onTurnLedOff = { viewModel.setLed(turnOn = false) },
                    onRefresh = { viewModel.refreshStatus(showLoading = true) },
                    onOpenSettings = { currentScreen = AppScreen.Settings },
                    onOpenFirmware = { currentScreen = AppScreen.Firmware },
                    onDismissError = { viewModel.dismissError() }
                )
            }
            AppScreen.Settings -> {
                SettingsScreen(
                    uiState = uiState,
                    onNavigateBack = {
                        viewModel.resetDiscovery()
                        currentScreen = AppScreen.Home
                    },
                    onStartDiscovery = { viewModel.startDiscovery() },
                    onApplyDiscoveredDevice = { result ->
                        viewModel.applyDiscoveredDevice(result)
                        currentScreen = AppScreen.Home
                    },
                    onSaveManualConfig = { ip, port, autoRefresh ->
                        viewModel.saveManualConfig(ip, port, autoRefresh)
                        currentScreen = AppScreen.Home
                    },
                    onClearSavedDevice = { viewModel.clearSavedDevice() },
                    onTestConnection = { ip, port -> viewModel.testConnection(ip, port) },
                    onOpenFirmware = { currentScreen = AppScreen.Firmware }
                )
            }
            AppScreen.Firmware -> {
                FirmwareScreen(
                    onNavigateBack = { currentScreen = AppScreen.Settings }
                )
            }
        }
    }
}
