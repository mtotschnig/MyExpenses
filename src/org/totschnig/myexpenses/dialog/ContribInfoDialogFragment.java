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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.TextUtils;

public class ContribInfoDialogFragment  extends DialogFragment implements OnClickListener {

  /**
   * @param reminderP yes if we are called from a reminder
   * @return
   */
  public static final ContribInfoDialogFragment newInstance(boolean reminderP) {
    ContribInfoDialogFragment dialogFragment = new ContribInfoDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putBoolean("reminderP", reminderP);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    CharSequence
      linefeed = Html.fromHtml("<br><br>"),
      message = TextUtils.concat(
        getText(R.string.dialog_contrib_text),
        linefeed,
        Utils.getContribFeatureLabelsAsFormattedList(getActivity(),null),
        linefeed,
        getString(R.string.thank_you));
    //tv.setMovementMethod(LinkMovementMethod.getInstance());
    AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity())
      .setTitle(R.string.menu_contrib);
      builder.setMessage(message)
        .setPositiveButton(R.string.dialog_contrib_yes, this);
      if (getArguments().getBoolean("reminderP")) {
        builder.setCancelable(false)
          .setNeutralButton(R.string.dialog_remind_later,this)
          .setNegativeButton(R.string.dialog_remind_no,this);
      } else {
        builder.setNegativeButton(R.string.dialog_contrib_no,null);
      }
    return builder.create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == AlertDialog.BUTTON_POSITIVE)
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.CONTRIB_BUY_COMMAND,null);
    else if (which == AlertDialog.BUTTON_NEUTRAL)
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.REMIND_LATER_COMMAND,"Contrib");
    else
      ((MessageDialogListener) getActivity())
        .dispatchCommand(R.id.REMIND_NO_COMMAND,"Contrib");
  }
}