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

package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;

public class EditTextPreferenceShowSummary extends EditTextPreference {

  private CharSequence defaultSummary;

  public EditTextPreferenceShowSummary(Context context, AttributeSet attrs) {
      super(context, attrs);
      init();
  }

  public EditTextPreferenceShowSummary(Context context) {
      super(context);
      init();
  }

  private void init() {

    defaultSummary = super.getSummary();

      setOnPreferenceChangeListener((arg0, arg1) -> {
        CharSequence newValue = (CharSequence) arg1;
        arg0.setSummary(TextUtils.isEmpty(newValue) ? defaultSummary : newValue);
          return true;
      });
  }

  @Override
  public CharSequence getSummary() {
    String currentValue = getPersistedString(null);
    return TextUtils.isEmpty(currentValue) ? defaultSummary : currentValue;
  }
}
