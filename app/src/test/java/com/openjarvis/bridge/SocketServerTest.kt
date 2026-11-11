package com.openjarvis.bridge

import org.junit.Assert.*
import org.junit.Test

class SocketServerTest {

    @Test
    fun `parseRequest with valid JSON returns command and requestId`() {
        val result = SocketServer.parseRequest("""{"cmd":"open chrome","requestId":"abc123"}""")

        assertNotNull(result)
        assertEquals("abc123", result?.first)
        assertEquals("open chrome", result?.second)
    }

    @Test
    fun `parseRequest with missing cmd returns null`() {
        val result = SocketServer.parseRequest("""{"requestId":"abc123"}""")
        assertNull(result)
    }

    @Test
    fun `parseRequest with invalid JSON returns null`() {
        assertNull(SocketServer.parseRequest("not json"))
    }

    @Test
    fun `parseRequest with missing requestId uses empty string`() {
        val result = SocketServer.parseRequest("""{"cmd":"status"}""")

        assertNotNull(result)
        assertEquals("", result?.first)
        assertEquals("status", result?.second)
    }

    @Test
    fun `escapeJson escapes special characters`() {
        val input = "hello\nworld\"test\tvalue"
        val escaped = SocketServer.escapeJson(input)

        assertTrue(escaped.contains("\\n"))
        assertTrue(escaped.contains("\\\""))
        assertTrue(escaped.contains("\\t"))
    }
    @Test
    fun `createErrorResponse formats request and error`() {
        val response = SocketServer.createErrorResponse("req123", "test error")

        assertTrue(response.contains("req123"))
        assertTrue(response.contains("error"))
        assertTrue(response.contains("test error"))
    }

    @Test
    fun `special command names are parsed without constructing Android services`() {
        val commands = listOf("status", "history", "memory", "providers")

        commands.forEachIndexed { index, command ->
            val result = SocketServer.parseRequest(
                """{"cmd":"$command","requestId":"r$index"}"""
            )
            assertEquals("r$index", result?.first)
            assertEquals(command, result?.second)
        }
    }

    @Test
    fun `requestId roundtrip preserved`() {
        val testIds = listOf("abc123", "xyz789", "test-id-001", "")

        for (id in testIds) {
            val json = if (id.isEmpty()) {
                """{"cmd":"test"}"""
            } else {
                """{"cmd":"test","requestId":"$id"}"""
            }

            val result = SocketServer.parseRequest(json)
            assertNotNull("Failed for id: $id", result)
            assertEquals(id, result?.first)
        }
    }

    @Test
    fun `malformed JSON handled gracefully`() {
        val malformedInputs = listOf("", "{", """{"cmd":""", "null", "[][]")

        for (input in malformedInputs) {
            assertNull("Should return null for: $input", SocketServer.parseRequest(input))
        }
    }
}
