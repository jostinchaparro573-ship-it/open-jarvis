package com.openjarvis.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class VisionTaskLifecycleTest {

    @Test
    fun closeIsDeferredUntilActiveTaskFinishes() {
        val closeCount = AtomicInteger()
        val lifecycle = VisionTaskLifecycle { closeCount.incrementAndGet() }

        assertTrue(lifecycle.beginTask())
        lifecycle.requestClose()

        assertEquals(0, closeCount.get())
        assertFalse(lifecycle.beginTask())

        lifecycle.finishTask()
        assertEquals(1, closeCount.get())
    }

    @Test
    fun multipleActiveTasksMustAllFinishBeforeClose() {
        val closeCount = AtomicInteger()
        val lifecycle = VisionTaskLifecycle { closeCount.incrementAndGet() }

        assertTrue(lifecycle.beginTask())
        assertTrue(lifecycle.beginTask())
        lifecycle.requestClose()

        lifecycle.finishTask()
        assertEquals(0, closeCount.get())

        lifecycle.finishTask()
        assertEquals(1, closeCount.get())
    }

    @Test
    fun closeWithoutActiveTasksHappensImmediatelyAndOnlyOnce() {
        val closeCount = AtomicInteger()
        val lifecycle = VisionTaskLifecycle { closeCount.incrementAndGet() }

        lifecycle.requestClose()
        lifecycle.requestClose()
        lifecycle.finishTask()

        assertEquals(1, closeCount.get())
        assertFalse(lifecycle.beginTask())
    }
}
