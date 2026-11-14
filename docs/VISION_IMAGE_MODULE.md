# Jarvis Vision & Image Module

## Current implementation

The dashboard includes a user-initiated photo picker based on Android's Storage Access Framework (GetContent). Jarvis receives only the image selected by the user; it does not request broad gallery access.

The selected image is decoded off the main thread and downsampled so its largest dimension is no more than 2048 pixels before OCR and image labeling. The downsampling calculation accounts for odd image dimensions. Bitmap copies and local model preprocessing also run away from the UI thread. This reduces memory pressure and UI stalls on entry-level Android devices.

Text recognition uses the bundled ML Kit Latin text-recognition model through VisionModule. Basic image labeling uses ML Kit's bundled image-labeling model (approximately 5.7 MB added to the app). Both run on-device without requiring a cloud inference API. The selected image is not uploaded to a remote provider by these features.

## On-device generative image understanding evaluation

The official ML Kit GenAI Image Description API uses Gemini Nano through Android AICore and can produce a short image description. It is not a general-purpose, free-form visual question-answering API. The documented language support is currently English, and availability must be checked at runtime with `checkFeatureStatus()`.

The official supported-device list does not currently include the HONOR X6C. The X6C's Helio G81 Ultra and Android 15 specifications do not establish Gemini Nano/AICore compatibility. Therefore, Jarvis must not add this API as a required dependency or present it as available on the target phone. Revisit only if Google adds the exact device or runtime checks on the physical X6C confirm support. If added later, treat it as an optional capability and retain the current local OCR/labels when unavailable.

References:
- [ML Kit GenAI overview and supported devices](https://developers.google.com/ml-kit/genai)
- [ML Kit GenAI Image Description API](https://developers.google.cn/ml-kit/genai/image-description/android?hl=en)
- [HONOR X6c official specifications](https://www.honor.com/latam/phones/honor-x6c/spec/)

## Current limits

- The module extracts visible text and returns up to five general image labels from ML Kit's on-device image-labeling model.
- Labels are broad predictions (for example, common objects or scene categories), not a detailed explanation or guaranteed identification.
- It does not yet answer free-form questions about objects, scenes, diagrams, or people in a photo.
- It does not generate or edit images.
- The current OCR recognizer is configured for Latin script. Other scripts require additional recognizer support and testing.
- OCR quality depends on focus, lighting, orientation, handwriting, and image resolution.

## Next milestones

1. Add camera capture using Android's camera APIs, without requesting unrelated permissions.
2. Add an image-understanding interface with explicit cloud-consent checks before any selected image leaves the device.
3. Evaluate a genuinely on-device image-understanding model against HONOR X6C memory, storage, latency, and thermal limits.
4. Evaluate lightweight local drawing generation for simple school illustrations. Do not advertise this as unlimited until the model and device performance are measured.
5. Add tests for image decoding bounds, cancellation, OCR/labeling failures, and privacy boundaries. The power-of-two downsampling calculation now has JVM unit tests.

## Cost and privacy policy

- Local photo selection and OCR must not require a paid API.
- Never upload a selected photo silently.
- Any cloud-based image analysis must disclose the selected provider and require explicit consent.
- Image generation must not silently fall back to a paid service. If no free/local option is available, explain that limitation.
