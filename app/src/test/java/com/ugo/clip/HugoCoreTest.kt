package com.ugo.clip

import com.ugo.clip.core.ai.HugoOrchestrator
import com.ugo.clip.core.security.Sentinel
import com.ugo.clip.core.security.SentinelResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HugoCoreTest {

    @Test
    fun `test P0 command - open youtube`() {
        val intent = HugoOrchestrator.parse("Hugo, abre o YouTube")
        assertEquals("OPEN_APP", intent.intentName)
        assertEquals("youtube", intent.parameters["app"])
    }

    @Test
    fun `test P0 command - open maps with destination`() {
        val intent = HugoOrchestrator.parse("Hugo, abre o Maps para Canasvieiras")
        assertEquals("OPEN_MAPS", intent.intentName)
        assertEquals("Canasvieiras", intent.parameters["destination"])
    }

    @Test
    fun `test P0 command - remember object location`() {
        val intent = HugoOrchestrator.parse("Hugo, lembra que deixei a furadeira no armário azul")
        assertEquals("STORE_MEMORY", intent.intentName)
        assertEquals("furadeira", intent.parameters["subject"])
        assertTrue(intent.parameters["value"]?.contains("armário azul") == true)
    }

    @Test
    fun `test P0 command - query object location`() {
        val intent = HugoOrchestrator.parse("Hugo, onde deixei a furadeira?")
        assertEquals("QUERY_MEMORY", intent.intentName)
        assertEquals("furadeira", intent.parameters["subject"])
    }

    @Test
    fun `test P0 command - prepare whatsapp message`() {
        val intent = HugoOrchestrator.parse("Hugo, prepara uma mensagem para João: chego às oito")
        assertEquals("PREPARE_MESSAGE", intent.intentName)
        assertEquals("João", intent.parameters["recipient"])
        assertEquals("chego às oito", intent.parameters["message"])
    }

    @Test
    fun `test P0 command - query today agenda`() {
        val intent = HugoOrchestrator.parse("Hugo, o que eu tenho hoje?")
        assertEquals("QUERY_AGENDA", intent.intentName)
        assertEquals("today", intent.parameters["scope"])
    }

    @Test
    fun `test P0 command - create note`() {
        val intent = HugoOrchestrator.parse("Hugo, anota comprar dois disjuntores")
        assertEquals("CREATE_NOTE", intent.intentName)
        assertTrue(intent.parameters["content"]?.contains("comprar dois disjuntores") == true)
    }

    @Test
    fun `test P0 system controls`() {
        val pauseIntent = HugoOrchestrator.parse("Hugo, pausa")
        assertEquals("SYSTEM_PAUSE", pauseIntent.intentName)

        val resumeIntent = HugoOrchestrator.parse("Hugo, volta a escutar")
        assertEquals("SYSTEM_RESUME", resumeIntent.intentName)

        val stopIntent = HugoOrchestrator.parse("Hugo, encerra")
        assertEquals("SYSTEM_STOP", stopIntent.intentName)
    }

    @Test
    fun `test Sentinel allows level A actions directly`() {
        val result = Sentinel.evaluate("OPEN_APP", mapOf("app" to "youtube"))
        assertTrue(result is SentinelResult.Allowed)
    }

    @Test
    fun `test Sentinel requires confirmation for message sending`() {
        val result = Sentinel.evaluate(
            "PREPARE_MESSAGE",
            mapOf("recipient" to "João", "message" to "Chego às oito")
        )
        assertTrue(result is SentinelResult.RequiresConfirmation)
    }

    @Test
    fun `test Sentinel blocks dangerous commands`() {
        val result = Sentinel.evaluate(
            "SYSTEM_ACTION",
            mapOf("target" to "transferir dinheiro via pix")
        )
        assertTrue(result is SentinelResult.Blocked)
    }

    @Test
    fun `test Gemini summarization intent`() {
        val intent = HugoOrchestrator.parse("Hugo, resume o que eu falei")
        assertEquals("SUMMARIZE_TRANSCRIPT", intent.intentName)

        val intentWithText = HugoOrchestrator.parse("Hugo, resume a reunião de hoje")
        assertEquals("SUMMARIZE_TRANSCRIPT", intentWithText.intentName)
    }

    @Test
    fun `test Gemini analysis intent`() {
        val intent = HugoOrchestrator.parse("Hugo, analisa o que eu disse")
        assertEquals("ANALYZE_TRANSCRIPT", intent.intentName)

        val intentWithTopic = HugoOrchestrator.parse("Hugo, o que você acha de comprar os disjuntores agora?")
        assertEquals("ANALYZE_TRANSCRIPT", intentWithTopic.intentName)
    }
}
