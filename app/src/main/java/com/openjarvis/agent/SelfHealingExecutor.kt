package com.openjarvis.agent

import android.content.Context
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.accessibility.ScreenReader
import com.openjarvis.llm.UniversalAdapter
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Retries only actions whose execution can be verified locally.
 * If accessibility is unavailable or an action cannot be verified, fails closed.
 */
class SelfHealingExecutor(private val appContext: Context) {

    private val llm = UniversalAdapter(appContext.applicationContext)
    private val maxAttempts = 3
    private val baseDelayMs = 1000L

    suspend fun executeWithHealing(
        action: Action,
        executionContext: ExecutionContext,
        attempt: Int = 1
    ): ActionResult {
        val result = tryExecuteAction(action)
        if (result is ActionResult.Success) return result
        if (attempt >= maxAttempts) return result

        val service = JarvisAccessibilityService.instance ?: return result
        val screenText = ScreenReader(service).extractAllText().take(500)
        val prompt = """
            An action failed.
            Action: ${action.action}
            Expected state: ${executionContext.expectedState ?: "task completion"}
            Current screen text: $screenText
            Attempt: $attempt of $maxAttempts.
            Suggest one alternative action as JSON with keys action, text, value, package.
            Do not suggest sending messages, deleting data, purchases, or changing security settings.
        """.trimIndent()

        val alternative = llm.complete(
            "Return one safe, reversible UI action as JSON. If unsure, return an empty response.",
            prompt
        ).getOrNull()

        delay(baseDelayMs * attempt)
        val alternativeAction = alternative?.let(::parseActionFromLLM)
        if (alternativeAction != null) {
            return executeWithHealing(alternativeAction, executionContext, attempt + 1)
        }
        return result
    }

    private fun tryExecuteAction(action: Action): ActionResult {
        val service = JarvisAccessibilityService.instance
            ?: return ActionResult.Failed("Accessibility service is not enabled")

        return try {
            when (action.action) {
                Action.TAP -> {
                    val ok = service.tapByText(action.text.orEmpty())
                    if (ok) ActionResult.Success("Tapped target")
                    else ActionResult.Failed("Tap target was not found or could not be activated")
                }
                Action.TYPE -> {
                    val ok = service.typeText(action.value.orEmpty())
                    if (ok) ActionResult.Success("Entered text")
                    else ActionResult.Failed("Focused text field was not available")
                }
                Action.OPEN_APP -> {
                    val packageName = action.packageName
                        ?: return ActionResult.Failed("Missing application package")
                    val launched = service.openAppByPackage(packageName)
                    if (launched) ActionResult.Success("Application launch requested")
                    else ActionResult.Failed("Application could not be opened")
                }
                Action.PRESS_BACK -> {
                    if (service.pressBack()) ActionResult.Success("Pressed back")
                    else ActionResult.Failed("Back action was not accepted")
                }
                else -> ActionResult.Failed("Action is not supported by the verified executor")
            }
        } catch (error: Exception) {
            ActionResult.Failed(error.message ?: "Action failed")
        }
    }

    private fun parseActionFromLLM(response: String): Action? = try {
        val json = JSONObject(response.trim())
        val actionName = json.optString("action")
        if (actionName !in setOf(Action.TAP, Action.TYPE, Action.OPEN_APP, Action.PRESS_BACK)) {
            null
        } else {
            Action(
                action = actionName,
                text = json.optString("text").takeIf { it.isNotBlank() },
                value = json.optString("value").takeIf { it.isNotBlank() },
                packageName = json.optString("package").takeIf { it.isNotBlank() }
            )
        }
    } catch (_: Exception) {
        null
    }

    data class ExecutionContext(
        val expectedState: String?,
        val phaseId: String,
        val previousPhaseOutcome: String?,
        val screenBefore: String
    )

    sealed class ActionResult {
        data class Success(val message: String) : ActionResult()
        data class Failed(val reason: String) : ActionResult()
    }
}
