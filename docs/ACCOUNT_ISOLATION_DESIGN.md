# Account and Profile Isolation — Design Before Implementation

**Status:** Proposed architecture; not implemented.  
**Reviewed:** 2026-11-12  
**Scope:** Android first; Google sign-in may be added later. The app must also work before sign-in without mixing one person's memory with another's.

## 1. Requirements and non-goals

- Each local profile has a separate memory space.
- Each authenticated Google identity maps to one stable profile, independent of display name or email changes.
- Signing out or switching profiles must never expose the previous profile's tasks, contacts, patterns, provider usage, credentials, preferences, or automation state.
- Existing pre-account data must not be silently attached to the first Google account that signs in.
- No personal data is uploaded merely because the user signs in.
- A user can inspect, export, and delete their profile data, subject to clear confirmation and documented consequences.
- App catalog facts that are truly device-global may be shared only after separating them from learned/personal metadata.

## 2. Identity model

Use an internal immutable `profileId` (UUID) as the local database scope key. Never use email, display name, phone number, or a mutable Google profile field as the primary key.

For Google sign-in, obtain the stable OpenID Connect subject (`sub`) from a verified authentication flow and store the provider identity as a mapping to the internal profile. Validate issuer, audience, signature/token state and nonce using a supported authentication library; do not trust a client-supplied email string as proof of identity.

Suggested conceptual tables:

- `profiles(profileId, createdAt, displayLabel, profileType, ...)`
- `external_identities(id, profileId, provider, issuer, subject, linkedAt)` with a unique constraint on (provider, issuer, subject)
- `profile_settings(profileId, ...)`

A local-only profile is created on first launch. Google sign-in can link or create a separate profile only after an explicit user choice. Do not silently merge local memory into a Google profile.

## 3. Data classification

**Profile-scoped personal data:** AppNode usage history, TaskNode commands/results, ContactNode records, PatternNode sequences, ProviderNode usage history, EdgeEntity relationships, provider credentials, voice preferences, automation schedules, notification/group preferences, and learned trust/reputation metadata.

**Potentially device-global reference data:** package names, installed app labels and static app capability catalog. If `AppIntelligenceEntity` contains only static package metadata, it can be moved to a separate device catalog. Its trust score, AI metadata, user labels, or learned capabilities are personal/learned and must be profile-scoped. Until the distinction is enforced, scope the whole entity by profile.

## 4. Room schema migration

Do not make a quick additive migration and assume isolation is solved. The implementation must:

1. Add a non-null `profileId` to every profile-scoped entity, including edge rows.
2. Add profile-aware DAO methods for every read, write, update, delete, count, Flow, search, suggestion and context-building query.
3. Make natural uniqueness constraints composite, e.g. (profileId, packageName), (profileId, sequenceHash), and (profileId, providerName).
4. Add indexes beginning with `profileId` for frequent scoped filters.
5. Ensure edge relationships cannot connect records across profiles; validate both endpoints in the repository/transaction layer.
6. Migrate old rows into a dedicated `legacy-local` profile, not the first Google account.
7. Perform migration transactionally and preserve data; add explicit rollback/failure handling and schema export.
8. Update every constructor/call site so profile scope is required, not optional with a global default.

A versioned migration must be tested against a real v3 database fixture. Do not use destructive migration in production.

## 5. Session and account switching

- A single ProfileSession source provides the active profile ID.
- Repositories receive an explicit profile-scoped session or profile ID; no DAO silently guesses the active user.
- On switch/sign-out, cancel in-flight tasks, stop profile-bound observers, clear in-memory Graphify context, invalidate cached contact/provider state and close profile-specific resources.
- Recreate flows only after the new profile is active.
- Do not log raw tokens, account subjects, phone numbers, or private memory.
- Credentials are stored separately using Android Keystore-backed encryption and keyed by profile; never copy credentials between profiles by default.

## 6. Encryption, retention, deletion

Database encryption is a separate design gate. Evaluate SQLCipher or an equivalent maintained solution, key management via Android Keystore, backup exclusion/backup policy, and key rotation/recovery. Device file-based encryption alone is not the same as app-level database encryption.

Define retention limits for task transcripts and contact history. Deletion must remove profile-scoped rows, edges, credentials, cached memory and pending work in one coordinated process. Explain that deletion of a local profile does not delete data already sent to an external AI provider.

## 7. Required tests before enabling multi-account

- Migration from v3 fixture preserves all rows in `legacy-local`.
- Two profiles can store identical natural keys without collision.
- Every DAO read/Flow/search/count returns only the requested profile.
- Every insert/update/delete requires profile scope.
- Edges cannot link nodes across profiles.
- Switching A → B → A never leaks cached or in-flight context.
- Signing out does not erase local data unless the user explicitly chooses deletion.
- A Google identity cannot be linked to two profiles without an explicit, safe conflict flow.
- Profile deletion removes all scoped data and secrets.
- Migration failure leaves the original database recoverable.

## 8. Implementation sequence

1. Inventory every entity, DAO, repository, direct database access and in-memory cache.
2. Implement Profile model/session and explicit local profile without Google sign-in.
3. Add profile-scoped schema and migration with fixture tests.
4. Scope all repository and DAO methods; add cross-profile negative tests.
5. Add encryption and deletion/retention controls.
6. Only then integrate Google sign-in and account switching.
7. Run instrumentation/migration tests in CI and validate on HONOR X6C.

## Current audit evidence

The current Graphify v3 schema has AppNode, TaskNode, ContactNode, PatternNode, ProviderNode, EdgeEntity and AppIntelligenceEntity. Their inspected fields have no profile key. The inspected DAO queries select across the entire table. This is a confirmed structural gap, not a claim that account switching is already present.
