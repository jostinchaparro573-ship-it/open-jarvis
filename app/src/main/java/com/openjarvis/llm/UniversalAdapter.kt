package com.openjarvis.llm

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.openjarvis.llm.providers.*
import com.openjarvis.local.ModelManager

class UniversalAdapter(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "jarvis_encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun getActiveProvider(): LLMProvider {
        val name = prefs.getString("provider_name", "Groq") ?: "Groq"
        val baseUrl = prefs.getString("provider_base_url", "") ?: ""
        val apiKey = prefs.getString("provider_api_key", "") ?: ""
        val model = prefs.getString("provider_model", null)
            ?.takeIf { it.isNotBlank() }
            ?: getDefaultModel(name)
        
        return when (name) {
            "Groq" -> GroqProvider(apiKey, model)
            "Google Gemini", "gemini" -> GeminiProvider(apiKey, model)
            "OpenRouter" -> OpenRouterProvider(apiKey, model)
            "Anthropic Claude" -> AnthropicProvider(apiKey, model)
            "OpenAI" -> OpenAIProvider(apiKey, model)
            "Ollama (Local)" -> OllamaProvider(baseUrl, model)
            "Local (LlamaCpp)" -> {
                val mm = getModelManager(context)
                val tier = mm.getDownloadedTier() ?: ModelManager.ModelTier.MINIMUM
                LlamaProvider(mm.getModelPath(tier), tier.displayName)
            }
            "Custom" -> CustomProvider(baseUrl, apiKey, model)
            else -> CustomProvider(baseUrl, apiKey, model)
        }
    }
    
    /**
     * Cloud providers receive the user's command plus any prompt context assembled
     * by the caller (which may include screen text and personal memory). A local
     * provider does not require this opt-in.
     */
    fun hasCloudDataConsent(): Boolean =
        prefs.getBoolean(KEY_CLOUD_DATA_CONSENT, false)

    fun setCloudDataConsent(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_DATA_CONSENT, enabled).apply()
    }

    fun isCloudProvider(name: String = getProviderName()): Boolean =
        name !in LOCAL_PROVIDERS

    suspend fun complete(
        systemPrompt: String,
        userMessage: String
    ): Result<String> {
        val providerName = getProviderName()
        if (isCloudProvider(providerName) && !hasCloudDataConsent()) {
            return Result.failure(CloudDataConsentRequiredException())
        }
        return getActiveProvider().complete(systemPrompt, userMessage)
    }

    suspend fun testConnection(): Result<Long> {
        val providerName = getProviderName()
        if (isCloudProvider(providerName) && !hasCloudDataConsent()) {
            return Result.failure(CloudDataConsentRequiredException())
        }
        return getActiveProvider().testConnection()
    }

    class CloudDataConsentRequiredException : Exception(
        "Cloud AI is disabled. Review and enable Cloud Data Sharing in Settings to send text to the selected provider."
    )
    
    fun saveSettings(name: String, baseUrl: String, apiKey: String, model: String) {
        prefs.edit().apply {
            putString("provider_name", name)
            putString("provider_base_url", baseUrl)
            putString("provider_api_key", apiKey)
            putString("provider_model", model)
            apply()
        }
    }
    
    fun getProviderName(): String = prefs.getString("provider_name", "Groq") ?: "Groq"
    
    companion object {
        private const val KEY_CLOUD_DATA_CONSENT = "cloud_data_sharing_enabled"
        private val LOCAL_PROVIDERS = setOf("Ollama (Local)", "Local (LlamaCpp)")

        private var modelManager: ModelManager? = null
        
        val AVAILABLE_PROVIDERS = listOf(
            "Groq",
            "Google Gemini",
            "OpenRouter",
            "Anthropic Claude",
            "OpenAI",
            "Ollama (Local)",
            "Local (LlamaCpp)"
        )
        
        fun getModelManager(context: Context): ModelManager {
            return modelManager ?: synchronized(this) {
                modelManager ?: ModelManager(context.applicationContext).also { modelManager = it }
            }
        }
        
        fun isLocalModelLoaded(): Boolean {
            return modelManager?.state?.value is ModelManager.ModelState.Loaded
        }
        
        suspend fun loadLocalModel(tier: ModelManager.ModelTier): Result<Unit> {
            return modelManager?.loadModel(tier) ?: Result.failure(Exception("ModelManager not initialized"))
        }
        
        fun getDefaultModel(provider: String): String = when (provider) {
            "Groq" -> "llama-3.1-70b-versatile"
            "Google Gemini", "gemini" -> "gemini-3.8-flash"
            "OpenRouter" -> "meta-llama/llama-3-8b-instruct:free"
            "Anthropic Claude" -> "claude-haiku-4-20250514"
            "OpenAI" -> "gpt-4o-mini"
            "Ollama (Local)" -> "llama3"
            else -> "gpt-4o-mini"
        }
    }
}