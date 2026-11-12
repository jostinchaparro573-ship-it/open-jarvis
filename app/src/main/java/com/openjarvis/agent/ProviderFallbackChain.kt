package com.openjarvis.agent

import android.content.Context
import com.openjarvis.llm.UniversalAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Safe single-provider execution until independent, profile-scoped provider
 * configurations are implemented. This class intentionally does not pretend
 * to fail over: the current settings store only one provider credential set.
 */
class ProviderFallbackChain(private val context: Context) {

    private val _notification = MutableStateFlow<String?>(null)

    /**
     * Executes the one configured provider at most once. A failed request is
     * returned to the caller; it is never replayed against the same provider
     * under a different fallback label.
     */
    suspend fun completeWithFallback(system: String, user: String): Result<String> {
        val adapter = UniversalAdapter(context)
        val provider = adapter.getActiveProvider()
        val providerName = provider.name

        return try {
            val result = provider.complete(system, user)
            if (result.isFailure) {
                val message = result.exceptionOrNull()?.message.orEmpty()
                _notification.value = when {
                    message.contains("429") ->
                        "$providerName is rate limited. No alternate provider is configured."
                    message.contains("401") ->
                        "$providerName rejected the API key. Check Settings."
                    else ->
                        "$providerName failed. No alternate provider is configured."
                }
            } else {
                _notification.value = null
            }
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _notification.value = "$providerName failed. No alternate provider is configured."
            Result.failure(error)
        }
    }

    fun getNotification() = _notification
}
