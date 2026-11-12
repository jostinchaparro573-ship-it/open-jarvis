# OpenJarvis — Ongoing Repository Audit

## Audit status

This is an ongoing static audit of the working branch jarvis/foundation-fixes-2026-11-09. It is not a release approval or an independent penetration test. The main branch remains untouched.

## Latest verified CI

Commit 6e7976cec8f1d321f1839c570c1641bc52eeba60 passed all three workflow steps:
- Kotlin compilation
- JVM unit tests
- Android lint

The workflow does not run Android instrumentation tests and does not test on a physical device.

## Corrections completed in the current branch

### Agent task concurrency
AgentCore uses a Mutex. The per-task working-memory reset now occurs inside the locked section, preventing a queued task from resetting memory while another task is executing.

### Accessibility node ownership
ScreenReader traversal now recycles visited nodes, drains queued nodes on early exit, and returns an independent copy for a matching node. CI passes. Android device behavior remains unverified.

### Action contract and bypasses
The LLM validator rejects unsupported and malformed plans. TAP and TYPE are removed from the accepted action contract. The identified direct paths in ActionExecutor, ScreenWatcher, and AIAppInteractor are also blocked until explicit confirmation is implemented. This is a temporary safety limitation, not a completed confirmation workflow.

### Service lifecycle and local bridge
AgentCore has a cancellable scope and is released when OverlayService is destroyed. MainActivity no longer creates an unused duplicate AgentCore. OverlayService does not start the unauthenticated TCP server.

### Debug logging
HTTP logging is BASIC rather than BODY, avoiding request/response body and header logging in debug builds.

### Contact matching
Graphify now has conservative local fuzzy matching for minor spelling differences, with JVM tests. A failed test exposed a whole-token length-filter bug; that rule was corrected and the subsequent CI run passed.

### Dashboard truthfulness
The dashboard Start overlay button now calls the real start callback. Its status no longer falsely claims that the system is active, and app/pattern counts are repository-derived rather than hardcoded zeros.

## Open findings and blockers

### Critical — account isolation is absent
Graphify uses a single graphify.db. Task, contact, pattern, provider, and edge records have no account/profile key, and DAO queries are not scoped. Google sign-in and an account-session model are not implemented. This fails the requirement that each Google account starts with independent memory.

Do not add accountId fields as a superficial fix. Design a stable profile identity, local-profile behavior, account switching, per-profile storage/credentials/automations, Room migrations, and tests proving no cross-profile access.

### High — database privacy
Graphify is not encrypted with SQLCipher or another app-level database encryption layer. Android sandbox/device encryption are not the same as application-level database encryption. No complete memory deletion/retention controls exist.

### High — cloud data disclosure
When a cloud provider is selected, user commands, system context, explicitly requested screen text, and included Graphify memory may be sent to the provider. Provider retention and privacy terms apply. The app must disclose this clearly and avoid claims that all data stays local.

### Medium — Room migration/test coverage
GraphifyDB is version 3 and only registers migration 2 to 3. Schema export is disabled. Android instrumentation tests exist but are not run by CI; migration and profile-isolation tests are missing.

### Medium — incomplete features must remain labelled as such
- Notification listener is not registered in the manifest.
- Automation scheduling does not execute user commands; the worker fails closed.
- ScreenWatcher is not integrated as a production feature; its direct tap/speech paths are blocked, and further lifecycle work is needed.
- ProviderFallbackChain currently returns the active provider regardless of the requested fallback name.
- AppBuilderMode does not perform an actual Gradle build or ZIP package.
- PromptEngine and AIAppInteractor require call-site and integration review before being described as working features.
- SettingsScreen does not yet load all saved provider/model settings.
- Voice is push-to-talk transcript input with manual Send; no wake phrase or hands-free execution. User-selected phrases are “OK Jarvis” and “Hey Jarvis”; implementation and device validation remain open.

### Medium — accessibility thread and device behavior
ScreenReader and AccessibilityService interactions still require thread/lifecycle validation and physical-device tests. No HONOR X6C test has been run.

### Medium — provenance and licensing
The upstream repository is MIT-licensed, but every bundled third-party dependency, model, icon, and resource still needs a license inventory before public distribution. MIT does not grant trademark rights or imply endorsement.

## Next audit sequence

1. Complete the permission, voice, and accessibility lifecycle review.
2. Design account/profile isolation before changing the Room schema.
3. Decide Graphify encryption, retention, deletion, and migration strategy.
4. Add Room migration and Android instrumentation tests to CI.
5. Correct provider settings and disclose cloud data transfer.
6. Audit all remaining screen interaction paths and placeholder modules.
7. Test on HONOR X6C Android 15/MagicOS 9.
8. Complete third-party license/provenance review.
9. Implement and validate opt-in offline “OK Jarvis” / “Hey Jarvis” detection under `docs/VOICE_ACTIVATION.md`.

## Release rule

No release APK, distribution, Play Store publication, or merge to main until the deep audit, required fixes, tests, privacy/security review, and explicit user approval are complete.
