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


import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
 
/**
 * Present references screen defined in Layout file
 * @author Michael Totschnig
 *
 */
public class MyPreferenceActivity extends PreferenceActivity {
  ListPreference mCurrencyInputFormat;
  EditTextPreference mShareTarget;
  
@Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle(getString(R.string.app_name) + " " + getString(R.string.menu_settings));
    addPreferencesFromResource(R.layout.preferences);
    PreferenceScreen prefs = getPreferenceScreen();
    
    mCurrencyInputFormat = (ListPreference) prefs.findPreference("currency_decimal_separator");
    if (mCurrencyInputFormat.getValue() == null) {
      String sep = Utils.getDefaultDecimalSeparator();
      //List<String> values =  Arrays.asList(getResources().getStringArray(R.array.pref_currency_decimal_separator_values));
      mCurrencyInputFormat.setValue(sep);
      //mCurrencyInputFormat.setValueIndex(values.indexOf(sep));
    }
    mShareTarget = (EditTextPreference) prefs.findPreference("share_target");
    mShareTarget.setSummary(getString(R.string.pref_share_target_summary) + ":\n" + 
        "ftp: \"ftp://login:password@my.example.org:port/my/directory/\"\n" +
        "mailto: \"mailto:john@my.example.com\"");
 }
}