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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

import java.io.Serializable;

public class MessageDialogFragment extends CommitSafeDialogFragment implements OnClickListener {

  private static final String KEY_TITLE = "title";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_POSITIVE = "positive";
  private static final String KEY_NEUTRAL = "neutral";
  private static final String KEY_NEGATIVE = "negative";

  public static class Button implements Serializable {
    int label;
    int command;
    Serializable tag;

    public Button(int label, int command, Serializable tag) {
      this.label = label;
      this.command = command;
      this.tag = tag;
    }

    public static final Button noButton() {
     return nullButton(android.R.string.no);
    }

    public static final Button okButton() {
      return nullButton(android.R.string.ok);
    }

    public static Button nullButton(int label) {
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
    bundle.putInt(KEY_TITLE, title);
    bundle.putCharSequence(KEY_MESSAGE, message);
    bundle.putSerializable(KEY_POSITIVE, positive);
    bundle.putSerializable(KEY_NEUTRAL, neutral);
    bundle.putSerializable(KEY_NEGATIVE, negative);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  
  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Bundle bundle = getArguments();
    Activity ctx  = getActivity();
    AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
        .setMessage(bundle.getCharSequence(KEY_MESSAGE));
    int title = bundle.getInt(KEY_TITLE);
    if (title != 0) {
      builder.setTitle(title);
    }
    Button positive = (Button) bundle.getSerializable(KEY_POSITIVE);
    Button neutral = (Button) bundle.getSerializable(KEY_NEUTRAL);
    Button negative = (Button) bundle.getSerializable(KEY_NEGATIVE);
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
      clicked = (Button) bundle.getSerializable(KEY_POSITIVE);
      break;
    case AlertDialog.BUTTON_NEUTRAL:
      clicked = (Button) bundle.getSerializable(KEY_NEUTRAL);
      break;
    case AlertDialog.BUTTON_NEGATIVE:
      clicked = (Button) bundle.getSerializable(KEY_NEGATIVE);
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