package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LedOffDarkGray
import com.example.ui.theme.LedOffGray
import com.example.ui.theme.LedOnGreen
import com.example.ui.theme.LedOnGreenLight
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun LedBulbIndicator(
    isLedOn: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    // Pulse animation when LED is ON
    val infiniteTransition = rememberInfiniteTransition(label = "LedGlowPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LedGlowPulseFloat"
    )

    val coreColor by animateColorAsState(
        targetValue = if (isLedOn && isConnected) LedOnGreen else LedOffGray,
        animationSpec = tween(400),
        label = "CoreColor"
    )

    val highlightColor by animateColorAsState(
        targetValue = if (isLedOn && isConnected) LedOnGreenLight else Slate700,
        animationSpec = tween(400),
        label = "HighlightColor"
    )

    Column(
        modifier = modifier
            .testTag("led_bulb_indicator")
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            // Radial Glow Halo
            if (isLedOn && isConnected) {
                Canvas(
                    modifier = Modifier
                        .size((190 * pulseScale).dp)
                        .clip(CircleShape)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                EmeraldLight.copy(alpha = 0.55f),
                                EmeraldPrimary.copy(alpha = 0.30f),
                                EmeraldGlow.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension / 2f
                        )
                    )
                }
            }

            // Outer metallic rim
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Slate700,
                                Slate900,
                                Slate800
                            )
                        )
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            if (isLedOn && isConnected) {
                                listOf(EmeraldLight, EmeraldPrimary, Slate700)
                            } else {
                                listOf(Slate700, Slate800, Slate700)
                            }
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Diode lens
                Canvas(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                ) {
                    val radius = size.minDimension / 2f
                    // Base lens gradient
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = if (isLedOn && isConnected) {
                                listOf(
                                    Color.White,
                                    LedOnGreenLight,
                                    LedOnGreen,
                                    EmeraldDark
                                )
                            } else {
                                listOf(
                                    Slate700,
                                    LedOffDarkGray,
                                    Slate900
                                )
                            },
                            center = Offset(center.x * 0.85f, center.y * 0.75f),
                            radius = radius
                        )
                    )

                    // Glass reflection arc on top
                    drawArc(
                        color = Color.White.copy(alpha = if (isLedOn && isConnected) 0.65f else 0.25f),
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(radius * 0.3f, radius * 0.2f),
                        size = androidx.compose.ui.geometry.Size(radius * 1.4f, radius * 0.8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }

                // Center Icon / State
                Icon(
                    imageVector = if (isLedOn && isConnected) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                    contentDescription = if (isLedOn) "LED ON" else "LED OFF",
                    tint = if (isLedOn && isConnected) Color.White else Slate700,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // State pill badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isLedOn && isConnected) {
                        EmeraldPrimary.copy(alpha = 0.2f)
                    } else {
                        Slate800
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isLedOn && isConnected) EmeraldPrimary else Slate700,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (!isConnected) {
                    "OFFLINE (LED UNKNOWN)"
                } else if (isLedOn) {
                    "PHYSICAL LED: ON"
                } else {
                    "PHYSICAL LED: OFF"
                },
                color = if (isLedOn && isConnected) EmeraldLight else LedOffGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

private val EmeraldDark = Color(0xFF047857)
