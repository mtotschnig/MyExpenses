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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.CommonCommands;
import org.totschnig.myexpenses.activity.MyExpenses;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

public class WelcomeDialogFragment extends CommitSafeDialogFragment
    implements DialogInterface.OnClickListener {
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
    CompoundButton themeSwitch = (CompoundButton) view.findViewById(R.id.TaType);
    if (Build.VERSION.SDK_INT>=14) {
      ((Switch) themeSwitch).setTextOn(getString(R.string.pref_ui_theme_light));
      ((Switch) themeSwitch).setTextOff(getString(R.string.pref_ui_theme_dark));
    } else {
      ((ToggleButton) themeSwitch).setTextOn(getString(R.string.pref_ui_theme_light));
      ((ToggleButton) themeSwitch).setTextOff(getString(R.string.pref_ui_theme_dark));
    }
    themeSwitch.setChecked(MyApplication.getThemeType().equals(MyApplication.ThemeType.light));
    themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        MyApplication.PrefKey.UI_THEME_KEY.putString(
            (isChecked ? MyApplication.ThemeType.light : MyApplication.ThemeType.dark).name());
        Intent intent = getActivity().getIntent();
        getActivity().finish();
        startActivity(intent);
      }
    });
    mDialog = new AlertDialog.Builder(ctx)
      .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.dialog_title_welcome))
      .setIcon(R.drawable.myexpenses)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
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

  public void showUnlockWelcome(int message) {
    Dialog dlg = getDialog();
    if (dlg != null) {
      TextView help_leading = (TextView) dlg.findViewById(R.id.help_leading);
      if (help_leading != null) {
        help_leading.setText(message);
      }
    }
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    int current_version = CommonCommands.getVersionNumber(getActivity());
    MyApplication.PrefKey.CURRENT_VERSION.putInt(current_version);
    MyApplication.PrefKey.FIRST_INSTALL_VERSION.putInt(current_version);
  }
}
