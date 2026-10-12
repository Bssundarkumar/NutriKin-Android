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

- **`app/` — fully verified end to end.** The Android SDK + a Pixel 7/API 34 emulator (`NutriKin_Pixel`)
  are installed (`brew install --cask android-commandlinetools`, see below). Not only does
  `./gradlew :app:assembleDebug` succeed - the complete flow has been run live on the emulator:
  "Continue with Google" -> real Google account picker -> Supabase token exchange -> deep link back into
  the app -> session persists across relaunch -> the Family screen queries the shared `members` table and
  renders correctly. Two real bugs were found and fixed this way (not from reading the code): a missing
  `handleDeeplinks()` call that silently dropped the OAuth callback, and a missing
  `kotlin("plugin.serialization")` plugin that broke `MemberDto` decoding. This is the same backend the
  iOS app uses - an account only sees a household once it's actually joined it (same invite-code model as
  iOS), so a fresh Google account correctly shows "No family members yet" rather than someone else's data.

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

## Running the full app

The SDK and an emulator are already set up on this machine:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools

# Boot the emulator (first boot takes a minute or two; already-created AVD: NutriKin_Pixel)
$ANDROID_HOME/emulator/emulator -avd NutriKin_Pixel &
$ANDROID_HOME/platform-tools/adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'

# Build, install, launch
./gradlew :app:assembleDebug
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
$ANDROID_HOME/platform-tools/adb shell am start -n com.nutrikin.app/.MainActivity
```

Or open the folder in Android Studio directly - it will find the same SDK via `local.properties`.

To actually complete a sign-in (not yet done - the UI renders, but no OAuth round-trip has been
exercised): in the Supabase dashboard, add the Android OAuth redirect (`nutrikin://login-callback`)
alongside the existing iOS one, and create a Google Cloud OAuth client for the app's package name +
debug SHA-1 (`./gradlew signingReport` prints it).

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
