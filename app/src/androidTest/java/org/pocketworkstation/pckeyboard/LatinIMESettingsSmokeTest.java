package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertNotNull;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * LatinIMESettings is a legacy android.preference.PreferenceActivity, now
 * themed via the app-wide Theme.HackersKeyboard (Theme.Material3.DayNight)
 * applied in Phase 4 -- unlike Main, it was NOT migrated to
 * AppCompatActivity/PreferenceFragmentCompat (deferred; see Phase 2/3 notes).
 * This is the one screen most likely to actually break by rendering under a
 * Material3 theme it wasn't designed for, so it gets its own smoke test
 * rather than relying on MainActivitySmokeTest to stand in for it.
 */
@RunWith(AndroidJUnit4.class)
public class LatinIMESettingsSmokeTest {
    @Test
    public void settingsActivity_launchesAndResumes() {
        try (ActivityScenario<LatinIMESettings> scenario =
                ActivityScenario.launch(LatinIMESettings.class)) {
            assertNotNull(scenario);
            scenario.moveToState(Lifecycle.State.RESUMED);
        }
    }
}
