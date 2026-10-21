package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NodeMcuUiState
import com.example.ui.components.LedBulbIndicator
import com.example.ui.theme.AlertRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LedOffDarkGray
import com.example.ui.theme.LedOffGray
import com.example.ui.theme.LedOnGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: NodeMcuUiState,
    onTurnLedOn: () -> Unit,
    onTurnLedOff: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFirmware: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isLedOn = uiState.status?.ledOn == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isConnected) EmeraldPrimary else Slate500)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NodeMCU LED Controller",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenFirmware,
                        modifier = Modifier.testTag("open_firmware_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Arduino Firmware",
                            tint = Slate400
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("open_settings_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings",
                            tint = Slate400
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
            // Loading indicator bar
            AnimatedVisibility(visible = uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = EmeraldPrimary,
                    trackColor = Slate800
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Device Header Card
                DeviceHeaderCard(
                    deviceName = uiState.status?.device ?: "NodeMCU ESP8266",
                    ipAddress = if (uiState.config.ipAddress.isNotBlank()) {
                        "${uiState.config.ipAddress}:${uiState.config.httpPort}"
                    } else {
                        "Not Configured"
                    },
                    isConnected = uiState.isConnected,
                    networkDescription = uiState.networkStatus.networkDescription,
                    isWifiConnected = uiState.networkStatus.isWifiConnected
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error Banner
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.errorMessage?.let { errorText ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("error_banner"),
                            colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = AlertRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Communication Error",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = AlertRed
                                    )
                                    Text(
                                        text = errorText,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                IconButton(onClick = onDismissError) {
                                    Text("✕", color = Slate400, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // LED Bulb Centerpiece
                LedBulbIndicator(
                    isLedOn = isLedOn,
                    isConnected = uiState.isConnected
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Control Action Buttons (Large LED ON & LED OFF)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Large LED ON Button
                    Button(
                        onClick = onTurnLedOn,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("led_on_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLedOn && uiState.isConnected) EmeraldDark else EmeraldPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = Slate800,
                            disabledContentColor = Slate500
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (isLedOn && uiState.isConnected) EmeraldLight else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "LED ON",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Large LED OFF Button
                    Button(
                        onClick = onTurnLedOff,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("led_off_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isLedOn && uiState.isConnected) LedOffDarkGray else Slate700,
                            contentColor = Color.White,
                            disabledContainerColor = Slate800,
                            disabledContentColor = Slate500
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "LED OFF",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Action Row (Refresh Status & Open Settings)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("refresh_status_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EmeraldLight
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Refresh Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("open_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate800,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(18.dp),
                            tint = Slate400
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Settings",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Telemetry & Details Card (if status returned)
                if (uiState.status != null) {
                    BoardTelemetryCard(status = uiState.status)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Last Command / Status Log Box
                LastCommandCard(
                    message = uiState.lastCommandMessage ?: "Awaiting first command...",
                    isConnected = uiState.isConnected
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DeviceHeaderCard(
    deviceName: String,
    ipAddress: String,
    isConnected: Boolean,
    networkDescription: String,
    isWifiConnected: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_header_card"),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Row 1: Device Name & Connection Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = deviceName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Physical Built-in LED (GPIO 2 Active-LOW)",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                }

                // Connection badge: CONNECTED or DISCONNECTED
                Box(
                    modifier = Modifier
                        .testTag("connection_indicator")
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isConnected) EmeraldPrimary.copy(alpha = 0.2f) else Slate800
                        )
                        .border(
                            1.dp,
                            if (isConnected) EmeraldPrimary else Slate700,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) EmeraldLight else Slate500)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) EmeraldLight else Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Device IP Address
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NodeMCU IP: ",
                    fontSize = 13.sp,
                    color = Slate400
                )
                Text(
                    text = ipAddress,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (ipAddress != "Not Configured") EmeraldLight else WarningAmber
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Phone Wi-Fi Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isWifiConnected) EmeraldLight else WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Phone Network: ",
                    fontSize = 13.sp,
                    color = Slate400
                )
                Text(
                    text = networkDescription,
                    fontSize = 13.sp,
                    color = if (isWifiConnected) Slate200 else WarningAmber,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BoardTelemetryCard(status: com.example.data.model.NodeMcuStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "BOARD TELEMETRY (LIVE)",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryItem(
                    label = "SSID",
                    value = status.ssid ?: "N/A"
                )
                TelemetryItem(
                    label = "RSSI SIGNAL",
                    value = if (status.rssi != null) "${status.rssi} dBm" else "N/A"
                )
                TelemetryItem(
                    label = "UPTIME",
                    value = if (status.uptime != null) "${status.uptime}s" else "N/A"
                )
                TelemetryItem(
                    label = "PORTS",
                    value = "H:${status.httpPort} U:${status.udpPort}"
                )
            }
        }
    }
}

@Composable
private fun TelemetryItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LastCommandCard(message: String, isConnected: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("last_command_card"),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = if (isConnected) EmeraldPrimary else Slate500,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "LAST COMMAND STATUS",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 1.sp
                )
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = Slate200,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private val Slate200 = Color(0xFFE2E8F0)
