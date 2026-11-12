package com.openjarvis.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

class ScreenReader(private val service: JarvisAccessibilityService) {

    fun extractAllText(): String {
        val rootNode = service.rootInActiveWindow ?: return ""
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        val builder = StringBuilder()
        pending.add(rootNode)

        try {
            while (pending.isNotEmpty()) {
                val current = pending.removeFirst()
                try {
                    current.text?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { builder.append(it).append(' ') }

                    current.contentDescription?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { builder.append(it).append(' ') }

                    for (index in 0 until current.childCount) {
                        current.getChild(index)?.let(pending::addLast)
                    }
                } finally {
                    current.recycle()
                }
            }
        } finally {
            recycleRemaining(pending)
        }

        return builder.toString().trim()
    }

    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        if (text.isBlank()) return null
        val rootNode = service.rootInActiveWindow ?: return null
        val normalizedText = text.trim().lowercase()
        return findNode(rootNode) { node ->
            node.text?.toString()?.lowercase()?.contains(normalizedText) == true ||
                node.contentDescription?.toString()?.lowercase()?.contains(normalizedText) == true
        }
    }

    fun findNodeByHint(hint: String): AccessibilityNodeInfo? {
        if (hint.isBlank()) return null
        val rootNode = service.rootInActiveWindow ?: return null
        val normalizedHint = hint.trim().lowercase()
        return findNode(rootNode) { node ->
            node.hintText?.toString()?.lowercase()?.contains(normalizedHint) == true
        }
    }

    private fun findNode(
        rootNode: AccessibilityNodeInfo,
        matches: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        var match: AccessibilityNodeInfo? = null
        pending.add(rootNode)

        try {
            while (pending.isNotEmpty()) {
                val current = pending.removeFirst()
                try {
                    if (matches(current)) {
                        // Return an independent copy; every traversal node is recycled here.
                        match = AccessibilityNodeInfo.obtain(current)
                        break
                    }

                    for (index in 0 until current.childCount) {
                        current.getChild(index)?.let(pending::addLast)
                    }
                } finally {
                    current.recycle()
                }
            }
        } finally {
            recycleRemaining(pending)
        }

        return match
    }

    private fun recycleRemaining(nodes: ArrayDeque<AccessibilityNodeInfo>) {
        while (nodes.isNotEmpty()) {
            nodes.removeFirst().recycle()
        }
    }

    fun getFocusedNode(): AccessibilityNodeInfo? {
        val rootNode = service.rootInActiveWindow ?: return null
        return try {
            rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } finally {
            rootNode.recycle()
        }
    }
}
