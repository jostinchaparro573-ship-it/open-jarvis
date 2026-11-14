package com.openjarvis.vision

/**
 * Coordinates detector lifetime with asynchronous ML Kit tasks.
 *
 * Closing is deferred until every task that successfully started has finished.
 * New tasks are rejected as soon as close is requested.
 */
internal class VisionTaskLifecycle(
    private val closeResources: () -> Unit
) {
    private val lock = Any()
    private var activeTasks = 0
    private var closeRequested = false
    private var resourcesClosed = false

    fun beginTask(): Boolean = synchronized(lock) {
        if (closeRequested) {
            false
        } else {
            activeTasks += 1
            true
        }
    }

    fun finishTask() {
        val shouldClose = synchronized(lock) {
            if (activeTasks > 0) activeTasks -= 1
            closeRequested && activeTasks == 0 && !resourcesClosed
        }
        if (shouldClose) closeOnce()
    }

    fun requestClose() {
        val shouldClose = synchronized(lock) {
            closeRequested = true
            activeTasks == 0 && !resourcesClosed
        }
        if (shouldClose) closeOnce()
    }

    private fun closeOnce() {
        val shouldClose = synchronized(lock) {
            if (resourcesClosed || !closeRequested || activeTasks != 0) {
                false
            } else {
                resourcesClosed = true
                true
            }
        }
        if (shouldClose) closeResources()
    }
}
