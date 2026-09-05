package org.pocketworkstation.pckeyboard;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

/**
 * Applies Material You dynamic color (wallpaper-derived palette) to every
 * Activity on Android 12+ (API 31+), on top of the static Theme.HackersKeyboard
 * fallback palette defined in values/colors.xml and values-night/colors.xml.
 * DynamicColors.applyToActivitiesIfAvailable() is a no-op on older versions,
 * so those keep the static palette automatically.
 *
 * Note this only covers the "chrome" Activities (Main, LatinIMESettings,
 * InputLanguageSelection, PrefScreen*); the actual keyboard is drawn by
 * LatinKeyboardBaseView outside the Activity theming system, and picks up
 * the wallpaper palette separately via the values-v31 color overrides for
 * the existing "Material" keyboard skins.
 */
public class HackersKeyboardApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
