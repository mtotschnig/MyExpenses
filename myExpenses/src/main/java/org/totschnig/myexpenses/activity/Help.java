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

package org.totschnig.myexpenses.activity;

import android.content.ComponentName;
import android.os.Bundle;

import org.totschnig.myexpenses.dialog.HelpDialogFragment;
import org.totschnig.myexpenses.util.Utils;

public class Help extends ProtectedFragmentActivity {

  public static final String KEY_CONTEXT = "context";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String context = getIntent().getStringExtra(KEY_CONTEXT);
    if (context == null) {
      ComponentName callingActivity = getCallingActivity();
      if (callingActivity != null) {
        context = Utils.getSimpleClassNameFromComponentName(callingActivity);
      } else {
        context = "MyExpenses";
      }
    }
    String variant = getIntent().getStringExtra(HelpDialogFragment.KEY_VARIANT);
    HelpDialogFragment.newInstance(context, variant).show(getSupportFragmentManager(), "HELP");
  }

  @Override
  protected int getSnackbarContainerId() {
    return android.R.id.content;
  }
}
