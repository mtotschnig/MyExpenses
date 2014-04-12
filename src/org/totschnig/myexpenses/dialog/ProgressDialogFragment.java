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

import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.ui.ScrollableProgressDialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class ProgressDialogFragment extends DialogFragment {
  private static final String KEY_PROGRESS_STYLE = "progressStyle";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_WITH_BUTTON = "withButton";
  private static final String KEY_TITLE = "title";
  private ProgressDialog mDialog;
  boolean mTaskCompleted = false;
  int progress = 0, max;
  String title, message;
 

  /**
   * @param message if different from 0 a resource string identifier displayed as the dialogs's message
   * @return the dialog fragment
   */
  public static ProgressDialogFragment newInstance(int message) {
    return newInstance(0,message,0, false);
  }
  /**
   * @param message if different from 0 a resource string identifier displayed as the dialogs's message
   * @param progressStyle {@link ProgressDialog#STYLE_SPINNER} or {@link ProgressDialog#STYLE_HORIZONTAL}
   * @param withButton if true dialog is rendered with an OK button that is initially disabled  
   * @return the dialog fragment
   */
  public static ProgressDialogFragment newInstance(int title, int message,int progressStyle, boolean withButton) {
    ProgressDialogFragment f = new ProgressDialogFragment ();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_MESSAGE, message);
    bundle.putInt(KEY_TITLE, title);
    bundle.putInt(KEY_PROGRESS_STYLE, progressStyle);
    bundle.putBoolean(KEY_WITH_BUTTON, withButton);
    f.setArguments(bundle);
    f.setCancelable(false);
    return f;
  }
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
  }
  @Override
  public void onResume() {
    super.onResume();
    mDialog.setProgress(progress);
    mDialog.setMax(max);
    mDialog.setTitle(title);
    mDialog.setMessage(message);
    if (mTaskCompleted)
      mDialog.setIndeterminateDrawable(null);
  }
  //http://stackoverflow.com/a/12434038/1199911
  @Override
  public void onDestroyView() {
      if (getDialog() != null && getRetainInstance())
          getDialog().setDismissMessage(null);
          super.onDestroyView();
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    int progressStyle = getArguments().getInt(KEY_PROGRESS_STYLE);
    int messageResId = getArguments().getInt(KEY_MESSAGE);
    mDialog = (progressStyle == ScrollableProgressDialog.STYLE_SPINNER) ?
        new ScrollableProgressDialog(getActivity()) :
        new ProgressDialog(getActivity());
    boolean withButton = getArguments().getBoolean(KEY_WITH_BUTTON);
      int titleResId = getArguments().getInt(KEY_TITLE);
      if (messageResId != 0) {
        //message might have been set through setmessage
        if (message == null) {
          message = getString(messageResId);
          mDialog.setMessage(message);
        }
      } else {
        if (titleResId != 0)  {
          if (title == null)
            title = getString(titleResId);
        } else {
          //unless setTitle is called now with non empty argument, calls after dialog is shown are ignored
          if (title == null)
            title = "...";
        }
        mDialog.setTitle(title);
      }
    if (progressStyle != ScrollableProgressDialog.STYLE_SPINNER) {
      mDialog.setProgressStyle(progressStyle);
    } else {
      mDialog.setIndeterminate(true);
    }
    if (withButton) {
      mDialog.setButton(DialogInterface.BUTTON_NEUTRAL,getString(android.R.string.ok),
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              ((MessageDialogListener) getActivity()).onMessageDialogDismissOrCancel();
            }
      });
    }
    return mDialog;
  }
  public void setProgress(int progress) {
    this.progress = progress;
    mDialog.setProgress(progress);
  }
  public void setMax(int max) {
    this.max = max;
    mDialog.setMax(max);
  }
  public void setTitle(String title) {
    this.title = title;
    mDialog.setTitle(title);
  }
  public void setMessage(String message) {
    this.message = message;
    mDialog.setMessage(message);
  }
  public void onTaskCompleted() {
    mTaskCompleted = true;
    mDialog.setIndeterminateDrawable(null);
    mDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(true);
  }
  @Override
  public void onCancel (DialogInterface dialog) {
      ((MessageDialogListener) getActivity()).onMessageDialogDismissOrCancel();
  }
}