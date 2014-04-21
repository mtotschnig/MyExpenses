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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class RemindRateDialogFragment  extends DialogFragment implements OnClickListener {

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    //tv.setMovementMethod(LinkMovementMethod.getInstance());
    return new AlertDialog.Builder(getActivity())
      .setTitle(R.string.app_name)
      .setMessage(R.string.dialog_remind_rate)
      .setCancelable(false)
      .setPositiveButton(R.string.dialog_remind_rate_yes, this)
      .setNeutralButton(R.string.dialog_remind_later,this)
      .setNegativeButton(R.string.dialog_remind_no,this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity()==null)
      return;
    if (which == AlertDialog.BUTTON_POSITIVE)
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.RATE_COMMAND,null);
    else if (which == AlertDialog.BUTTON_NEUTRAL)
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.REMIND_LATER_COMMAND,"Rate");
    else
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.REMIND_NO_COMMAND,"Rate");
  }
}