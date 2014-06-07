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
  
  public static class Button implements Serializable {
    public Button(int label, int command, Serializable tag) {
      this.label = label;
      this.command = command;
      this.tag = tag;
    }
    public static final Button noButton() {
     return nullButton(android.R.string.no);
    }
    int label;
    int command;
    Serializable tag;
    public static final Button okButton() {
      return nullButton(android.R.string.ok);
    }
    private static Button nullButton(int label) {
      return new Button(label,R.id.NO_COMMAND,null);
    }
  }
  public static final MessageDialogFragment newInstance(
      int title, int message, Button positive, Button neutral, Button negative) {
    return newInstance(title, MyApplication.getInstance().getString(message),
        positive, neutral, negative);
  }
  public static final MessageDialogFragment newInstance(
      int title, CharSequence message, Button positive, Button neutral, Button negative) {
    MessageDialogFragment dialogFragment = new MessageDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt("title", title);
    bundle.putCharSequence("message", message);
    bundle.putSerializable("positive", positive);
    bundle.putSerializable("neutral", neutral);
    bundle.putSerializable("negative", negative);
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
    Button positive = (Button) bundle.getSerializable("positive");
    Button neutral = (Button) bundle.getSerializable("neutral");
    Button negative = (Button) bundle.getSerializable("negative");
    if (positive != null)
      builder.setPositiveButton(positive.label, this);
    if (neutral != null)
      builder.setNeutralButton(neutral.label, this);
    if (negative != null)
      builder.setNegativeButton(negative.label, this);
    return builder.create();
  }
  @Override
  public void onCancel (DialogInterface dialog) {
    if (getActivity()==null) {
      return;
    }
    ((MessageDialogListener) getActivity()).onMessageDialogDismissOrCancel();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity()==null) {
      return;
    }
    Bundle bundle = getArguments();
    Button clicked = null;
    switch(which) {
    case AlertDialog.BUTTON_POSITIVE:
      clicked = (Button) bundle.getSerializable("positive");
      break;
    case AlertDialog.BUTTON_NEUTRAL:
      clicked = (Button) bundle.getSerializable("neutral");
      break;
    case AlertDialog.BUTTON_NEGATIVE:
      clicked = (Button) bundle.getSerializable("negative");
      break;
    }
    if (clicked.command == R.id.NO_COMMAND)
      onCancel(dialog);
    else 
      ((MessageDialogListener) getActivity())
          .dispatchCommand(clicked.command, clicked.tag);
  }
  public interface MessageDialogListener {
    boolean dispatchCommand(int command, Object tag);
    void onMessageDialogDismissOrCancel();
  }
}