package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Confirms the LatinIME InputMethodService is still correctly declared and
 * resolvable after the Phase 2/3 manifest changes (namespace migration,
 * android:exported, package-visibility <queries>) -- without actually
 * enabling/binding the IME, which needs shell-level "ime enable" access this
 * lightweight smoke test deliberately doesn't take on.
 */
@RunWith(AndroidJUnit4.class)
public class ImeServiceDeclarationTest {
    @Test
    public void imeService_isDeclaredAndExported() throws PackageManager.NameNotFoundException {
        PackageManager pm = InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getPackageManager();
        String pkg = InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
        ComponentName component = new ComponentName(pkg, "org.pocketworkstation.pckeyboard.LatinIME");

        ServiceInfo info = pm.getServiceInfo(component, PackageManager.GET_META_DATA);

        assertNotNull(info);
        assertTrue("LatinIME service must be exported for the system to bind it as an IME",
                info.exported);
        assertNotNull("Missing android.view.im metadata (res/xml/method.xml)", info.metaData);
    }
}
