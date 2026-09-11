package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertNotNull;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke tests for the four PreferenceActivity screens that weren't already covered by
 * LatinIMESettingsSmokeTest: PrefScreenActions/Feedback/View and InputLanguageSelection.
 * These all load their own prefs*.xml/language_prefs.xml and, like
 * LatinIMESettingsSmokeTest covers for prefs.xml, are the ones most likely to actually
 * break by rendering under a custom preference row layout it wasn't designed for -- see
 * the pref_item_row.xml/pref_category_header.xml redesign these XML files now
 * reference via android:layout=.
 */
@RunWith(AndroidJUnit4.class)
public class PrefScreensSmokeTest {
    @Test
    public void prefScreenActions_launchesAndResumes() {
        try (ActivityScenario<PrefScreenActions> scenario =
                ActivityScenario.launch(PrefScreenActions.class)) {
            assertNotNull(scenario);
            scenario.moveToState(Lifecycle.State.RESUMED);
        }
    }

    @Test
    public void prefScreenFeedback_launchesAndResumes() {
        try (ActivityScenario<PrefScreenFeedback> scenario =
                ActivityScenario.launch(PrefScreenFeedback.class)) {
            assertNotNull(scenario);
            scenario.moveToState(Lifecycle.State.RESUMED);
        }
    }

    @Test
    public void prefScreenView_launchesAndResumes() {
        try (ActivityScenario<PrefScreenView> scenario =
                ActivityScenario.launch(PrefScreenView.class)) {
            assertNotNull(scenario);
            scenario.moveToState(Lifecycle.State.RESUMED);
        }
    }

    @Test
    public void inputLanguageSelection_launchesAndResumes() {
        try (ActivityScenario<InputLanguageSelection> scenario =
                ActivityScenario.launch(InputLanguageSelection.class)) {
            assertNotNull(scenario);
            scenario.moveToState(Lifecycle.State.RESUMED);
        }
    }
}
