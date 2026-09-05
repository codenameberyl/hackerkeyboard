## Overview ##

This is a fork of [klausw/hackerskeyboard](https://github.com/klausw/hackerskeyboard) (originally developed in 2011 for Android 2.3 Gingerbread) with a modernization pass applied on top: current Gradle/AGP, full AndroidX migration, current `compileSdk`/`targetSdk`, fixes for several Android 12+/13+/14+ runtime behavior changes, and a first pass at Material Design theming with Material You dynamic color support. See [CHANGELOG.md](CHANGELOG.md) for the detailed list of what changed and why, and the notes at the bottom of this file for known gaps and Play Store submission caveats.

The description below is inherited from upstream and still describes the app's actual features and layouts accurately.

Are you missing the key layout you're used to from your computer when using an Android device? This software keyboard has separate number keys, punctuation in the usual places, and arrow keys. It is based on the AOSP Gingerbread soft keyboard, so it supports multitouch for the modifier keys.

This keyboard is especially useful if you use ConnectBot for SSH access. It provides working Tab/Ctrl/Esc keys, and the arrow keys are essential for devices such as the Xoom tablet or Nexus S that don't have a trackball or D-Pad.

The supported keyboard layouts include Armenian (Հայերեն), Arabic (العربية),
British (en\_GB), Bulgarian (български език), Czech (Čeština), Danish (dansk),
Carpalx English (language "en-CX"), Dvorak English (language "en-DV"), English
(QWERTY), Finnish (Suomi), French (Français, AZERTY), German (Deutsch, QWERTZ),
German Neo2 (Deutsch, language "de-NE"),
Greek (ελληνικά), Hebrew (עברית), Hungarian (Magyar), Italian (Italiano), Lao
(ພາສາລາວ), Norwegian (Norsk bokmål), Persian (فارسی), Portuguese (Português),
Romanian (Română), Russian (Русский), Russian phonetic (Русский, ru-rPH),
Serbian (Српски), Slovak (Slovenčina), Slovenian
(Slovenščina)/Bosnian/Croatian/Latin Serbian, Spanish (Español, Español
Latinoamérica), Swedish (Svenska), Tamil (தமிழ்), Thai (ไทย), Turkish (Türkçe),
and Ukrainian (українська мова).

To install, get **[Hacker's
Keyboard](https://play.google.com/store/apps/details?id=org.pocketworkstation.pckeyboard)**
from the Play Store, plus optional [dictionary
packs](https://play.google.com/store/apps/developer?id=Klaus+Weidner).

## Additional resources ##

See the **[Release Notes](https://github.com/klausw/hackerskeyboard/wiki/ReleaseNotes)** for changes in the Play Store released versions.

Having problems? See the **[User's Guide](https://github.com/klausw/hackerskeyboard/wiki/UsersGuide)** and **[FAQ](https://github.com/klausw/hackerskeyboard/wiki/FrequentlyAskedQuestions)**, and check the [issue tracker](https://github.com/klausw/hackerskeyboard/issues) for known bugs or filing new ones.

Comments, requests, or contributions? Join the [discussion group](http://groups.google.com/group/hackerskeyboard/).

Application developers: see [the page about keyboard support in applications](https://github.com/klausw/hackerskeyboard/wiki/KeyboardSupportInApplications) if you want to enable the additional keys in your Android application, the same method also works for hardware USB or Bluetooth keyboards.

![hk-5row-en-s.png](hk-5row-en-s.png)

## Modernization notes (this fork) ##

Current build configuration: Gradle 9.7.1, Android Gradle Plugin 9.4.0, `compileSdk`/`targetSdk` 37 (Android 17), `minSdk` 23 (Android 6.0), full AndroidX, Material Components 1.14.0 with Material You dynamic color.

**Known gaps / deferred work**, not addressed by this modernization pass:
- `LatinIMESettings`, `InputLanguageSelection`, and the `PrefScreen*` activities still use the legacy `android.preference.PreferenceActivity`/`PreferenceScreen` framework classes rather than `PreferenceFragmentCompat`. They inherit the app's Material3 theme colors, but their row layouts are still the platform's default preference-row style, not true Material 3 list items. Migrating them is a larger, separate rewrite.
- `LatinIMESettings`'s "official build" version label uses the deprecated `PackageManager.GET_SIGNATURES` API instead of `GET_SIGNING_CERTIFICATES`; it's a cosmetic label, not a functional or security issue.
- No emulator/device testing existed before this pass; three minimal instrumented smoke tests were added (`app/src/androidTest`) and wired into CI across API 33/34/35, but they only confirm the app launches without crashing -- they are not a substitute for manually exercising the actual keyboard (typing, language switching, popup keys, all the selectable keyboard skins) on a real device.
- **The API 33 (Android 13) CI emulator job is unreliable in this environment** and is marked non-blocking (`continue-on-error`) rather than fixed: its emulator image consistently timed out waiting to boot on the CI runner across two separate attempts (at both a 600s and a 900s timeout), while an identical config boots API 34/35 in ~8 minutes without issue. This looks like a real limitation of that specific runner/image combination, not a flake or an app problem -- API 34 and 35 passing is good evidence the app itself, the test setup, and the emulator infrastructure are all sound. If you have a working Android 13 emulator or device locally, that's the one version this modernization pass could not verify by itself; see "Manual testing" below.

**Manual testing** (beyond what CI covers): install a debug build (`./gradlew installDebug`) and, ideally on a real Android 13 device/emulator specifically (see the CI gap above), plus at least one Android 12+ device for the dynamic color check:
1. Enable Hacker's Keyboard under Settings > System > Languages & input > On-screen keyboard, and switch to it in a text field.
2. Type in it -- verify keys, popup accented characters, and the language-switch spacebar swipe all still work.
3. Try a couple of the selectable keyboard skins (Settings > look and feel), including "Material Dark"/"Material Light" -- these are the two skins that now pull from Material You dynamic color on Android 12+; check they look reasonable with a colorful wallpaper set as well as a plain one.
4. Open the app's own screens (`Main`, keyboard settings, language selection) in both system light and dark mode to confirm the new day/night theme renders correctly, especially the `PreferenceActivity`-based settings screens flagged above as not fully Material-ized.
5. Turn on the optional "keyboard notification" preference and confirm the permission prompt appears (Android 13+) and the notification's Settings action button opens the settings screen.

**Before Play Store submission**, review:
- **Privacy policy / data safety form.** The app posts a notification (opt-in preference) and reads/writes the user dictionary; Play Console's Data Safety section needs to accurately reflect what the app does. This wasn't audited as part of this pass.
- **Screenshots and store listing** almost certainly predate this UI pass (Material theming, dynamic color) and should be refreshed.
- **`versionCode`/`versionName`** were left untouched by this modernization (still `1041001` / `v1.41.1`); bump them before releasing.
- **Manual device testing** (see above) should happen before release, especially the parts of the app CI cannot exercise.
