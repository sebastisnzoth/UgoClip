package com.ugo.clip.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ugo.clip.core.voice.HugoVoiceState
import com.ugo.clip.ui.theme.HugoAmberWarning
import com.ugo.clip.ui.theme.HugoCyanPrimary
import com.ugo.clip.ui.theme.HugoRedSentinel
import com.ugo.clip.ui.theme.HugoVioletSecondary

@Composable
fun VoiceOrb(
    state: HugoVoiceState,
    audioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (state == HugoVoiceState.LISTENING) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val waveRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_rotation"
    )

    val dynamicScale = if (state == HugoVoiceState.LISTENING) {
        1.0f + (audioLevel * 0.25f)
    } else {
        1.0f
    }

    val primaryColor = when (state) {
        HugoVoiceState.LISTENING -> HugoCyanPrimary
        HugoVoiceState.PROCESSING -> HugoVioletSecondary
        HugoVoiceState.SPEAKING -> Color(0xFFFF4081)
        HugoVoiceState.PAUSED -> HugoAmberWarning
        HugoVoiceState.ERROR -> HugoRedSentinel
        HugoVoiceState.STARTING -> HugoCyanPrimary.copy(alpha = 0.6f)
        HugoVoiceState.OFF -> Color(0xFF475569)
    }

    val stateLabel = when (state) {
        HugoVoiceState.LISTENING -> "ESCUTANDO"
        HugoVoiceState.PROCESSING -> "PROCESSANDO"
        HugoVoiceState.SPEAKING -> "FALANDO"
        HugoVoiceState.PAUSED -> "PAUSADO"
        HugoVoiceState.ERROR -> "ERRO"
        HugoVoiceState.STARTING -> "INICIANDO..."
        HugoVoiceState.OFF -> "MODO HUGO DESATIVADO"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .testTag("voice_orb_touch_area")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Background ripple waves
            if (state == HugoVoiceState.LISTENING || state == HugoVoiceState.SPEAKING) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseScale)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.12f),
                        radius = (size.minDimension / 2) * (0.9f + audioLevel * 0.3f)
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.25f),
                        radius = (size.minDimension / 2) * 0.75f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            if (state == HugoVoiceState.PROCESSING) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, primaryColor, primaryColor)
                        ),
                        startAngle = waveRotation,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }

            // Core Orb Body
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(dynamicScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = if (state == HugoVoiceState.OFF) 0.15f else 0.45f),
                                Color(0xFF0D1424),
                                Color(0xFF070B14)
                            )
                        )
                    )
                    .border(
                        width = if (state == HugoVoiceState.LISTENING) 3.dp else 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(primaryColor, primaryColor.copy(alpha = 0.3f))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    HugoVoiceState.LISTENING -> {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Microfone Ativo",
                            tint = HugoCyanPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    HugoVoiceState.PROCESSING -> {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Processando Áudio",
                            tint = HugoVioletSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    HugoVoiceState.SPEAKING -> {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "HUGO Falando",
                            tint = Color(0xFFFF4081),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    HugoVoiceState.PAUSED -> {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Escuta Pausada",
                            tint = HugoAmberWarning,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    HugoVoiceState.OFF -> {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Hugo Desativado",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Status",
                            tint = primaryColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stateLabel,
            color = primaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}
