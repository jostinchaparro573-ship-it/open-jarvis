package com.openjarvis.graphify

import com.openjarvis.graphify.nodes.ContactNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactNameMatcherTest {

    @Test
    fun matchesSmallSpellingDifferences() {
        val contact = ContactNode(name = "John Doe", contactCount = 5)

        assertEquals(
            "John Doe",
            ContactNameMatcher.findBestMatch("Jhon Doe", listOf(contact))?.name
        )
        assertEquals(
            "John Doe",
            ContactNameMatcher.findBestMatch("Jon Doe", listOf(contact))?.name
        )
    }

    @Test
    fun matchesAnExactNameTokenWithinAFullContactName() {
        val contact = ContactNode(name = "John Doe")

        assertEquals(
            "John Doe",
            ContactNameMatcher.findBestMatch("john", listOf(contact))?.name
        )
    }

    @Test
    fun normalizesAccentsAndCase() {
        val contact = ContactNode(name = "José Álvarez")

        assertEquals(
            "José Álvarez",
            ContactNameMatcher.findBestMatch("jose alvarez", listOf(contact))?.name
        )
    }

    @Test
    fun rejectsUnrelatedNamesAndBlankQueries() {
        val contact = ContactNode(name = "John Doe")

        assertNull(ContactNameMatcher.findBestMatch("Mariana Pérez", listOf(contact)))
        assertNull(ContactNameMatcher.findBestMatch("   ", listOf(contact)))
    }
}
