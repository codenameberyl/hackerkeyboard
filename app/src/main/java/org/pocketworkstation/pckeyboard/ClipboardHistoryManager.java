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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks a small system-wide clipboard history: every time the user copies or cuts text
 * (in this app or any other), it's captured via {@link ClipboardManager.OnPrimaryClipChangedListener}
 * and prepended to a persisted list, so it can be pasted back later even after the item
 * has been overwritten by a subsequent copy. Shown via the "Clipboard" entry in the
 * options menu -- see LatinIME#showClipboardHistory().
 *
 * <p>Entries can be pinned ({@link #togglePinned}) so they survive both the MAX_ENTRIES
 * cap and {@link #clear()} -- e.g. a phone number or address the user expects to keep
 * pasting over the next few days, as opposed to whatever they happened to copy in
 * between.
 */
class ClipboardHistoryManager implements ClipboardManager.OnPrimaryClipChangedListener {
    private static final String TAG = "HK/ClipboardHistory";
    private static final String PREF_KEY = "clipboard_history";
    private static final String JSON_KEY_TEXT = "text";
    private static final String JSON_KEY_PINNED = "pinned";
    private static final int MAX_ENTRIES = 20;

    /** A single clipboard history entry. Immutable -- {@link #togglePinned} replaces it. */
    static final class Entry {
        final String text;
        final boolean pinned;

        Entry(String text, boolean pinned) {
            this.text = text;
            this.pinned = pinned;
        }
    }

    private final Context mContext;
    private final ClipboardManager mClipboardManager;
    // Most-recently-copied first, without regard to pinned state -- see getHistory()
    // for the pinned-first view shown in the UI, and trimUnpinned() for how the
    // MAX_ENTRIES cap only ever evicts unpinned entries from here.
    private final List<Entry> mHistory = new ArrayList<>();

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
        String entryText = text.toString();
        // If this text is already in the history (e.g. the user copied it again, or
        // pasted an existing entry back out), move it to the front instead of adding a
        // duplicate, carrying over whatever pinned state it already had.
        boolean pinned = false;
        for (int i = 0; i < mHistory.size(); i++) {
            if (mHistory.get(i).text.equals(entryText)) {
                pinned = mHistory.remove(i).pinned;
                break;
            }
        }
        mHistory.add(0, new Entry(entryText, pinned));
        trimUnpinned();
        save();
    }

    /** Removes the oldest unpinned entries past MAX_ENTRIES; pinned entries never count
     * against the cap and are never removed here. */
    private void trimUnpinned() {
        int unpinnedCount = 0;
        for (Entry entry : mHistory) {
            if (!entry.pinned) unpinnedCount++;
        }
        for (int i = mHistory.size() - 1; i >= 0 && unpinnedCount > MAX_ENTRIES; i--) {
            if (!mHistory.get(i).pinned) {
                mHistory.remove(i);
                unpinnedCount--;
            }
        }
    }

    /** Pinned entries first (most-recently-copied among those first), then unpinned
     * entries, also most-recently-copied first. */
    List<Entry> getHistory() {
        List<Entry> ordered = new ArrayList<>(mHistory.size());
        List<Entry> unpinned = new ArrayList<>();
        for (Entry entry : mHistory) {
            if (entry.pinned) {
                ordered.add(entry);
            } else {
                unpinned.add(entry);
            }
        }
        ordered.addAll(unpinned);
        return ordered;
    }

    /** Flips the pinned state of the entry with this exact text, if it's still present. */
    void togglePinned(String text) {
        for (int i = 0; i < mHistory.size(); i++) {
            Entry entry = mHistory.get(i);
            if (entry.text.equals(text)) {
                mHistory.set(i, new Entry(entry.text, !entry.pinned));
                break;
            }
        }
        save();
    }

    /** Clears everything except pinned entries -- pinning is exactly the signal that an
     * entry should survive this. */
    void clear() {
        for (int i = mHistory.size() - 1; i >= 0; i--) {
            if (!mHistory.get(i).pinned) {
                mHistory.remove(i);
            }
        }
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
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    mHistory.add(new Entry(obj.getString(JSON_KEY_TEXT),
                            obj.optBoolean(JSON_KEY_PINNED, false)));
                } else {
                    // Pre-pinning format: a plain JSON string, always unpinned.
                    mHistory.add(new Entry(arr.getString(i), false));
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse saved clipboard history, discarding it", e);
        }
    }

    private void save() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        JSONArray arr = new JSONArray();
        for (Entry entry : mHistory) {
            JSONObject obj = new JSONObject();
            try {
                obj.put(JSON_KEY_TEXT, entry.text);
                obj.put(JSON_KEY_PINNED, entry.pinned);
            } catch (JSONException e) {
                // Only thrown for a null key, which JSON_KEY_TEXT/JSON_KEY_PINNED never are.
                throw new AssertionError(e);
            }
            arr.put(obj);
        }
        prefs.edit().putString(PREF_KEY, arr.toString()).apply();
    }
}
