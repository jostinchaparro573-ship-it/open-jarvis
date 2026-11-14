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

    fun hasConsentForProvider(
        providerId: String,
        enabled: Boolean,
        consentProviderId: String?
    ): Boolean = enabled && consentProviderId == providerId

    /**
     * Image bytes require their own opt-in. Text/prompt consent must never
     * authorize sending a photograph to a cloud provider.
     */
    fun hasImageConsentForProvider(
        providerId: String,
        enabled: Boolean,
        consentProviderId: String?
    ): Boolean = requiresConsent(providerId) &&
        enabled &&
        consentProviderId == providerId
}
