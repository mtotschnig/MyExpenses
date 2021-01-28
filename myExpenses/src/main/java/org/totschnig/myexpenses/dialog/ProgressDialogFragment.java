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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import org.totschnig.myexpenses.ui.ScrollableProgressDialog;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import androidx.annotation.NonNull;

import static android.app.ProgressDialog.STYLE_SPINNER;

@Deprecated
public class ProgressDialogFragment extends BaseDialogFragment {
  private static final String KEY_PROGRESS_STYLE = "progressStyle";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_WITH_BUTTON = "withButton";
  private static final String KEY_TITLE = "title";
  private static final String KEY_TASK_COMPLETED = "taskCompleted";
  private static final String KEY_PROGRESS = "progress";
  private static final String KEY_MAX = "max";
  private AlertDialog mDialog;
  private boolean mTaskCompleted = false;
  private int progress = 0, max = 0;
  private String title, message;
  private final int dialogButton = DialogInterface.BUTTON_POSITIVE;


  /**
   * @param message if different from 0 a resource string identifier displayed as the dialogs's message
   * @return the dialog fragment
   */
  @Deprecated
  public static ProgressDialogFragment newInstance(String message) {
    return newInstance(message, false);
  }

  @Deprecated
  public static ProgressDialogFragment newInstance(String message, boolean withButton) {
    return newInstance(null, message, STYLE_SPINNER, withButton);
  }

  /**
   * @param message       the dialogs's message
   * @param progressStyle {@link ProgressDialog#STYLE_SPINNER} or {@link ProgressDialog#STYLE_HORIZONTAL}
   * @param withButton    if true dialog is rendered with an OK button that is initially disabled
   * @return the dialog fragment
   */
  @Deprecated
  public static ProgressDialogFragment newInstance(String title, String message, int progressStyle, boolean withButton) {
    ProgressDialogFragment f = new ProgressDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString(KEY_MESSAGE, message);
    bundle.putString(KEY_TITLE, title);
    bundle.putInt(KEY_PROGRESS_STYLE, progressStyle);
    bundle.putBoolean(KEY_WITH_BUTTON, withButton);
    f.setArguments(bundle);
    f.setCancelable(false);
    return f;
  }

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      mTaskCompleted = savedInstanceState.getBoolean(KEY_TASK_COMPLETED, false);
      message = savedInstanceState.getString(KEY_MESSAGE);
      title = savedInstanceState.getString(KEY_TITLE);
      progress = savedInstanceState.getInt(KEY_PROGRESS);
      max = savedInstanceState.getInt(KEY_MAX);
    }
    setRetainInstance(true);
  }

  @Override
  public void onResume() {
    super.onResume();
    setProgress(progress);
    setMax(max);
    mDialog.setTitle(title);
    if (!TextUtils.isEmpty(message)) {
      mDialog.setMessage(message);
    }
    if (mTaskCompleted) {
      unsetIndeterminateDrawable();
    } else {
      Button b = mDialog.getButton(dialogButton);
      if (b != null) {
        b.setEnabled(false);
      }
    }
  }

  //http://stackoverflow.com/a/12434038/1199911
  @Override
  public void onDestroyView() {
    if (getDialog() != null && getRetainInstance())
      getDialog().setDismissMessage(null);
    super.onDestroyView();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    int progressStyle = getArguments().getInt(KEY_PROGRESS_STYLE);
    String messageFromArguments = getArguments().getString(KEY_MESSAGE);
    String titleFromArguments = getArguments().getString(KEY_TITLE);
    if (progressStyle == STYLE_SPINNER) {
      mDialog = new ScrollableProgressDialog(getActivity());
    } else {
      ProgressDialog progressDialog = new ProgressDialog(getActivity());
      progressDialog.setProgressStyle(progressStyle);
      mDialog = progressDialog;
    }
    boolean withButton = getArguments().getBoolean(KEY_WITH_BUTTON);
    if (messageFromArguments != null) {
      //message might have been set through setMessage
      if (message == null) {
        message = messageFromArguments + " …";
        mDialog.setMessage(message);
      }
    } else {
      if (titleFromArguments != null) {
        if (title == null)
          title = titleFromArguments;
      } else {
        //unless setTitle is called now with non empty argument, calls after dialog is shown are ignored
        if (title == null)
          title = "...";
      }
      mDialog.setTitle(title);
    }
    if (withButton) {
      mDialog.setButton(dialogButton, getString(android.R.string.ok),
          (dialog, which) -> {
            if (getActivity() == null) {
              return;
            }
            ((ProgressDialogListener) getActivity()).onProgressDialogDismiss();
          });
    }
    return mDialog;
  }

  public void setProgress(int progress) {
    this.progress = progress;
    if (mDialog instanceof ProgressDialog) {
      ((ProgressDialog) mDialog).setProgress(progress);
    }
  }

  public void setMax(int max) {
    this.max = max;
    if (mDialog instanceof ProgressDialog) {
      ((ProgressDialog) mDialog).setMax(max);
    }
  }

  public int getMax() {
    return max;
  }

  public void setTitle(String title) {
    this.title = title;
    mDialog.setTitle(title);
  }

  public void appendToMessage(@NonNull String newMessage) {
    if (TextUtils.isEmpty(this.message)) {
      this.message = newMessage;
    } else {
      this.message += "\n" + newMessage;
    }
    mDialog.setMessage(this.message);
  }

  public void onTaskCompleted() {
    mTaskCompleted = true;
    try {
      unsetIndeterminateDrawable();
    } catch (NullPointerException e) {
      //seen on samsung SM-G900F
      CrashHandler.report(e);
    }
    mDialog.getButton(dialogButton).setEnabled(true);
  }

  private void unsetIndeterminateDrawable() {
    if (mDialog instanceof ProgressDialog) {
      ((ProgressDialog) mDialog).setIndeterminateDrawable(null);
    } else {
      ((ScrollableProgressDialog) mDialog).unsetIndeterminateDrawable();
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(KEY_TASK_COMPLETED, mTaskCompleted);
    outState.putString(KEY_TITLE, title);
    outState.putString(KEY_MESSAGE, message);
    outState.putInt(KEY_PROGRESS, progress);
    outState.putInt(KEY_MAX, max);
  }

  public interface ProgressDialogListener {
    void onProgressDialogDismiss();
  }
}