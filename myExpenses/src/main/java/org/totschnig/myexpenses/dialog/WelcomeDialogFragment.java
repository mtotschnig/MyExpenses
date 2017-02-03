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
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.CommonCommands;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.preference.PrefKey;

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
    Activity ctx  = getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    //noinspection InflateParams
    View view = li.inflate(R.layout.welcome_dialog, null);
    ((TextView) view.findViewById(R.id.help_leading)).setText(getString(R.string.help_leading, BuildConfig.PLATTFORM));
    mProgress = (ProgressBar) view.findViewById(R.id.progress);
    //noinspection SetTextI18n
    ((TextView) view.findViewById(R.id.help_intro))
      .setText("- " + TextUtils.join("\n- ", getResources().getStringArray(R.array.help_intro)));
    SwitchCompat themeSwitch = (SwitchCompat) view.findViewById(R.id.TaType);
    boolean isLight = MyApplication.getThemeType().equals(MyApplication.ThemeType.light);
    themeSwitch.setChecked(isLight);
    setContentDescriptonToThemeSwitch(themeSwitch, isLight);
    themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setContentDescriptonToThemeSwitch(buttonView, isChecked);
        PrefKey.UI_THEME_KEY.putString(
            (isChecked ? MyApplication.ThemeType.light : MyApplication.ThemeType.dark).name());
        Intent intent = getActivity().getIntent();
        getActivity().finish();
        startActivity(intent);
      }
    });
    mDialog = new AlertDialog.Builder(ctx)
      .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.dialog_title_welcome))
      .setIcon(R.mipmap.ic_myexpenses)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .create();
    return mDialog;
  }

  private void setContentDescriptonToThemeSwitch(View themeSwitch, boolean isLight) {
    themeSwitch.setContentDescription(getString(
        isLight ? R.string.pref_ui_theme_light : R.string.pref_ui_theme_dark));
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
    PrefKey.CURRENT_VERSION.putInt(current_version);
    PrefKey.FIRST_INSTALL_VERSION.putInt(current_version);
  }
}
