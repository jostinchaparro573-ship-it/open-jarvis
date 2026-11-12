# LLM Providers - Open Jarvis

## Overview

Open Jarvis uses a universal adapter pattern to support multiple LLM providers through a single interface. Any provider that speaks OpenAI-compatible JSON can be added easily.

## Architecture

```
┌─────────────────────────────────────────────┐
│              AgentCore                        │
│    (parses commands → builds action plans)    │
└─────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────┐
│            UniversalAdapter                 │
│    (routes to selected provider)              │
└─────────────────────────────────────────────┘
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
   ┌──────────┐ ┌──────────┐ ┌──────────┐
   │ Groq    │ │ Gemini  │ │ Custom  │
   │Provider │ │Provider │ │Provider │
   └──────────┘ └──────────┘ └──────────┘
```

## Adding a Custom Provider

### Step 1: Implement the Interface

```kotlin
package com.openjarvis.llm.providers

import com.openjarvis.llm.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyCustomProvider(
    override val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : LLMProvider {
    
    override val name: String = "My Custom"
    
    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        context: String
    ): String = withContext(Dispatchers.IO) {
        // Implement API call here
        val requestBody = buildRequestBody(systemPrompt, userMessage, context)
        val response = httpClient.post(baseUrl) {
            header("Authorization", "Bearer $apiKey")
            header("Content-Type", "application/json")
            setBody(requestBody)
        }
        parseResponse(response)
    }
    
    private fun buildRequestBody(system: String, user: String, context: String): String {
        // Build provider-specific JSON
    }
    
    private fun parseResponse(response: Response): String {
        // Parse provider-specific response
    }
}
```

### Step 2: Register in UniversalAdapter

```kotlin
// In UniversalAdapter.kt
fun setProviderByName(name: String, baseUrl: String = ""): LLMProvider {
    currentProvider = when (name.lowercase()) {
        "mycustom" -> MyCustomProvider(baseUrl, apiKey, model)
        // ... other providers
    }
    return currentProvider
}
```

### Step 3: Add to Settings UI

```kotlin
// In SettingsActivity.kt
val availableProviders = listOf(
    "Groq",
    "Google Gemini",
    "OpenRouter",
    "Anthropic Claude",
    "OpenAI",
    "Ollama (Local)",
    "My Custom"  // Add here
)
```

## Provider Implementation Examples

### OpenAI-Compatible API

For servers that follow OpenAI's `/v1/chat/completions` format:

```kotlin
class OpenAICompatibleProvider(
    override val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : LLMProvider {
    
    override val name: String = "Custom (OpenAI Compatible)"
    
    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        context: String
    ): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val requestBody = """
        {
            "model": "$model",
            "messages": [
                {"role": "system", "content": "$systemPrompt"},
                {"role": "user", "content": "$userMessage\n\nContext: $context"}
            ],
            "temperature": 0.7,
            "max_tokens": 1024
        }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build()
        
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body()?.string())
        json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}
```

### Groq API (Direct)

```kotlin
class GroqProvider(
    private val apiKey: String,
    private val model: String = "llama-3.1-70b-versatile"
) : LLMProvider {
    
    override val name: String = "Groq"
    override val baseUrl: String = "https://api.groq.com/openai/v1"
    
    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        context: String
    ): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val requestBody = """
        {
            "model": "$model",
            "messages": [
                {"role": "system", "content": "$systemPrompt"},
                {"role": "user", "content": "$userMessage\n\nContext: $context"}
            ],
            "temperature": 0.7,
            "max_tokens": 1024
        }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build()
        
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body()?.string())
        json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}
```

### Gemini API

```kotlin
class GeminiProvider(
    private val apiKey: String,
    private val model: String = "gemini-3.8-flash"
) : LLMProvider {
    
    override val name: String = "Google Gemini"
    override val baseUrl: String = "https://generativelanguage.googleapis.com/v1"
    
    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        context: String
    ): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val fullPrompt = "$systemPrompt\n\nUser: $userMessage\n\nContext: $context"
        
        val requestBody = """
        {
            "contents": [{"parts": [{"text": "$fullPrompt"}]}],
            "generationConfig": {
                "temperature": 0.7,
                "maxOutputTokens": 1024
            }
        }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$baseUrl/models/$model:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build()
        
        val response = client.newCall(request).execute()
        val json = JSONObject(response.body()?.string())
        json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }
}
```


### Gemini API key requirements (checked 2026-11-12)

- Prefer a Gemini authorization (auth) key created in Google AI Studio. New AI Studio keys are auth keys by default.
- Unrestricted standard keys are rejected. If an existing standard key must be used temporarily, explicitly restrict it to the Gemini API; Google documents the September 2026 migration away from standard keys.
- Treat every key as a secret. Never commit it, print it to logs, or hard-code it into a distributed mobile APK. EncryptedSharedPreferences protects the locally saved value at rest, but a key embedded in a client app can still be extracted from a compromised device or app package. A production multi-user deployment should proxy provider calls through a backend that protects service credentials.
- Google reference: [Using Gemini API keys](https://ai.google.dev/gemini-api/docs/api-key).

## Hydra.js Compatibility

Open Jarvis works with [Hydra.js](https://github.com/hydra-ai/hydra) or any local proxy that translates to OpenAI format:

```
┌──────────────┐      OpenAI Format       ┌──────────────┐
│ Open Jarvis  │ ───────────────────────► │ Hydra.js     │
│ (Universal  │      HTTP/JSON          │ (local proxy)│
│  Adapter)   │ ◄─────────────────────── │              │
└──────────────┘      HTTP/JSON          └──────────────┘
                                                │
                                                ▼
                                         ┌──────────────┐
                                         │ llama.cpp    │
                                         │ or other     │
                                         │ local model │
                                         └──────────────┘
```

## Settings Storage

Provider settings are stored in EncryptedSharedPreferences:

| Key | Description |
|-----|-------------|
| `provider_name` | Selected provider (Groq, Gemini, etc.) |
| `provider_base_url` | Custom Base URL (if Custom provider) |
| `provider_api_key` | API key (encrypted) |
| `provider_model` | Model name |

## Environment Variables

For development, you can also use environment variables:

```bash
# In Termux 或 local development
export JARVIS_PROVIDER=groq
export JARVIS_API_KEY=your_api_key
export JARVIS_MODEL=llama-3.1-70b-versatile
```

## Error Handling

Each provider should handle:

| Error | Handling |
|-------|----------|
| Invalid API key | Show error in UI, prompt to check settings |
| Rate limit | Implement exponential backoff |
| Network error | Show offline message, use cache if available |
| Model not found | Fallback to default model |

## Current implementation limitations (verified 2026-11-12)

- Only one active provider configuration is stored at a time in the shared EncryptedSharedPreferences namespace: provider name, base URL, API key, and model. This is not a per-provider credential vault and is not profile-isolated.
- `ProviderFallbackChain` has been made fail-closed: it now calls the one configured provider at most once and reports that no alternate provider is configured. Genuine multi-provider failover remains unavailable until independent provider settings and credentials are implemented.
- Do not describe provider fallback as functional until each provider has its own separately saved configuration, selection by provider ID, independent credential retrieval, and tests that prove a failure in provider A actually routes to provider B.
- Gemini requests in the current branch use the `x-goog-api-key` header rather than putting the key in the URL query string. This keeps the secret out of the HTTP request line logged by the current BASIC interceptor. Integration testing is still required.
- Provider and voice preferences are encrypted at rest using EncryptedSharedPreferences, but the preference files are shared across local profiles because profile isolation has not been implemented.

### Required provider configuration model

Use a profile-scoped provider configuration record keyed by (profileId, providerId), with provider type, base URL, model, credential reference, enabled state, and priority. Store secrets separately in an encrypted vault; do not store plaintext keys in Room. Fallback must skip disabled/unconfigured providers, respect rate limits, avoid retrying the same provider, and surface the provider that ultimately answered. Before sending a user's prompt to a different cloud provider, show the data-transfer implications and respect the user's configured consent.


## Gemini model compatibility update (2026-11-12)

The previous default `gemini-1.5-flash` is shut down and cannot be used. The default was updated in the app to `gemini-3.8-flash`, a currently listed stable Gemini model. The API key is sent in the `x-goog-api-key` header. Users may still enter a different model ID manually, so model availability should be validated with a real provider connection test before release.

Only one provider credential set is currently persisted. When the user changes provider in Settings, the UI clears the previous provider's key and base URL instead of accidentally reusing credentials for a different provider. Settings now reload the saved provider, model, base URL and API key when reopened.
