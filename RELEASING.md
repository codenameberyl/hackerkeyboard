# Releasing

This fork publishes to **GitHub Releases only** — there is no Google Play
Console account or Play Store listing for it. Release APKs are self-signed
with a dedicated keystore (not Play App Signing), generated once via
`keytool` and stored only as GitHub Actions repo secrets — never committed
to this repository.

## One-time setup: configuring the signing secrets

`publish-stable-release` (`.github/workflows/build.yml`) signs the release
APK automatically if — and only if — this repo has these four secrets set
(**Settings → Secrets and variables → Actions → Repository secrets**):

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | The keystore file, base64-encoded (`base64 -w0 my-release.jks`) |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore's store password |
| `RELEASE_KEY_ALIAS` | The key alias inside that keystore |
| `RELEASE_KEY_PASSWORD` | The key's password (same as the store password for a PKCS12 keystore — `keytool`'s default format — since PKCS12 doesn't support the two being different) |

If these aren't set, `publish-stable-release` still runs and produces an
**unsigned** APK exactly as it did before signing was set up — see
"Installing an unsigned release APK" below. Nothing else about the release
process changes either way; `app/build.gradle`'s release `signingConfig` is
applied only when all four of the matching `RELEASE_KEYSTORE_PATH`/
`RELEASE_KEYSTORE_PASSWORD`/`RELEASE_KEY_ALIAS`/`RELEASE_KEY_PASSWORD`
environment variables are present, which the workflow populates from these
secrets.

**Keep your own copy of the keystore file and its passwords somewhere
durable (a password manager, an encrypted backup) — GitHub Actions secrets
are write-only, so if you lose your own copy there is no way to retrieve it
back out, even from this repo's own settings.** Losing the keystore entirely
(both your copy and the secret) permanently blocks signing any future
release under the same identity; the fix would be generating a new keystore
and accepting that Android treats it as a different app for install/update
purposes (existing installs would need to be uninstalled before an update
signed with the new key can be installed over them).

To generate a new keystore from scratch (only needed if you don't already
have one, or are deliberately rotating):
```sh
keytool -genkeypair -v -keystore release.jks \
    -alias <your-alias> -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks   # paste this as RELEASE_KEYSTORE_BASE64
```

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
4. Pushing a `vX.Y.Z` tag triggers the `publish-stable-release` job: it
   builds `assembleRelease` (signed, if the secrets above are configured),
   waits on `build` + `instrumented-tests` for that same commit, and
   publishes a real (non-prerelease, non-draft) GitHub Release named
   `Hacker's Keyboard (by CodenameBeryl) vX.Y.Z` with the APK attached.

This is separate from the rolling `latest-debug-build` prerelease, which
republishes an unsigned/debug-signed debug APK on every push to the
modernization branch for quick manual testing — see the README's "Manual
testing" section for that one.

## Installing a signed release APK

If the release's notes say the APK is signed, just install it:
`adb install app-release.apk`, or copy it to the device and open it (with
"install unknown apps" allowed for whichever app you copied it with). If you
already have a build of this app installed under a *different* signing key
(e.g. an old self-signed build from before this release's key existed, or a
Play Store release), Android refuses the install outright — same
`applicationId` (`org.pocketworkstation.pckeyboard`), different signature.
Uninstall the old one first.

## Installing an unsigned release APK

If a release's notes say the APK is unsigned (the signing secrets above
weren't configured for that build), you need to sign it yourself first,
with any key:

1. One-time: generate a local keystore (needs a JDK's `keytool`, already on
   your PATH if you have Android Studio or a JDK installed):
   ```sh
   keytool -genkeypair -v -keystore my-local-key.jks \
       -alias my-local-key -keyalg RSA -keysize 2048 -validity 10000
   ```
   Keep this file — you'll reuse it for every future update signed this
   same way, so later releases can install over earlier ones.
2. Download `app-release-unsigned.apk` from the release's assets, then sign
   it with [`apksigner`](https://developer.android.com/tools/apksigner)
   (bundled with the Android SDK build-tools):
   ```sh
   apksigner sign --ks my-local-key.jks \
       --ks-key-alias my-local-key \
       --out app-release-signed.apk app-release-unsigned.apk
   ```
3. Install it the same way as a signed release APK, above.
