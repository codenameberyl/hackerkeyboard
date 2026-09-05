# Changelog

This documents the modernization pass applied to this fork on top of
[klausw/hackerskeyboard](https://github.com/klausw/hackerskeyboard) `v1.41.1`
(last upstream release, circa 2018). Upstream's own release notes remain at
its [wiki](https://github.com/klausw/hackerskeyboard/wiki/ReleaseNotes) for
everything before this point.

## Build tooling

- Gradle wrapper: not previously committed (`.gitignore`'d) -> 9.7.1, with
  `distributionSha256Sum` verification.
- Android Gradle Plugin: 3.2.1 (2018) -> 9.4.0.
- Repositories: `jcenter()` (shut down) -> `mavenCentral()`.
- `compileSdk`/`targetSdk`: 26 -> 37 (Android 17).
- `minSdk`: 14 -> 23, required by current `androidx.appcompat`.
- Full AndroidX migration: `com.android.support:appcompat-v7`/`support-compat`
  -> `androidx.appcompat`/`androidx.core`; `android.support.test.*` test
  deps -> `androidx.test.*`.
- Added the `namespace` declaration `app/build.gradle` now requires; removed
  the manifest's legacy `package=` attribute.
- Added `android:exported` to every manifest component with an intent
  filter (mandatory once `targetSdk >= 31`; preserves prior behavior).
- `compileOptions` source/target compatibility -> Java 17 (AGP 9.4 minimum).
- `lintOptions {}` -> `lint {}` (renamed Gradle DSL block).
- `app/CMakeLists.txt`: `cmake_minimum_required` bumped off the long-removed
  legacy `3.4.1` placeholder to `3.22.1`.
- Two switch statements (`LatinKeyboardBaseView`, `LatinKeyboardView`) that
  switched on `R.styleable.*` constants were converted to if/else chains --
  AGP 9's default `android.enableAppCompileTimeRClass=true` makes app-module
  `R` fields non-final, and Java requires constant case labels.
- `getDefaultProguardFile('proguard-android.txt')` -> `'proguard-android-optimize.txt'`
  (the former, bundling `-dontoptimize`, was removed in AGP 9.4).
- Added `.github/workflows/build.yml`: `assembleDebug` + `lint` on every
  push, plus (see Testing below) instrumented tests on real emulators.

## Permissions & platform behavior

Auditing against current Android behavior surfaced several real runtime
bugs the `targetSdk` jump would have introduced or exposed:

- `Context.registerReceiver()` without an exported flag throws
  `SecurityException` at runtime once `targetSdk >= 33` (Android 13). Fixed
  all 3 dynamically-registered receivers via
  `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)`.
- `PendingIntent.getBroadcast(..., 0)` throws `IllegalArgumentException` on
  Android 12+ once targeting S+ without an explicit mutability flag. Added
  `FLAG_IMMUTABLE` to both of the app's notification `PendingIntent`s.
- The notification's "Settings" action button launched an Activity from a
  `BroadcastReceiver` -- a "notification trampoline", blocked outright on
  Android 14+ for apps targeting API 34+. Changed to launch the settings
  Activity directly via `PendingIntent.getActivity()`.
- Added the `POST_NOTIFICATIONS` runtime permission (required on API 33+ to
  show the optional persistent keyboard notification), with a proper
  runtime-request flow from the settings screen and a `checkSelfPermission`
  guard around the actual `notify()` call.
- Added a `<queries>` manifest block for the two custom intent actions
  `PluginManager` uses to discover installed dictionary-pack add-ons.
  Without it, Android 11+'s package-visibility filtering makes those
  `PackageManager` queries silently return empty lists.

Confirmed not applicable: the app has no external/shared storage access
(scoped storage doesn't apply); its originally-declared permissions
(`VIBRATE`, `READ_USER_DICTIONARY`, `WRITE_USER_DICTIONARY`) remain
normal-protection; the (unused, commented-out) `ContactsDictionary` would
need `READ_CONTACTS` only if ever re-enabled.

## UI modernization

- Added `com.google.android.material:material:1.14.0`.
- New `Theme.HackersKeyboard` (`Theme.Material3.DayNight`), applied
  app-wide -- the app's first-ever day/night support for its settings/chrome
  screens (previously there was no `values-night/` at all).
- New `HackersKeyboardApplication` applies Material You dynamic
  (wallpaper-derived) color to every Activity on Android 12+, via
  `DynamicColors.applyToActivitiesIfAvailable()`; older versions keep the
  static fallback palette automatically.
- The existing "Material Dark"/"Material Light" keyboard skins get a
  `values-v31` color override so the actual key backgrounds/text pull from
  the platform's wallpaper-derived tonal palette on Android 12+, with the
  previous static colors as the pre-12 fallback.
- `Main` (the app's simple launcher screen) migrated from a plain `Activity`
  with `<Button>` widgets to `AppCompatActivity` with `MaterialButton`.
- Key labels request a Medium (500) weight from the system's variable
  default font on API 28+, instead of plain Regular -- deliberately without
  a Downloadable Fonts dependency, since the keyboard's core render path
  needs to keep working fully offline.
- Fixed the deprecated single-arg `Vibrator.vibrate(long)` to use
  `VibrationEffect` on API 26+.
- Fixed the deprecated single-arg `Html.fromHtml(String)` in `Main`.

**Deferred:** `LatinIMESettings`/`InputLanguageSelection`/`PrefScreen*`
remain on the legacy `android.preference.*` widget set; only their colors
inherit from the new theme. A full Material Design settings UI would need
a `PreferenceFragmentCompat` rewrite.

## Testing

- The repository had no `androidTest`/`test` source sets at all before this
  pass, despite the test dependencies already being declared. Added three
  minimal instrumented smoke tests (`app/src/androidTest`): `Main` and
  `LatinIMESettings` each launch and reach `RESUMED`, and the `LatinIME`
  service is still correctly declared/exported. These run on real emulators
  (API 33/34/35, i.e. Android 13/14/15) in CI.
- API 34 and 35 pass reliably on real emulators every run. API 33's
  emulator image is flaky: it timed out waiting to boot twice (600s, then
  900s timeout) before booting and passing on a third attempt, while 34/35
  boot in ~8 minutes every time under the identical config. The API 33 CI
  job is marked `continue-on-error` (still runs, results still visible,
  doesn't block the workflow) to absorb that boot-time variance rather than
  being flaky-retried indefinitely or silently dropped.
- These are launch-only smoke tests either way, not a substitute for
  manually exercising the keyboard itself (typing, language switching,
  popup keys, the various keyboard skins) -- see the README's
  "Modernization notes" for what still needs manual verification before a
  release.
