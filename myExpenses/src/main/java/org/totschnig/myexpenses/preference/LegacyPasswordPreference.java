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
import android.util.AttributeSet;

import org.totschnig.myexpenses.R;

import androidx.preference.DialogPreference;

public class LegacyPasswordPreference extends DialogPreference {
  private boolean mValueSet;

  public LegacyPasswordPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setDialogLayoutResource(R.layout.password_dialog);
  }

  public LegacyPasswordPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setDialogLayoutResource(R.layout.password_dialog);
  }

  public void setValue(boolean value) {
    boolean oldValue = getValue();
    boolean changed = value != oldValue;
    if (changed || !this.mValueSet) {
      this.mValueSet = true;
      this.persistBoolean(value);
      if (changed) {
        this.notifyChanged();
      }
    }
  }

  public boolean getValue() {
    return getPersistedBoolean(false);
  }
}
