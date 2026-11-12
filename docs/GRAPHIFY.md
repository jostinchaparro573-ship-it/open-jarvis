# Graphify — Current Implementation and Open Design Work

## Current role

Graphify is the local Room/SQLite data layer used by OpenJarvis. It stores task history, app usage, contacts inferred from commands, learned sequence patterns, provider statistics, and relationships between records.

This document describes the code currently in the working branch. It is not a claim that account isolation, encryption, or production-grade graph learning is complete.

## Database

- Database file: graphify.db
- Room schema version: 3
- Database class: GraphifyDB
- Migration currently registered: 2 to 3, creating app_intelligence and its indexes
- Schema export: disabled
- Database encryption at the application layer: not implemented
- Account/profile scoping: not implemented

## Current entities

| Entity | Current fields | Purpose |
|---|---|---|
| AppNode | id, packageName, label, lastUsed, useCount | App launch/use history |
| TaskNode | id, command, result, timestamp, providerUsed, latencyMs | User task history |
| ContactNode | id, name, phoneNumber, email, lastContacted, contactCount, contactMethod | Contacts inferred/stored by Jarvis |
| PatternNode | id, sequenceHash, sequenceString, occurrenceCount, lastSeen, confidence, timeOfDayMask, dayOfWeekMask | Repeated task-sequence patterns |
| ProviderNode | id, providerName, modelName, lastUsed, useCount, successCount, totalLatencyMs | Provider usage metrics |
| EdgeEntity | id, fromId, fromType, toId, toType, edgeType, weight, lastUpdated | Relationships between graph records |
| AppIntelligenceEntity | packageName, appName, category, capabilities, trustScore, lastAnalyzed, isAIApp, aiMeta | Device-level app metadata |

AppIntelligenceEntity is device/app metadata. The other listed records can contain personal activity or inferred relationship information.

## Repository behavior

GraphifyRepository currently provides:
- app launch logging and recent/most-used app queries
- task logging, recent-task retrieval, search, and aggregate statistics
- memory-context assembly from recent tasks, patterns, and contacts
- contact insertion, lookup, and interaction counts
- pattern insertion, lookup, confidence updates, and time-mask updates
- provider usage statistics
- edge insertion
- a one-shot suggestions flow based on recent patterns and contacts

The memory context is capped at 1,500 characters. If a cloud LLM provider is selected, the assembled context may be included in data sent to that provider.

## Pattern analysis

AnalysisEngine currently:
1. reads up to ten recent tasks
2. detects repeated three-task sequences when the same sequence appears at least twice
3. updates pattern confidence or inserts a new pattern
4. associates time-of-day information with patterns
5. attempts to infer contacts from command text
6. decays confidence for stale patterns

ContactNameMatcher provides conservative local fuzzy matching for small spelling differences and exact whole-token matches. It is a convenience matcher, not proof of a person's identity. Any future consequential action involving a contact must still require user confirmation.

## Important limitations

### Account isolation — release blocker

All personal records currently share one graphify.db file. The entities and DAO queries do not include an account/profile identifier. There is no Google sign-in/session provider or account-switching lifecycle. Therefore the requirement that every Google account starts with an independent memory is not met.

Do not add an accountId column as a quick patch. First define:
- a stable profile identity based on the authenticated provider's stable subject, not the mutable email address
- a local-only profile for use without sign-in
- explicit behavior when linking a local profile to a Google account
- separate data scopes for tasks, contacts, patterns, provider credentials, and automations
- account switching that cancels active tasks and clears in-memory context
- Room migration and tests proving no cross-profile reads or writes
- explicit export/delete behavior for each profile

A per-profile database file keyed by a non-reversible stable profile identifier is one possible design. Device-wide app catalog metadata can remain device-scoped if it contains no personal usage history.

### Storage and privacy

The Room database is not encrypted with SQLCipher or another application-level database encryption layer. Android's app sandbox and device encryption are helpful protections, but they are not equivalent to per-database encryption. There is no complete memory deletion/retention UI.

Provider API keys are stored separately using EncryptedSharedPreferences. That does not encrypt Graphify records.

### Migrations and tests

Only migration 2 to 3 is registered. There are no verified migration tests for the current schema, and exportSchema is false. Android instrumentation tests exist but are not run by the current GitHub Actions workflow.

### Data transfer

When a cloud provider is active, task text, the system prompt, and any included screen text or memory context can be sent to that provider. The app must clearly disclose this and must not claim that all information stays on-device.

## Next Graphify work

1. Design account/profile identity and data partitioning.
2. Decide database encryption and per-profile retention/deletion.
3. Enable Room schema export and add migration tests.
4. Add account-isolation tests before adding multi-account UI.
5. Review contact extraction for Spanish commands and ambiguous names.
6. Measure query performance using representative data rather than undocumented estimates.
