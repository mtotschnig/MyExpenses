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
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 *  http://stackoverflow.com/a/8004498/1199911
 *
 */
public class ListPreferenceShowSummary extends ListPreference {

  private final static String TAG = ListPreferenceShowSummary.class.getName();

  public ListPreferenceShowSummary(Context context, AttributeSet attrs) {
      super(context, attrs);
      init();
  }

  public ListPreferenceShowSummary(Context context) {
      super(context);
      init();
  }

  private void init() {

      setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

          @Override
          public boolean onPreferenceChange(Preference arg0, Object arg1) {
              arg0.setSummary(getEntry());
              return true;
          }
      });
  }

  @Override
  public CharSequence getSummary() {
      return super.getEntry();
  }
}
