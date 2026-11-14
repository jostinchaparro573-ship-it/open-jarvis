package com.openjarvis.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device OCR for user-selected images and explicitly authorized screen captures.
 *
 * This class only extracts visible text. It does not upload images or call a cloud provider.
 */
class VisionModule(context: Context) {

    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
    private val released = AtomicBoolean(false)

    suspend fun extractText(bitmap: Bitmap): String =
        recognize(bitmap) { visionText -> visionText.text }

    suspend fun extractStructured(bitmap: Bitmap): ScreenOCR =
        recognize(bitmap) { visionText ->
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <T> recognize(
        bitmap: Bitmap,
        transform: (com.google.mlkit.vision.text.Text) -> T
    ): T = suspendCancellableCoroutine { continuation ->
        if (released.get()) {
            continuation.resumeWithException(IllegalStateException("VisionModule has been released"))
            return@suspendCancellableCoroutine
        }

        // ML Kit can keep processing after the caller is cancelled. Give it an owned
        // bitmap so the UI can safely recycle its decoded image in its finally block.
        val processingBitmap = try {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } catch (error: Exception) {
            continuation.tryResumeWithException(error)?.let(continuation::completeResume)
            return@suspendCancellableCoroutine
        }

        if (processingBitmap == null) {
            continuation.tryResumeWithException(
                IllegalStateException("Could not create an image copy for OCR")
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
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    try {
                        if (continuation.isActive) {
                            val value = transform(visionText)
                            continuation.tryResume(value)?.let(continuation::completeResume)
                        }
                    } catch (error: Exception) {
                        continuation.tryResumeWithException(error)?.let(continuation::completeResume)
                    } finally {
                        releaseProcessingBitmap()
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "On-device image text recognition failed", error)
                    releaseProcessingBitmap()
                    continuation.tryResumeWithException(error)?.let(continuation::completeResume)
                }
        } catch (error: Exception) {
            releaseProcessingBitmap()
            continuation.tryResumeWithException(error)?.let(continuation::completeResume)
        }
    }

    /**
     * Release this recognizer only when its owning application component is permanently done.
     * The process-wide singleton should normally live for the application process.
     */
    fun release() {
        if (released.compareAndSet(false, true)) {
            recognizer.close()
            synchronized(this) {
                if (instance === this) instance = null
            }
        }
    }

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

        @Volatile
        private var instance: VisionModule? = null

        fun getInstance(context: Context): VisionModule {
            return instance ?: synchronized(this) {
                instance ?: VisionModule(context.applicationContext).also { instance = it }
            }
        }
    }
}
