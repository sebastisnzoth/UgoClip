package com.ugo.clip.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ugo.clip.MainActivity
import com.ugo.clip.R
import com.ugo.clip.core.voice.HugoSpeechManager
import com.ugo.clip.core.voice.HugoTtsManager
import com.ugo.clip.core.voice.HugoVoiceState

class HugoVoiceService : Service() {

    private var speechManager: HugoSpeechManager? = null
    private var ttsManager: HugoTtsManager? = null
    private var isServiceRunning = false
    private var isPaused = false

    companion object {
        const val CHANNEL_ID = "hugo_voice_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.ugo.clip.service.ACTION_START"
        const val ACTION_PAUSE = "com.ugo.clip.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.ugo.clip.service.ACTION_RESUME"
        const val ACTION_STOP = "com.ugo.clip.service.ACTION_STOP"
        const val ACTION_LISTEN_ONCE = "com.ugo.clip.service.ACTION_LISTEN_ONCE"

        fun startService(context: Context) {
            val intent = Intent(context, HugoVoiceService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, HugoVoiceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        ttsManager = HugoTtsManager(this) { isSpeaking ->
            if (isSpeaking) {
                HugoServiceBridge.updateVoiceState(HugoVoiceState.SPEAKING)
                speechManager?.setMutedForTts(true)
            } else {
                if (!isPaused && isServiceRunning) {
                    HugoServiceBridge.updateVoiceState(HugoVoiceState.LISTENING)
                }
                speechManager?.setMutedForTts(false)
            }
            updateNotification()
        }

        speechManager = HugoSpeechManager(
            context = this,
            onTranscript = { transcript ->
                HugoServiceBridge.processCommand(transcript)
            },
            onPartialTranscript = { partial ->
                HugoServiceBridge.updateTranscript(partial)
            },
            onRmsChanged = { rms ->
                HugoServiceBridge.updateAudioLevel(rms)
            },
            onStateChange = { state ->
                HugoServiceBridge.updateVoiceState(state)
                updateNotification()
            },
            onErrorMsg = { error ->
                HugoServiceBridge.setErrorMessage(error)
            }
        )

        // Connect ServiceBridge callbacks
        HugoServiceBridge.onServicePauseRequested = {
            pauseListening()
        }
        HugoServiceBridge.onServiceResumeRequested = {
            resumeListening()
        }
        HugoServiceBridge.onServiceStopRequested = {
            stopSelf()
        }
        HugoServiceBridge.onServiceSpeakRequested = { text ->
            ttsManager?.speak(text)
        }
        HugoServiceBridge.onServiceListenOnceRequested = {
            speechManager?.startListening(continuous = false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                isServiceRunning = true
                isPaused = false
                speechManager?.startListening(continuous = true)
                HugoServiceBridge.updateVoiceState(HugoVoiceState.LISTENING)
            }
            ACTION_PAUSE -> {
                pauseListening()
            }
            ACTION_RESUME -> {
                resumeListening()
            }
            ACTION_STOP -> {
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_LISTEN_ONCE -> {
                speechManager?.startListening(continuous = false)
            }
        }
        return START_STICKY
    }

    private fun pauseListening() {
        isPaused = true
        speechManager?.pauseListening()
        HugoServiceBridge.updateVoiceState(HugoVoiceState.PAUSED)
        updateNotification()
    }

    private fun resumeListening() {
        isPaused = false
        speechManager?.resumeListening()
        HugoServiceBridge.updateVoiceState(HugoVoiceState.LISTENING)
        updateNotification()
    }

    private fun stopListening() {
        isServiceRunning = false
        isPaused = false
        speechManager?.stopListening()
        ttsManager?.stop()
        HugoServiceBridge.updateVoiceState(HugoVoiceState.OFF)
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        if (!isServiceRunning) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, HugoVoiceService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, HugoVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when {
            isPaused -> "Pausado • Toque para retomar"
            HugoServiceBridge.uiState.value.serviceState == HugoVoiceState.SPEAKING -> "Falando..."
            HugoServiceBridge.uiState.value.serviceState == HugoVoiceState.PROCESSING -> "Processando comando..."
            else -> "● Microfone ativo • Fale 'Hugo...'"
        }

        val pauseActionText = if (isPaused) "Retomar" else "Pausar"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("HUGO está escutando")
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, pauseActionText, pausePendingIntent)
            .addAction(0, "Encerrar", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sessão de Voz HUGO",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação persistente indicando que o microfone do HUGO está ativo."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        speechManager?.destroy()
        ttsManager?.shutdown()
        speechManager = null
        ttsManager = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
