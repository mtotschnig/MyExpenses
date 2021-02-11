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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.totschnig.myexpenses.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

/**
 * This class presents a simple dialog asking user to confirm a message. Optionally the dialog can also
 * present a checkbox that allows user to provide some secondary decision. If the Bundle provided
 * in {@link #newInstance(Bundle)} provides an entry with key {@link #KEY_PREFKEY}, the value of the
 * checkbox will be stored in a preference with this key, and R.string.confirmation_dialog_dont_show_again
 * will be set as text for the checkbox. If the Bundle provides {@link #KEY_CHECKBOX_LABEL}, this will
 * be used as as resource identifier for the checkbox label. In that case, the calling activity
 * must implement {@link org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogCheckedListener}
 * and handle {@link #KEY_COMMAND_POSITIVE} in {@link org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogCheckedListener#onPositive(Bundle, boolean)}
 */
public class ConfirmationDialogFragment extends BaseDialogFragment implements OnClickListener {

  private CheckBox checkBox;

  public static final String KEY_TITLE = "title";
  public static final String KEY_TITLE_STRING = "titleString";
  public static final String KEY_MESSAGE = "message";
  public static final String KEY_COMMAND_POSITIVE = "positiveCommand";
  public static final String KEY_COMMAND_NEGATIVE = "negativeCommand";
  public static final String KEY_TAG_POSITIVE = "positiveTag";
  public static final String KEY_PREFKEY = "prefKey";
  public static final String KEY_CHECKBOX_LABEL = "checkboxLabel";
  public static final String KEY_POSITIVE_BUTTON_LABEL = "positiveButtonLabel";
  public static final String KEY_POSITIVE_BUTTON_CHECKED_LABEL = "positiveButtonCheckedLabel";
  public static final String KEY_NEGATIVE_BUTTON_LABEL = "negativeButtonLabel";

  public static ConfirmationDialogFragment newInstance(Bundle args) {
    ConfirmationDialogFragment dialogFragment = new ConfirmationDialogFragment();
    dialogFragment.setArguments(args);
    return dialogFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final Bundle bundle = getArguments();
    Activity ctx = getActivity();
    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(ctx);
    int title = bundle.getInt(KEY_TITLE, 0);
    if (title != 0) {
      builder.setTitle(title);
    } else {
      String titleString = bundle.getString(KEY_TITLE_STRING, null);
      if (titleString != null) {
        builder.setTitle(titleString);
      }
    }
    builder.setMessage(bundle.getCharSequence(KEY_MESSAGE));
    int checkboxLabel = bundle.getInt(KEY_CHECKBOX_LABEL, 0);
    if (bundle.getString(KEY_PREFKEY) != null ||
        checkboxLabel != 0) {
      //noinspection InflateParams
      View cb = LayoutInflater.from(builder.getContext()).inflate(R.layout.checkbox, null);
      checkBox = cb.findViewById(R.id.checkBox);
      checkBox.setText(
          checkboxLabel != 0 ? checkboxLabel :
              R.string.confirmation_dialog_dont_show_again);
      builder.setView(cb);
    }
    int positiveLabel = bundle.getInt(KEY_POSITIVE_BUTTON_LABEL);
    int negativeLabel = bundle.getInt(KEY_NEGATIVE_BUTTON_LABEL);
    int checkedLabel = bundle.getInt(KEY_POSITIVE_BUTTON_CHECKED_LABEL);
    if (checkedLabel != 0) {
      checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
          ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setText(
              isChecked ? checkedLabel : positiveLabel));
    }
    builder.setPositiveButton(positiveLabel == 0 ? android.R.string.ok : positiveLabel, this);
    builder.setNegativeButton(negativeLabel == 0 ? android.R.string.cancel : negativeLabel, this);
    return builder.create();
  }

  @Override
  public void onCancel(@NonNull DialogInterface dialog) {
    ConfirmationDialogBaseListener ctx = (ConfirmationDialogBaseListener) getActivity();
    if (ctx != null) {
      ctx.onDismissOrCancel(getArguments());
    }
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    ConfirmationDialogBaseListener ctx = (ConfirmationDialogBaseListener) getActivity();
    if (ctx == null) {
      return;
    }
    Bundle bundle = getArguments();
    String prefKey = bundle.getString(KEY_PREFKEY);
    if (prefKey != null && checkBox.isChecked()) {
      prefHandler.putBoolean(prefKey, true);
    }
    if (which == AlertDialog.BUTTON_POSITIVE) {
      if (bundle.getInt(KEY_CHECKBOX_LABEL, 0) == 0) {
        ((ConfirmationDialogListener) ctx).onPositive(bundle);
      } else {
        ((ConfirmationDialogCheckedListener) ctx).onPositive(bundle, checkBox.isChecked());
      }
    } else {
      int negativeCommand = getArguments().getInt(KEY_COMMAND_NEGATIVE);
      if (negativeCommand != 0) {
        ctx.onNegative(bundle);
      } else {
        onCancel(dialog);
      }
    }
  }

  public interface ConfirmationDialogBaseListener {
    void onNegative(Bundle args);

    void onDismissOrCancel(Bundle args);
  }

  public interface ConfirmationDialogListener extends ConfirmationDialogBaseListener {
    void onPositive(Bundle args);

  }

  public interface ConfirmationDialogCheckedListener extends ConfirmationDialogBaseListener {
    void onPositive(Bundle args, boolean checked);
  }
}