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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

public class ConfirmationDialogFragment extends CommitSafeDialogFragment implements OnClickListener {
  
  CheckBox dontShowAgain;
  
  public static String KEY_TITLE = "title";
  public static String KEY_MESSAGE = "message";
  public static String KEY_COMMAND = "command";
  public static String KEY_PREFKEY = "prefKey";
  public static String KEY_POSITIVE_BUTTON_LABEL = "positiveButtonLabel";
  public static String KEY_NEGATIVE_BUTTON_LABEL = "negativeButtonLabel";

  public static final ConfirmationDialogFragment newInstance(Bundle args) {
    ConfirmationDialogFragment dialogFragment = new ConfirmationDialogFragment();
    dialogFragment.setArguments(args);
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Bundle bundle = getArguments();
    Activity ctx  = getActivity();
    Context wrappedCtx = DialogUtils.wrapContext1(ctx);
    AlertDialog.Builder builder = new AlertDialog.Builder(wrappedCtx)
      .setTitle(bundle.getInt(KEY_TITLE))
      .setMessage(bundle.getCharSequence(KEY_MESSAGE));
    if (bundle.getString(KEY_PREFKEY) != null) {
      View cb = LayoutInflater.from(wrappedCtx).inflate(R.layout.checkbox, null);
      dontShowAgain = (CheckBox) cb.findViewById(R.id.skip);
      dontShowAgain.setText(R.string.confirmation_dialog_dont_show_again);
      builder.setView(cb);
    }
    int positiveLabel = bundle.getInt(KEY_POSITIVE_BUTTON_LABEL);
    int negativeLabel = bundle.getInt(KEY_NEGATIVE_BUTTON_LABEL);
    builder.setPositiveButton(positiveLabel == 0 ? android.R.string.ok : positiveLabel, this);
    builder.setNegativeButton(negativeLabel == 0 ? android.R.string.cancel: negativeLabel, this);
    return builder.create();
  }
  @Override
  public void onCancel (DialogInterface dialog) {
    ConfirmationDialogListener ctx = (ConfirmationDialogListener) getActivity();
    if (ctx != null) {
      ctx.onConfirmationDialogDismissOrCancel(getArguments().getInt(KEY_COMMAND));
    }
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    ConfirmationDialogListener ctx = (ConfirmationDialogListener) getActivity();
    if (ctx == null)  {
      return;
    }
    Bundle bundle = getArguments();
    if (dontShowAgain != null && dontShowAgain.isChecked()) {
      SharedPreferencesCompat.apply(
        MyApplication.getInstance().getSettings().edit()
        .putBoolean(bundle.getString(KEY_PREFKEY), true));
    }
    if (which == AlertDialog.BUTTON_POSITIVE) {
      ctx.dispatchCommand(bundle.getInt(KEY_COMMAND), bundle);
    } else {
      onCancel(dialog);
    }
  }
  public interface ConfirmationDialogListener {
    void dispatchCommand(int command, Bundle args);
    void onConfirmationDialogDismissOrCancel(int command);
  }
}