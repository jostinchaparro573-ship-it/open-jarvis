package com.openjarvis.vision

/**
 * Calculates a power-of-two BitmapFactory sample size without upscaling.
 * Invalid dimensions return 1 so callers can report the decode failure themselves.
 */
internal fun calculateInSampleSize(
    width: Int,
    height: Int,
    maxDimension: Int = 2048
): Int {
    if (width <= 0 || height <= 0 || maxDimension <= 0) return 1

    var sampleSize = 1
    // Use ceiling division: floor division can leave an odd-sized bitmap one pixel
    // above the intended maximum (for example, 4097 / 2 = 2048 by floor, but 2049 actual).
    fun scaledDimension(dimension: Int): Long =
        (dimension.toLong() + sampleSize - 1L) / sampleSize

    while (scaledDimension(width) > maxDimension ||
        scaledDimension(height) > maxDimension
    ) {
        if (sampleSize > Int.MAX_VALUE / 2) return sampleSize
        sampleSize *= 2
    }
    return sampleSize
}
