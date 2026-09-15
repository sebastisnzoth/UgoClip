package com.ugo.clip.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ugo.clip.core.voice.HugoUiState
import com.ugo.clip.core.voice.HugoVoiceState
import com.ugo.clip.data.model.ReminderEntity
import com.ugo.clip.data.model.TaskEntity
import com.ugo.clip.ui.components.SentinelBanner
import com.ugo.clip.ui.components.VoiceOrb
import com.ugo.clip.ui.theme.HugoAmberWarning
import com.ugo.clip.ui.theme.HugoCyanPrimary
import com.ugo.clip.ui.theme.HugoGreenActive
import com.ugo.clip.ui.theme.HugoSlateCard
import com.ugo.clip.ui.theme.HugoSlateCardElevated

@Composable
fun HomeScreen(
    uiState: HugoUiState,
    todayReminders: List<ReminderEntity>,
    todayTasks: List<TaskEntity>,
    onToggleHugoMode: () -> Unit,
    onPauseHugo: () -> Unit,
    onResumeHugo: () -> Unit,
    onStopHugo: () -> Unit,
    onSendCommand: (String) -> Unit,
    onConfirmSentinel: () -> Unit,
    onCancelSentinel: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onToggleReminder: (ReminderEntity) -> Unit,
    onStartVoiceInput: () -> Unit = {},
    onSummarizeTranscript: () -> Unit = {},
    onAnalyzeTranscript: () -> Unit = {},
    onClearGeminiAnalysis: () -> Unit = {},
    onSaveSummaryAsNote: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var manualInputText by remember { mutableStateOf("") }

    val quickCommands = listOf(
        "Hugo, me ajuda a organizar o dia",
        "Hugo, resume o que eu falei",
        "Hugo, analisa minhas tarefas",
        "Hugo, o que eu tenho hoje?",
        "Hugo, onde deixei a furadeira?",
        "Hugo, prepara uma mensagem para João: chego às oito",
        "Hugo, lembra de cobrar o João quinta às nove",
        "Hugo, anota comprar dois disjuntores",
        "Hugo, abre o Maps para Canasvieiras",
        "Hugo, abre o YouTube",
        "Hugo, pausa",
        "Hugo, volta a escutar"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sentinel Confirmation Banner if pending
        if (uiState.pendingConfirmation != null) {
            item {
                SentinelBanner(
                    confirmation = uiState.pendingConfirmation,
                    onConfirm = onConfirmSentinel,
                    onCancel = onCancelSentinel
                )
            }
        }

        // 2. Central Voice Orb Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(HugoSlateCard)
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VoiceOrb(
                    state = uiState.serviceState,
                    audioLevel = uiState.audioLevelRms,
                    onClick = {
                        if (uiState.serviceState == HugoVoiceState.OFF) {
                            onToggleHugoMode()
                        } else if (uiState.serviceState == HugoVoiceState.PAUSED) {
                            onResumeHugo()
                        } else {
                            onPauseHugo()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Transcript ticker / guidance
                val guidanceText = when {
                    uiState.transcript.isNotBlank() -> "“${uiState.transcript}”"
                    uiState.serviceState == HugoVoiceState.LISTENING -> "“Fale normalmente. Hugo está ouvindo...”"
                    uiState.serviceState == HugoVoiceState.PAUSED -> "“Microfone pausado”"
                    uiState.serviceState == HugoVoiceState.OFF -> "“Ative o Modo HUGO para começar”"
                    else -> "“${uiState.lastResponse}”"
                }

                Text(
                    text = guidanceText,
                    color = Color(0xFFF1F5F9),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = uiState.lastResponse,
                    color = HugoCyanPrimary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick AI Intelligence Action Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = onSummarizeTranscript,
                        label = { Text("✨ Resumir com Gemini", fontSize = 11.sp, color = HugoCyanPrimary, fontWeight = FontWeight.SemiBold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF0F2636)
                        ),
                        border = BorderStroke(1.dp, HugoCyanPrimary.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SuggestionChip(
                        onClick = onAnalyzeTranscript,
                        label = { Text("🧠 Analisar com IA", fontSize = 11.sp, color = Color(0xFFA5B4FC), fontWeight = FontWeight.SemiBold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF1E1B4B)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.serviceState == HugoVoiceState.OFF) {
                        Button(
                            onClick = onToggleHugoMode,
                            modifier = Modifier.testTag("activate_hugo_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HugoCyanPrimary,
                                contentColor = Color(0xFF00363D)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ativar Modo HUGO", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        if (uiState.serviceState == HugoVoiceState.PAUSED) {
                            Button(
                                onClick = onResumeHugo,
                                modifier = Modifier.testTag("resume_hugo_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = HugoGreenActive, contentColor = Color(0xFF003817)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retomar", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onPauseHugo,
                                modifier = Modifier.testTag("pause_hugo_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = HugoAmberWarning, contentColor = Color(0xFF452B00)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pausar", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onStopHugo,
                            modifier = Modifier.testTag("stop_hugo_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Desativar")
                        }
                    }
                }
            }
        }

        // 2.1. Gemini Loading Indicator
        if (uiState.isGeminiLoading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("gemini_loading_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                    border = BorderStroke(1.dp, HugoCyanPrimary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = HugoCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GEMINI 3.5 FLASH PROCESSANDO...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HugoCyanPrimary,
                                letterSpacing = 1.1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = HugoCyanPrimary,
                            trackColor = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Analisando semântica e gerando síntese inteligente com IA...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // 2.2. Gemini Intelligence Result Card
        if (!uiState.isGeminiLoading && (uiState.geminiAnalysis != null || !uiState.geminiSummary.isNullOrBlank())) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("gemini_analysis_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C192E)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = HugoCyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ANÁLISE GEMINI 3.5 FLASH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HugoCyanPrimary,
                                    letterSpacing = 1.1.sp
                                )
                            }
                            IconButton(
                                onClick = onClearGeminiAnalysis,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar Análise",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val summaryText = uiState.geminiAnalysis?.summary ?: uiState.geminiSummary.orEmpty()
                        if (summaryText.isNotBlank()) {
                            Text(
                                text = "RESUMO EXECUTIVO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7DD3FC),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = summaryText,
                                fontSize = 13.sp,
                                color = Color(0xFFF1F5F9),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        val intelligentResp = uiState.geminiAnalysis?.intelligentResponse
                        if (!intelligentResp.isNullOrBlank() && intelligentResp != summaryText) {
                            Text(
                                text = "RESPOSTA INTELIGENTE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5B4FC),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = intelligentResp,
                                fontSize = 13.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        val keyPoints = uiState.geminiAnalysis?.keyPoints.orEmpty()
                        if (keyPoints.isNotEmpty()) {
                            Text(
                                text = "PONTOS PRINCIPAIS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF93C5FD),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            keyPoints.forEach { point ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("• ", color = HugoCyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(point, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        val actionItems = uiState.geminiAnalysis?.actionableItems.orEmpty()
                        if (actionItems.isNotEmpty()) {
                            Text(
                                text = "AÇÕES IDENTIFICADAS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            actionItems.forEach { action ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("✓ ", color = Color(0xFF86EFAC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(action, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onSaveSummaryAsNote(summaryText) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salvar como Nota", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Acceptance Test Chips
        item {
            Column {
                Text(
                    text = "COMANDOS RÁPIDOS DO MVP",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickCommands.forEach { command ->
                        SuggestionChip(
                            onClick = { onSendCommand(command) },
                            label = { Text(command, fontSize = 12.sp, color = Color(0xFFE2E8F0)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = HugoSlateCardElevated
                            ),
                            border = BorderStroke(1.dp, Color(0xFF2E3D58))
                        )
                    }
                }
            }
        }

        // 4. Direct Command Input (Supports both real voice and emulator keyboard)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualInputText,
                    onValueChange = { manualInputText = it },
                    placeholder = { Text("Ou digite um comando de teste...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_command_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HugoSlateCard,
                        unfocusedContainerColor = HugoSlateCard,
                        focusedBorderColor = HugoCyanPrimary,
                        unfocusedBorderColor = Color(0xFF28354E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (manualInputText.isNotBlank()) {
                                onSendCommand(manualInputText)
                                manualInputText = ""
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onStartVoiceInput,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (uiState.serviceState == HugoVoiceState.LISTENING) HugoCyanPrimary else HugoSlateCardElevated)
                        .testTag("voice_input_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Falar com HUGO",
                        tint = if (uiState.serviceState == HugoVoiceState.LISTENING) Color(0xFF00363D) else HugoCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (manualInputText.isNotBlank()) {
                            onSendCommand(manualInputText)
                            manualInputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(HugoCyanPrimary)
                        .testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color(0xFF00363D),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 5. Today's Overview ("Hoje" section matching Section 11 of doc)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HugoSlateCard),
                border = BorderStroke(1.dp, Color(0xFF28354E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hoje",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${todayReminders.count { !it.isDone }} compromissos • ${todayTasks.count { !it.isCompleted }} tarefas",
                            color = HugoCyanPrimary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (todayReminders.isEmpty() && todayTasks.isEmpty()) {
                        Text(
                            text = "Nenhum compromisso ou tarefa para hoje. Diga \"Hugo, lembra de...\" ou \"Hugo, anota...\"",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    } else {
                        // Reminders
                        todayReminders.take(4).forEach { reminder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onToggleReminder(reminder) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (reminder.isDone) Icons.Default.CheckCircle else Icons.Default.Event,
                                    contentDescription = null,
                                    tint = if (reminder.isDone) HugoGreenActive else HugoAmberWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "• ${reminder.timeDescription} ${reminder.title}",
                                    color = if (reminder.isDone) Color.Gray else Color(0xFFE2E8F0),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Tasks
                        todayTasks.take(4).forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onToggleTask(task) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (task.isCompleted) HugoGreenActive else HugoCyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "• ${task.title}",
                                    color = if (task.isCompleted) Color.Gray else Color(0xFFE2E8F0),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
