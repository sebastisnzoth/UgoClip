package com.ugo.clip.core.security

enum class SecurityLevel {
    LEVEL_A, // Direct execution without confirmation
    LEVEL_B, // Requires user confirmation ("Enviar para João: 'Chego às oito'. Confirmar?")
    LEVEL_C  // Blocked / Security policy violation
}

sealed class SentinelResult {
    data object Allowed : SentinelResult()
    data class RequiresConfirmation(
        val confirmationPrompt: String,
        val actionTitle: String
    ) : SentinelResult()
    data class Blocked(val reason: String) : SentinelResult()
}

object Sentinel {
    /**
     * Evaluates whether an action requested by the user or proposed by AI is safe to execute.
     */
    fun evaluate(intentName: String, parameters: Map<String, String>): SentinelResult {
        // Level C: Blocked security violations
        val target = parameters["target"]?.lowercase().orEmpty()
        val content = parameters["content"]?.lowercase().orEmpty()
        val combined = "$intentName $target $content".lowercase()

        val dangerousKeywords = listOf(
            "transferir dinheiro", "pix", "senha", "password", "cartao de credito",
            "formatar", "apagar tudo", "root", "factory reset", "banco", "pagamento"
        )
        if (dangerousKeywords.any { combined.contains(it) }) {
            return SentinelResult.Blocked("Ação bloqueada pelo Sentinel: Operação sensível de segurança/financeira não permitida por voz.")
        }

        // Level B: Sensitive actions requiring confirmation
        when (intentName) {
            "SEND_MESSAGE", "PREPARE_MESSAGE" -> {
                val recipient = parameters["recipient"] ?: "Destinatário"
                val message = parameters["message"] ?: ""
                return SentinelResult.RequiresConfirmation(
                    confirmationPrompt = "Enviar para $recipient: \"$message\". Confirmar envio?",
                    actionTitle = "Envio de Mensagem"
                )
            }
            "MAKE_CALL", "INITIATE_CALL" -> {
                val contact = parameters["contact"] ?: "Contato"
                return SentinelResult.RequiresConfirmation(
                    confirmationPrompt = "Iniciar ligação para $contact?",
                    actionTitle = "Ligação Telefônica"
                )
            }
            "DELETE_ALL", "MASS_DELETE" -> {
                return SentinelResult.Blocked("Sentinel impediu exclusão em massa via comando de voz.")
            }
            "DELETE_ITEM" -> {
                val item = parameters["item"] ?: "item selecionado"
                return SentinelResult.RequiresConfirmation(
                    confirmationPrompt = "Tem certeza que deseja apagar $item?",
                    actionTitle = "Exclusão de Registro"
                )
            }
        }

        // Level A: Normal autonomous actions
        return SentinelResult.Allowed
    }
}
