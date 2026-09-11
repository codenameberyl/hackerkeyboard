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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
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
        String html = getString(R.string.main_body);
        html += "<p><i>Version: " + getString(R.string.auto_version) + "</i></p>";
        // Html.fromHtml(String) is deprecated since API 24; the single-arg
        // form is kept only as the pre-24 fallback.
        Spanned content = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
                : Html.fromHtml(html);
        TextView description = (TextView) findViewById(R.id.main_description);
        description.setMovementMethod(LinkMovementMethod.getInstance());
        description.setText(content, BufferType.SPANNABLE);


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

    @Override
    protected void onResume() {
        super.onResume();
        // TEMPORARY: surfaces the round-3 suggestions diagnostic LatinIME
        // wrote to SharedPreferences (see its saveDiagnostic()) -- Toasts
        // from a background IME service are silently dropped by some OEM
        // "background pop-up" restrictions in other apps, so this is the
        // reliable way to read back what happened while testing there:
        // switch away, type a word, then switch back to this screen.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String view = prefs.getString(LatinIME.PREF_DIAG_VIEW, null);
        String sugg = prefs.getString(LatinIME.PREF_DIAG_SUGG, null);
        TextView diag = (TextView) findViewById(R.id.main_diag);
        if (TextUtils.isEmpty(view) && TextUtils.isEmpty(sugg)) {
            diag.setVisibility(View.GONE);
        } else {
            StringBuilder sb = new StringBuilder("Last suggestions diagnostic:\n");
            if (!TextUtils.isEmpty(view)) sb.append(view).append('\n');
            if (!TextUtils.isEmpty(sugg)) sb.append(sugg).append('\n');
            diag.setText(sb.toString());
            diag.setVisibility(View.VISIBLE);
        }
    }
}

