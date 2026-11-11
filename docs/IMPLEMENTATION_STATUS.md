# OpenJarvis / Jarvis — Implementation Status

Last reviewed: 2026-11-10  
Working branch: `jarvis/foundation-fixes-2026-11-09`

This file separates changes visible in source from features that are still only planned. Static code review is not a substitute for a successful build or device test.

## Platform targets

| Area | Current status |
|---|---|
| Android 12+ | Minimum SDK is set to API 31. Build and device compatibility are not yet verified. |
| Windows 11+ | Target is documented; a Windows client has not been implemented. |
| iOS | Out of scope for this phase. |

## Changes made in this branch

- Fixed duplicate companion-object declarations in the notification listener and socket bridge.
- Fixed several Android source references/imports and the launcher activity path.
- Added the human-first safety charter and included its principles in the agent prompt.
- Made screen reading optional at agent initialization so basic tasks are not blocked merely because Accessibility is off.
- Restricted generated agent plans to actions currently implemented, validate required arguments, and reject partial plans containing unsupported or malformed actions.
- Agent execution now reports failure when supported accessibility actions do not actually execute, rather than always recording success.
- Corrected the automation Room DAO/entity mismatch, added a schema migration, and separated periodic work from one-time work scheduling.
- Fixed a no-op Graphify pattern update so time-of-day masks are actually persisted through a Room update query.
- Confirmed the app display name as **OpenJarvis** and updated the launcher/app-name resource.
- Automation commands are deliberately **not executed** yet. The worker records that execution is unavailable and fails closed; it must not report false success.
- Disabled the unauthenticated TCP bridge until a secure local IPC/authentication design is implemented. The socket is loopback-only and rejects requests.
- Added an Android CI workflow for Kotlin compilation, unit tests, and lint. It does not build or publish an APK.
- Fixed Compose alpha/scale modifier usage in the dashboard.
- Removed a call to a nonexistent VoiceManager initialization method; voice engines are constructed by VoiceManager.
- Changed app startup so the basic dashboard is available without forcing Accessibility or overlay permissions. These permissions must remain optional and feature-specific.

## Important incomplete areas

- CI validation is mixed and must be read per commit: run `43104889690` passed Kotlin compilation, unit tests, and lint at commit `6740355e`; later changes were made after that success. The latest run before the current fixes, `43115522287` at commit `55ebb671`, failed in Kotlin compilation because `OverlayService.kt` imported ComposeView's internal `setContent` symbol and could not resolve `ViewTreeLifecycleOwner`. The latest branch now contains a correction removing the invalid import, cleaning duplicate task loading, and explicitly declaring lifecycle-runtime; a fresh CI run is required to verify it. Unit tests and lint were skipped after the compile failure. No physical-device test or APK build has been verified.
- The Android notification listener is not registered in the manifest. Its existing broad notification handling must be replaced with explicit opt-in and a strict WhatsApp group allowlist for the requested `11-2` group before enabling it.
- Voice recognition is not yet connected end-to-end to AgentCore; there is no verified always-on “Jarvis” wake phrase.
- Spanish-first speech output and bilingual voice selection remain incomplete.
- Google sign-in, account-isolated memory, encrypted cloud synchronization, and backup/restore are not implemented.
- The three laws are documented and partially reflected in plan validation, but independent safety checks, confirmation gates, permission allowlists, and adversarial tests remain release blockers.
- The Windows 11+ client and its separate build/test pipeline are not implemented.
- HONOR X6C side-button integration and real-device background microphone behavior remain unverified.

## New requested capability — autonomous phone-call agent

The project scope now includes a future voice-call agent that can, only after the user explicitly authorizes each call, contact businesses such as dental clinics, identify itself as an AI assistant, speak naturally in Spanish and English, ask about availability, negotiate alternative appointment times, read back the proposed details, and book only after the user-defined confirmation policy is satisfied. It must never impersonate a human, invent calendar availability, disclose unnecessary personal data, or claim a booking succeeded without confirmation from the recipient or booking system.

This capability is **planned, not implemented**. It depends on end-to-end speech, real-time conversational audio, a compliant outbound telephony/VoIP provider, call-state handling, user consent and confirmation gates, and an appointment/calendar record with verified outcomes. Android SIM-call restrictions mean the design must not assume unrestricted control of native cellular calls.

## Release rule

Do not generate or install a release APK, publish to an app store, or merge this branch into `main` until the core features, safety controls, build, tests, privacy review, and HONOR X6C validation have passed and the user has explicitly approved the next step.
