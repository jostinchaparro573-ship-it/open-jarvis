package com.openjarvis.llm

/**
 * Fail-closed provider classification based on the provider identifier stored
 * in settings. Unknown/custom providers are treated as cloud providers.
 */
object CloudDataPolicy {
    private val localProviderIds = setOf(
        "Ollama (Local)",
        "Local (LlamaCpp)"
    )

    fun requiresConsent(providerId: String): Boolean =
        providerId !in localProviderIds
}
