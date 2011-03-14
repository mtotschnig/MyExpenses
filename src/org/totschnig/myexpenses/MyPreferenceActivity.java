package org.totschnig.myexpenses;

import android.preference.PreferenceActivity;
import android.os.Bundle;

public class MyPreferenceActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);
    }
    
}