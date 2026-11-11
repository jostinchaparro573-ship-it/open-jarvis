package com.openjarvis.intelligence

import android.content.Context
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.accessibility.ScreenReader
import kotlinx.coroutines.delay

class AIAppInteractor(private val context: Context) {
    
    private fun readScreen(): String = JarvisAccessibilityService.instance?.let { ScreenReader(it).extractAllText() } ?: ""
    private val workingMemory = TaskWorkingMemory()
    
    fun openAIApp(meta: AIAppMeta): Boolean {
        val service = JarvisAccessibilityService.instance ?: return false
        return service.openAppByPackage(meta.packageName)
    }
    
    suspend fun clearContext() {
        try {
            JarvisAccessibilityService.instance?.pressBack()
            delay(300)
        } catch (e: Exception) { }
    }
    
    fun typePrompt(prompt: String): Boolean {
        return JarvisAccessibilityService.instance?.typeText(prompt) ?: false
    }
    
    suspend fun waitForResponse(timeoutMs: Long = 60_000): String {
        val startTime = System.currentTimeMillis()
        var lastText = ""
        var sameCount = 0
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            delay(1000)
            
            val currentText = readScreen()
            
            if (currentText == lastText) {
                sameCount++
                if (sameCount >= 2) {
                    return currentText
                }
            } else {
                sameCount = 0
                lastText = currentText
            }
        }
        
        return lastText
    }
    
    fun extractResponse(meta: AIAppMeta): String {
        when (meta.responseExtraction) {
            ResponseExtraction.SCREEN_TEXT -> {
                return readScreen()
            }
            ResponseExtraction.OCR_REQUIRED -> {
                return readScreen()
            }
            ResponseExtraction.COPY_BUTTON -> {
                return tryCopyFromUI()
            }
            ResponseExtraction.SHARE_MENU -> {
                return tryShareFromUI()
            }
        }
        return ""
    }
    
    fun getWorkingMemory(): TaskWorkingMemory = workingMemory
    
    private fun tryCopyFromUI(): String = readScreen()
    
    private fun tryShareFromUI(): String = readScreen()
}