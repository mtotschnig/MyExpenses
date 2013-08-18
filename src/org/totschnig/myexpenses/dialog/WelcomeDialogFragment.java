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

package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class WelcomeDialogFragment extends DialogFragment {
  
  public static final WelcomeDialogFragment newInstance() {
    return new WelcomeDialogFragment();
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx  = (Activity) getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.welcome_dialog, null);
    return new AlertDialog.Builder(ctx)
      .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.dialog_title_welcome))
      .setIcon(R.drawable.icon)
      .setView(view)
      .setPositiveButton(android.R.string.ok,null)
      .create();
  }
}
