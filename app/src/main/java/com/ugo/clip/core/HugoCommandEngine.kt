package com.ugo.clip.core

import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

sealed class HugoAction {
    data class OpenApp(val alias: String) : HugoAction()
    data class OpenMaps(val query: String?) : HugoAction()
    data class SaveNote(val text: String) : HugoAction()
    data class RememberLocation(val subject: String, val location: String) : HugoAction()
    data class FindLocation(val subject: String) : HugoAction()
    data class PrepareWhatsApp(val text: String) : HugoAction()
    data class AddAgenda(val title: String, val startsAt: Long) : HugoAction()
    data class ReadAgenda(val dayOffset: Long) : HugoAction()
    object Pause : HugoAction()
    object Stop : HugoAction()
    object None : HugoAction()
}

data class HugoDecision(
    val action: HugoAction,
    val spoken: String? = null
)

object HugoCommandEngine {

    fun parse(rawTranscript: String): HugoDecision? {
        val withoutWake = rawTranscript.replace(
            Regex("^\\s*hugo[\\s,.:;!?-]*", RegexOption.IGNORE_CASE),
            ""
        ).trim()

        // Continuous mode only acts on explicit HUGO commands.
        if (withoutWake == rawTranscript.trim()) return null
        if (withoutWake.isBlank()) {
            return HugoDecision(HugoAction.None, "Estou ouvindo.")
        }

        val normalized = normalize(withoutWake)

        if (normalized == "pausa" || normalized == "pausar" || normalized == "para de escutar") {
            return HugoDecision(HugoAction.Pause, "Modo HUGO pausado. Use a notificação para voltar a escutar.")
        }

        if (
            normalized == "encerra" || normalized == "encerrar" || normalized == "desliga" ||
            normalized == "desligar" || normalized == "para"
        ) {
            return HugoDecision(HugoAction.Stop, "Encerrando o Modo HUGO.")
        }

        parseMaps(normalized)?.let { return it }
        parseOpenApp(normalized)?.let { return it }
        parseRememberLocation(withoutWake, normalized)?.let { return it }
        parseFindLocation(withoutWake, normalized)?.let { return it }
        parseNote(withoutWake, normalized)?.let { return it }
        parseWhatsApp(withoutWake, normalized)?.let { return it }
        parseAgenda(withoutWake, normalized)?.let { return it }
        parseAgendaRead(normalized)?.let { return it }

        return HugoDecision(
            HugoAction.None,
            "Ainda não sei fazer isso. Tente abrir um app, anotar algo, guardar onde deixou um objeto ou criar um compromisso."
        )
    }

    private fun parseMaps(normalized: String): HugoDecision? {
        val starters = listOf("abre o maps", "abre maps", "abre o mapa", "abrir maps", "abrir o mapa")
        val starter = starters.firstOrNull { normalized.startsWith(it) } ?: return null
        val tail = normalized.removePrefix(starter).trim()
        val query = tail.removePrefix("para ").removePrefix("em ").trim().ifBlank { null }
        return HugoDecision(HugoAction.OpenMaps(query))
    }

    private fun parseOpenApp(normalized: String): HugoDecision? {
        val openPrefix = when {
            normalized.startsWith("abre ") -> "abre "
            normalized.startsWith("abrir ") -> "abrir "
            normalized.startsWith("abra ") -> "abra "
            else -> return null
        }

        val target = normalized.removePrefix(openPrefix).removePrefix("o ").removePrefix("a ").trim()
        val alias = when {
            target.startsWith("youtube") -> "youtube"
            target.startsWith("whatsapp") -> "whatsapp"
            target.startsWith("spotify") -> "spotify"
            target.startsWith("instagram") -> "instagram"
            target.startsWith("chrome") || target.startsWith("navegador") -> "chrome"
            else -> return null
        }
        return HugoDecision(HugoAction.OpenApp(alias))
    }

    private fun parseNote(original: String, normalized: String): HugoDecision? {
        val prefixes = listOf("anota ", "anote ", "faz uma nota ", "faca uma nota ")
        val prefix = prefixes.firstOrNull { normalized.startsWith(it) } ?: return null
        val normalizedBody = normalized.removePrefix(prefix).trim()
        if (normalizedBody.isBlank()) return HugoDecision(HugoAction.None, "O que você quer que eu anote?")

        val originalIndex = original.normalizeForSearch().indexOf(normalizedBody)
        val body = if (originalIndex >= 0 && originalIndex < original.length) {
            original.substring(originalIndex).trim()
        } else {
            normalizedBody
        }
        return HugoDecision(HugoAction.SaveNote(body), "Anotado.")
    }

    private fun parseRememberLocation(original: String, normalized: String): HugoDecision? {
        if (!normalized.contains("deixei")) return null
        if (!(normalized.startsWith("lembra que") || normalized.startsWith("lembre que") || normalized.startsWith("guarda que"))) {
            return null
        }

        val regex = Regex("deixei\\s+(?:a|o|meu|minha)?\\s*(.+?)\\s+(?:no|na|em)\\s+(.+)$")
        val match = regex.find(normalized) ?: return null
        val subject = match.groupValues[1].trim()
        val location = match.groupValues[2].trim()
        if (subject.isBlank() || location.isBlank()) return null

        return HugoDecision(
            HugoAction.RememberLocation(subject, location),
            "Certo. Vou lembrar que $subject está em $location."
        )
    }

    private fun parseFindLocation(original: String, normalized: String): HugoDecision? {
        if (!(normalized.startsWith("onde deixei") || normalized.startsWith("onde esta") || normalized.startsWith("onde ficou"))) {
            return null
        }
        val subject = normalized
            .replaceFirst(Regex("^onde\\s+(deixei|esta|ficou)\\s+"), "")
            .removePrefix("a ")
            .removePrefix("o ")
            .removePrefix("meu ")
            .removePrefix("minha ")
            .trim(' ', '?', '.', '!')
        if (subject.isBlank()) return null
        return HugoDecision(HugoAction.FindLocation(subject))
    }

    private fun parseWhatsApp(original: String, normalized: String): HugoDecision? {
        val isMessageCommand = normalized.contains("mensagem") || normalized.startsWith("manda whatsapp") || normalized.startsWith("envia whatsapp")
        if (!isMessageCommand) return null

        val content = when {
            normalized.contains("dizendo que ") -> {
                val marker = "dizendo que "
                normalized.substringAfter(marker).trim()
            }
            normalized.contains("com a mensagem ") -> normalized.substringAfter("com a mensagem ").trim()
            else -> ""
        }

        if (content.isBlank()) {
            return HugoDecision(HugoAction.None, "Diga também o texto da mensagem.")
        }
        return HugoDecision(
            HugoAction.PrepareWhatsApp(content),
            "Vou abrir o WhatsApp com a mensagem preparada. Confira o contato antes de enviar."
        )
    }

    private fun parseAgenda(original: String, normalized: String): HugoDecision? {
        val isAdd = normalized.startsWith("lembra de ") || normalized.startsWith("me lembra de ") ||
            normalized.startsWith("lembre de ") || normalized.startsWith("adiciona ") ||
            normalized.startsWith("adicionar ") || normalized.startsWith("agenda ")
        if (!isAdd || !normalized.contains("amanha")) return null

        val time = extractTomorrowTime(normalized) ?: return HugoDecision(
            HugoAction.None,
            "Entendi que é para amanhã, mas preciso do horário. Por exemplo: amanhã às nove."
        )

        val title = normalized
            .replaceFirst(Regex("^(me\\s+)?(lembra|lembre)\\s+de\\s+"), "")
            .replaceFirst(Regex("^(adiciona|adicionar|agenda)\\s+"), "")
            .substringBefore("amanha")
            .trim(' ', ',', '.', '-')
            .ifBlank { "Compromisso" }

        return HugoDecision(
            HugoAction.AddAgenda(title, time),
            "Vou adicionar $title amanhã às ${formatHour(time)}. Confirme no calendário."
        )
    }

    private fun parseAgendaRead(normalized: String): HugoDecision? {
        val asksAgenda = normalized.contains("o que tenho") || normalized.contains("minha agenda") || normalized.contains("meus compromissos")
        if (!asksAgenda) return null
        return when {
            normalized.contains("amanha") -> HugoDecision(HugoAction.ReadAgenda(1))
            normalized.contains("hoje") -> HugoDecision(HugoAction.ReadAgenda(0))
            else -> HugoDecision(HugoAction.ReadAgenda(0))
        }
    }

    private fun extractTomorrowTime(normalized: String): Long? {
        val numeric = Regex("\\b(?:as|a)\\s+(\\d{1,2})(?::(\\d{2}))?").find(normalized)
        val hour: Int
        val minute: Int

        if (numeric != null) {
            hour = numeric.groupValues[1].toIntOrNull() ?: return null
            minute = numeric.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
        } else {
            val words = mapOf(
                "zero" to 0, "uma" to 1, "duas" to 2, "tres" to 3, "quatro" to 4, "cinco" to 5,
                "seis" to 6, "sete" to 7, "oito" to 8, "nove" to 9, "dez" to 10, "onze" to 11,
                "doze" to 12, "treze" to 13, "quatorze" to 14, "catorze" to 14, "quinze" to 15,
                "dezesseis" to 16, "dezessete" to 17, "dezoito" to 18, "dezenove" to 19,
                "vinte" to 20, "vinte e uma" to 21, "vinte e duas" to 22, "vinte e tres" to 23
            )
            val afterAs = normalized.substringAfter(" as ", "").trim()
            val match = words.entries
                .sortedByDescending { it.key.length }
                .firstOrNull { afterAs.startsWith(it.key) }
                ?: return null
            hour = match.value
            minute = 0
        }

        if (hour !in 0..23 || minute !in 0..59) return null
        val dateTime = LocalDate.now().plusDays(1).atTime(hour, minute)
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun formatHour(epochMillis: Long): String {
        val time = java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return if (time.minute == 0) "%02d:00".format(time.hour) else "%02d:%02d".format(time.hour, time.minute)
    }

    private fun normalize(value: String): String = value.normalizeForSearch()

    private fun String.normalizeForSearch(): String {
        return Normalizer.normalize(this.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9:?\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
