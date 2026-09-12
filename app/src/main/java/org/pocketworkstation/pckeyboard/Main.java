/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class Main extends AppCompatActivity {

    private final static String MARKET_URI = "market://search?q=pub:\"Klaus Weidner\"";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // main.xml already renders its own styled header (icon + app name +
        // tagline), so a native ActionBar title repeating the same app name
        // in plain text would just be a redundant duplicate. Theme.HackersKeyboard.Main
        // (see the manifest's android:theme for this Activity) is a NoActionBar
        // variant for exactly this reason, so there's nothing to hide here at runtime.
        String html = getString(R.string.main_body);
        // Html.fromHtml(String) is deprecated since API 24; the single-arg
        // form is kept only as the pre-24 fallback.
        Spanned content = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
                : Html.fromHtml(html);
        TextView description = (TextView) findViewById(R.id.main_description);
        description.setMovementMethod(LinkMovementMethod.getInstance());
        description.setText(content, BufferType.SPANNABLE);

        TextView version = (TextView) findViewById(R.id.main_version);
        version.setText(getString(R.string.main_version_format, getString(R.string.auto_version)));

        final MaterialButton setup1 = (MaterialButton) findViewById(R.id.main_setup_btn_configure_imes);
        setup1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
            }
        });

        final MaterialButton setup2 = (MaterialButton) findViewById(R.id.main_setup_btn_set_ime);
        setup2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.showInputMethodPicker();
            }
        });
        
        final AppCompatActivity that = this;

        final MaterialButton setup4 = (MaterialButton) findViewById(R.id.main_setup_btn_input_lang);
        setup4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(that, InputLanguageSelection.class), 0);
            }
        });

        final MaterialButton setup3 = (MaterialButton) findViewById(R.id.main_setup_btn_get_dicts);
        setup3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI));
                try {
                	startActivity(it);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(
                            		R.string.no_market_warning), Toast.LENGTH_LONG)
                            .show();
                }
            }
        });
        // PluginManager.getPluginDictionaries(getApplicationContext()); // why?

        final MaterialButton setup5 = (MaterialButton) findViewById(R.id.main_setup_btn_settings);
        setup5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(that, LatinIMESettings.class), 0);
            }
        });
    }
}

