package com.openjarvis.graphify

import com.openjarvis.graphify.nodes.ContactNode
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * Conservative fuzzy matching for contact names.
 * Only small spelling differences are accepted; this is not identity verification.
 */
internal object ContactNameMatcher {

    private data class Candidate(val contact: ContactNode, val distance: Int)

    fun findBestMatch(query: String, candidates: List<ContactNode>): ContactNode? {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank() || normalizedQuery.length > MAX_NAME_LENGTH) return null

        val queryTokens = normalizedQuery.split(' ').filter(String::isNotBlank)
        val maxDistance = if (normalizedQuery.length <= 4) 1 else 2

        return candidates.asSequence()
            .mapNotNull { contact ->
                val candidateName = normalize(contact.name)
                if (candidateName.isBlank() || candidateName.length > MAX_NAME_LENGTH) {
                    return@mapNotNull null
                }

                val candidateTokens = candidateName.split(' ').filter(String::isNotBlank)
                val distance = if (queryTokens.all { it in candidateTokens }) {
                    0
                } else {
                    levenshteinDistance(normalizedQuery, candidateName)
                }

                if (distance > maxDistance || abs(normalizedQuery.length - candidateName.length) > maxDistance) {
                    null
                } else {
                    Candidate(contact, distance)
                }
            }
            .sortedWith(
                compareBy<Candidate> { it.distance }
                    .thenByDescending { it.contact.contactCount }
                    .thenByDescending { it.contact.lastContacted }
            )
            .firstOrNull()
            ?.contact
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun levenshteinDistance(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }

        for (leftIndex in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = leftIndex + 1

            for (rightIndex in right.indices) {
                val substitutionCost = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + substitutionCost
                )
            }
            previous = current
        }

        return previous[right.length]
    }

    private const val MAX_NAME_LENGTH = 64
}
