package com.ugo.clip

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ugo.clip.core.voice.HugoVoiceState
import com.ugo.clip.ui.HugoViewModel
import com.ugo.clip.ui.components.WearableClipCard
import com.ugo.clip.ui.screens.AgendaScreen
import com.ugo.clip.ui.screens.HistoryScreen
import com.ugo.clip.ui.screens.HomeScreen
import com.ugo.clip.ui.screens.MemoryScreen
import com.ugo.clip.ui.theme.HugoCyanPrimary
import com.ugo.clip.ui.theme.HugoGreenActive
import com.ugo.clip.ui.theme.HugoObsidianBg
import com.ugo.clip.ui.theme.HugoRedSentinel
import com.ugo.clip.ui.theme.HugoSlateCard
import com.ugo.clip.ui.theme.HugoSlateSurface
import com.ugo.clip.ui.theme.HugoVioletSecondary
import com.ugo.clip.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: HugoViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val notes by viewModel.notes.collectAsStateWithLifecycle()
                val tasks by viewModel.tasks.collectAsStateWithLifecycle()
                val reminders by viewModel.reminders.collectAsStateWithLifecycle()
                val memories by viewModel.memories.collectAsStateWithLifecycle()
                val commandLogs by viewModel.commandLogs.collectAsStateWithLifecycle()
                val geminiInteractions by viewModel.geminiInteractions.collectAsStateWithLifecycle()

                val context = LocalContext.current
                var showClipModal by remember { mutableStateOf(false) }

                // Permission Launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
                    if (!recordGranted) {
                        Toast.makeText(
                            context,
                            "Permissão de microfone necessária para o Modo HUGO.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // Voice Recognition Launcher using Android SpeechRecognizer Intent API
                val speechRecognizerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        val spokenText = matches?.firstOrNull()?.trim()
                        if (!spokenText.isNullOrBlank()) {
                            viewModel.sendVoiceCommand(spokenText)
                        }
                    }
                }

                val onStartVoiceInput: () -> Unit = {
                    val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (!hasAudio) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    } else {
                        // Launch native Speech Recognition dialog for instant dictation
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale com o HUGO Clip...")
                        }
                        try {
                            speechRecognizerLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Reconhecimento de voz: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            // Fallback: start Hugo service listening mode
                            viewModel.startHugoMode()
                        }
                    }
                }

                // Check permissions on start
                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val missing = permissionsToRequest.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    }
                }

                HugoAppContent(
                    viewModel = viewModel,
                    uiState = uiState,
                    notes = notes,
                    tasks = tasks,
                    reminders = reminders,
                    memories = memories,
                    commandLogs = commandLogs,
                    geminiInteractions = geminiInteractions,
                    showClipModal = showClipModal,
                    onToggleClipModal = { showClipModal = !showClipModal },
                    onStartVoiceInput = onStartVoiceInput,
                    onRequestPermissions = {
                        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HugoAppContent(
    viewModel: HugoViewModel,
    uiState: com.ugo.clip.core.voice.HugoUiState,
    notes: List<com.ugo.clip.data.model.NoteEntity>,
    tasks: List<com.ugo.clip.data.model.TaskEntity>,
    reminders: List<com.ugo.clip.data.model.ReminderEntity>,
    memories: List<com.ugo.clip.data.model.MemoryFactEntity>,
    commandLogs: List<com.ugo.clip.data.model.CommandLogEntity>,
    geminiInteractions: List<com.ugo.clip.data.model.GeminiInteractionEntity>,
    showClipModal: Boolean,
    onToggleClipModal: () -> Unit,
    onStartVoiceInput: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HugoObsidianBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "HUGO",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(HugoCyanPrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CLIP",
                                        color = HugoCyanPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "MD MAESTRO • SECRETÁRIO DE VOZ",
                                color = Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                actions = {
                    // Wearable Clip Hardware Trigger button
                    IconButton(
                        onClick = onToggleClipModal,
                        modifier = Modifier.testTag("clip_modal_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = "Clip Wearable",
                            tint = if (uiState.isBleClipConnected) HugoCyanPrimary else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Live mic indicator
                    val statusDotColor = when (uiState.serviceState) {
                        HugoVoiceState.LISTENING -> HugoCyanPrimary
                        HugoVoiceState.PROCESSING -> HugoVioletSecondary
                        HugoVoiceState.SPEAKING -> Color(0xFFFF4081)
                        HugoVoiceState.PAUSED -> Color(0xFFFFAB00)
                        HugoVoiceState.OFF -> Color(0xFF475569)
                        HugoVoiceState.ERROR -> HugoRedSentinel
                        HugoVoiceState.STARTING -> HugoCyanPrimary
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HugoSlateSurface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = HugoSlateSurface,
                contentColor = HugoCyanPrimary
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voz"
                        )
                    },
                    label = { Text("Voz", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HugoCyanPrimary,
                        selectedTextColor = HugoCyanPrimary,
                        indicatorColor = HugoSlateCard,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Memória"
                        )
                    },
                    label = { Text("Memória", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HugoCyanPrimary,
                        selectedTextColor = HugoCyanPrimary,
                        indicatorColor = HugoSlateCard,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Agenda"
                        )
                    },
                    label = { Text("Agenda", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HugoCyanPrimary,
                        selectedTextColor = HugoCyanPrimary,
                        indicatorColor = HugoSlateCard,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Histórico"
                        )
                    },
                    label = { Text("Histórico", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HugoCyanPrimary,
                        selectedTextColor = HugoCyanPrimary,
                        indicatorColor = HugoSlateCard,
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Optional Wearable Clip hardware banner / drawer
            if (showClipModal) {
                WearableClipCard(
                    isConnected = uiState.isBleClipConnected,
                    batteryPercent = uiState.clipBatteryPercent,
                    voiceState = uiState.serviceState,
                    onToggleConnection = { viewModel.toggleBleClip() },
                    onSingleTap = { viewModel.clipButtonSingleTap() },
                    onDoubleTap = { viewModel.clipButtonDoubleTap() },
                    onLongPress = { viewModel.clipButtonLongPress() },
                    modifier = Modifier.padding(16.dp)
                )
            }

            when (selectedTab) {
                0 -> HomeScreen(
                    uiState = uiState,
                    todayReminders = reminders,
                    todayTasks = tasks,
                    onToggleHugoMode = {
                        onRequestPermissions()
                        viewModel.startHugoMode()
                    },
                    onPauseHugo = { viewModel.pauseHugo() },
                    onResumeHugo = { viewModel.resumeHugo() },
                    onStopHugo = { viewModel.stopHugoMode() },
                    onSendCommand = { viewModel.sendVoiceCommand(it) },
                    onConfirmSentinel = { viewModel.confirmSentinel() },
                    onCancelSentinel = { viewModel.cancelSentinel() },
                    onToggleTask = { viewModel.toggleTask(it) },
                    onToggleReminder = { viewModel.toggleReminder(it) },
                    onStartVoiceInput = onStartVoiceInput,
                    onSummarizeTranscript = { viewModel.summarizeCurrentTranscript() },
                    onAnalyzeTranscript = { viewModel.analyzeCurrentTranscript() },
                    onClearGeminiAnalysis = { viewModel.clearGeminiAnalysis() },
                    onSaveSummaryAsNote = { summary ->
                        viewModel.addNote("Resumo Gemini", summary)
                    }
                )
                1 -> MemoryScreen(
                    memories = memories,
                    notes = notes,
                    onAddMemory = { subject, value -> viewModel.addMemory(subject, value) },
                    onDeleteMemory = { viewModel.deleteMemory(it) },
                    onAddNote = { title, content -> viewModel.addNote(title, content) },
                    onDeleteNote = { viewModel.deleteNote(it) }
                )
                2 -> AgendaScreen(
                    reminders = reminders,
                    tasks = tasks,
                    onToggleReminder = { viewModel.toggleReminder(it) },
                    onDeleteReminder = { viewModel.deleteReminder(it) },
                    onAddReminder = { title, timeDesc -> viewModel.addReminder(title, timeDesc) },
                    onToggleTask = { viewModel.toggleTask(it) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onAddTask = { viewModel.addTask(it) }
                )
                3 -> HistoryScreen(
                    commandLogs = commandLogs,
                    geminiInteractions = geminiInteractions,
                    onClearHistory = { viewModel.clearCommandLogs() },
                    onClearGeminiInteractions = { viewModel.clearGeminiInteractions() },
                    onDeleteGeminiInteraction = { viewModel.deleteGeminiInteraction(it) },
                    onSaveToNotes = { title, content -> viewModel.addNote(title, content) }
                )
            }
        }
    }
}
