package org.pocketworkstation.pckeyboard;

import android.app.Application;

/**
 * No-op onCreate() kept as the manifest's declared application class (see
 * android:name in AndroidManifest.xml).
 *
 * This used to also apply Material You dynamic color (wallpaper-derived
 * palette) to every "chrome" Activity via
 * DynamicColors.applyToActivitiesIfAvailable() on Android 12+. That's been
 * removed: the app now uses a deliberate fixed black background + yellow
 * accent brand palette (see Theme.HackersKeyboard in values/themes.xml and
 * the md_theme_* colors in values/colors.xml / values-night/colors.xml)
 * rather than one that adapts to the user's wallpaper, so applying dynamic
 * color here would override that choice on Android 12+ devices.
 *
 * The actual keyboard is drawn by LatinKeyboardBaseView outside the
 * Activity theming system entirely, and still separately picks up the
 * wallpaper palette via the values-v31 color overrides for the existing
 * "Material" keyboard skins -- that's a distinct, still-customizable
 * per-user preference ("Theme and label settings"), not the app's own
 * chrome, so it's unaffected by this change.
 */
public class HackersKeyboardApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
