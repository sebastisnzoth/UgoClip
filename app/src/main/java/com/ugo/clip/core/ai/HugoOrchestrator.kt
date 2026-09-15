package com.ugo.clip.core.ai

data class OrchestratedIntent(
    val intentName: String,
    val parameters: Map<String, String>,
    val userFriendlySummary: String,
    val voiceReply: String
)

object HugoOrchestrator {

    /**
     * Parses the spoken or typed Portuguese command into structured intent and parameters.
     */
    fun parse(rawInput: String): OrchestratedIntent {
        val cleanInput = rawInput.trim()
        // Strip wake-phrase if present ("Hugo,", "Hugo", "Ei Hugo", "Ok Hugo")
        var text = cleanInput
        val prefixes = listOf("ei hugo", "ok hugo", "hugo,", "hugo", "ugo")
        for (prefix in prefixes) {
            if (text.lowercase().startsWith(prefix)) {
                text = text.substring(prefix.length).trim().removePrefix(",").trim()
                break
            }
        }
        val lower = text.lowercase()

        // 1. System controls
        if (lower.startsWith("pausa") || lower == "para" || lower == "parar" || lower == "pausar") {
            return OrchestratedIntent(
                intentName = "SYSTEM_PAUSE",
                parameters = emptyMap(),
                userFriendlySummary = "Pausar escuta ativa do HUGO",
                voiceReply = "Modo de escuta pausado. Diga 'Hugo, volta a escutar' para reativar."
            )
        }
        if (lower.contains("volta a escutar") || lower.contains("retoma") || lower.contains("continuar")) {
            return OrchestratedIntent(
                intentName = "SYSTEM_RESUME",
                parameters = emptyMap(),
                userFriendlySummary = "Retomar escuta ativa do HUGO",
                voiceReply = "Escutando novamente."
            )
        }
        if (lower.startsWith("encerra") || lower.startsWith("encerrar") || lower.startsWith("desligar") || lower.startsWith("desativar")) {
            return OrchestratedIntent(
                intentName = "SYSTEM_STOP",
                parameters = emptyMap(),
                userFriendlySummary = "Encerrar Modo HUGO",
                voiceReply = "Encerrando modo HUGO. Até logo."
            )
        }

        // 2. Open apps / YouTube / Maps
        if (lower.startsWith("abre o ") || lower.startsWith("abrir o ") || lower.startsWith("abre ") || lower.startsWith("abrir ")) {
            val target = lower.removePrefix("abrir o ")
                .removePrefix("abre o ")
                .removePrefix("abrir ")
                .removePrefix("abre ")
                .trim()

            if (target.startsWith("maps") || target.startsWith("mapa")) {
                val destination = if (text.contains(" para ", ignoreCase = true)) {
                    text.substring(text.indexOf(" para ", ignoreCase = true) + 6).trim()
                } else if (target.contains("para ")) {
                    target.substringAfter("para ").trim()
                } else {
                    ""
                }
                return OrchestratedIntent(
                    intentName = "OPEN_MAPS",
                    parameters = mapOf("destination" to destination),
                    userFriendlySummary = if (destination.isNotBlank()) "Abrir Maps para $destination" else "Abrir Google Maps",
                    voiceReply = if (destination.isNotBlank()) "Abrindo o Maps para $destination." else "Abrindo o Maps."
                )
            }

            if (target.contains("youtube")) {
                return OrchestratedIntent(
                    intentName = "OPEN_APP",
                    parameters = mapOf("app" to "youtube"),
                    userFriendlySummary = "Abrir YouTube",
                    voiceReply = "Abrindo o YouTube."
                )
            }
            if (target.contains("whatsapp")) {
                return OrchestratedIntent(
                    intentName = "OPEN_APP",
                    parameters = mapOf("app" to "whatsapp"),
                    userFriendlySummary = "Abrir WhatsApp",
                    voiceReply = "Abrindo o WhatsApp."
                )
            }
            if (target.contains("spotify") || target.contains("música")) {
                return OrchestratedIntent(
                    intentName = "OPEN_APP",
                    parameters = mapOf("app" to "spotify"),
                    userFriendlySummary = "Abrir Spotify",
                    voiceReply = "Abrindo o reprodutor de música."
                )
            }

            return OrchestratedIntent(
                intentName = "OPEN_APP",
                parameters = mapOf("app" to target),
                userFriendlySummary = "Abrir $target",
                voiceReply = "Abrindo $target."
            )
        }

        // 3. Memory recall: "onde deixei a furadeira?" / "onde está a furadeira?"
        if (lower.startsWith("onde deixei ") || lower.startsWith("onde guardei ") || lower.startsWith("onde está ") || lower.startsWith("onde fica ")) {
            val subject = lower.removePrefix("onde deixei ")
                .removePrefix("onde guardei ")
                .removePrefix("onde está ")
                .removePrefix("onde fica ")
                .removeSuffix("?")
                .removePrefix("a ")
                .removePrefix("o ")
                .trim()
            return OrchestratedIntent(
                intentName = "QUERY_MEMORY",
                parameters = mapOf("subject" to subject),
                userFriendlySummary = "Buscar localização de: $subject",
                voiceReply = "Buscando localização de $subject na memória."
            )
        }

        // 4. Memory storage: "lembra que deixei a furadeira no armário azul"
        if (lower.contains("lembra que deixei ") || lower.contains("lembra que guardei ") || lower.contains("deixei a ") || lower.contains("deixei o ") || lower.contains("guardei a ") || lower.contains("guardei o ")) {
            // parse subject and location
            var cleanFact = lower
            if (cleanFact.contains("lembra que ")) {
                cleanFact = cleanFact.substringAfter("lembra que ")
            }
            if (cleanFact.startsWith("deixei ") || cleanFact.startsWith("guardei ")) {
                cleanFact = cleanFact.removePrefix("deixei ").removePrefix("guardei ").trim()
            }
            // split by "no " or "na " or "em "
            val delimiters = listOf(" no ", " na ", " em ", " nos ", " nas ", " sobre o ", " sobre a ")
            var subject = cleanFact
            var location = "local registrado"
            for (delim in delimiters) {
                if (cleanFact.contains(delim)) {
                    val parts = cleanFact.split(delim, limit = 2)
                    subject = parts[0].removePrefix("a ").removePrefix("o ").trim()
                    location = (delim.trim() + " " + parts[1]).trim()
                    break
                }
            }

            return OrchestratedIntent(
                intentName = "STORE_MEMORY",
                parameters = mapOf("subject" to subject, "value" to location, "category" to "OBJECT_LOCATION"),
                userFriendlySummary = "Memorizar: $subject -> $location",
                voiceReply = "Entendido. Guardei que você deixou $subject $location."
            )
        }

        // 5. Query agenda: "o que eu tenho hoje?" / "o que tenho amanhã?" / "próximos compromissos"
        if (lower.contains("o que eu tenho") || lower.contains("o que tenho") || lower.contains("meus compromissos") || lower.contains("minha agenda") || lower.contains("agenda de hoje")) {
            val scope = if (lower.contains("amanhã")) "tomorrow" else "today"
            val label = if (scope == "tomorrow") "amanhã" else "hoje"
            return OrchestratedIntent(
                intentName = "QUERY_AGENDA",
                parameters = mapOf("scope" to scope),
                userFriendlySummary = "Consultar compromissos de $label",
                voiceReply = "Consultando seus compromissos para $label."
            )
        }

        // 6. Messaging: "prepara uma mensagem para Ariel: chego às oito"
        if (lower.contains("prepara uma mensagem") || lower.contains("enviar mensagem") || lower.contains("manda mensagem") || lower.contains("mensagem para")) {
            // parse recipient and message
            var targetRecipient = "Contato"
            var messageContent = "Olá"

            val paraIndex = text.indexOf(" para ", ignoreCase = true)
            if (paraIndex != -1) {
                val afterPara = text.substring(paraIndex + 6).trim()
                if (afterPara.contains(":") || afterPara.contains("dizendo que ") || afterPara.contains("falando que ")) {
                    val splitWord = when {
                        afterPara.contains(":") -> ":"
                        afterPara.contains("dizendo que ") -> "dizendo que "
                        else -> "falando que "
                    }
                    val parts = afterPara.split(splitWord, limit = 2)
                    targetRecipient = parts[0].trim()
                    messageContent = parts[1].trim()
                } else {
                    targetRecipient = afterPara
                }
            }

            return OrchestratedIntent(
                intentName = "PREPARE_MESSAGE",
                parameters = mapOf("recipient" to targetRecipient, "message" to messageContent),
                userFriendlySummary = "Mensagem para $targetRecipient: \"$messageContent\"",
                voiceReply = "Preparando mensagem para $targetRecipient: '$messageContent'. Confirmar envio?"
            )
        }

        // 7. Phone call: "liga para João" / "fazer ligação para João"
        if (lower.startsWith("liga para ") || lower.startsWith("ligar para ") || lower.startsWith("inicia ligação para ") || lower.startsWith("chama o ")) {
            val contact = text.removePrefix("liga para ")
                .removePrefix("ligar para ")
                .removePrefix("inicia ligação para ")
                .removePrefix("chama o ")
                .trim()
            return OrchestratedIntent(
                intentName = "INITIATE_CALL",
                parameters = mapOf("contact" to contact),
                userFriendlySummary = "Iniciar ligação para $contact",
                voiceReply = "Iniciar ligação para $contact. Confirmar?"
            )
        }

        // 8. Reminders: "lembra de cobrar o João quinta às nove" / "lembra de comprar pão amanhã às nove"
        if (lower.startsWith("lembra de ") || lower.startsWith("lembrete de ") || lower.startsWith("me lembra de ")) {
            val reminderBody = text.removePrefix("me lembra de ")
                .removePrefix("lembra de ")
                .removePrefix("lembrete de ")
                .trim()
            return OrchestratedIntent(
                intentName = "CREATE_REMINDER",
                parameters = mapOf("title" to reminderBody, "time" to "Em breve"),
                userFriendlySummary = "Criar lembrete: $reminderBody",
                voiceReply = "Lembrete criado: $reminderBody."
            )
        }

        // 9. Notes & Tasks: "anota comprar dois disjuntores" / "anota: ..." / "guarda esta ideia" / "adiciona tarefa..."
        if (lower.startsWith("anota") || lower.startsWith("anotar") || lower.startsWith("guarda esta ideia") || lower.startsWith("tarefa ")) {
            var noteBody = text.removePrefix("anotar:")
                .removePrefix("anota:")
                .removePrefix("anotar")
                .removePrefix("anota")
                .removePrefix("guarda esta ideia:")
                .removePrefix("guarda esta ideia")
                .removePrefix("tarefa")
                .trim()
                .removePrefix(":")
                .trim()
            if (noteBody.isBlank()) noteBody = "Nova anotação"
            return OrchestratedIntent(
                intentName = "CREATE_NOTE",
                parameters = mapOf("content" to noteBody, "title" to noteBody.take(28)),
                userFriendlySummary = "Salvar anotação: $noteBody",
                voiceReply = "Anotado: $noteBody."
            )
        }

        // 10. Summarization with Gemini: "resume o que eu falei", "resume isso", "resuma: ..."
        if (lower.startsWith("resume ") || lower.startsWith("resuma ") || lower.startsWith("faz um resumo") || lower == "resume" || lower == "resuma" || lower.contains("resumir o que eu falei") || lower.contains("resume o que eu falei") || lower.contains("resume o que eu disse") || lower.contains("resumo da") || lower == "resumo") {
            val contentToSummarize = when {
                lower.startsWith("resume:") || lower.startsWith("resuma:") -> text.substringAfter(":").trim()
                lower.startsWith("resume ") -> text.substringAfter("resume ").trim()
                lower.startsWith("resuma ") -> text.substringAfter("resuma ").trim()
                lower.startsWith("faz um resumo de ") -> text.substringAfter("faz um resumo de ").trim()
                lower.startsWith("faz um resumo sobre ") -> text.substringAfter("faz um resumo sobre ").trim()
                lower.startsWith("faz um resumo:") -> text.substringAfter(":").trim()
                else -> ""
            }
            return OrchestratedIntent(
                intentName = "SUMMARIZE_TRANSCRIPT",
                parameters = mapOf("content" to contentToSummarize, "rawInput" to rawInput),
                userFriendlySummary = "Resumir com Gemini 3.5 Flash: ${if (contentToSummarize.isNotBlank()) contentToSummarize else "transcrição atual"}",
                voiceReply = "Sintetizando resumo com inteligência Gemini."
            )
        }

        // 11. Analysis with Gemini: "analisa o que eu falei", "analisa isso", "o que você acha de..."
        if (lower.startsWith("analisa ") || lower.startsWith("analise ") || lower.startsWith("analisar ") || lower.contains("analisa o que eu falei") || lower.contains("analisa o que eu disse") || lower.contains("o que você acha de") || lower == "analisa" || lower == "analise") {
            val contentToAnalyze = when {
                lower.startsWith("analisa:") || lower.startsWith("analise:") -> text.substringAfter(":").trim()
                lower.startsWith("analisa ") -> text.substringAfter("analisa ").trim()
                lower.startsWith("analise ") -> text.substringAfter("analise ").trim()
                lower.startsWith("analisar ") -> text.substringAfter("analisar ").trim()
                lower.contains("o que você acha de ") -> text.substringAfter("o que você acha de ").trim()
                else -> ""
            }
            return OrchestratedIntent(
                intentName = "ANALYZE_TRANSCRIPT",
                parameters = mapOf("content" to contentToAnalyze, "rawInput" to rawInput),
                userFriendlySummary = "Analisar com Gemini 3.5 Flash: ${if (contentToAnalyze.isNotBlank()) contentToAnalyze else "transcrição atual"}",
                voiceReply = "Analisando transcrição com Gemini."
            )
        }

        // 12. Fallback / Knowledge assistant query
        val reply = when {
            lower.contains("quem é você") || lower.contains("o que você faz") ->
                "Eu sou o HUGO, seu secretário pessoal de voz integrado ao Gemini 3.5 Flash. Estou sempre ouvindo para abrir apps, salvar memórias, responder dúvidas e organizar seu dia."
            lower.contains("horas") || lower.contains("que horas são") ->
                "Agora são exatamente ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}."
            lower.contains("obrigado") || lower.contains("valeu") ->
                "Às ordens. Sempre pronto para ajudar."
            else ->
                "Consultando Gemini 3.5 Flash para responder: '$rawInput'."
        }

        return OrchestratedIntent(
            intentName = "GENERAL_QUERY",
            parameters = mapOf("query" to rawInput),
            userFriendlySummary = "Pergunta: $rawInput",
            voiceReply = reply
        )
    }
}
