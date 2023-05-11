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

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;

import androidx.preference.EditTextPreferenceDialogFragmentCompat;

public class SecurityQuestionDialogFragmentCompat extends EditTextPreferenceDialogFragmentCompat {
  private EditText answer;

    @Override
    protected void onBindDialogView(View view) {
      super.onBindDialogView(view);
      answer = view.findViewById(R.id.answer);
   }
    @Override
    public void onDialogClosed(boolean positiveResult) {
      super.onDialogClosed(positiveResult);
      if (positiveResult) {
          ((MyApplication) requireContext().getApplicationContext()).getAppComponent().prefHandler()
                  .putString(PrefKey.SECURITY_ANSWER, Utils.md5(answer.getText().toString()));
      }
    }

  public static SecurityQuestionDialogFragmentCompat newInstance(String key) {
    SecurityQuestionDialogFragmentCompat fragment = new SecurityQuestionDialogFragmentCompat();
    Bundle bundle = new Bundle(1);
    bundle.putString("key", key);
    fragment.setArguments(bundle);
    return fragment;
  }
}
