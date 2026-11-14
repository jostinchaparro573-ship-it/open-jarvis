# Jarvis Vision & Image Module

## Current implementation

The dashboard includes a user-initiated photo picker based on Android's Storage Access Framework (GetContent). Jarvis receives only the image selected by the user; it does not request broad gallery access.

The selected image is decoded off the main thread and downsampled so its largest dimension is no more than 2048 pixels before OCR. This reduces memory pressure on entry-level Android devices.

Text recognition uses the bundled ML Kit Latin text-recognition model through VisionModule. Basic image labeling uses ML Kit's bundled image-labeling model (approximately 5.7 MB added to the app). Both run on-device without requiring a cloud inference API. The selected image is not uploaded to a remote provider by these features.

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
