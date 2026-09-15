package com.ugo.clip.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ugo.clip.data.model.CommandLogEntity
import com.ugo.clip.data.model.GeminiInteractionEntity
import com.ugo.clip.ui.theme.HugoAmberWarning
import com.ugo.clip.ui.theme.HugoCyanPrimary
import com.ugo.clip.ui.theme.HugoGreenActive
import com.ugo.clip.ui.theme.HugoRedSentinel
import com.ugo.clip.ui.theme.HugoSlateCard
import com.ugo.clip.ui.theme.HugoSlateSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    commandLogs: List<CommandLogEntity>,
    geminiInteractions: List<GeminiInteractionEntity>,
    onClearHistory: () -> Unit,
    onClearGeminiInteractions: () -> Unit,
    onDeleteGeminiInteraction: (GeminiInteractionEntity) -> Unit,
    onSaveToNotes: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedHistoryTab by remember { mutableIntStateOf(0) } // 0 = Gemini AI Interactions, 1 = Sentinel Audit Logs
    var searchQuery by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<GeminiInteractionEntity?>(null) }
    var selectedDetailItem by remember { mutableStateOf<GeminiInteractionEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss • dd/MM/yyyy", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Tab Selection: Gemini AI History vs Sentinel Audit Logs
        TabRow(
            selectedTabIndex = selectedHistoryTab,
            containerColor = HugoSlateSurface,
            contentColor = HugoCyanPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedHistoryTab]),
                    color = HugoCyanPrimary,
                    height = 3.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedHistoryTab == 0,
                onClick = { selectedHistoryTab = 0 },
                modifier = Modifier.testTag("tab_gemini_interactions"),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (selectedHistoryTab == 0) HugoCyanPrimary else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Interações Gemini (${geminiInteractions.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedHistoryTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedHistoryTab == 0) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            )

            Tab(
                selected = selectedHistoryTab == 1,
                onClick = { selectedHistoryTab = 1 },
                modifier = Modifier.testTag("tab_sentinel_audit"),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (selectedHistoryTab == 1) HugoCyanPrimary else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auditoria Sentinel (${commandLogs.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedHistoryTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedHistoryTab == 1) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            )
        }

        // Subheader and Clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedHistoryTab == 0) "TRANSCRIÇÕES & RESPOSTAS DA IA" else "LOGS DE COMANDOS DO SISTEMA",
                color = HugoCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )

            val hasItems = if (selectedHistoryTab == 0) geminiInteractions.isNotEmpty() else commandLogs.isNotEmpty()
            if (hasItems) {
                OutlinedButton(
                    onClick = {
                        if (selectedHistoryTab == 0) onClearGeminiInteractions() else onClearHistory()
                    },
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpar", fontSize = 11.sp)
                }
            }
        }

        // Search Bar for quick filtering
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (selectedHistoryTab == 0) "Buscar transcrição ou resposta IA..." else "Buscar nos comandos de auditoria...",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpar busca",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HugoSlateSurface,
                unfocusedContainerColor = HugoSlateSurface,
                focusedBorderColor = HugoCyanPrimary,
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("history_search_input")
        )

        // TAB 0: GEMINI AI INTERACTIONS
        if (selectedHistoryTab == 0) {
            val filteredInteractions = remember(geminiInteractions, searchQuery) {
                if (searchQuery.isBlank()) {
                    geminiInteractions
                } else {
                    geminiInteractions.filter {
                        it.transcribedText.contains(searchQuery, ignoreCase = true) ||
                                it.aiResponse.contains(searchQuery, ignoreCase = true) ||
                                it.interactionType.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

            if (filteredInteractions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F1E36)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = HugoCyanPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Nenhuma interação com IA gravada no Room." else "Nenhum resultado para \"$searchQuery\".",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Faça perguntas livres ou peça resumos com Gemini para registrar o histórico.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        lineHeight = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gemini_interactions_list"),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredInteractions, key = { it.id }) { item ->
                        val badgeColor = when (item.interactionType) {
                            "SUMMARY" -> Color(0xFF38BDF8) // Light blue
                            "ANALYSIS" -> Color(0xFFA5B4FC) // Indigo
                            else -> Color(0xFF34D399) // Emerald
                        }
                        val badgeText = when (item.interactionType) {
                            "SUMMARY" -> "RESUMO GEMINI"
                            "ANALYSIS" -> "ANÁLISE SEMÂNTICA"
                            else -> "PERGUNTA & RESPOSTA"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDetailItem = item }
                                .testTag("gemini_item_${item.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = HugoSlateCard),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Top row: Badge + Timestamp + Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(badgeColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                color = badgeColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.modelName,
                                            color = Color(0xFF64748B),
                                            fontSize = 10.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dateFormat.format(Date(item.timestamp)),
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { itemToDelete = item },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Excluir",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Transcribed Speech section
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = "Transcrição",
                                        tint = HugoCyanPrimary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "TRANSCRIÇÃO DE VOZ",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            letterSpacing = 0.8.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "“${item.transcribedText}”",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // AI Response section
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini",
                                        tint = badgeColor,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "RESPOSTA GEMINI AI",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            letterSpacing = 0.8.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.aiResponse,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                // If key points are present
                                if (item.keyPointsJson.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "PONTOS-CHAVE:",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        letterSpacing = 0.8.sp
                                    )
                                    item.keyPointsJson.lines().filter { it.isNotBlank() }.forEach { pt ->
                                        Text(
                                            text = "• $pt",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Bottom Card Bar: Save to notes button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            onSaveToNotes("Síntese Gemini", item.aiResponse)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = HugoCyanPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Salvar em Notas",
                                            fontSize = 11.sp,
                                            color = HugoCyanPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 1: SENTINEL AUDIT LOGS
        if (selectedHistoryTab == 1) {
            val filteredCommandLogs = remember(commandLogs, searchQuery) {
                if (searchQuery.isBlank()) {
                    commandLogs
                } else {
                    commandLogs.filter {
                        it.transcript.contains(searchQuery, ignoreCase = true) ||
                                it.response.contains(searchQuery, ignoreCase = true) ||
                                it.intent.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

            if (filteredCommandLogs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Nenhum comando registrado no histórico." else "Nenhum log para \"$searchQuery\".",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("command_logs_list"),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCommandLogs, key = { it.id }) { log ->
                        val badgeColor = when (log.securityLevel) {
                            "LEVEL_A" -> HugoCyanPrimary
                            "LEVEL_B" -> HugoAmberWarning
                            "LEVEL_C" -> HugoRedSentinel
                            else -> HugoGreenActive
                        }
                        val badgeText = when (log.securityLevel) {
                            "LEVEL_A" -> "NÍVEL A • DIRETO"
                            "LEVEL_B" -> "NÍVEL B • CONFIRMADO"
                            "LEVEL_C" -> "NÍVEL C • BLOQUEADO"
                            else -> "SENTINEL"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = HugoSlateCard),
                            border = BorderStroke(1.dp, Color(0xFF28354E))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = badgeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = HugoCyanPrimary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "“${log.transcript}”",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = if (log.status == "BLOCKED") Icons.Default.Security else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (log.status == "BLOCKED") HugoRedSentinel else HugoGreenActive,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = log.response,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog for deleting individual interaction
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Excluir Interação", color = Color.White) },
            text = {
                Text(
                    text = "Deseja remover esta transcrição e a resposta do histórico local?",
                    color = Color(0xFFCBD5E1)
                )
            },
            containerColor = HugoSlateCard,
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGeminiInteraction(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HugoRedSentinel)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Detail dialog when tapping an item
    selectedDetailItem?.let { detail ->
        AlertDialog(
            onDismissRequest = { selectedDetailItem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = HugoCyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Detalhes da Interação IA", color = Color.White, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text("TRANSCRIÇÃO DE VOZ:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HugoCyanPrimary)
                    Text("“${detail.transcribedText}”", fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("RESPOSTA COMPLETA:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                    Text(detail.aiResponse, fontSize = 13.sp, color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                    if (detail.keyPointsJson.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PONTOS-CHAVE:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF93C5FD))
                        detail.keyPointsJson.lines().forEach {
                            if (it.isNotBlank()) Text("• $it", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                        }
                    }

                    if (detail.actionItemsJson.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AÇÕES IDENTIFICADAS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF86EFAC))
                        detail.actionItemsJson.lines().forEach {
                            if (it.isNotBlank()) Text("✓ $it", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gravado no Room em: ${dateFormat.format(Date(detail.timestamp))}", fontSize = 10.sp, color = Color(0xFF64748B))
                }
            },
            containerColor = HugoSlateCard,
            confirmButton = {
                Button(
                    onClick = {
                        onSaveToNotes("Resumo: ${detail.transcribedText.take(20)}", detail.aiResponse)
                        selectedDetailItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HugoCyanPrimary, contentColor = Color(0xFF00363D))
                ) {
                    Text("Salvar como Nota")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDetailItem = null }) {
                    Text("Fechar", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
