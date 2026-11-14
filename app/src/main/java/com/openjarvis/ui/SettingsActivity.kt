package com.openjarvis.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.openjarvis.R
import com.openjarvis.llm.UniversalAdapter
import com.openjarvis.ui.settings.SettingsScreen
import com.openjarvis.ui.theme.OpenJarvisTheme

class SettingsActivity : ComponentActivity() {
    
    private lateinit var masterKey: MasterKey
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var voicePrefs: android.content.SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        prefs = EncryptedSharedPreferences.create(
            this,
            "jarvis_encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        voicePrefs = EncryptedSharedPreferences.create(
            this,
            "voice_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        setContent {
            OpenJarvisTheme {
                SettingsScreen(
                    onNavigateBack = { finish() },
                    initialProviderName = prefs.getString("provider_name", "Groq") ?: "Groq",
                    initialBaseUrl = prefs.getString("provider_base_url", "") ?: "",
                    initialApiKey = prefs.getString("provider_api_key", "") ?: "",
                    initialModel = prefs.getString("provider_model", null)
                        ?.takeIf { it.isNotBlank() }
                        ?: UniversalAdapter.getDefaultModel(
                            prefs.getString("provider_name", "Groq") ?: "Groq"
                        ),
                    onSaveProvider = { name, baseUrl, apiKey, model ->
                        saveProviderSettings(name, baseUrl, apiKey, model)
                    },
                    initialCloudDataConsent = prefs.getBoolean("cloud_data_sharing_enabled", false) &&
                        prefs.getString("cloud_data_sharing_provider", null) ==
                        (prefs.getString("provider_name", "Groq") ?: "Groq"),
                    onCloudDataConsentChange = { providerId, enabled ->
                        val editor = prefs.edit().putBoolean("cloud_data_sharing_enabled", enabled)
                        if (enabled) editor.putString("cloud_data_sharing_provider", providerId)
                        else editor.remove("cloud_data_sharing_provider")
                        editor.apply()
                    },
                    initialCloudImageConsent = prefs.getBoolean("cloud_image_sharing_enabled", false) &&
                        prefs.getString("cloud_image_sharing_provider", null) ==
                        (prefs.getString("provider_name", "Groq") ?: "Groq"),
                    onCloudImageConsentChange = { providerId, enabled ->
                        val editor = prefs.edit().putBoolean("cloud_image_sharing_enabled", enabled)
                        if (enabled) editor.putString("cloud_image_sharing_provider", providerId)
                        else editor.remove("cloud_image_sharing_provider")
                        editor.apply()
                    },
                    initialVoiceEnabled = voicePrefs.getBoolean("voice_enabled", false),
                    initialSpeakResults = voicePrefs.getBoolean("speak_results", true),
                    onVoiceEnabledChange = { enabled ->
                        voicePrefs.edit().putBoolean("voice_enabled", enabled).apply()
                    },
                    onSpeakResultsChange = { enabled ->
                        voicePrefs.edit().putBoolean("speak_results", enabled).apply()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    
    private fun saveProviderSettings(name: String, baseUrl: String, apiKey: String, model: String) {
        prefs.edit().apply {
            putString("provider_name", name)
            putString("provider_base_url", baseUrl)
            putString("provider_api_key", apiKey)
            putString("provider_model", model)
            apply()
        }
    }
}