package com.openjarvis.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageDecodeSizingTest {

    @Test
    fun smallImagesAreNotUpscaled() {
        assertEquals(1, calculateInSampleSize(640, 480))
    }

    @Test
    fun landscapeImageIsReducedBelowMaximumDimension() {
        assertEquals(4, calculateInSampleSize(8000, 4000))
    }

    @Test
    fun portraitImageUsesItsTallerDimension() {
        assertEquals(8, calculateInSampleSize(3000, 9000))
    }

    @Test
    fun invalidDimensionsReturnSafeDefault() {
        assertEquals(1, calculateInSampleSize(0, 9000))
        assertEquals(1, calculateInSampleSize(9000, -1))
        assertEquals(1, calculateInSampleSize(9000, 9000, 0))
    }

    @Test
    fun customMaximumDimensionIsRespected() {
        assertEquals(8, calculateInSampleSize(16000, 9000, 2048))
        assertEquals(1, calculateInSampleSize(1024, 768, 4096))
    }
}
