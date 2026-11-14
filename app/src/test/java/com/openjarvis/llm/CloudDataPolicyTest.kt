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
    fun consentIsBoundToTheSelectedProvider() {
        assertTrue(CloudDataPolicy.hasConsentForProvider("Groq", true, "Groq"))
        assertFalse(CloudDataPolicy.hasConsentForProvider("OpenAI", true, "Groq"))
        assertFalse(CloudDataPolicy.hasConsentForProvider("Groq", false, "Groq"))
        assertFalse(CloudDataPolicy.hasConsentForProvider("Groq", true, null))
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

    @Test
    fun imageConsentIsSeparateFromTextConsentAndBoundToProvider() {
        // Text consent alone cannot authorize image upload.
        assertFalse(CloudDataPolicy.hasImageConsentForProvider("Groq", false, null))
        assertFalse(CloudDataPolicy.hasImageConsentForProvider("Groq", false, "Groq"))

        // Image consent must match the currently selected provider.
        assertTrue(CloudDataPolicy.hasImageConsentForProvider("Groq", true, "Groq"))
        assertFalse(CloudDataPolicy.hasImageConsentForProvider("OpenAI", true, "Groq"))
        assertFalse(CloudDataPolicy.hasImageConsentForProvider("Groq", true, null))
    }

    @Test
    fun localProvidersNeverNeedCloudImageConsent() {
        assertFalse(CloudDataPolicy.hasImageConsentForProvider("Ollama (Local)", true, "Ollama (Local)"))
        assertFalse(CloudDataPolicy.hasImageConsentForProvider("Local (LlamaCpp)", true, "Local (LlamaCpp)"))
    }

    @Test
    fun localProvidersCanProcessImagesWithoutCloudConsent() {
        assertTrue(CloudDataPolicy.canSendImage("Ollama (Local)", false, null))
        assertTrue(CloudDataPolicy.canSendImage("Local (LlamaCpp)", false, null))
    }

    @Test
    fun cloudImageProcessingFailsClosedWithoutMatchingImageConsent() {
        assertFalse(CloudDataPolicy.canSendImage("Groq", false, null))
        assertFalse(CloudDataPolicy.canSendImage("Groq", true, "OpenAI"))
        assertTrue(CloudDataPolicy.canSendImage("Groq", true, "Groq"))
    }
}
