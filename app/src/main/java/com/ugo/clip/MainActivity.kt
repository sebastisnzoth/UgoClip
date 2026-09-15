package com.ugo.clip

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ugo.clip.voice.HugoVoiceService

class MainActivity : ComponentActivity() {

    private var serviceState by mutableStateOf("DESATIVADO")
    private var lastTranscript by mutableStateOf("Ainda não ouvi nenhum comando.")
    private var infoMessage by mutableStateOf("Ative o Modo HUGO e fale normalmente.")
    private var isRunning by mutableStateOf(false)

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != HugoVoiceService.ACTION_STATUS) return
            intent.getStringExtra(HugoVoiceService.EXTRA_STATE)?.let { state ->
                serviceState = state
                isRunning = state != "DESATIVADO" && state != "ENCERRADO" && state != "ERRO"
            }
            intent.getStringExtra(HugoVoiceService.EXTRA_TRANSCRIPT)?.let {
                if (it.isNotBlank()) lastTranscript = it
            }
            intent.getStringExtra(HugoVoiceService.EXTRA_MESSAGE)?.let {
                if (it.isNotBlank()) infoMessage = it
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val audioGranted = result[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (audioGranted) {
            startHugo()
        } else {
            infoMessage = "HUGO precisa da permissão de microfone para escutar seus comandos."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "HUGO",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Seu secretário de voz",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("ESTADO", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    serviceState,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(infoMessage)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { ensurePermissionsAndStart() },
                                enabled = !isRunning,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ativar HUGO")
                            }
                            OutlinedButton(
                                onClick = { stopHugo() },
                                enabled = isRunning,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Encerrar")
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("ÚLTIMO COMANDO", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(8.dp))
                                Text(lastTranscript, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        Text(
                            "Teste agora",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("• Hugo, abre o YouTube")
                        Text("• Hugo, abre o Maps para Canasvieiras")
                        Text("• Hugo, anota comprar dois disjuntores")
                        Text("• Hugo, lembra que deixei a furadeira no armário azul")
                        Text("• Hugo, onde deixei a furadeira?")
                        Text("• Hugo, prepara uma mensagem dizendo que chego às oito")
                        Text("• Hugo, lembra de comprar pão amanhã às 9")
                        Text("• Hugo, o que tenho amanhã?")
                        Text("• Hugo, pausa")
                        Text("• Hugo, encerra")

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "Privacidade",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Quando o Modo HUGO está ativo, o Android mantém uma notificação visível indicando o uso do microfone. O MVP não grava arquivos de áudio."
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(HugoVoiceService.ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(statusReceiver, filter)
        }

        val prefs = getSharedPreferences(HugoVoiceService.PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(HugoVoiceService.PREF_RUNNING, false)) {
            serviceState = prefs.getString(HugoVoiceService.PREF_STATE, "ESCUTANDO") ?: "ESCUTANDO"
            isRunning = true
        }
    }

    override fun onStop() {
        runCatching { unregisterReceiver(statusReceiver) }
        super.onStop()
    }

    private fun ensurePermissionsAndStart() {
        val audioGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (audioGranted) {
            startHugo()
            return
        }

        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startHugo() {
        val intent = Intent(this, HugoVoiceService::class.java).apply {
            action = HugoVoiceService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
        serviceState = "INICIANDO"
        isRunning = true
        infoMessage = "Iniciando microfone e reconhecimento de voz…"
    }

    private fun stopHugo() {
        startService(Intent(this, HugoVoiceService::class.java).apply {
            action = HugoVoiceService.ACTION_STOP
        })
    }
}
