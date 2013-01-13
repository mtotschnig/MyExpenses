/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses;


import java.net.URI;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;
 
/**
 * Present references screen defined in Layout file
 * @author Michael Totschnig
 *
 */
public class MyPreferenceActivity extends PreferenceActivity implements OnPreferenceChangeListener {
  ListPreference mCurrencyInputFormat;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getPrefThemeId());
    super.onCreate(savedInstanceState);
    setTitle(getString(R.string.app_name) + " " + getString(R.string.menu_settings));
    addPreferencesFromResource(R.layout.preferences);
    PreferenceScreen prefs = getPreferenceScreen();
    
    mCurrencyInputFormat = (ListPreference) 
        prefs.findPreference(MyApplication.PREFKEY_CURRENCY_DECIMAL_SEPARATOR);
    if (mCurrencyInputFormat.getValue() == null) {
      String sep = Utils.getDefaultDecimalSeparator();
      //List<String> values =  Arrays.asList(getResources().getStringArray(R.array.pref_currency_decimal_separator_values));
      mCurrencyInputFormat.setValue(sep);
      //mCurrencyInputFormat.setValueIndex(values.indexOf(sep));
    }
    EditTextPreference shareTarget = (EditTextPreference) prefs.findPreference(MyApplication.PREFKEY_SHARE_TARGET);
    shareTarget.setSummary(getString(R.string.pref_share_target_summary) + ":\n" + 
        "ftp: \"ftp://login:password@my.example.org:port/my/directory/\"\n" +
        "mailto: \"mailto:john@my.example.com\"");
    shareTarget.setOnPreferenceChangeListener(this);
    ListPreference uiTheme = (ListPreference) prefs.findPreference(MyApplication.PREFKEY_UI_THEME_KEY);
    uiTheme.setOnPreferenceChangeListener(this);
  }
  @Override
  public boolean onPreferenceChange(Preference pref, Object value) {
    if (pref.getKey().equals(MyApplication.PREFKEY_SHARE_TARGET)) {
      String target = (String) value;
      URI uri;
      if (!target.equals("")) {
        uri = Utils.validateUri(target);
        if (uri == null) {
          Toast.makeText(getBaseContext(),getString(R.string.ftp_uri_malformed,target), Toast.LENGTH_LONG).show();
          return false;
        }
        String scheme = uri.getScheme();
        if (!(scheme.equals("ftp") || scheme.equals("mailto"))) {
          Toast.makeText(getBaseContext(),getString(R.string.share_scheme_not_supported,scheme), Toast.LENGTH_LONG).show();
          return false;
        }
        final PackageManager packageManager = getPackageManager();
        Intent intent;
        if (scheme.equals("ftp")) {
          intent = new Intent(android.content.Intent.ACTION_SENDTO);
          intent.setData(android.net.Uri.parse(target));
          if (packageManager.queryIntentActivities(intent,0).size() == 0) {
            showDialog(R.id.FTP_DIALOG_ID);
          }
        }
      }
    } else if (pref.getKey().equals(MyApplication.PREFKEY_UI_THEME_KEY)) {
      Intent intent = getIntent();
      finish();
      startActivity(intent);
    }
    return true;
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    return Utils.sendWithFTPDialog((Activity) this);
  }
}