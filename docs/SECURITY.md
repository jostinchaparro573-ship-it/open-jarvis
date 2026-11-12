# Security and Privacy — OpenJarvis

## Status

OpenJarvis is under active development. This document describes observed code behavior, not an independent security certification. Do not treat the app as release-ready.

## Safeguards currently present

- Android app components are generally non-exported unless they need to be launchable.
- Android backup is disabled in the manifest.
- Provider credentials and voice preferences use EncryptedSharedPreferences.
- LLM action responses are validated; malformed or unsupported action plans fail closed.
- Generic TAP and TYPE actions are blocked in AgentCore and the validator. Additional identified paths in ActionExecutor, ScreenWatcher, and AIAppInteractor are also blocked pending a real confirmation workflow.
- The local TCP bridge is not started by OverlayService. Authentication has not been implemented, so the bridge must remain disabled.
- Voice recognition is push-to-talk. A recognized transcript is placed in the command field and the user must tap Send.
- The notification listener is not registered in AndroidManifest and is not an active feature.

## Android permissions and special access

The manifest declares:
- INTERNET for configured LLM providers
- RECORD_AUDIO for voice input; runtime permission is requested before enabling voice
- SYSTEM_ALERT_WINDOW for the floating overlay; the user must grant special access
- FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE, and FOREGROUND_SERVICE_DATA_SYNC for foreground services
- POST_NOTIFICATIONS for notifications
- WAKE_LOCK

The AccessibilityService is declared but must be explicitly enabled by the user in Android settings. It can inspect visible UI nodes and perform supported global actions. Accessibility and overlay access should remain optional and feature-specific.

The current manifest does not request Contacts, Location, Camera, or broad external-storage permissions. The notification listener is not declared as a service.

## Data stored locally

Graphify stores task commands/results, app usage, contact records, patterns, provider usage, and graph edges in a Room database named graphify.db. The database does not currently use an application-level encryption layer. Android's app sandbox and device encryption provide baseline protection but do not satisfy the project's planned per-account isolation requirement.

Provider API keys and voice preferences are stored in EncryptedSharedPreferences. This is separate from the Graphify database.

## Data that may leave the device

When a cloud LLM provider is selected, OpenJarvis may send:
- the user's command
- the system prompt and task context
- screen text when the user explicitly requests screen reading
- relevant Graphify memory context included in the prompt

The selected provider's terms, retention policy, and privacy practices apply. The app cannot guarantee that provider-side data is not retained. Sensitive screen content should not be sent to a cloud provider without clear user awareness and consent. A local model path must be verified before it is described as fully offline.

HTTP debug logging was reduced to BASIC to avoid logging request/response bodies and headers. URLs and other diagnostic metadata still require review.

## Features intentionally disabled or incomplete

- Screen TAP/TYPE and direct prompt typing are blocked until per-action confirmation exists.
- ScreenWatcher tap actions are blocked; its built-in YouTube auto-skip rule is disabled.
- The TCP bridge is not started and has no client authentication.
- Automation scheduling may create work, but automation execution is deliberately unavailable and must not report success.
- WhatsApp notification capture is not active. Historical messages cannot be read by the current notification listener.
- There is no always-on Jarvis wake phrase.
- Autonomous phone calls and appointment booking are planned only.
- AppBuilderMode does not currently produce a verified APK or ZIP package.

## Open security and privacy work

1. Account/profile isolation across Graphify, automation records, settings, and provider credentials.
2. Decide whether to encrypt Graphify at the application layer and add migration tests.
3. Add user-visible data retention, per-profile deletion, and export controls.
4. Clearly disclose cloud-provider data transfer and obtain appropriate user consent.
5. Build explicit confirmation screens for consequential actions; test adversarial and ambiguous prompts.
6. Review all direct AccessibilityService call paths to ensure no confirmation bypass remains.
7. Implement strict WhatsApp group consent and matching before registering any notification listener.
8. Review provider settings so saved provider/model values are loaded accurately and credentials are not accidentally overwritten.
9. Audit third-party dependency licenses, update policy, and secret handling.
10. Run Android instrumentation tests and real-device tests on the HONOR X6C.

## Reporting

Do not send security reports to the original upstream project's contact address. Use the private reporting channel configured for this repository if one is available; otherwise contact the repository owner privately and avoid publishing exploit details in a public issue.
