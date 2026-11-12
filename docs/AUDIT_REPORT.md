# OpenJarvis — Ongoing Repository Audit

## Audit status

This is an ongoing static audit of the working branch jarvis/foundation-fixes-2026-11-09. It is not a release approval or an independent penetration test. The main branch remains untouched.

## Latest verified CI

Commit 6e7976cec8f1d321f1839c570c1641bc52eeba60 passed all three workflow steps:
- Kotlin compilation
- JVM unit tests
- Android lint

The workflow configuration now includes Android instrumentation tests on an API 34 emulator. The latest workflow run for commit `346b49cfcc8b8bf7918c1e906094e53fc4b0b68e` has passed Kotlin compilation, JVM unit tests, and Android lint; emulator instrumentation is still running as of this audit snapshot. Emulator success does not prove behavior on a physical device.

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
- ProviderFallbackChain now executes the one configured provider at most once and explicitly reports that no alternate provider is configured. Genuine multi-provider failover remains unimplemented.
- AppBuilderMode does not perform an actual Gradle build or ZIP package.
- PromptEngine and AIAppInteractor require call-site and integration review before being described as working features.
- SettingsScreen now receives and restores the saved provider, base URL, API key, and model. This was implemented in the latest branch commit and is awaiting the current CI run.
- Voice is push-to-talk transcript input with manual Send; no wake phrase or hands-free execution. User-selected phrases are “OK Jarvis” and “Hey Jarvis”; implementation and device validation remain open.

### Medium — accessibility thread and device behavior
ScreenReader and AccessibilityService interactions still require thread/lifecycle validation and physical-device tests. No HONOR X6C test has been run.

### Medium — provenance and licensing
The upstream repository is MIT-licensed, but every bundled third-party dependency, model, icon, and resource still needs a license inventory before public distribution. MIT does not grant trademark rights or imply endorsement.

## Next audit sequence

1. Complete the permission, voice, and accessibility lifecycle review.
2. Design account/profile isolation before changing the Room schema.
3. Decide Graphify encryption, retention, deletion, and migration strategy.
4. Keep Android instrumentation tests in CI; add Room migration and profile-isolation test fixtures before introducing profile schema changes.
5. Correct provider settings and disclose cloud data transfer.
6. Audit all remaining screen interaction paths and placeholder modules.
7. Test on HONOR X6C Android 15/MagicOS 9.
8. Complete third-party license/provenance review.
9. Implement and validate opt-in offline “OK Jarvis” / “Hey Jarvis” detection under `docs/VOICE_ACTIVATION.md`.

## Release rule

No release APK, distribution, Play Store publication, or merge to main until the deep audit, required fixes, tests, privacy/security review, and explicit user approval are complete.


### Account isolation — expanded static audit (2026-11-12)

The audit inspected AppNode, TaskNode, ContactNode, PatternNode, ProviderNode, EdgeEntity, AppIntelligenceEntity and their DAOs. All personal Graphify records are globally queried within one database; edge queries are also unscoped. App intelligence has a packageName-only primary key and needs an explicit decision on whether it is device-global catalog data or per-profile learned data. No Google account identity/session implementation was found in the inspected repository tree. Do not patch only the entities: query scoping, composite uniqueness/indexes, relationship integrity, migration, deletion and account-switch behavior must be implemented together. See `docs/ACCOUNT_ISOLATION_DESIGN.md`.


### Critical — additional stores are not profile-scoped

A second Room database, `automations.db`, stores AutomationEntity rows with no profile ID. AutomationWorker currently fails closed rather than executing commands, but its lookup is globally by automation ID and its WorkManager input carries no profile identity. This must be scoped before automation execution is enabled.

SettingsActivity stores provider selection/API credentials in EncryptedSharedPreferences (`jarvis_encrypted_prefs`) and voice preferences in `voice_prefs`. These are encrypted but app-global; encryption does not provide account separation. They must be partitioned by profile and keys protected with Android Keystore.

GraphifyRepository is constructed independently in AgentCore, PromptEngine, AnalysisEngine, MainActivity, and OverlayService. A single explicit ProfileSession must be passed through these entry points. PromptEngine conversation context, TaskWorkingMemory, provider instances, Flow collectors, and scheduled work must also be invalidated on profile switch.

The Gemini provider previously placed its API key in the request URL query parameter, which can be exposed in BASIC request-line logs. The current branch now sends the key using the x-goog-api-key request header for both generation and connection testing, keeping it out of the URL. This still needs a provider integration test and review of any proxy/crash logging.


### Medium — provider failover is not real failover

`ProviderFallbackChain.getProvider(name, adapter)` ignores the requested provider name and always returns the one active provider from UniversalAdapter. The fallback loop can therefore send the same request repeatedly to the same provider, despite displaying different provider names. UniversalAdapter stores only one provider's key/model/base URL at a time. This can waste quota and duplicate external data transfers; do not treat the feature as operational until per-provider configuration and routing tests exist. Provider credentials/preferences also remain app-global, despite encryption.

### Security fix in latest branch commit

Gemini generation and connection-test requests now put the API key in the `x-goog-api-key` header rather than the URL query parameter. The reason is that the current BASIC HTTP logging interceptor records request lines, which can include query parameters. CI is pending for this change; provider integration testing is still outstanding.


### High — Gemini model default was retired

The code previously defaulted to `gemini-1.5-flash`, which Google shut down on 2025-09-29. The current default is updated to `gemini-3.8-flash`. The Settings screen also now restores the saved provider configuration and resets provider-specific credentials when changing provider, avoiding accidental reuse of one provider's API key with another. A real Gemini connection test is still required. Google's current model/deprecation documentation is the source of truth for model availability.


### CI and provider documentation update (2026-11-12)

The workflow was extended to run connected Android instrumentation tests on an API 34 emulator. On commit `346b49cfcc8b8bf7918c1e906094e53fc4b0b68e`, Kotlin compilation, JVM unit tests, and Android lint have passed; the emulator test step was still in progress at the time of this snapshot. No physical HONOR X6C validation has occurred.

The provider guide's Gemini sample was corrected to use the current `gemini-3.8-flash` default and the `x-goog-api-key` header, rather than the retired `gemini-1.5-flash` model and a key in the URL. The guide now recommends Gemini authorization keys, or explicitly restricted standard keys where still supported, and warns against embedding secrets in a distributed mobile APK. See Google's [Gemini API key guidance](https://ai.google.dev/gemini-api/docs/api-key).


### Provider retry safety correction (2026-11-12)

`ProviderFallbackChain` now obtains the actual configured provider once and calls it at most once. It no longer loops through fallback labels while repeatedly returning the same active provider. On failure it surfaces a truthful message that no alternate provider is configured. Coroutine cancellation is rethrown instead of being converted into an ordinary provider failure. This is intentionally a safe limitation, not genuine multi-provider failover; independent credentials, routing, and tests are still required.
