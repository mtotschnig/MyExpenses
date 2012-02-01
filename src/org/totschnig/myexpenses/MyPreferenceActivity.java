package org.totschnig.myexpenses;


import android.os.Bundle;
import android.preference.PreferenceActivity;
 
public class MyPreferenceActivity extends PreferenceActivity {
@Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
     
    addPreferencesFromResource(R.layout.preferences);
  }
}