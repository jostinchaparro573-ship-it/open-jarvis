package com.openjarvis.llm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudDataPolicyTest {
    @Test
    fun knownLocalProvidersDoNotRequireCloudConsent() {
        assertFalse(CloudDataPolicy.requiresConsent("Ollama (Local)"))
        assertFalse(CloudDataPolicy.requiresConsent("Local (LlamaCpp)"))
    }

    @Test
    fun cloudAndCustomProvidersRequireExplicitConsent() {
        listOf(
            "Groq",
            "Google Gemini",
            "gemini",
            "OpenRouter",
            "Anthropic Claude",
            "OpenAI",
            "Custom",
            "Unrecognized provider"
        ).forEach { provider ->
            assertTrue("$provider must be treated as cloud", CloudDataPolicy.requiresConsent(provider))
        }
    }
}
