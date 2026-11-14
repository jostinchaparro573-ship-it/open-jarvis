# Jarvis Vision & Image Module

## Current implementation

The dashboard includes a user-initiated photo picker based on Android's Storage Access Framework (GetContent). Jarvis receives only the image selected by the user; it does not request broad gallery access.

The selected image is decoded off the main thread and downsampled so its largest dimension is no more than 2048 pixels before OCR. This reduces memory pressure on entry-level Android devices.

Text recognition uses the bundled ML Kit Latin text-recognition model through VisionModule. Recognition runs on-device. The selected image is not uploaded to a remote provider by this feature.

## Current limits

- This feature extracts visible text; it does not yet understand the semantic content of a photograph.
- It does not yet answer free-form questions about objects, scenes, diagrams, or people in a photo.
- It does not generate or edit images.
- The current OCR recognizer is configured for Latin script. Other scripts require additional recognizer support and testing.
- OCR quality depends on focus, lighting, orientation, handwriting, and image resolution.

## Next milestones

1. Add camera capture using Android's camera APIs, without requesting unrelated permissions.
2. Add an image-understanding interface with explicit cloud-consent checks before any selected image leaves the device.
3. Evaluate a genuinely on-device image-understanding model against HONOR X6C memory, storage, latency, and thermal limits.
4. Evaluate lightweight local drawing generation for simple school illustrations. Do not advertise this as unlimited until the model and device performance are measured.
5. Add tests for image decoding bounds, cancellation, OCR failures, and privacy boundaries.

## Cost and privacy policy

- Local photo selection and OCR must not require a paid API.
- Never upload a selected photo silently.
- Any cloud-based image analysis must disclose the selected provider and require explicit consent.
- Image generation must not silently fall back to a paid service. If no free/local option is available, explain that limitation.
