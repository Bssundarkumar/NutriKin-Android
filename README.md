# NutriKin — Android

A native Android client for the same family the iOS app (`../NutriKin`) serves — same Supabase backend,
same households, same members, same data. A family with one iPhone and one Android phone shares one
account's worth of data; nothing here is a separate silo.

## What's actually verified vs. scaffolded

Be precise about this before building on top of it:

- **`core/` — real, tested, verified.** Pure Kotlin, no Android dependency. Ported from the iOS
  `ScoringEngine`/`AlcoholGuidance`/`Models`: the allergy hard block, the alcohol-for-children hard block
  (including the brand-name backstop for products like Grey Goose that have no OFF ingredient/category
  data — the exact bug fixed on iOS this session), and general sugar/sodium caution scoring. 8 unit tests
  pass (`./gradlew :core:test`), including the Grey Goose regression case.

  **Not yet ported from the iOS engine** — do not treat Android scores as equivalent to iOS's until these
  land: honey/juice hard blocks for infants, caffeine/mercury-fish/choking-hazard/cured-meat child-safety
  caps, pregnancy guidance, supplement caution, the full `Condition`/JSON decoding for what's stored in
  Postgres's `conditions` jsonb column (currently left as raw `JsonElement` in `MemberDto`, see below).

- **`app/` — written, not build-verified.** This machine has Java 17 + Gradle but no Android SDK, so the
  Compose/Android module could not actually be compiled or run here (no `ANDROID_HOME`, no emulator). The
  code follows standard Android/Compose/Supabase-kt patterns and should build once opened in Android
  Studio with an SDK installed, but treat it as a first draft to sanity-check there, not as proven.

## Why the backend needs nothing new

`app/src/main/kotlin/com/nutrikin/app/data/SupabaseConfig.kt` points at the exact same Supabase project
URL and anon key as the iOS app's `SupabaseConfig.swift`. The anon key is safe to ship on both platforms —
it identifies the project, not a user; every table is protected by the RLS policies in `../NutriKin/backend`
(see `is_household_member()` in `migration_005_auth.sql`). Reading/writing `members`, `households`, scans,
workouts, etc. from Android uses the exact same tables, policies, and trust model — no new backend code,
no second database, no sync layer.

Auth: the iOS app offers email+password, "Continue with Apple," and "Continue with Google." Apple Sign-In
has no Android equivalent, so this scaffold wires up **Google Sign-In only** — same Supabase Auth
provider, so an account that signed in with Google on iPhone signs into the same household here. Email
magic-link could be added the same way if you want it on Android too.

## Project layout

```
core/   pure Kotlin — data models, ScoringEngine, AlcoholGuidance (ported, tested)
app/    Android/Compose — MainActivity, sign-in, Family screen (written, unverified)
```

## Running what's verified

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17   # or wherever your JDK 17 lives
./gradlew :core:test
```

## Opening the full app

Needs Android Studio (or a standalone Android SDK) that this environment doesn't have:

1. Open the `NutriKin-Android/` folder in Android Studio.
2. Let it install the Android SDK/platform if prompted.
3. In the Supabase dashboard, add the Android OAuth redirect (`nutrikin://login-callback`) alongside
   the existing iOS one, and register the app's SHA-1 for Google Sign-In if using Credential Manager
   instead of the browser-based OAuth flow this scaffold uses.
4. Run on an emulator or device.

## Honest list of what's next, roughly in priority order

1. Finish porting `ScoringEngine`'s remaining guardrails (see above) before this is safe to call
   feature-equivalent to iOS for child safety.
2. Decode `MemberDto.conditions` (currently raw `JsonElement`) into `core.Condition`, matching the exact
   JSON shape `conditions` jsonb rows use on iOS (`[{"type":"diabetes"}, {"type":"allergy","allergen":"peanuts"}]`).
3. Scan flow: barcode (CameraX + ML Kit or ZXing) hitting the same Open Food Facts API the iOS app uses,
   feeding `core.Product`.
4. Health Connect as the Android equivalent of HealthKit, behind the same kind of optional-linked-member
   pattern `HealthKitManager` uses on iOS.
5. The on-device AI tier (`AppleAI.swift`'s Apple Intelligence path) has no Android equivalent — Android
   likely needs to be key-only (Gemini is the obvious first choice, same provider already supported as a
   BYOK option on iOS) or go without a free/private tier.
6. Everything else iOS has: Groceries, Medications, Activity/workouts, Gym Buddies, the full Today
   dashboard — same backend tables are already there, each is its own screen+ViewModel port.
