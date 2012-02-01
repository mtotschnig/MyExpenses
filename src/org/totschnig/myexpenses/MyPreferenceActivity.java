package org.totschnig.myexpenses;


import android.os.Bundle;
import android.preference.PreferenceActivity;
 
/**
 * Present references screen defined in Layout file
 * @author Michael Totschnig
 *
 */
public class MyPreferenceActivity extends PreferenceActivity {
@Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
     
    addPreferencesFromResource(R.layout.preferences);
  }
}