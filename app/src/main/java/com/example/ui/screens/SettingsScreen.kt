package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiscoveryResult
import com.example.ui.DiscoveryUiState
import com.example.ui.NodeMcuUiState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: NodeMcuUiState,
    onNavigateBack: () -> Unit,
    onStartDiscovery: () -> Unit,
    onApplyDiscoveredDevice: (DiscoveryResult) -> Unit,
    onSaveManualConfig: (ip: String, port: Int, autoRefresh: Boolean) -> Unit,
    onClearSavedDevice: () -> Unit,
    onTestConnection: (ip: String, port: Int) -> Unit,
    onOpenFirmware: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var manualIp by remember(uiState.config.ipAddress) {
        mutableStateOf(uiState.config.ipAddress)
    }
    var manualPort by remember(uiState.config.httpPort) {
        mutableStateOf(uiState.config.httpPort.toString())
    }
    var autoRefresh by remember(uiState.config.autoRefresh) {
        mutableStateOf(uiState.config.autoRefresh)
    }

    var showInstructions by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_to_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return to Home",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenFirmware) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "View Firmware",
                            tint = EmeraldLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Wi-Fi Setup Instructions Accordion
                WifiInstructionsCard(
                    isExpanded = showInstructions,
                    onToggle = { showInstructions = !showInstructions }
                )

                // 1. Automatic UDP Device Discovery Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("udp_discovery_section"),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Automatic UDP Discovery",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Broadcasts DISCOVER_NODEMCU on UDP port 4210 to automatically find the ESP8266 board on your Wi-Fi network or hotspot.",
                            fontSize = 12.sp,
                            color = Slate400,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Trigger button
                        val isSearching = uiState.discoveryState is DiscoveryUiState.Searching
                        Button(
                            onClick = onStartDiscovery,
                            enabled = !isSearching,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("discover_device_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.Black
                            )
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Searching on port 4210...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Start UDP Device Discovery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Discovery Result Feedback
                        when (val state = uiState.discoveryState) {
                            is DiscoveryUiState.Found -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                DiscoveredDeviceCard(
                                    result = state.result,
                                    onUseDevice = {
                                        manualIp = state.result.ip
                                        manualPort = state.result.httpPort.toString()
                                        onApplyDiscoveredDevice(state.result)
                                    }
                                )
                            }
                            is DiscoveryUiState.Failed -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Discovery Failed",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = AlertRed
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = state.error,
                                            fontSize = 12.sp,
                                            color = Slate400,
                                            lineHeight = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tip: Some mobile hotspots isolate UDP broadcast. Check the serial monitor in Arduino IDE for the IP and enter it manually below.",
                                            fontSize = 11.sp,
                                            color = WarningAmber
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                // 2. Manual IP & Port Configuration
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_config_section"),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Manual Connection Settings",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter the exact IP address assigned to the ESP8266 by your router or hotspot.",
                            fontSize = 12.sp,
                            color = Slate400
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Manual IP Address Field
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it.trim() },
                            label = { Text("NodeMCU IP Address") },
                            placeholder = { Text("e.g. 192.168.1.120") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = EmeraldPrimary
                                )
                            },
                            trailingIcon = {
                                if (manualIp.isNotBlank()) {
                                    IconButton(onClick = { manualIp = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate500)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_ip_field"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = Slate700,
                                focusedLabelColor = EmeraldPrimary,
                                unfocusedLabelColor = Slate400,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // HTTP Port Field
                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it.filter { ch -> ch.isDigit() } },
                            label = { Text("HTTP Port (Default 80)") },
                            placeholder = { Text("80") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_port_field"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = Slate700,
                                focusedLabelColor = EmeraldPrimary,
                                unfocusedLabelColor = Slate400,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Auto-Refresh Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Slate800)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Auto-Poll Status (every 5s)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Keeps physical LED indicator in sync",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                            Switch(
                                checked = autoRefresh,
                                onCheckedChange = { autoRefresh = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldLight,
                                    checkedTrackColor = EmeraldDark
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Test Connection Button
                        OutlinedButton(
                            onClick = {
                                val portInt = manualPort.toIntOrNull() ?: 80
                                onTestConnection(manualIp, portInt)
                            },
                            enabled = manualIp.isNotBlank() && !uiState.isTestingConnection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("test_connection_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, InfoCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = InfoCyan)
                        ) {
                            if (uiState.isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = InfoCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pinging board...")
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Connection", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Test connection message
                        if (uiState.testConnectionMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.testConnectionMessage,
                                fontSize = 12.sp,
                                color = if (uiState.testConnectionMessage.startsWith("Success")) EmeraldLight else AlertRed,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate800, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Save Settings Button
                        Button(
                            onClick = {
                                val portInt = manualPort.toIntOrNull() ?: 80
                                onSaveManualConfig(manualIp, portInt, autoRefresh)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_device_settings_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Device Settings",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. Danger Zone: Clear Saved Device
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Reset & Storage",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Clears the saved IP and removes local board configuration.",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("clear_saved_device_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Saved Device", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Saved Device?") },
            text = { Text("This will remove the saved IP address and mark the device disconnected. You can re-discover or re-enter it at any time.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        manualIp = ""
                        manualPort = "80"
                        onClearSavedDevice()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900,
            titleContentColor = Color.White,
            textContentColor = Slate400
        )
    }
}

@Composable
private fun DiscoveredDeviceCard(
    result: DiscoveryResult,
    onUseDevice: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discovered_device_card"),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Discovered Device!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = EmeraldLight
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(EmeraldDark)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "UDP 4210",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${result.device ?: "NodeMCU ESP8266"} at ${result.ip}:${result.httpPort}",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onUseDevice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("apply_discovered_ip_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Save & Use This Discovered IP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun WifiInstructionsCard(
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("wifi_instructions_card"),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = InfoCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Wi-Fi & Hotspot Setup Guide",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Slate400
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Scenario 1
                    Row {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Scenario 1: Home/Office Wi-Fi Router",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "1. Enter your Wi-Fi SSID and password in the ESP8266 Arduino firmware.\n2. Power on the NodeMCU; it connects to your router.\n3. Make sure your Android phone is connected to the same Wi-Fi.\n4. Tap 'Start UDP Device Discovery' or enter the assigned IP.",
                                fontSize = 12.sp,
                                color = Slate400,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Scenario 2
                    Row {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = InfoCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Scenario 2: Mobile Hotspot (Android / iPhone)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "1. Turn on Portable Hotspot on your phone.\n2. In the firmware, set WIFI_SSID & WIFI_PASSWORD to your Hotspot name and password.\n3. Power on the NodeMCU; your phone's hotspot screen will show the connected ESP8266.\n4. Note: Some hotspots block UDP broadcast. If discovery doesn't find it, check your Hotspot Connected Devices list and type the IP manually.",
                                fontSize = 12.sp,
                                color = Slate400,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
