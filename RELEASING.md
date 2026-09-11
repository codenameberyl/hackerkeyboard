# Releasing

This fork publishes to **GitHub Releases only** — there is no Google Play
Console account or Play Store listing for it, and no dedicated production
signing key is managed by this repo or its CI. That's a deliberate choice:
setting one up means generating a key, handing it to whoever owns the
release, and trusting them to back it up forever (losing it permanently
blocks all future updates under that identity) — out of scope unless/until
that's explicitly wanted.

## Cutting a stable release

1. Make sure `claude/hackers-keyboard-modernize-cz10e9` (or whatever branch
   you're releasing from) is green: build/lint + instrumented tests passing
   in [Actions](../../actions).
2. Bump `versionName` in `app/build.gradle` if it doesn't already reflect
   the release (e.g. `"v1.42.0"`).
3. Tag the commit and push the tag:
   ```sh
   git tag v1.42.0
   git push origin v1.42.0
   ```
4. Pushing a `vX.Y.Z` tag triggers the `publish-stable-release` job in
   `.github/workflows/build.yml`: it builds `assembleRelease`, waits on
   `build` + `instrumented-tests` for that same commit, and publishes a
   real (non-prerelease, non-draft) GitHub Release named
   `Hacker's Keyboard (by CodenameBeryl) vX.Y.Z` with the APK attached.

This is separate from the rolling `latest-debug-build` prerelease, which
republishes an unsigned/debug-signed debug APK on every push to the
modernization branch for quick manual testing — see the README's "Manual
testing" section for that one.

## Installing a release APK

**The release APK is unsigned.** Android's package installer refuses to
install a completely unsigned APK as-is — you need to sign it yourself
first, with any key (it doesn't need to be anyone's "official" key, since
there isn't one):

1. One-time: generate a local keystore (needs a JDK's `keytool`, already on
   your PATH if you have Android Studio or a JDK installed):
   ```sh
   keytool -genkeypair -v -keystore my-local-key.jks \
       -alias my-local-key -keyalg RSA -keysize 2048 -validity 10000
   ```
   Keep this file — you'll reuse it for every future update, so later
   releases can install over earlier ones. If you ever generate a new one
   instead, you'll need to uninstall the app first.
2. Download `app-release-unsigned.apk` from the release's assets, then
   sign it with [`apksigner`](https://developer.android.com/tools/apksigner)
   (bundled with the Android SDK build-tools):
   ```sh
   apksigner sign --ks my-local-key.jks \
       --ks-key-alias my-local-key \
       --out app-release-signed.apk app-release-unsigned.apk
   ```
3. Install it: `adb install app-release-signed.apk`, or copy it to the
   device and open it (with "install unknown apps" allowed for whichever
   app you copied it with).

If you already have any build of this app installed (debug or a previous
release), it must be signed with the *same* key as that install, or Android
refuses the install outright — same `applicationId`
(`org.pocketworkstation.pckeyboard`), different signature. Uninstall the
old one first if you're switching keys.
