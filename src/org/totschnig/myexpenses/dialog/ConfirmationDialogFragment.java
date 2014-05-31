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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

public class ConfirmationDialogFragment extends DialogFragment implements OnClickListener {
  
  CheckBox dontShowAgain;
  
  public static String KEY_TITLE = "title";
  public static String KEY_MESSAGE = "message";
  public static String KEY_COMMAND = "command";
  public static String KEY_PREFKEY = "prefKey";
  
  public static final ConfirmationDialogFragment newInstance(Bundle args) {
    ConfirmationDialogFragment dialogFragment = new ConfirmationDialogFragment();
    dialogFragment.setArguments(args);
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Bundle bundle = getArguments();
    Activity ctx  = getActivity();
    Context wrappedCtx = DialogUtils.wrapContext2(ctx);
    AlertDialog.Builder builder = new AlertDialog.Builder(wrappedCtx)
      .setTitle(bundle.getInt(KEY_TITLE))
      .setMessage(bundle.getCharSequence(KEY_MESSAGE));
    if (bundle.getString(KEY_PREFKEY) != null) {
      View cb = LayoutInflater.from(wrappedCtx).inflate(R.layout.checkbox, null);
      dontShowAgain = (CheckBox) cb.findViewById(R.id.skip);
      dontShowAgain.setText(R.string.confirmation_dialog_dont_show_again);
      builder.setView(cb);
    }
    builder.setPositiveButton(android.R.string.ok, this);
    builder.setNegativeButton(android.R.string.cancel, this);
    return builder.create();
  }
  @Override
  public void onCancel (DialogInterface dialog) {
    ConfirmationDialogListener ctx = (ConfirmationDialogListener) getActivity();
    if (ctx != null) {
      ctx.onMessageDialogDismissOrCancel();
    }
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    ConfirmationDialogListener ctx = (ConfirmationDialogListener) getActivity();
    if (ctx == null)  {
      return;
    }
    Bundle bundle = getArguments();
    if (which == AlertDialog.BUTTON_POSITIVE) {
      if (dontShowAgain != null && dontShowAgain.isChecked()) {
        SharedPreferencesCompat.apply(
          MyApplication.getInstance().getSettings().edit()
          .putBoolean(bundle.getString(KEY_PREFKEY), true));
      }
      ctx.dispatchCommand(bundle.getInt(KEY_COMMAND), bundle);
    } else {
      onCancel(dialog);
    }
  }
  public interface ConfirmationDialogListener {
    boolean dispatchCommand(int command, Bundle args);
    void onMessageDialogDismissOrCancel();
  }
}