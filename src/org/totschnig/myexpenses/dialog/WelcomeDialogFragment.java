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
import org.totschnig.myexpenses.activity.MyExpenses;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class WelcomeDialogFragment extends CommitSafeDialogFragment {
  private static final String KEY_SETUP_COMPLETED = "taskCompleted";
  private AlertDialog mDialog;
  private ProgressBar mProgress;
  boolean mSetupCompleted = false;
  
  public static final WelcomeDialogFragment newInstance() {
    WelcomeDialogFragment f = new WelcomeDialogFragment();
    f.setCancelable(false);
    return f;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      mSetupCompleted = savedInstanceState.getBoolean(KEY_SETUP_COMPLETED,false);
    }
    setRetainInstance(true);
  }

  //http://stackoverflow.com/a/12434038/1199911
  @Override
  public void onDestroyView() {
    if (getDialog() != null && getRetainInstance())
        getDialog().setDismissMessage(null);
        super.onDestroyView();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!mSetupCompleted && ((MyExpenses) getActivity()).setupComplete) {
      mSetupCompleted = true;
    }
    if (mSetupCompleted) {
      mProgress.setVisibility(View.GONE);
    }
    else {
      Button b =  mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
      if (b != null) {
        b.setEnabled(false); 
      }
    }
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx  = (Activity) getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.welcome_dialog, null);
    mProgress = (ProgressBar) view.findViewById(R.id.progress);
    ((TextView) view.findViewById(R.id.help_intro))
      .setText("- " + TextUtils.join("\n- ", getResources().getStringArray(R.array.help_intro)));
    mDialog = new AlertDialog.Builder(ctx)
      .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.dialog_title_welcome))
      .setIcon(R.drawable.myexpenses)
      .setView(view)
      .setPositiveButton(android.R.string.ok,null)
      .create();
    return mDialog;
  }
  public void setSetupComplete() {
    mSetupCompleted = true;
    Button b =  mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
    if (b != null) {
      b.setEnabled(true); 
    }
    mProgress.setVisibility(View.GONE);
  }
  @Override
  public void onSaveInstanceState(Bundle outState) {
    // TODO Auto-generated method stub
    super.onSaveInstanceState(outState);
    outState.putBoolean(KEY_SETUP_COMPLETED, mSetupCompleted);
  }
}
