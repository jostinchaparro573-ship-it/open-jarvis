package com.openjarvis.agent

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object LLMResponseValidator {
    
    // Only actions currently implemented by AgentCore are accepted.
    // Unsupported actions fail validation instead of being silently ignored.
    private val knownActions = setOf(
        "open_app", "tap", "type", "press_back", "press_home",
        "press_recents", "extract_text", "error"
    )
    
    fun validate(rawResponse: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        val clean = rawResponse
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        
        if (!clean.startsWith("[") && !clean.startsWith("{")) {
            errors.add("Response is not JSON")
            return ValidationResult(false, emptyList(), errors, false)
        }
        
        val parsed = try {
            if (clean.startsWith("{")) {
                JSONArray("[$clean]")
            } else {
                JSONArray(clean)
            }
        } catch (e: JSONException) {
            errors.add("Invalid JSON: ${e.message}")
            return ValidationResult(false, emptyList(), errors, false)
        }
        
        val actions = mutableListOf<Action>()
        
        for (i in 0 until parsed.length()) {
            val obj = try {
                parsed.getJSONObject(i)
            } catch (e: Exception) {
                errors.add("Item $i is not an object")
                continue
            }
            
            val actionType = obj.optString("action", "")
            if (actionType.isBlank()) {
                errors.add("Item $i missing 'action' field")
                continue
            }
            
            if (actionType !in knownActions) {
                errors.add("Unsupported action type: $actionType — skipping")
                continue
            }

            val missingRequiredField = when (actionType) {
                "open_app" -> obj.optString("label").isBlank()
                "tap" -> obj.optString("text").isBlank()
                "type" -> !obj.has("value") || obj.isNull("value")
                "error" -> obj.optString("message").isBlank()
                else -> false
            }
            if (missingRequiredField) {
                errors.add("Action $actionType is missing a required field")
                continue
            }

            actions.add(parseAction(obj))
        }
        
        if (actions.size > 50) {
            errors.add("Suspiciously long action plan (${actions.size} steps)")
            return ValidationResult(false, emptyList(), errors, false)
        }

        // Fail closed: never execute a partial plan after skipping unsupported or malformed actions.
        return ValidationResult(
            isValid = actions.isNotEmpty() && errors.isEmpty(),
            actions = if (errors.isEmpty()) actions else emptyList(),
            errors = errors,
            wasRepaired = false
        )
    }
    
    private fun parseAction(obj: JSONObject): Action {
        return Action(
            action = obj.getString("action"),
            packageName = obj.optString("package", null).takeIf { it.isNotBlank() },
            label = obj.optString("label", null).takeIf { it.isNotBlank() },
            text = obj.optString("text", null).takeIf { it.isNotBlank() },
            value = obj.optString("value", null).takeIf { it.isNotBlank() },
            hint = obj.optString("hint", null).takeIf { it.isNotBlank() },
            direction = obj.optString("direction", null).takeIf { it.isNotBlank() },
            x = if (obj.has("x")) obj.getInt("x") else null,
            y = if (obj.has("y")) obj.getInt("y") else null,
            distance = obj.optString("distance", null).takeIf { it.isNotBlank() },
            timeoutMs = obj.optLong("timeout_ms", 3000L),
            message = obj.optString("message", null).takeIf { it.isNotBlank() },
            prompt = obj.optString("prompt", null).takeIf { it.isNotBlank() },
            outputKey = obj.optString("outputKey", null).takeIf { it.isNotBlank() }
        )
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val actions: List<Action>,
        val errors: List<String>,
        val wasRepaired: Boolean
    )
}