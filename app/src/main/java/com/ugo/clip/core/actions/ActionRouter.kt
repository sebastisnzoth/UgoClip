package com.ugo.clip.core.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ugo.clip.core.ai.GeminiAnalysis
import com.ugo.clip.core.ai.GeminiResult
import com.ugo.clip.core.ai.HugoGeminiClient
import com.ugo.clip.core.ai.OrchestratedIntent
import com.ugo.clip.data.repository.HugoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ExecutionResult(
    val success: Boolean,
    val vocalResponse: String,
    val statusText: String,
    val geminiSummary: String? = null,
    val geminiAnalysis: GeminiAnalysis? = null
)

class ActionRouter(
    private val context: Context,
    private val repository: HugoRepository
) {

    suspend fun execute(intent: OrchestratedIntent): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            when (intent.intentName) {
                "OPEN_APP" -> handleOpenApp(intent.parameters["app"].orEmpty())
                "OPEN_MAPS" -> handleOpenMaps(intent.parameters["destination"].orEmpty())
                "CREATE_REMINDER" -> handleCreateReminder(
                    intent.parameters["title"].orEmpty(),
                    intent.parameters["time"] ?: "Hoje"
                )
                "CREATE_NOTE" -> handleCreateNote(
                    intent.parameters["title"] ?: "Anotação",
                    intent.parameters["content"].orEmpty()
                )
                "STORE_MEMORY" -> handleStoreMemory(
                    intent.parameters["subject"].orEmpty(),
                    intent.parameters["value"].orEmpty()
                )
                "QUERY_MEMORY" -> handleQueryMemory(intent.parameters["subject"].orEmpty())
                "QUERY_AGENDA" -> handleQueryAgenda(intent.parameters["scope"] ?: "today")
                "PREPARE_MESSAGE" -> handlePrepareMessage(
                    intent.parameters["recipient"].orEmpty(),
                    intent.parameters["message"].orEmpty()
                )
                "INITIATE_CALL" -> handleInitiateCall(intent.parameters["contact"].orEmpty())
                "SUMMARIZE_TRANSCRIPT" -> handleSummarizeTranscript(
                    intent.parameters["content"].orEmpty(),
                    intent.parameters["rawInput"].orEmpty()
                )
                "ANALYZE_TRANSCRIPT" -> handleAnalyzeTranscript(
                    intent.parameters["content"].orEmpty(),
                    intent.parameters["rawInput"].orEmpty()
                )
                "GENERAL_QUERY" -> handleGeneralQuery(
                    intent.parameters["query"].orEmpty(),
                    intent.voiceReply
                )
                "SYSTEM_PAUSE", "SYSTEM_RESUME", "SYSTEM_STOP" -> {
                    ExecutionResult(true, intent.voiceReply, intent.userFriendlySummary)
                }
                else -> {
                    ExecutionResult(true, intent.voiceReply, intent.userFriendlySummary)
                }
            }
        } catch (e: Exception) {
            ExecutionResult(
                success = false,
                vocalResponse = "Ocorreu uma falha ao executar a ação: ${e.localizedMessage ?: "Erro desconhecido"}",
                statusText = "Erro: ${e.message}"
            )
        }
    }

    private fun handleOpenApp(appName: String): ExecutionResult {
        val target = appName.lowercase().trim()
        val pm = context.packageManager

        // Known common mappings
        val intentToLaunch = when {
            target.contains("youtube") -> {
                val launchIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
                launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            }
            target.contains("whatsapp") -> {
                val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com"))
            }
            target.contains("spotify") -> {
                val launchIntent = pm.getLaunchIntentForPackage("com.spotify.music")
                launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
            }
            else -> {
                // Try finding matching package
                val installed = pm.getInstalledApplications(0)
                val matchedApp = installed.firstOrNull {
                    it.loadLabel(pm).toString().lowercase().contains(target)
                }
                if (matchedApp != null) {
                    pm.getLaunchIntentForPackage(matchedApp.packageName)
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$target"))
                }
            }
        }

        intentToLaunch?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(this)
        }

        return ExecutionResult(
            success = true,
            vocalResponse = "Abrindo $appName.",
            statusText = "Aplicativo $appName aberto"
        )
    }

    private fun handleOpenMaps(destination: String): ExecutionResult {
        val uri = if (destination.isNotBlank()) {
            Uri.parse("geo:0,0?q=" + Uri.encode(destination))
        } else {
            Uri.parse("geo:0,0?q=maps")
        }
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val webUri = if (destination.isNotBlank()) {
                Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(destination))
            } else {
                Uri.parse("https://maps.google.com")
            }
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }

        val speech = if (destination.isNotBlank()) "Abrindo navegação no Maps para $destination." else "Abrindo o Maps."
        return ExecutionResult(true, speech, "Maps aberto")
    }

    private suspend fun handleCreateReminder(title: String, time: String): ExecutionResult {
        repository.insertReminder(title = title, timeDescription = time)
        return ExecutionResult(
            success = true,
            vocalResponse = "Lembrete criado: $title.",
            statusText = "Lembrete salvo com sucesso"
        )
    }

    private suspend fun handleCreateNote(title: String, content: String): ExecutionResult {
        repository.insertNote(title = title, content = content)
        return ExecutionResult(
            success = true,
            vocalResponse = "Anotado: $content.",
            statusText = "Nota registrada na memória"
        )
    }

    private suspend fun handleStoreMemory(subject: String, value: String): ExecutionResult {
        repository.saveMemory(subject, value, category = "OBJECT_LOCATION")
        return ExecutionResult(
            success = true,
            vocalResponse = "Entendido. Guardei que você deixou $subject $value.",
            statusText = "Memória gravada: $subject -> $value"
        )
    }

    private suspend fun handleQueryMemory(subject: String): ExecutionResult {
        val results = repository.searchMemory(subject)
        return if (results.isNotEmpty()) {
            val item = results.first()
            val text = "Você me disse que deixou ${item.subject} ${item.value}."
            ExecutionResult(true, text, text)
        } else {
            val text = "Ainda não encontrei registros sobre '$subject' na sua memória."
            ExecutionResult(true, text, text)
        }
    }

    private suspend fun handleQueryAgenda(scope: String): ExecutionResult {
        val reminders = repository.reminders.first()
        val tasks = repository.tasks.first()
        val pendingReminders = reminders.filter { !it.isDone }
        val pendingTasks = tasks.filter { !it.isCompleted }

        val builder = StringBuilder()
        val isToday = scope == "today"
        val dayLabel = if (isToday) "hoje" else "amanhã"

        if (pendingReminders.isEmpty() && pendingTasks.isEmpty()) {
            builder.append("Você não tem compromissos ou tarefas pendentes para $dayLabel.")
        } else {
            builder.append("Para $dayLabel você tem: ")
            if (pendingReminders.isNotEmpty()) {
                builder.append(pendingReminders.take(3).joinToString(", ") { "${it.title} (${it.timeDescription})" })
            }
            if (pendingTasks.isNotEmpty()) {
                if (pendingReminders.isNotEmpty()) builder.append(". E as tarefas: ")
                builder.append(pendingTasks.take(3).joinToString(", ") { it.title })
            }
            builder.append(".")
        }

        val speech = builder.toString()
        return ExecutionResult(true, speech, speech)
    }

    fun handlePrepareMessage(recipient: String, message: String): ExecutionResult {
        val encodedMessage = Uri.encode(message)
        val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(sendIntent, "Enviar mensagem via").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
        return ExecutionResult(
            success = true,
            vocalResponse = "Mensagem para $recipient preparada.",
            statusText = "Mensagem aberta no WhatsApp/SMS"
        )
    }

    fun handleInitiateCall(contact: String): ExecutionResult {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(dialIntent)
        } catch (_: Exception) {}

        return ExecutionResult(
            success = true,
            vocalResponse = "Discador aberto para ligar para $contact.",
            statusText = "Discador telefônico acionado"
        )
    }

    private suspend fun handleSummarizeTranscript(content: String, rawInput: String): ExecutionResult {
        val targetText = if (content.isNotBlank()) {
            content
        } else {
            val latestNote = repository.notes.first().firstOrNull()?.content
            val latestLog = repository.commandLogs.first().firstOrNull()?.transcript
            latestNote ?: latestLog ?: rawInput
        }

        val result = HugoGeminiClient.summarizeTranscript(targetText)
        return when (result) {
            is GeminiResult.Success -> {
                val vocal = "Resumo concluído: " + result.text.take(160).replace("\n", " ")
                repository.logGeminiInteraction(
                    transcribedText = targetText,
                    aiResponse = result.text,
                    interactionType = "SUMMARY",
                    modelName = result.model,
                    isRealAi = result.model.contains("gemini")
                )
                ExecutionResult(
                    success = true,
                    vocalResponse = vocal,
                    statusText = "Resumo gerado com Gemini 3.5 Flash",
                    geminiSummary = result.text
                )
            }
            is GeminiResult.Error -> {
                ExecutionResult(
                    success = false,
                    vocalResponse = "Não foi possível gerar o resumo: ${result.message}",
                    statusText = result.message
                )
            }
        }
    }

    private suspend fun handleAnalyzeTranscript(content: String, rawInput: String): ExecutionResult {
        val targetText = if (content.isNotBlank()) {
            content
        } else {
            val latestNote = repository.notes.first().firstOrNull()?.content
            val latestLog = repository.commandLogs.first().firstOrNull()?.transcript
            latestNote ?: latestLog ?: rawInput
        }

        val analysis = HugoGeminiClient.analyzeTranscription(targetText)
        val vocal = "Análise do Gemini: " + analysis.summary.take(160).replace("\n", " ")
        repository.logGeminiInteraction(
            transcribedText = targetText,
            aiResponse = analysis.intelligentResponse.ifBlank { analysis.summary },
            interactionType = "ANALYSIS",
            keyPoints = analysis.keyPoints,
            actionItems = analysis.actionableItems,
            modelName = if (analysis.isRealAi) "gemini-3.5-flash" else "local-heuristic",
            isRealAi = analysis.isRealAi
        )
        return ExecutionResult(
            success = true,
            vocalResponse = vocal,
            statusText = "Análise semântica gerada com Gemini 3.5 Flash",
            geminiSummary = analysis.summary,
            geminiAnalysis = analysis
        )
    }

    private suspend fun handleGeneralQuery(query: String, defaultReply: String): ExecutionResult {
        val lower = query.lowercase().trim()
        if (lower.contains("horas") || lower.contains("que horas são") || lower.contains("obrigado") || lower.contains("valeu")) {
            return ExecutionResult(true, defaultReply, query)
        }

        val geminiResult = HugoGeminiClient.generateIntelligentResponse(query)
        return when (geminiResult) {
            is GeminiResult.Success -> {
                repository.logGeminiInteraction(
                    transcribedText = query,
                    aiResponse = geminiResult.text,
                    interactionType = "QUERY",
                    modelName = geminiResult.model,
                    isRealAi = geminiResult.model.contains("gemini")
                )
                ExecutionResult(
                    success = true,
                    vocalResponse = geminiResult.text,
                    statusText = "Resposta inteligente (Gemini 3.5 Flash)",
                    geminiSummary = geminiResult.text
                )
            }
            is GeminiResult.Error -> {
                ExecutionResult(
                    success = true,
                    vocalResponse = defaultReply,
                    statusText = "Processado localmente ($query)"
                )
            }
        }
    }
}
