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

## Real-device bug fixes (post-modernization)

CI (build/lint/instrumented smoke tests) doesn't catch everything --
these were found and fixed through live testing on real Android 13-15
hardware, most substantially an Infinix device running Android 15/XOS.

- **Settings-key crash.** Tapping the gear key crashed the app: it called
  `startActivity()` with no flags from a `Service` context, which Android
  requires `FLAG_ACTIVITY_NEW_TASK` for. Adding the flag alone didn't
  fully fix it -- the crash persisted because the call still ran
  synchronously inside `PointerTracker`'s touch-event dispatch for that
  same key press, tearing down the input view mid-gesture. Per user
  request, a short tap now opens the emoji picker directly instead of
  Settings (long-press still reaches the full menu, Settings included),
  deferred via the handler for the same reason.
- **Emoji picker**, via `androidx.emoji2:emoji2-emojipicker`. Crashed on
  first use: `MaterialButton`/`EmojiPickerView` read Material3 theme
  attributes while inflating, and an `InputMethodService`'s own
  `LayoutInflater` doesn't resolve those the way an Activity's manifest
  theme does. Fixed by inflating through a `LayoutInflater` whose context
  is wrapped in the app's own theme. Also had no background of its own
  (rendered transparent over whatever was behind the keyboard window) --
  given an explicit `?attr/colorSurface` background.
- **Suggestions never appearing, root cause #1: dictionary lookup.** The
  bundled dictionary is loaded via `Resources.getIdentifier("main", "raw",
  packageName)`, where `packageName` was hardcoded to the compile-time
  Java package rather than the actual runtime package name
  (`Context.getPackageName()`). Those only match when there's no
  `applicationIdSuffix` -- the debug build adds one (`.codenameberyl`, so
  it installs alongside a Play Store copy), so the lookup always silently
  returned 0 and the dictionary never loaded. No crash, no suggestions,
  ever. Fixed by passing the real runtime package name through.
- **Suggestions never appearing, root cause #2: the legacy candidates
  frame.** Even after the dictionary loaded correctly, the suggestion
  strip stayed visually blank on the Android 15/XOS test device --
  confirmed via targeted diagnostics that the dictionary, prediction
  state, and computed suggestions were all correct; only the actual
  on-screen rendering failed. Traced to `InputMethodService`'s own
  "candidates frame" mechanism (`setCandidatesView()` +
  `setCandidatesViewShown()`), which looks up its container via
  `findViewById(android.R.id.candidatesArea)` inside the *platform's own*
  default IME window layout -- a resource an OEM Android skin can
  customize away. This matches a still-open upstream report
  ([klausw/hackerskeyboard#964](https://github.com/klausw/hackerskeyboard/issues/964))
  of the exact same "suggestions computed correctly, strip stays blank"
  symptom on Android 13+. Fixed by no longer using that mechanism at all:
  the candidate strip is now embedded as an ordinary child view stacked
  above the keyboard, inside the same view `onCreateInputView()` returns
  for the keyboard itself -- the one view guaranteed to actually render.
- **Crash while fixing the above.** `KeyboardSwitcher`'s keyboard-view
  (re)creation posts a `Runnable` that independently hands the bare
  keyboard view to `InputMethodService.setInputView()`. Once the
  suggestion-strip fix started wrapping that same view in its own
  container, this posted call ran shortly after and tried to attach a
  view that was already attached elsewhere -- Android throws for that,
  unconditionally, on every keyboard open. Fixed by routing that call
  through the same wrapper. `onCreateInputView()` and the new
  `refreshInputView()` now both fall back to the bare keyboard view on
  any `RuntimeException` from the wrapping logic, so an unforeseen edge
  case in this area degrades to "no suggestion strip" rather than a
  keyboard that won't open.
- **Android 15 edge-to-edge.** Added
  `android:windowOptOutEdgeToEdgeEnforcement="true"`, the official opt-out
  for apps targeting SDK 35+ whose layouts (this one included -- fixed-dip
  Views with no `WindowInsets` handling) don't account for it.

**Known remaining issue:** the keyboard's landscape layout is cut off on
the right edge on Android 15 --
[klausw/hackerskeyboard#957](https://github.com/klausw/hackerskeyboard/issues/957),
not yet fixed in this fork either.

## New features (post-modernization)

- **Settings defaults tidy-up.** Reset the out-of-the-box defaults to match
  how most people actually use a keyboard today: portrait/landscape height
  30%/35% (was 35%/55%), Full 5-row layout in both orientations (was the
  4-row "Gingerbread" layout in portrait), suggestions shown in landscape
  too, auto-correct on, touch-to-correct-words on, and Ctrl-A working as
  plain select-all (was disabled by default, requiring Ctrl-Alt-A).
- **Gear key opens Emoji.** A short tap on the gear/settings key now opens
  the emoji picker directly instead of the options menu; long-press still
  opens the full menu (Select input method / Settings / Emoji).
- **Emoji picker backspace button.** Added a delete/backspace button next
  to the "ABC" button in the emoji picker, wired to the same
  `handleBackspace()` the physical Delete key uses (so it un-composes
  text / reverts auto-correct correctly, not a raw character delete).
- **Cursor control on spacebar.** Swiping left/right on the spacebar now
  moves the text cursor one character per ~8dp of drag, trackpad-style,
  instead of typing spaces -- tapping the spacebar normally still inserts
  a space. Implemented by reusing the existing "lock touch into the
  spacebar and track horizontal drag" mechanism that this fork's
  drag-to-switch-language gesture already relied on
  (`LatinKeyboard#isInside()`), converting drag distance into real
  `KEYCODE_DPAD_LEFT`/`KEYCODE_DPAD_RIGHT` key events (via the same
  `sendSpecialKey()` path used for the hardware-keyboard-style arrow
  keys) rather than manipulating the app's text selection directly, so it
  works the same as a real arrow key in any app. On by default; toggle
  with the new "Cursor control on spacebar" setting. Since both gestures
  repurpose the same horizontal spacebar drag, enabling this supersedes
  drag-to-switch-language for multi-locale setups -- switch input
  languages from Settings -> International -> Input Languages instead, or
  turn this setting off to restore the old drag-to-switch gesture.
- **Clipboard history.** The keyboard now tracks the last 20 pieces of text
  copied or cut anywhere on the device (via
  `ClipboardManager.OnPrimaryClipChangedListener`), persisted across
  restarts. Accessible via the new "Clipboard" entry in the options menu
  (long-press the gear key); tapping an entry pastes it and returns to the
  keyboard. Shown the same way as the emoji picker -- swapped in as the
  IME's input view rather than a new `Keyboard` mode.
- **GIFs & Stickers.** A new "GIFs & Stickers" entry in the options menu
  lets you pick an image (GIF/PNG/JPEG/WebP) from the device and send it
  to whatever app you're typing into, via Android's rich-content API
  (`InputConnectionCompat#commitContent()`), the same mechanism apps like
  Gboard use. `InputMethodService` has no `startActivityForResult()` of
  its own, so this uses a small invisible trampoline activity
  (`StickerPickerActivity`) to run the system's image picker and hand the
  result back; the picked file is copied into the app's own cache and
  vended via a `FileProvider` (rather than forwarding the picker's own
  URI directly, which isn't reliably supported across all
  `DocumentsProvider` implementations and receiving apps). If the
  currently-focused text field doesn't declare support for rich content,
  a toast explains why nothing happened instead of silently failing.
- **Black + yellow Home screen redesign.** Replaced the old plain
  `TableLayout` of buttons with a card-based setup wizard (numbered
  "Enable keyboard" / "Set input method" / "Input languages" steps, an
  outlined Settings shortcut, and a bordered "try it out" test field),
  and gave the app's chrome (`Theme.HackersKeyboard`, covering Home,
  Settings, Input Languages and the Pref* screens) a deliberate fixed
  black background + yellow accent brand palette -- overriding
  `colorPrimary`/`colorSurface`/`colorOutline`/background tokens
  explicitly in `values/colors.xml`, with `values-night/colors.xml`
  intentionally defining the *same* colors so the look doesn't change
  with system day/night mode. Material You dynamic (wallpaper-based)
  color, previously applied to these screens on Android 12+, is removed
  for the same reason -- it would override this deliberate choice. This
  only covers the "chrome" Activities; the keyboard's own key-drawing
  skin is unaffected and remains separately customizable via "Theme and
  label settings".

  The Settings screen itself is still Android's `PreferenceActivity`
  list rendering (not the card treatment from the Home screen mockup
  this was based on) -- it inherits the same black+yellow color tokens
  since it shares `Theme.HackersKeyboard`, so it's visually consistent
  with the new Home screen, but a full redesign into matching cards is
  a larger follow-up, not done here.

## Post-release fixes

- **Clipboard history: instant crash on opening it.** Confirmed on real
  hardware. `populateClipboardHistoryList()` inflated each history row
  (`clipboard_history_item.xml`, which references Material3-only attrs
  like `?attr/colorOnSurface`) using the IME service's own plain
  `getLayoutInflater()`. Unlike an Activity, `InputMethodService` isn't
  themed via the manifest's `android:theme`, so its base context doesn't
  define those attrs at all -- inflation threw immediately. An empty
  clipboard history never hit this (an early return skips row inflation
  entirely), which is why it wasn't caught before: it only crashed once
  there was at least one entry to actually render, which real-world
  phones almost always already have. Fixed by inflating each row with
  `LayoutInflater.from(container.getContext())` instead -- the
  container itself is already correctly inflated under a
  `Theme.HackersKeyboard`-wrapped `ContextThemeWrapper`, so its
  `getContext()` carries that theme through to its children. Added
  `ClipboardHistoryViewTest` (instrumented) as a regression test, since
  none of the existing instrumented tests exercised any LatinIME-hosted
  custom view (emoji picker, clipboard history) at all -- only the
  separate Activities (Main, Settings).
- **Clipboard history: didn't pick up text already on the clipboard.**
  `ClipboardHistoryManager` only captured *future* clipboard changes via
  `OnPrimaryClipChangedListener`; whatever was already on the clipboard
  when the keyboard first started (e.g. copied before installing/
  enabling it) was never added, since no "change" event fires for it.
  `start()` now also reads and records the current primary clip once,
  in addition to registering the listener.
- **Voice input: off by default.** The "Voice input" `ListPreference`'s
  XML default and `LatinIME`'s own in-code fallback (used before the
  Settings screen has ever been opened, since nothing seeds XML
  defaults into `SharedPreferences` otherwise) both pointed at
  `voice_mode_symbols`/`voice_mode_main` respectively; both now default
  to `voice_mode_off`.
- **Settings screens: full redesign.** All five `PreferenceActivity`
  screens (`LatinIMESettings`, `PrefScreenActions`, `PrefScreenFeedback`,
  `PrefScreenView`, `InputLanguageSelection`) now render every
  preference row and category header through two new custom layouts
  (`pref_item_row.xml`, `pref_category_header.xml`) instead of the
  system's default `Preference` look, applied via `android:layout=` on
  every individual preference/category element across all five
  `prefs*.xml`/`language_prefs.xml` files (not a theme-attribute
  override, to avoid relying on the legacy `android.preference`
  framework's undocumented style-inheritance chain for `ListPreference`/
  `DialogPreference`-derived rows). Category headers get the same bold
  yellow-accent section-label treatment as "SETUP"/"TRY IT OUT" on the
  Home screen; rows get comfortable Material spacing, a two-line
  title/summary layout, and the black+yellow color tokens already
  applied to `Theme.HackersKeyboard`. Both new layouts keep the
  framework's own view ids (`@android:id/title`, `@android:id/summary`,
  `@android:id/widget_frame`) so `Preference`/`CheckBoxPreference`'s
  normal binding code, and the actual checkbox widget
  `CheckBoxPreference` auto-inflates into `widget_frame`, keep working
  completely unchanged -- only the row's own visual styling/spacing
  changes, not the binding logic. The root `<PreferenceScreen>` of each
  file (which isn't itself a row) is deliberately excluded. Added
  `PrefScreensSmokeTest` (instrumented) covering the four screens that
  had no test coverage before this (only `LatinIMESettings` did).
- **Settings screens: title bar/first row overlapping the status bar.**
  Reported with screenshots on real hardware: the settings screens'
  title bar and top content were drawing underneath the status bar
  (clock/battery), with no padding, on top of possibly losing the
  title bar entirely. `Theme.HackersKeyboard` had picked up
  `android:statusBarColor`/`android:navigationBarColor`/
  `android:windowLightStatusBar` alongside the black+yellow palette;
  these are exactly the kind of attribute that signals to the platform
  "the app is managing its own system bars," and the legacy
  `android.preference.PreferenceActivity` screens this app still uses
  for Settings/Input Languages/Pref* aren't insets-aware code -- unlike
  the modern `AppCompatActivity`-based Home screen, they never actually
  handle the resulting `WindowInsets` to pad their own content, so
  removing that padding-managed-for-you assumption left content drawn
  straight under the status bar. Removed those three attributes and
  added `android:fitsSystemWindows="true"` instead -- the classic,
  well-established way to tell the system to keep padding this app's
  content for the status/nav bars automatically, regardless of the
  underlying cause. This is a purely visual bug no automated test can
  catch (CI only proves screens launch without crashing, not that they
  look right), so it's a best-effort fix pending the user's
  confirmation on their device.
- **Added the naira sign (₦) as the first suggested punctuation**, per
  request -- `suggested_punctuations`/`suggested_punctuations_default`
  in `donottranslate.xml`.
- **Settings screens still missing their title, even after the status
  bar overlap fix.** Confirmed with a follow-up screenshot:
  `fitsSystemWindows` fixed the overlap, but there was still no
  "Settings for Hacker's Keyboard"-style heading above the first
  category at all -- the legacy `android.preference.PreferenceActivity`
  screens have no support-`Toolbar` of their own (they aren't
  `AppCompatActivity`), and the `Theme.Material3.DayNight` chain
  `Theme.HackersKeyboard` extends disables the old native window title
  bar those screens would otherwise fall back to (AppCompat/Material
  themes have always assumed you replace it with your own `Toolbar`,
  since ActionBar-on-old-Android was originally backported that way).
  Rather than fight that theme/window interaction, each of the five
  `PreferenceActivity` screens (`LatinIMESettings`, `PrefScreenActions`/
  `Feedback`/`View`, `InputLanguageSelection`) now prepends its own
  title via the plain, reliable `ListView#addHeaderView()` API instead
  -- a new `pref_screen_header.xml` layout, inflated and given the
  screen's own title string in each Activity's `onCreate()`.
- **App showing "HK codenameberyl" as its own name, not just in the
  launcher.** `english_ime_name` -- the shared string used for the
  launcher label *and* every in-app title (Home screen, every Settings
  screen) -- had a debug-build-only override
  (`app/src/debug/res/values/strings.xml`) to keep a sideloaded debug
  build visually distinguishable from a future Play Store release.
  That override leaked into the app's own UI text, not just the
  launcher icon it was meant for. Removed; the debug build's
  `versionNameSuffix` (already in `app/build.gradle`, shown on the
  Home screen's version line) remains as a less intrusive way to tell
  a debug build apart from a release one.
