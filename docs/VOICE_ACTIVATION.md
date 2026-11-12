# Voice Activation Requirements — OpenJarvis

## User-selected wake phrases

The user has selected two equivalent wake phrases:
- "OK Jarvis"
- "Hey Jarvis"

Either phrase should activate the assistant. The wake phrase should not be limited to the single word "Jarvis".

## Intended interaction

1. While the user has explicitly enabled wake-word mode, the app listens for the selected phrase using a low-power, on-device wake-word detector where technically feasible.
2. Detection of either phrase wakes the assistant and transitions into command listening.
3. Jarvis indicates visibly and audibly that it is listening for the command.
4. The user speaks a command without repeating the wake phrase.
5. Jarvis transcribes the command, presents or interprets it, and only performs consequential actions after the required user confirmation.
6. The assistant returns to the wake-word state after the interaction, or stops listening when the user says a supported stop phrase, disables the feature, revokes microphone permission, or the service is stopped.

## Privacy and safety requirements

- Wake-word listening must be opt-in, clearly shown in the UI, and easy to pause/disable.
- Prefer an offline wake-word engine so raw microphone audio is not continuously sent to cloud services.
- Do not retain continuous audio. Keep only the minimum short rolling audio buffer required by the detector; discard it immediately after detection/non-detection.
- Do not upload microphone audio for wake-word detection.
- The microphone indicator and Android foreground-service notification must make active listening apparent.
- Request RECORD_AUDIO permission at runtime and handle revocation, process death, reboot, and service shutdown.
- Do not silently bypass Android microphone/foreground-service restrictions or OEM battery controls.
- Respect Android versions and HONOR/MagicOS background execution policies. If background/lock-screen listening is not supported in a given state, tell the user rather than pretending it works.
- Provide push-to-talk as a reliable fallback.
- Never automatically send messages, make calls, purchase, delete, publish, or perform other consequential actions merely because speech was detected. Keep confirmation gates.

## Technical investigation before implementation

Evaluate maintained offline wake-word options such as Picovoice Porcupine and openWakeWord, checking Android SDK availability, licensing/commercial terms, model support for "OK Jarvis" and "Hey Jarvis", memory/CPU/battery cost, and compatibility with Android 12+ and HONOR X6C. Do not assume a pretrained model recognizes these custom phrases; custom model training or a supported phrase model may be required.

Also investigate:
- Android foreground microphone service requirements and notification UX
- Android SpeechRecognizer lifecycle and whether it can be started reliably after wake-word detection
- Audio focus and microphone contention between wake-word detector, SpeechRecognizer, and TTS
- TTS echo/self-trigger prevention
- lock-screen behavior, battery optimization, OEM restrictions, and reboot behavior
- explicit stop/pause controls and observability
- tests for false positives, false negatives, noise, Spanish/English speech, and long sessions

## Current implementation status

**Not implemented.** The current code uses Android SpeechRecognizer push-to-talk. It has no always-on wake-word detector and does not implement "OK Jarvis" or "Hey Jarvis" activation. The current VAD-named path is not a real integrated continuous VAD pipeline. Do not present wake-word functionality as available until implemented and tested.

## Acceptance criteria

- Either wake phrase activates the assistant.
- No wake phrase audio is uploaded or stored.
- The assistant listens for the command after activation without requiring a repeated wake phrase.
- Visible microphone/listening state and a working stop/pause control are present.
- Permission denial/revocation, background restrictions, process death, reboot, and microphone conflicts fail safely.
- Battery and thermal use are measured on the HONOR X6C during representative idle and active sessions.
- False activation and missed activation rates are evaluated in quiet and noisy environments.
- Manual push-to-talk continues to work as a fallback.
- No consequential action bypasses the existing explicit confirmation policy.
