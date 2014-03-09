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
import android.support.v4.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

public class ProgressDialogFragment extends DialogFragment {
  private static final String KEY_PROGRESS_STYLE = "progressStyle";
  private static final String KEY_MESSAGE = "message";
  private ProgressDialog dialog;

  public static ProgressDialogFragment newInstance(int message) {
    return newInstance(message,0);
  }
  public static ProgressDialogFragment newInstance(int message,int progressStyle) {
    ProgressDialogFragment f = new ProgressDialogFragment ();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_MESSAGE, message);
    bundle.putInt(KEY_PROGRESS_STYLE, progressStyle);
    f.setArguments(bundle);
    //f.setCancelable(false);
    return f;
  }
 
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) { 
    dialog = new ProgressDialog(getActivity());
    int message = getArguments().getInt(KEY_MESSAGE);
    int progressStyle = getArguments().getInt(KEY_PROGRESS_STYLE);
    if (message != 0) {
      dialog.setMessage(getString(message));
    } else {
      //unless setTitle is called now with non empty argument, calls after dialog is shown are ignored
      dialog.setTitle("...");
    }
    if (progressStyle != 0) {
      dialog.setProgressStyle(progressStyle);
    } else {
      dialog.setIndeterminate(true);
    }
    dialog.setProgress(0);
    return dialog;
  }
  public void setProgress(int progress) {
   dialog.setProgress(progress);
  }
  public void setMax(int max) {
   dialog.setMax(max);
  }
  public void setTitle(String title) {
   dialog.setTitle(title);
  }
}