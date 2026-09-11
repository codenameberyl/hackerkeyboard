package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.button.MaterialButton;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Regression test for a real crash: LatinIME#populateClipboardHistoryList() used to
 * inflate clipboard_history_item.xml with the IME service's own plain
 * getLayoutInflater() rather than a Theme.HackersKeyboard-wrapped context. That layout
 * references Material3-only attrs (?attr/colorOnSurface), which the IME service's base
 * theme doesn't define -- InputMethodService, unlike an Activity, isn't themed via the
 * manifest's android:theme, so this crashed instantly for any user with at least one
 * clipboard entry (an empty history took an early-return path that never hit the bug).
 * This mirrors what LatinIME#getClipboardHistoryContainer()/populateClipboardHistoryList()
 * actually do -- inflate the container once under a ContextThemeWrapper, then inflate
 * each row via LayoutInflater.from(container.getContext()) -- without needing to bind a
 * real IME service in the test.
 */
@RunWith(AndroidJUnit4.class)
public class ClipboardHistoryViewTest {
    @Test
    public void clipboardHistoryContainerAndItem_inflateUnderThemedContext() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context themedContext =
                new ContextThemeWrapper(targetContext, R.style.Theme_HackersKeyboard);
        LayoutInflater themedInflater = LayoutInflater.from(themedContext);

        View container = themedInflater.inflate(R.layout.clipboard_history_container, null);
        assertNotNull(container);

        MaterialButton backButton = container.findViewById(R.id.clipboard_history_back);
        MaterialButton clearButton = container.findViewById(R.id.clipboard_history_clear);
        LinearLayout list = container.findViewById(R.id.clipboard_history_list);
        assertNotNull(backButton);
        assertNotNull(clearButton);
        assertNotNull(list);

        // The exact call LatinIME#populateClipboardHistoryList() makes for each history
        // entry: LayoutInflater.from(container.getContext()), not getLayoutInflater().
        LayoutInflater itemInflater = LayoutInflater.from(container.getContext());
        TextView row = (TextView) itemInflater.inflate(
                R.layout.clipboard_history_item, list, false);
        assertNotNull(row);
        row.setText("test clip");
        list.addView(row);
    }
}
