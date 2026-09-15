package com.ugo.clip.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ugo.clip.core.voice.HugoVoiceState
import com.ugo.clip.ui.theme.HugoAmberWarning
import com.ugo.clip.ui.theme.HugoCyanPrimary
import com.ugo.clip.ui.theme.HugoGreenActive
import com.ugo.clip.ui.theme.HugoRedSentinel
import com.ugo.clip.ui.theme.HugoSlateCard
import com.ugo.clip.ui.theme.HugoVioletSecondary

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WearableClipCard(
    isConnected: Boolean,
    batteryPercent: Int,
    voiceState: HugoVoiceState,
    onToggleConnection: () -> Unit,
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ledColor = when {
        !isConnected -> Color.DarkGray
        voiceState == HugoVoiceState.LISTENING -> HugoCyanPrimary
        voiceState == HugoVoiceState.PROCESSING -> HugoVioletSecondary
        voiceState == HugoVoiceState.SPEAKING -> Color(0xFFFF4081)
        voiceState == HugoVoiceState.PAUSED -> HugoAmberWarning
        voiceState == HugoVoiceState.ERROR -> HugoRedSentinel
        else -> HugoGreenActive
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HugoSlateCard),
        border = BorderStroke(1.dp, Color(0xFF28354E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Clip Status & BLE Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) HugoGreenActive else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HUGO CLIP WEARABLE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isConnected) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Bateria",
                            tint = HugoGreenActive,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$batteryPercent%",
                            color = HugoGreenActive,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = onToggleConnection,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_ble_clip_button")
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                            contentDescription = "Bluetooth Status",
                            tint = if (isConnected) HugoCyanPrimary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clip hardware physical body simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Physical circular Clip model with button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF2C394F), Color(0xFF141A24), Color(0xFF090D14))
                            )
                        )
                        .border(2.dp, Color(0xFF3E506D), CircleShape)
                        .combinedClickable(
                            onClick = onSingleTap,
                            onDoubleClick = onDoubleTap,
                            onLongClick = onLongPress
                        )
                        .testTag("clip_hardware_button"),
                    contentAlignment = Alignment.Center
                ) {
                    // Central physical button with subtle LED ring
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B2332))
                            .border(1.5.dp, ledColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ledColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isConnected) "Botão do Clip Ativo" else "Clip Desconectado",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "• 1 toque: Falar / Ouvir\n• 2 toques: Pausar / Retomar\n• Segurar: Desligar",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
