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
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;
 
/**
 * Present references screen defined in Layout file
 * @author Michael Totschnig
 *
 */
public class MyPreferenceActivity extends PreferenceActivity implements OnPreferenceChangeListener {
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setTitle(getString(R.string.app_name) + " " + getString(R.string.menu_settings));
    addPreferencesFromResource(R.layout.preferences);
    MyApplication.updateUIWithAppColor(this);
    
    ListPreference listPref = (ListPreference) 
        findPreference(MyApplication.PREFKEY_CURRENCY_DECIMAL_SEPARATOR);
    if (listPref.getValue() == null) {
      String sep = Utils.getDefaultDecimalSeparator();
      //List<String> values =  Arrays.asList(getResources().getStringArray(R.array.pref_currency_decimal_separator_values));
      listPref.setValue(sep);
      //mCurrencyInputFormat.setValueIndex(values.indexOf(sep));
    }
    Preference pref = findPreference(MyApplication.PREFKEY_SHARE_TARGET);
    pref.setSummary(getString(R.string.pref_share_target_summary) + ":\n" + 
        "ftp: \"ftp://login:password@my.example.org:port/my/directory/\"\n" +
        "mailto: \"mailto:john@my.example.com\"");
    pref.setOnPreferenceChangeListener(this);
    findPreference(MyApplication.PREFKEY_UI_THEME_KEY)
      .setOnPreferenceChangeListener(this);
    findPreference(MyApplication.PREFKEY_CONTRIB_INSTALL).setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          Utils.viewContribApp(MyPreferenceActivity.this);
          return true;
        }
    });
    findPreference(MyApplication.PREFKEY_REQUEST_LICENCE).setOnPreferenceClickListener(new OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        String androidId = Secure.getString(getContentResolver(),Secure.ANDROID_ID);
        Intent i = new Intent(android.content.Intent.ACTION_SEND);
        i.setType("plain/text");
        i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ MyExpenses.FEEDBACK_EMAIL });
        i.putExtra(android.content.Intent.EXTRA_SUBJECT,
            "[" + getString(R.string.app_name) + "] " + getString(R.string.contrib_key));
        i.putExtra(android.content.Intent.EXTRA_TEXT,
            getString(R.string.request_licence_mail_body,androidId));
        startActivity(i);
        return true;
      }
    });
    findPreference(MyApplication.PREFKEY_ENTER_LICENCE)
    .setOnPreferenceChangeListener(this);
  }
  @Override
  public boolean onPreferenceChange(Preference pref, Object value) {
    String key = pref.getKey();
    if (key.equals(MyApplication.PREFKEY_SHARE_TARGET)) {
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
        Intent intent;
        if (scheme.equals("ftp")) {
          intent = new Intent(android.content.Intent.ACTION_SENDTO);
          intent.setData(android.net.Uri.parse(target));
          if (!Utils.isIntentAvailable(this,intent)) {
            showDialog(R.id.FTP_DIALOG_ID);
          }
        }
      }
    } else if (key.equals(MyApplication.PREFKEY_UI_THEME_KEY)) {
      Intent intent = getIntent();
      finish();
      startActivity(intent);
    } else if (key.equals(MyApplication.PREFKEY_ENTER_LICENCE)) {
     if (Utils.verifyLicenceKey((String)value)) {
       Toast.makeText(getBaseContext(), R.string.licence_validation_success, Toast.LENGTH_LONG).show();
       MyApplication.getInstance().isContribEnabled = true;
     } else {
       Toast.makeText(getBaseContext(), R.string.licence_validation_failure, Toast.LENGTH_LONG).show();
       MyApplication.getInstance().isContribEnabled = false;
     }
    }
    return true;
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    switch(id) {
    case R.id.FTP_DIALOG_ID:
    return Utils.sendWithFTPDialog((Activity) this);
    }
    return null;
  }
}