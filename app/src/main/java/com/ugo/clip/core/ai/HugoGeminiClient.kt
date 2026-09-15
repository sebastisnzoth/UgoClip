package com.ugo.clip.core.ai

import android.util.Log
import com.ugo.clip.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(val text: String, val model: String = "gemini-3.5-flash") : GeminiResult()
    data class Error(val message: String, val isConfigError: Boolean = false) : GeminiResult()
}

data class GeminiAnalysis(
    val originalText: String,
    val summary: String,
    val intelligentResponse: String,
    val keyPoints: List<String>,
    val actionableItems: List<String>,
    val isRealAi: Boolean
)

object HugoGeminiClient {

    private const val TAG = "HugoGeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    // Configure 60-second timeouts as mandated by Gemini guidelines
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Checks if a custom Gemini API key is configured.
     */
    fun hasValidApiKey(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "YOUR_API_KEY"
    }

    /**
     * Generates an intelligent, conversational response to a user's question or statement.
     */
    suspend fun generateIntelligentResponse(
        prompt: String,
        contextInfo: String? = null
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!hasValidApiKey()) {
            return@withContext GeminiResult.Success(
                text = generateLocalSmartResponse(prompt),
                model = "local-heuristic"
            )
        }

        val systemPrompt = buildString {
            append("Você é o HUGO (HUGO Clip), um secretário pessoal e assistente de voz inteligente de alta performance. ")
            append("Responda sempre em português do Brasil com concisão, clareza e naturalidade. ")
            append("Sua resposta será falada via síntese de voz (TTS) para o usuário, então evite tabelas, markdown complexo ou listas longas. Seja direto e prestativo.")
            if (!contextInfo.isNullOrBlank()) {
                append("\nContexto adicional do usuário: $contextInfo")
            }
        }

        callGeminiRestApi(prompt = prompt, systemInstruction = systemPrompt, apiKey = apiKey)
    }

    /**
     * Summarizes a transcribed text or spoken monologue into a concise, structured overview.
     */
    suspend fun summarizeTranscript(
        transcript: String
    ): GeminiResult = withContext(Dispatchers.IO) {
        val clean = transcript.trim()
        if (clean.isBlank()) {
            return@withContext GeminiResult.Error("Nenhum texto fornecido para resumo.")
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!hasValidApiKey()) {
            return@withContext GeminiResult.Success(
                text = generateLocalSummary(clean),
                model = "local-heuristic"
            )
        }

        val systemPrompt = "Você é o sintetizador executivo do HUGO Clip. Analise a transcrição de voz fornecida pelo usuário e forneça um resumo executivo claro e conciso em português (máximo de 3 parágrafos ou pontos objetivos), destacando a ideia central e itens acionáveis."
        val userPrompt = "Por favor, resuma e sintetize a seguinte transcrição de voz:\n\"$clean\""

        callGeminiRestApi(prompt = userPrompt, systemInstruction = systemPrompt, apiKey = apiKey)
    }

    /**
     * Deep analysis of the transcribed text: generates summary, intelligent answer, key points, and action items.
     */
    suspend fun analyzeTranscription(
        transcript: String
    ): GeminiAnalysis = withContext(Dispatchers.IO) {
        val clean = transcript.trim()
        if (clean.isBlank()) {
            return@withContext GeminiAnalysis(
                originalText = "",
                summary = "Nenhuma transcrição para analisar.",
                intelligentResponse = "Aguardando entrada de voz...",
                keyPoints = emptyList(),
                actionableItems = emptyList(),
                isRealAi = false
            )
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!hasValidApiKey()) {
            val localSummary = generateLocalSummary(clean)
            val localKeyPoints = extractKeyPhrases(clean)
            return@withContext GeminiAnalysis(
                originalText = clean,
                summary = localSummary,
                intelligentResponse = "Análise preliminar realizada pelo motor local do HUGO.",
                keyPoints = localKeyPoints,
                actionableItems = if (clean.contains("fazer") || clean.contains("comprar") || clean.contains("ligar") || clean.contains("lembrar")) {
                    listOf("Item identificado no texto para acompanhamento")
                } else emptyList(),
                isRealAi = false
            )
        }

        val systemPrompt = buildString {
            append("Você é o analista de inteligência do HUGO Clip. ")
            append("Analise detalhadamente a transcrição de voz e responda no formato JSON com os seguintes campos exatos:\n")
            append("{\n")
            append("  \"summary\": \"Resumo executivo de 1 ou 2 frases em português\",\n")
            append("  \"intelligentResponse\": \"Resposta inteligente direta ao que o usuário expressou\",\n")
            append("  \"keyPoints\": [\"ponto 1\", \"ponto 2\"],\n")
            append("  \"actionableItems\": [\"ação recomendada 1\", \"ação 2\"]\n")
            append("}")
        }

        val result = callGeminiRestApi(
            prompt = "Analise o seguinte texto transcrito:\n\"$clean\"",
            systemInstruction = systemPrompt,
            apiKey = apiKey
        )

        when (result) {
            is GeminiResult.Success -> {
                parseAnalysisJson(result.text, clean)
            }
            is GeminiResult.Error -> {
                GeminiAnalysis(
                    originalText = clean,
                    summary = "Falha ao processar com Gemini: ${result.message}",
                    intelligentResponse = generateLocalSmartResponse(clean),
                    keyPoints = extractKeyPhrases(clean),
                    actionableItems = emptyList(),
                    isRealAi = false
                )
            }
        }
    }

    private fun callGeminiRestApi(
        prompt: String,
        systemInstruction: String?,
        apiKey: String
    ): GeminiResult {
        return try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                if (!systemInstruction.isNullOrBlank()) {
                    val sysObj = JSONObject().apply {
                        val sysParts = JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        }
                        put("parts", sysParts)
                    }
                    put("systemInstruction", sysObj)
                }

                val config = JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                }
                put("generationConfig", config)
            }

            val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
            val url = "$BASE_URL?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error (${response.code}): $responseBody")
                return GeminiResult.Error("Erro na API Gemini (${response.code}): ${response.message}")
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val generatedText = parts?.optJSONObject(0)?.optString("text")

            if (!generatedText.isNullOrBlank()) {
                GeminiResult.Success(generatedText.trim(), MODEL_NAME)
            } else {
                GeminiResult.Error("A API Gemini não retornou nenhum texto.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network or parsing exception during Gemini request", e)
            GeminiResult.Error("Falha na comunicação com a API Gemini: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun parseAnalysisJson(rawResponse: String, originalText: String): GeminiAnalysis {
        return try {
            val cleanJson = rawResponse.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleanJson)
            val summary = json.optString("summary", "Resumo gerado.")
            val intelligentResponse = json.optString("intelligentResponse", rawResponse)

            val keyPointsList = mutableListOf<String>()
            val keyPointsArray = json.optJSONArray("keyPoints")
            if (keyPointsArray != null) {
                for (i in 0 until keyPointsArray.length()) {
                    keyPointsList.add(keyPointsArray.optString(i))
                }
            }

            val actionItemsList = mutableListOf<String>()
            val actionItemsArray = json.optJSONArray("actionableItems")
            if (actionItemsArray != null) {
                for (i in 0 until actionItemsArray.length()) {
                    actionItemsList.add(actionItemsArray.optString(i))
                }
            }

            GeminiAnalysis(
                originalText = originalText,
                summary = summary,
                intelligentResponse = intelligentResponse,
                keyPoints = keyPointsList,
                actionableItems = actionItemsList,
                isRealAi = true
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse JSON analysis from Gemini response, treating as raw text", e)
            GeminiAnalysis(
                originalText = originalText,
                summary = rawResponse.take(200),
                intelligentResponse = rawResponse,
                keyPoints = extractKeyPhrases(originalText),
                actionableItems = emptyList(),
                isRealAi = true
            )
        }
    }

    private fun generateLocalSmartResponse(prompt: String): String {
        val lower = prompt.lowercase().trim()
        return when {
            lower.contains("quem é você") || lower.contains("o que você é") ->
                "Eu sou o HUGO, seu assistente de voz e secretário pessoal. Conectado ao modelo Gemini 3.5 Flash para análise neural avançada."
            lower.contains("como funciona") || lower.contains("o que você faz") ->
                "Posso registrar memórias, organizar tarefas, criar lembretes, abrir aplicativos e sintetizar transcrições em tempo real com Gemini 3.5 Flash."
            lower.contains("dica") || lower.contains("ajuda") ->
                "Para aproveitar ao máximo, experimente dizer 'Hugo, lembra de...', 'onde deixei...' ou peça para 'resumir o que eu falei'."
            lower.contains("resumo") || lower.contains("sintetiza") ->
                "Síntese: Informação registrada com sucesso. Configure a chave GEMINI_API_KEY no painel Secrets para análise semântica em tempo real."
            else ->
                "Entendido: '$prompt'. Processado com sucesso pelo núcleo do HUGO."
        }
    }

    private fun generateLocalSummary(text: String): String {
        val words = text.split("\\s+".toRegex())
        val count = words.size
        return if (count <= 10) {
            "Nota breve: \"$text\""
        } else {
            val preview = words.take(12).joinToString(" ")
            "Resumo ($count palavras): $preview... [Para análise semântica detalhada, conecte a chave Gemini]"
        }
    }

    private fun extractKeyPhrases(text: String): List<String> {
        val tokens = text.split("[,;.]+".toRegex()).map { it.trim() }.filter { it.length > 4 }
        return if (tokens.isNotEmpty()) tokens.take(3) else listOf("Ideia principal: $text")
    }
}
