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

public class ConfirmationDialogFragment extends DialogFragment {
  
  public static final ConfirmationDialogFragment newInstance(
      int title, int message,int command, Bundle commandArgs, String prefKey) {
    return newInstance(title, MyApplication.getInstance().getString(message),command,commandArgs,prefKey);
  }
  public static final ConfirmationDialogFragment newInstance(
      int title, CharSequence message, int command, Bundle commandArgs, String prefKey) {
    Bundle bundle = new Bundle();
    bundle.putInt("title", title);
    bundle.putCharSequence("message", message);
    bundle.putInt("command",command);
    bundle.putParcelable("commandArgs", commandArgs);
    bundle.putString("prefKey", prefKey);
    ConfirmationDialogFragment dialogFragment = new ConfirmationDialogFragment();
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Bundle bundle = getArguments();
    Activity ctx  = getActivity();
    Context wrappedCtx = DialogUtils.wrapContext2(ctx);
    View cb = LayoutInflater.from(wrappedCtx).inflate(R.layout.checkbox, null);
    final CheckBox dontShowAgain = (CheckBox) cb.findViewById(R.id.skip);
    dontShowAgain.setText(R.string.confirmation_dialog_dont_show_again);
    AlertDialog.Builder builder = new AlertDialog.Builder(wrappedCtx)
        .setTitle(bundle.getInt("title"))
        .setView(cb)
        .setMessage(bundle.getCharSequence("message"));
    builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
      
      @Override
      public void onClick(DialogInterface dialog, int which) {
        if (dontShowAgain.isChecked()) {
          SharedPreferencesCompat.apply(
            MyApplication.getInstance().getSettings().edit()
            .putBoolean(bundle.getString("prefKey"), true));
        }
        ((ConfirmationDialogListener) getActivity())
        .dispatchCommand(bundle.getInt("command"), (Bundle) bundle.getParcelable("commandArgs"));
      }
    });
    builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
      
      @Override
      public void onClick(DialogInterface dialog, int which) {
        onCancel(dialog);
      }
    });
    return builder.create();
  }
  @Override
  public void onCancel (DialogInterface dialog) {
      ((ConfirmationDialogListener) getActivity()).onMessageDialogDismissOrCancel();
  }
  public interface ConfirmationDialogListener {
    boolean dispatchCommand(int command, Bundle args);
    void onMessageDialogDismissOrCancel();
  }
}