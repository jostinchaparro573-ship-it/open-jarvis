package com.openjarvis.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LLMResponseValidatorTest {

    @Test
    fun acceptsImplementedOpenAppAction() {
        val result = LLMResponseValidator.validate(
            """[{"action":"open_app","label":"Spotify"}]"""
        )

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.actions.single().action == Action.OPEN_APP)
    }

    @Test
    fun rejectsUnsupportedAction() {
        val result = LLMResponseValidator.validate(
            """[{"action":"tap_coords","x":120,"y":300}]"""
        )

        assertFalse(result.isValid)
        assertTrue(result.actions.isEmpty())
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun rejectsWholePlanWhenOneActionIsUnsupported() {
        val result = LLMResponseValidator.validate(
            """[{"action":"open_app","label":"Spotify"},{"action":"swipe","direction":"up"}]"""
        )

        assertFalse(result.isValid)
        assertTrue(result.actions.isEmpty())
    }

    @Test
    fun rejectsActionWithMissingRequiredArgument() {
        val result = LLMResponseValidator.validate(
            """[{"action":"tap"}]"""
        )

        assertFalse(result.isValid)
        assertTrue(result.actions.isEmpty())
    }
}
