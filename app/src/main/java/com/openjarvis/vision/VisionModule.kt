package com.openjarvis.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.labeling.ImageLabeling
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * On-device image text extraction and basic image labels.
 *
 * Both features run locally. This module never uploads images or calls a cloud provider.
 */
class VisionModule private constructor() {

    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
    private val imageLabeler: ImageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.DEFAULT_OPTIONS
    )
    private val released = AtomicBoolean(false)

    suspend fun extractText(bitmap: Bitmap): String =
        processImage(bitmap, recognizer::process) { visionText -> visionText.text }

    suspend fun extractStructured(bitmap: Bitmap): ScreenOCR =
        processImage(bitmap, recognizer::process) { visionText ->
            val blocks = visionText.textBlocks.map { block ->
                OCRBlock(
                    text = block.text,
                    confidence = 0f,
                    boundingBox = block.boundingBox?.let { rect ->
                        BoundingBox(rect.left, rect.top, rect.right, rect.bottom)
                    },
                    lines = block.lines.map { line ->
                        OCRLine(
                            text = line.text,
                            confidence = 0f,
                            boundingBox = line.boundingBox?.let { rect ->
                                BoundingBox(rect.left, rect.top, rect.right, rect.bottom)
                            }
                        )
                    }
                )
            }
            ScreenOCR(fullText = visionText.text, blocks = blocks)
        }

    /** Returns basic object/scene labels, not a natural-language explanation. */
    suspend fun labelImage(bitmap: Bitmap): List<ImageTag> =
        processImage(bitmap, imageLabeler::process) { labels ->
            labels
                .sortedByDescending { it.confidence }
                .take(MAX_IMAGE_LABELS)
                .map { label ->
                    ImageTag(
                        label = label.text,
                        confidence = label.confidence,
                        index = label.index
                    )
                }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <T, R> processImage(
        bitmap: Bitmap,
        taskFactory: (InputImage) -> Task<T>,
        transform: (T) -> R
    ): R = suspendCancellableCoroutine { continuation ->
        if (released.get()) {
            continuation.resumeWithException(IllegalStateException("VisionModule has been released"))
            return@suspendCancellableCoroutine
        }

        // ML Kit may continue after coroutine cancellation. Keep an owned copy until
        // its Task completes, so callers can safely recycle their decoded bitmap.
        val processingBitmap = try {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } catch (error: Exception) {
            continuation.tryResumeWithException(error)?.let(continuation::completeResume)
            return@suspendCancellableCoroutine
        }

        if (processingBitmap == null) {
            continuation.tryResumeWithException(
                IllegalStateException("Could not create an image copy for on-device analysis")
            )?.let(continuation::completeResume)
            return@suspendCancellableCoroutine
        }

        val bitmapReleased = AtomicBoolean(false)
        fun releaseProcessingBitmap() {
            if (bitmapReleased.compareAndSet(false, true) && !processingBitmap.isRecycled) {
                processingBitmap.recycle()
            }
        }

        try {
            val image = InputImage.fromBitmap(processingBitmap, 0)
            taskFactory(image)
                .addOnSuccessListener { result ->
                    try {
                        if (continuation.isActive) {
                            val value = transform(result)
                            continuation.tryResume(value)?.let(continuation::completeResume)
                        }
                    } catch (error: Exception) {
                        continuation.tryResumeWithException(error)?.let(continuation::completeResume)
                    } finally {
                        releaseProcessingBitmap()
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "On-device image analysis failed", error)
                    releaseProcessingBitmap()
                    continuation.tryResumeWithException(error)?.let(continuation::completeResume)
                }
        } catch (error: Exception) {
            releaseProcessingBitmap()
            continuation.tryResumeWithException(error)?.let(continuation::completeResume)
        }
    }

    /** Release only when the application process is permanently done with this singleton. */
    fun release() {
        if (released.compareAndSet(false, true)) {
            recognizer.close()
            imageLabeler.close()
            synchronized(this) {
                if (instance === this) instance = null
            }
        }
    }

    data class ImageTag(
        val label: String,
        val confidence: Float,
        val index: Int
    )

    data class ScreenOCR(
        val fullText: String,
        val blocks: List<OCRBlock>
    )

    data class OCRBlock(
        val text: String,
        val confidence: Float,
        val boundingBox: BoundingBox?,
        val lines: List<OCRLine>
    )

    data class OCRLine(
        val text: String,
        val confidence: Float,
        val boundingBox: BoundingBox?
    )

    data class BoundingBox(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    companion object {
        private const val TAG = "VisionModule"
        private const val MAX_IMAGE_LABELS = 5

        @Volatile
        private var instance: VisionModule? = null

        @Suppress("UNUSED_PARAMETER")
        fun getInstance(context: Context): VisionModule {
            return instance ?: synchronized(this) {
                instance ?: VisionModule().also { instance = it }
            }
        }
    }
}
