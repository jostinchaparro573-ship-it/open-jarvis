# Jarvis Platform Support Policy

Status: product and engineering requirement. This document defines the supported platform targets; it does not mean the Windows or Android builds are already implemented or tested.

## Supported platforms

### Android
- Minimum supported OS: **Android 12 (API 31)**.
- Target: current stable Android release and the latest supported Android SDK at build time.
- Maintain compatibility with Android 12 and later supported releases, including current Android versions, subject to device/OEM restrictions.
- Primary first test device: HONOR X6C running Android 15 / MagicOS 9.
- Do not add Android 11 or earlier to the supported-device matrix.
- Use Android version-aware APIs and permissions. Check foreground-service, microphone, notification, accessibility, background execution, and battery restrictions on each major Android version.
- Optional capabilities must degrade gracefully when a device or OEM does not support them. Never bypass platform privacy or permission restrictions.

### Windows
- Supported desktop platform: **Windows 11 and later Windows releases**.
- Build a native Windows application or a clearly separated Windows client; do not assume Android code can run unchanged on Windows.
- Verify compatibility with currently supported Windows 11 releases and future supported Windows releases through CI and real-device testing.
- Use supported Windows APIs and request only the permissions needed for each feature.
- Voice activation, startup behavior, notifications, file access, and background operation must follow Windows security and privacy controls.

## Shared product behavior
- Keep the same core Jarvis identity, safety charter, user-controlled permissions, and confirmation rules on both platforms.
- Keep platform-specific capabilities behind separate adapters/modules so Android-only features do not break Windows and vice versa.
- Keep user profiles and memories isolated by account. If synchronization is implemented, use authenticated, encrypted sync and explicit user controls.
- Spanish is the primary language and English is secondary on both platforms.
- Offline/basic functionality should be designed where practical; clearly indicate when a feature requires internet or a platform service.

## Safety and privacy parity
- The Human-First Safety Charter applies equally to Android and Windows, in Spanish and English.
- A feature must not be enabled on one platform if it bypasses the same safety protections required on the other.
- Require explicit permission and clear user-visible status for microphone, camera, location, notifications, accessibility/UI automation, and file access.
- Require confirmation for consequential actions such as sending messages, calls, sharing data, deleting files, purchases, or changing security settings.
- Do not silently monitor people, record ambient audio, or access another person's private information.

## iPhone / iOS
- iOS is **out of scope for the current phase**. Do not spend implementation effort on iPhone until Windows and Android targets are stable and the user explicitly reopens the decision.

## Release gates
- Maintain separate Android and Windows build/test pipelines.
- Test minimum supported OS versions and current supported OS versions on real devices or reliable emulators/VMs.
- Test install, upgrade, permissions, revocation, offline behavior, crashes, resource use, voice, safety refusals, and data isolation on each platform.
- Never claim platform compatibility until the corresponding build and tests have passed.
