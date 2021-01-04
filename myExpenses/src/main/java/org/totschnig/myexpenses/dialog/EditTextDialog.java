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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import org.totschnig.myexpenses.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class EditTextDialog extends BaseDialogFragment implements OnEditorActionListener {

  public static final String KEY_RESULT = "result";
  public static final String KEY_DIALOG_TITLE = "dialogTitle";
  public static final String KEY_VALUE = "value";
  public static final String KEY_REQUEST_CODE = "requestCode";
  public static final String KEY_INPUT_TYPE = "inputType";
  public static final String KEY_MAX_LENGTH = "maxLenght";

  public interface EditTextDialogListener {
    void onFinishEditDialog(Bundle args);

    void onCancelEditDialog();
  }

  private EditText mEditText;

  public static EditTextDialog newInstance(Bundle args) {
    EditTextDialog dialogFragment = new EditTextDialog();
    dialogFragment.setArguments(args);
    return dialogFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = initBuilderWithView(R.layout.edit_text_dialog);
    mEditText = ((EditText) dialogView.findViewById(R.id.EditTextDialogInput));
    Bundle args = getArguments();
    mEditText.setInputType(args.getInt(KEY_INPUT_TYPE, InputType.TYPE_CLASS_TEXT));
    mEditText.setOnEditorActionListener(this);

    mEditText.setText(args.getString(KEY_VALUE));
    int maxLength = args.getInt(KEY_MAX_LENGTH);
    if (maxLength != 0) {
      mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
    }
    AlertDialog dialog = builder.setTitle(args.getString(KEY_DIALOG_TITLE))
        .create();
    dialog.getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    return dialog;
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    if (getActivity() == null) {
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
          showSnackbar(R.string.no_title_given);
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