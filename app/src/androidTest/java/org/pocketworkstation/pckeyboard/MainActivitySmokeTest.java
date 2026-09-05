package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertNotNull;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Minimal instrumented smoke test: does the app's launcher Activity come up
 * and reach RESUMED without crashing, on a real device/emulator? This is
 * deliberately narrow -- it exercises the Phase 4 theme/DynamicColors/
 * MaterialButton changes (and everything under them: manifest, namespace,
 * AndroidX migration) actually inflating on-device, which static build+lint
 * verification in CI cannot catch.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {
    @Test
    public void mainActivity_launchesAndResumes() {
        try (ActivityScenario<Main> scenario = ActivityScenario.launch(Main.class)) {
            assertNotNull(scenario);
            scenario.moveToState(Lifecycle.State.RESUMED);
        }
    }
}
