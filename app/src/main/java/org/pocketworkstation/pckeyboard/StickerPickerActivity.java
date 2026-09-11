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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Invisible trampoline: an {@link InputMethodService} (LatinIME) has no
 * startActivityForResult() of its own, so this activity exists purely to launch the
 * system's document picker for images/GIFs on its behalf, copy whatever was picked into
 * our own cache directory (so we can vend it via FileProvider -- forwarding the picker's
 * own content:// URI onward via commitContent() is not reliably supported across all
 * DocumentsProvider implementations and receiving apps), and hand the resulting
 * FileProvider URI back to LatinIME. See LatinIME#launchStickerPicker()/
 * #onStickerPicked()/#handleStickerPicked().
 */
public class StickerPickerActivity extends Activity {
    private static final String TAG = "HK/StickerPicker";
    private static final String[] MIME_TYPES =
            { "image/gif", "image/png", "image/jpeg", "image/webp" };
    private static final int REQUEST_PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, MIME_TYPES);
        try {
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No activity to handle ACTION_OPEN_DOCUMENT for images", e);
            LatinIME.onStickerPicked(null, null);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_IMAGE) return;
        Uri pickedUri = (resultCode == RESULT_OK && data != null) ? data.getData() : null;
        if (pickedUri == null) {
            LatinIME.onStickerPicked(null, null);
            finish();
            return;
        }
        copyToCacheAndNotify(pickedUri);
        finish();
    }

    private void copyToCacheAndNotify(Uri sourceUri) {
        ContentResolver resolver = getContentResolver();
        String mimeType = resolver.getType(sourceUri);
        if (mimeType == null) mimeType = "image/*";
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension == null) extension = "bin";

        File stickerDir = new File(getCacheDir(), "stickers");
        if (!stickerDir.exists() && !stickerDir.mkdirs()) {
            Log.w(TAG, "Failed to create cache/stickers dir");
            LatinIME.onStickerPicked(null, null);
            return;
        }
        File outFile = new File(stickerDir, "sticker_" + System.currentTimeMillis() + "." + extension);

        try (InputStream in = resolver.openInputStream(sourceUri);
             OutputStream out = new FileOutputStream(outFile)) {
            if (in == null) throw new IOException("openInputStream returned null");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to copy picked sticker/GIF into cache", e);
            LatinIME.onStickerPicked(null, null);
            return;
        }

        // Authority must track the runtime package name (getPackageName()), not a
        // hardcoded literal -- the debug build appends applicationIdSuffix
        // ".codenameberyl", and the manifest's matching ${applicationId}.fileprovider
        // authority already resolves that way at build time.
        Uri contentUri = FileProvider.getUriForFile(
                this, getPackageName() + ".fileprovider", outFile);
        LatinIME.onStickerPicked(contentUri, mimeType);
    }
}
