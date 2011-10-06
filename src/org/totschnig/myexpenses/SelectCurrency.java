/* based on com.android.settings.LocalePicker*/
/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.totschnig.myexpenses;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

public class SelectCurrency extends ListActivity {
    private static final String TAG = "LocalePicker";
    private static final boolean DEBUG = true;

    Loc[] mLocales;

    private static class Loc implements Comparable {
        static Collator sCollator = Collator.getInstance();

        String label;
        Locale locale;

        public Loc(String label, Locale locale) {
            this.label = label;
            this.locale = locale;
        }

        @Override
        public String toString() {
            return this.label;
        }

        public int compareTo(Object o) {
            return sCollator.compare(this.label, ((Loc) o).label);
        }
    }

    int getContentView() {
        return  R.layout.locale_picker;
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(getContentView());

        String[] locales = getAssets().getLocales();
        Arrays.sort(locales);

        final int origSize = locales.length;
        Loc[] preprocess = new Loc[origSize];
        int finalSize = 0;
        for (int i = 0 ; i < origSize; i++ ) {
            String s = locales[i];
            int len = s.length();
            if (len == 5) {
                String language = s.substring(0, 2);
                String country = s.substring(3, 5);
                Locale l = new Locale(language, country);

                if (finalSize == 0) {
                    if (DEBUG) {
                        Log.v(TAG, "adding initial "+ toTitleCase(l.getDisplayLanguage(l)));
                    }
                    preprocess[finalSize++] =
                            new Loc(toTitleCase(l.getDisplayLanguage(l)), l);
                } else {
                    // check previous entry:
                    //  same lang and a country -> upgrade to full name and
                    //    insert ours with full name
                    //  diff lang -> insert ours with lang-only name
                    if (preprocess[finalSize-1].locale.getLanguage().equals(
                            language)) {
                        if (DEBUG) {
                            Log.v(TAG, "backing up and fixing "+
                                    preprocess[finalSize-1].label+" to "+
                                    getDisplayName(preprocess[finalSize-1].locale));
                        }
                        preprocess[finalSize-1].label = toTitleCase(
                                getDisplayName(preprocess[finalSize-1].locale));
                        if (DEBUG) {
                            Log.v(TAG, "  and adding "+ toTitleCase(getDisplayName(l)));
                        }
                        preprocess[finalSize++] =
                                new Loc(toTitleCase(getDisplayName(l)), l);
                    } else {
                        String displayName;
                        if (s.equals("zz_ZZ")) {
                            displayName = "Pseudo...";
                        } else {
                            displayName = toTitleCase(l.getDisplayLanguage(l));
                        }
                        if (DEBUG) {
                            Log.v(TAG, "adding "+displayName);
                        }
                        preprocess[finalSize++] = new Loc(displayName, l);
                    }
                }
            }
        }
        mLocales = new Loc[finalSize];
        for (int i = 0; i < finalSize ; i++) {
            mLocales[i] = preprocess[i];
        }
        Arrays.sort(mLocales);
        int layoutId = R.layout.locale_picker_item;
        int fieldId = R.id.locale;
        ArrayAdapter<Loc> adapter =
                new ArrayAdapter<Loc>(this, layoutId, fieldId, mLocales);
        getListView().setAdapter(adapter);
    }

    private static String toTitleCase(String s) {
        if (s.length() == 0) {
            return s;
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String getDisplayName(Locale l) {
        return l.getDisplayName(l);
    }

    @Override
    public void onResume() {
        super.onResume();
        getListView().requestFocus();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
      Locale loc = mLocales[position].locale;
      Intent intent=new Intent();
      intent.putExtra("locale",loc);
      setResult(RESULT_OK,intent); 
      finish();
    }
}
