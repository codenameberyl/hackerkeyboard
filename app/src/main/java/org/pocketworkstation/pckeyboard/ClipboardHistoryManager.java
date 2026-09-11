/*
 * Copyright (C) 2026 The Hacker's Keyboard contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.pocketworkstation.pckeyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks a small system-wide clipboard history: every time the user copies or cuts text
 * (in this app or any other), it's captured via {@link ClipboardManager.OnPrimaryClipChangedListener}
 * and prepended to a persisted list, so it can be pasted back later even after the item
 * has been overwritten by a subsequent copy. Shown via the "Clipboard" entry in the
 * options menu -- see LatinIME#showClipboardHistory().
 */
class ClipboardHistoryManager implements ClipboardManager.OnPrimaryClipChangedListener {
    private static final String TAG = "HK/ClipboardHistory";
    private static final String PREF_KEY = "clipboard_history";
    private static final int MAX_ENTRIES = 20;

    private final Context mContext;
    private final ClipboardManager mClipboardManager;
    private final List<String> mHistory = new ArrayList<>();

    ClipboardHistoryManager(Context context) {
        mContext = context;
        mClipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        load();
    }

    /**
     * Starts listening for clipboard changes, and also captures whatever is already on
     * the clipboard right now. Without this second part, text copied before the
     * keyboard was ever started (e.g. right after installing it, before it's been used
     * as an IME at least once) would never show up: OnPrimaryClipChangedListener only
     * fires for *future* changes, and there's no OS API to retrieve clipboard history
     * from before it was registered. Call from LatinIME#onCreate().
     */
    void start() {
        mClipboardManager.addPrimaryClipChangedListener(this);
        addClip(mClipboardManager.getPrimaryClip());
    }

    /** Stops listening for clipboard changes. Call from LatinIME#onDestroy(). */
    void stop() {
        mClipboardManager.removePrimaryClipChangedListener(this);
    }

    @Override
    public void onPrimaryClipChanged() {
        addClip(mClipboardManager.getPrimaryClip());
    }

    private void addClip(ClipData clip) {
        if (clip == null || clip.getItemCount() == 0) return;
        CharSequence text = clip.getItemAt(0).coerceToText(mContext);
        if (TextUtils.isEmpty(text)) return;
        String entry = text.toString();
        // If this text is already in the history (e.g. the user copied it again, or
        // pasted an existing entry back out), move it to the front instead of adding a
        // duplicate.
        mHistory.remove(entry);
        mHistory.add(0, entry);
        while (mHistory.size() > MAX_ENTRIES) {
            mHistory.remove(mHistory.size() - 1);
        }
        save();
    }

    /** Most-recently-copied entry first. */
    List<String> getHistory() {
        return mHistory;
    }

    void clear() {
        mHistory.clear();
        save();
    }

    private void load() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        String json = prefs.getString(PREF_KEY, null);
        mHistory.clear();
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                mHistory.add(arr.getString(i));
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse saved clipboard history, discarding it", e);
        }
    }

    private void save() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        JSONArray arr = new JSONArray();
        for (String entry : mHistory) {
            arr.put(entry);
        }
        prefs.edit().putString(PREF_KEY, arr.toString()).apply();
    }
}
