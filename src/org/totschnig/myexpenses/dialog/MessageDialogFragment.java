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

import java.io.Serializable;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class MessageDialogFragment extends DialogFragment implements OnClickListener {

  public static DialogFragment newInstance(
      int title,
      int message) {
    return newInstance(
        title,
        message,
        R.id.NO_COMMAND,
        null);
  }
  public static final MessageDialogFragment newInstance(
      int title,
      int message,
      int yesCommand,
      Serializable yesTag) {
    return newInstance(
        title,
        message,
        yesCommand,
        yesTag,
        R.id.NO_COMMAND,
        null);
  }
  public static final MessageDialogFragment newInstance(
      int title,
      int message,
      int yesCommand,
      Serializable yesTag,
      int noCommand,
      Serializable noTag) {
    return newInstance(
        title,
        MyApplication.getInstance().getString(message),
        yesCommand,
        yesTag,
        android.R.string.yes,
        noCommand,
        noTag,
        android.R.string.no);
  }
  public static final MessageDialogFragment newInstance(
      int title,
      CharSequence message,
      int yesCommand,
      Serializable yesTag) {
    return newInstance(
        title,
        message,
        yesCommand,
        yesTag,
        android.R.string.yes,
        android.R.string.no);
  }
  public static final MessageDialogFragment newInstance(
      int title,
      CharSequence message,
      int yesCommand,
      Serializable yesTag,
      int yesButton) {
    return newInstance(
        title,
        message,
        yesCommand,
        yesTag,
        yesButton,
        R.id.NO_COMMAND,
        null,
        android.R.string.no);
  }
  public static final MessageDialogFragment newInstance(
      int title,
      CharSequence message,
      int yesCommand,
      Serializable yesTag,
      int yesButton,
      int noButton) {
    return newInstance(
        title,
        message,
        yesCommand,
        yesTag,
        yesButton,
        R.id.NO_COMMAND,
        null,
        noButton);
  }
  public static final MessageDialogFragment newInstance(
      int title,
      CharSequence message,
      int yesCommand,
      Serializable yesTag,
      int yesButton,
      int noCommand,
      Serializable noTag,
      int noButton) {
    MessageDialogFragment dialogFragment = new MessageDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("title", title);
    bundle.putCharSequence("message", message);
    bundle.putInt("yesCommand", yesCommand);
    bundle.putInt("noCommand", noCommand);
    bundle.putSerializable("yesTag", yesTag);
    bundle.putSerializable("noTag", noTag);
    bundle.putInt("yesButton",yesButton);
    bundle.putInt("noButton",noButton);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Bundle bundle = getArguments();
    Activity ctx  = getActivity();
    Context wrappedCtx = DialogUtils.wrapContext2(ctx);
    AlertDialog.Builder builder = new AlertDialog.Builder(wrappedCtx)
        .setTitle(bundle.getInt("title"))
        .setMessage(bundle.getCharSequence("message"));
    if (bundle.getInt("yesCommand") != R.id.NO_COMMAND) {
      builder
          .setNegativeButton(bundle.getInt("noButton"),this)
          .setPositiveButton(bundle.getInt("yesButton"),this);
    } else {
      builder.setNeutralButton(bundle.getInt("yesButton"), this);
    }
    return builder.create();
  }
  public void onCancel (DialogInterface dialog) {
    Bundle bundle = getArguments();
    int noCommand = bundle.getInt("noCommand");
    if (noCommand != R.id.NO_COMMAND)
      ((MessageDialogListener) getActivity())
        .dispatchCommand(noCommand, bundle.getSerializable("noTag"));
    else
      ((MessageDialogListener) getActivity()).cancelDialog();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    Bundle bundle = getArguments();
    if (which == AlertDialog.BUTTON_POSITIVE)
      ((MessageDialogListener) getActivity())
          .dispatchCommand(bundle.getInt("yesCommand"), bundle.getSerializable("yesTag"));
    else {
      onCancel(dialog);
    }
  }
  public interface MessageDialogListener {
    boolean dispatchCommand(int command, Object tag);
    void cancelDialog();
  }
}