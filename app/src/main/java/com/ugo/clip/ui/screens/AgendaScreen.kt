package com.ugo.clip.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ugo.clip.data.model.ReminderEntity
import com.ugo.clip.data.model.TaskEntity
import com.ugo.clip.ui.theme.HugoAmberWarning
import com.ugo.clip.ui.theme.HugoCyanPrimary
import com.ugo.clip.ui.theme.HugoGreenActive
import com.ugo.clip.ui.theme.HugoSlateCard
import com.ugo.clip.ui.theme.HugoSlateCardElevated

@Composable
fun AgendaScreen(
    reminders: List<ReminderEntity>,
    tasks: List<TaskEntity>,
    onToggleReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onAddReminder: (String, String) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onAddTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = HugoCyanPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = HugoCyanPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Lembretes & Agenda (${reminders.count { !it.isDone }})",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Tarefas (${tasks.count { !it.isCompleted }})",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTab == 0) {
                    if (reminders.isEmpty()) {
                        item {
                            EmptyStateNotice(
                                message = "Nenhum compromisso agendado.\nDiga: \"Hugo, lembra de cobrar o João quinta às nove\"."
                            )
                        }
                    } else {
                        items(reminders, key = { it.id }) { reminder ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleReminder(reminder) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = HugoSlateCard),
                                border = BorderStroke(1.dp, Color(0xFF28354E))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (reminder.isDone) Icons.Default.CheckCircle else Icons.Default.Event,
                                        contentDescription = null,
                                        tint = if (reminder.isDone) HugoGreenActive else HugoAmberWarning,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reminder.title,
                                            color = if (reminder.isDone) Color.Gray else Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = reminder.timeDescription,
                                            color = if (reminder.isDone) Color.Gray else HugoCyanPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteReminder(reminder) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (tasks.isEmpty()) {
                        item {
                            EmptyStateNotice(
                                message = "Nenhuma tarefa pendente.\nDiga: \"Hugo, anota: comprar dois disjuntores\"."
                            )
                        }
                    } else {
                        items(tasks, key = { it.id }) { task ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleTask(task) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = HugoSlateCard),
                                border = BorderStroke(1.dp, Color(0xFF28354E))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (task.isCompleted) HugoGreenActive else HugoCyanPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            color = if (task.isCompleted) Color.Gray else Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (task.dueDate != null) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Prazo: ${task.dueDate}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteTask(task) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_agenda_fab"),
            containerColor = HugoCyanPrimary,
            contentColor = Color(0xFF00363D)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar")
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var timeDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (selectedTab == 0) "Novo Lembrete" else "Nova Tarefa",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (selectedTab == 0) "Lembrete" else "Descrição da Tarefa") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    if (selectedTab == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = timeDesc,
                            onValueChange = { timeDesc = it },
                            label = { Text("Quando? (ex: Quinta às 09:00)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            if (selectedTab == 0) {
                                onAddReminder(title, if (timeDesc.isBlank()) "Hoje" else timeDesc)
                            } else {
                                onAddTask(title)
                            }
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HugoCyanPrimary, contentColor = Color(0xFF00363D))
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = HugoSlateCardElevated
        )
    }
}

@Composable
private fun EmptyStateNotice(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Event,
            contentDescription = null,
            tint = Color(0xFF475569),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
