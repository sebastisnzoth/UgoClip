package com.ugo.clip.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CalendarContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.ugo.clip.MainActivity
import com.ugo.clip.core.HugoAction
import com.ugo.clip.core.HugoCommandEngine
import com.ugo.clip.core.HugoDecision
import com.ugo.clip.data.HugoStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HugoVoiceService : Service(), RecognitionListener, TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private lateinit var store: HugoStore

    private var currentState = "DESATIVADO"
    private var paused = false
    private var speaking = false
    private var listening = false
    private var shuttingDown = false
    private var foregroundStarted = false
    private var ttsReady = false
    private var resumeAfterSpeech = true
    private var afterSpeech: (() -> Unit)? = null

    private val recognitionIntent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = HugoStore(this)
        createNotificationChannel()
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_STOP -> {
                if (!foregroundStarted) {
                    shutdownService()
                } else {
                    speak("Encerrando o Modo HUGO.", resume = false) { shutdownService() }
                }
                return START_NOT_STICKY
            }

            ACTION_PAUSE -> {
                pauseListening()
                return START_NOT_STICKY
            }

            ACTION_RESUME -> {
                if (!foregroundStarted) promoteToForeground()
                paused = false
                setState("ESCUTANDO", "Modo HUGO reativado.")
                startListening(150)
                return START_NOT_STICKY
            }

            else -> {
                promoteToForeground()
                paused = false
                setState("INICIANDO", "Preparando reconhecimento de voz…")
                initializeRecognizer()
                startListening(300)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promoteToForeground() {
        if (foregroundStarted) {
            updateNotification()
            return
        }
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
        foregroundStarted = true
        prefs().edit().putBoolean(PREF_RUNNING, true).apply()
    }

    private fun initializeRecognizer() {
        if (recognizer != null || shuttingDown) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            setState("ERRO", "Este Android não tem um serviço de reconhecimento de voz disponível.")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also {
            it.setRecognitionListener(this)
        }
    }

    private fun startListening(delayMs: Long = 0L) {
        mainHandler.postDelayed({
            if (shuttingDown || paused || speaking || listening) return@postDelayed
            initializeRecognizer()
            val target = recognizer ?: return@postDelayed
            runCatching {
                setState("ESCUTANDO", "Fale começando por “Hugo”.")
                target.startListening(recognitionIntent)
            }.onFailure {
                setState("RECUPERANDO", "Reconectando o reconhecimento de voz…")
                scheduleRecognizerReset(900)
            }
        }, delayMs)
    }

    private fun scheduleRecognizerReset(delayMs: Long) {
        mainHandler.postDelayed({
            if (shuttingDown || paused || speaking) return@postDelayed
            runCatching { recognizer?.destroy() }
            recognizer = null
            listening = false
            initializeRecognizer()
            startListening(250)
        }, delayMs)
    }

    private fun pauseListening() {
        paused = true
        listening = false
        runCatching { recognizer?.cancel() }
        setState("PAUSADO", "Microfone pausado. Toque em Retomar na notificação.")
    }

    private fun handleFinalTranscript(transcript: String) {
        broadcastStatus(transcript = transcript)
        val decision = HugoCommandEngine.parse(transcript)
        if (decision == null) {
            // Ignore ordinary conversation that did not begin with the wake word.
            startListening(250)
            return
        }
        execute(decision)
    }

    private fun execute(decision: HugoDecision) {
        when (val action = decision.action) {
            HugoAction.Pause -> {
                paused = true
                listening = false
                runCatching { recognizer?.cancel() }
                setState("PAUSADO", decision.spoken ?: "Modo HUGO pausado.")
                speak(decision.spoken ?: "Modo HUGO pausado.", resume = false)
            }

            HugoAction.Stop -> {
                speak(decision.spoken ?: "Encerrando o Modo HUGO.", resume = false) {
                    shutdownService()
                }
            }

            is HugoAction.OpenApp -> {
                val opened = openKnownApp(action.alias)
                val message = if (opened) "Abrindo ${action.alias}." else "Não encontrei ${action.alias} neste celular."
                speak(message)
            }

            is HugoAction.OpenMaps -> {
                val opened = openMaps(action.query)
                val message = if (opened) {
                    action.query?.let { "Abrindo o mapa para $it." } ?: "Abrindo o Maps."
                } else {
                    "Não consegui abrir o mapa."
                }
                speak(message)
            }

            is HugoAction.SaveNote -> {
                store.addNote(action.text)
                speak(decision.spoken ?: "Anotado.")
            }

            is HugoAction.RememberLocation -> {
                store.rememberLocation(action.subject, action.location)
                speak(decision.spoken ?: "Certo. Vou lembrar.")
            }

            is HugoAction.FindLocation -> {
                val location = store.findLocation(action.subject)
                val message = if (location != null) {
                    "Você me disse que ${action.subject} está em $location."
                } else {
                    "Ainda não tenho salvo onde está ${action.subject}."
                }
                speak(message)
            }

            is HugoAction.PrepareWhatsApp -> {
                val opened = prepareWhatsApp(action.text)
                val message = if (opened) {
                    decision.spoken ?: "Abri o WhatsApp com a mensagem preparada. Confira o contato antes de enviar."
                } else {
                    "Não consegui abrir o WhatsApp."
                }
                speak(message)
            }

            is HugoAction.AddAgenda -> {
                store.addAgenda(action.title, action.startsAt)
                val opened = openCalendarInsert(action.title, action.startsAt)
                val message = if (opened) {
                    decision.spoken ?: "Compromisso preparado no calendário."
                } else {
                    "Guardei o compromisso na agenda do HUGO, mas não consegui abrir o calendário."
                }
                speak(message)
            }

            is HugoAction.ReadAgenda -> {
                val date = LocalDate.now().plusDays(action.dayOffset)
                val items = store.agendaFor(date)
                val dayName = if (action.dayOffset == 1L) "amanhã" else "hoje"
                val message = if (items.isEmpty()) {
                    "Não tenho compromissos salvos para $dayName."
                } else {
                    val formatter = DateTimeFormatter.ofPattern("HH:mm")
                    val summary = items.joinToString("; ") { item ->
                        val time = Instant.ofEpochMilli(item.startsAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .format(formatter)
                        "$time, ${item.title}"
                    }
                    "Para $dayName você tem: $summary."
                }
                speak(message)
            }

            HugoAction.None -> {
                speak(decision.spoken ?: "Estou ouvindo.")
            }
        }
    }

    private fun openKnownApp(alias: String): Boolean {
        val packageName = when (alias) {
            "youtube" -> "com.google.android.youtube"
            "whatsapp" -> "com.whatsapp"
            "spotify" -> "com.spotify.music"
            "instagram" -> "com.instagram.android"
            "chrome" -> "com.android.chrome"
            else -> return false
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching { startActivity(launchIntent); true }.getOrDefault(false)
        }

        val fallback = when (alias) {
            "youtube" -> "https://www.youtube.com"
            "whatsapp" -> "https://www.whatsapp.com"
            "spotify" -> "https://open.spotify.com"
            "instagram" -> "https://www.instagram.com"
            else -> null
        } ?: return false

        return openUri(Uri.parse(fallback))
    }

    private fun openMaps(query: String?): Boolean {
        val uri = if (query.isNullOrBlank()) {
            Uri.parse("geo:0,0?q=")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        }
        val mapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { startActivity(mapsIntent); true }.getOrDefault(false)) return true

        val web = if (query.isNullOrBlank()) {
            Uri.parse("https://maps.google.com")
        } else {
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
        }
        return openUri(web)
    }

    private fun prepareWhatsApp(text: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { startActivity(intent); true }.getOrDefault(false)
    }

    private fun openCalendarInsert(title: String, startsAt: Long): Boolean {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startsAt)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startsAt + 60 * 60 * 1000L)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { startActivity(intent); true }.getOrDefault(false)
    }

    private fun openUri(uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { startActivity(intent); true }.getOrDefault(false)
    }

    private fun speak(text: String, resume: Boolean = true, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) {
            onDone?.invoke() ?: if (resume) startListening(200) else Unit
            return
        }

        broadcastStatus(message = text)
        listening = false
        runCatching { recognizer?.cancel() }

        if (!ttsReady || textToSpeech == null) {
            onDone?.invoke() ?: if (resume) startListening(350) else Unit
            return
        }

        speaking = true
        resumeAfterSpeech = resume
        afterSpeech = onDone
        setState("FALANDO", text)
        val utteranceId = "hugo-${System.nanoTime()}"
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            ttsReady = false
            broadcastStatus(message = "Resposta por voz indisponível; reconhecimento continua ativo.")
            return
        }
        val engine = textToSpeech ?: return
        val languageResult = engine.setLanguage(Locale("pt", "BR"))
        ttsReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    speaking = false
                    val callback = afterSpeech
                    afterSpeech = null
                    if (callback != null) {
                        callback.invoke()
                    } else if (resumeAfterSpeech && !paused && !shuttingDown) {
                        startListening(250)
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onDone(utteranceId)
            }
        })
    }

    override fun onReadyForSpeech(params: Bundle?) {
        listening = true
        setState("ESCUTANDO", "Fale começando por “Hugo”.")
    }

    override fun onBeginningOfSpeech() {
        setState("OUVINDO", "Estou ouvindo…")
    }

    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        listening = false
        setState("PROCESSANDO", "Entendendo o comando…")
    }

    override fun onError(error: Int) {
        listening = false
        if (shuttingDown || paused || speaking) return

        val delay = when (error) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1200L
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 1800L
            else -> 500L
        }

        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED) {
            scheduleRecognizerReset(delay)
        } else {
            startListening(delay)
        }
    }

    override fun onResults(results: Bundle?) {
        listening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val transcript = matches?.firstOrNull()?.trim().orEmpty()
        if (transcript.isBlank()) {
            startListening(350)
            return
        }
        handleFinalTranscript(transcript)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partial = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (partial.isNotBlank()) broadcastStatus(transcript = partial)
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun setState(state: String, message: String? = null) {
        currentState = state
        prefs().edit()
            .putBoolean(PREF_RUNNING, state != "ENCERRADO" && state != "DESATIVADO" && state != "ERRO")
            .putString(PREF_STATE, state)
            .apply()
        broadcastStatus(state = state, message = message)
        if (foregroundStarted && !shuttingDown) updateNotification()
    }

    private fun broadcastStatus(
        state: String = currentState,
        transcript: String? = null,
        message: String? = null
    ) {
        sendBroadcast(Intent(ACTION_STATUS).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATE, state)
            transcript?.let { putExtra(EXTRA_TRANSCRIPT, it) }
            message?.let { putExtra(EXTRA_MESSAGE, it) }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Modo HUGO",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mostra quando HUGO está usando o microfone."
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stop = PendingIntent.getService(
            this,
            2,
            Intent(this, HugoVoiceService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseOrResumeAction = if (paused) ACTION_RESUME else ACTION_PAUSE
        val pauseOrResume = PendingIntent.getService(
            this,
            3,
            Intent(this, HugoVoiceService::class.java).setAction(pauseOrResumeAction),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stateLabel = when {
            paused -> "Microfone pausado"
            speaking -> "HUGO está respondendo"
            else -> "Microfone ativo · diga “Hugo…”"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("HUGO Clip · $currentState")
            .setContentText(stateLabel)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (paused) "Retomar" else "Pausar",
                pauseOrResume
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Encerrar", stop)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun shutdownService() {
        if (shuttingDown) return
        shuttingDown = true
        paused = true
        listening = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
        runCatching { textToSpeech?.stop() }
        runCatching { textToSpeech?.shutdown() }
        textToSpeech = null
        prefs().edit()
            .putBoolean(PREF_RUNNING, false)
            .putString(PREF_STATE, "ENCERRADO")
            .apply()
        currentState = "ENCERRADO"
        broadcastStatus(message = "Modo HUGO encerrado.")
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    override fun onDestroy() {
        if (!shuttingDown) {
            shuttingDown = true
            mainHandler.removeCallbacksAndMessages(null)
            runCatching { recognizer?.destroy() }
            runCatching { textToSpeech?.shutdown() }
            prefs().edit()
                .putBoolean(PREF_RUNNING, false)
                .putString(PREF_STATE, "ENCERRADO")
                .apply()
        }
        super.onDestroy()
    }

    private fun prefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val ACTION_START = "com.ugo.clip.action.START"
        const val ACTION_STOP = "com.ugo.clip.action.STOP"
        const val ACTION_PAUSE = "com.ugo.clip.action.PAUSE"
        const val ACTION_RESUME = "com.ugo.clip.action.RESUME"
        const val ACTION_STATUS = "com.ugo.clip.action.STATUS"

        const val EXTRA_STATE = "state"
        const val EXTRA_TRANSCRIPT = "transcript"
        const val EXTRA_MESSAGE = "message"

        const val PREFS_NAME = "hugo_service"
        const val PREF_RUNNING = "running"
        const val PREF_STATE = "state"

        private const val CHANNEL_ID = "hugo_voice_mode"
        private const val NOTIFICATION_ID = 1101
    }
}
