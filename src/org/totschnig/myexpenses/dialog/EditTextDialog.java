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

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class EditTextDialog extends CommitSafeDialogFragment implements OnEditorActionListener {

  public static final String KEY_RESULT = "result";
  public static final String KEY_DIALOG_TITLE = "dialogTitle";
  public static final String KEY_VALUE = "value";

  public interface EditTextDialogListener {
    void onFinishEditDialog(Bundle args);
    void onCancelEditDialog();
  }

  private EditText mEditText;

  public static final EditTextDialog newInstance(Bundle args) {
    EditTextDialog dialogFragment = new EditTextDialog();
    dialogFragment.setArguments(args);
    return dialogFragment;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    Bundle args = getArguments();
    mEditText = new EditText(getActivity());
    getDialog().setTitle(args.getString(KEY_DIALOG_TITLE));
    // Show soft keyboard automatically
    mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
    mEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
    mEditText.requestFocus();
    getDialog().getWindow().setSoftInputMode(
        LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    mEditText.setOnEditorActionListener(this);
    mEditText.setId(1);
    mEditText.setText(args.getString(KEY_VALUE));
    //input.setSingleLine();
    return mEditText;
  }

  @Override
  public void onCancel (DialogInterface dialog) {
    if (getActivity()==null) {
      return;
    }
    ((EditTextDialogListener) getActivity()).onCancelEditDialog();
  }

  @Override
  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
    if (EditorInfo.IME_ACTION_DONE == actionId ||
        (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN)) {
      // Return input text to activity
      EditTextDialogListener activity = (EditTextDialogListener) getActivity();
      if (activity != null) {
        Bundle args = getArguments();
        String result = mEditText.getText().toString();
        if (result.equals("")) {
          Toast.makeText(getActivity(),getString(R.string.no_title_given), Toast.LENGTH_LONG).show();
        } else {
          args.putString(KEY_RESULT, result);
          activity.onFinishEditDialog(args);
          this.dismiss();
          return true;
        }
      }
    }
    return false;
  }
}