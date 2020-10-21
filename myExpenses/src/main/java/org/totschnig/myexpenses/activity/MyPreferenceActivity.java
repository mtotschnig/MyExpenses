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

package org.totschnig.myexpenses.activity;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.fragment.SettingsFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.DistributionHelper;
import org.totschnig.myexpenses.util.PermissionHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import static org.totschnig.myexpenses.activity.ConstantsKt.CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP;
import static org.totschnig.myexpenses.preference.PrefKey.PERFORM_PROTECTION_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PLANNER_CALENDAR_ID;
import static org.totschnig.myexpenses.preference.PrefKey.UI_HOME_SCREEN_SHORTCUTS;

/**
 * Present references screen defined in Layout file
 *
 * @author Michael Totschnig
 */
public class MyPreferenceActivity extends ProtectedFragmentActivity implements
    ContribIFace, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

  public static final String KEY_OPEN_PREF_KEY = "openPrefKey";
  private String initialPrefToShow;
  private String TAG = SettingsFragment.class.getSimpleName();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);
    setupToolbar(true);
    if (savedInstanceState == null) {
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.replace(R.id.fragment_container, new SettingsFragment(), TAG);
      ft.commit();
    }
    initialPrefToShow = savedInstanceState == null ?
        getIntent().getStringExtra(KEY_OPEN_PREF_KEY) : null;

    //when a user no longer has access to auto backup we do not want him to believe that it works
    if (!ContribFeature.AUTO_BACKUP.hasAccess() && ContribFeature.AUTO_BACKUP.usagesLeft(prefHandler) < 1) {
      AUTO_BACKUP.putBoolean(false);
    }
  }

  private SettingsFragment getFragment() {
    return ((SettingsFragment) getSupportFragmentManager().findFragmentByTag(TAG));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    //currently no help menu
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home && getSupportFragmentManager().getBackStackEntryCount() > 0) {
      getSupportFragmentManager().popBackStack();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case R.id.FTP_DIALOG:
        return DialogUtils.sendWithFTPDialog(this);
      case R.id.MORE_INFO_DIALOG:
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        //noinspection InflateParams
        View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.more_info, null);
        ((TextView) view.findViewById(R.id.aboutVersionCode)).setText(DistributionHelper.getVersionInfo(this));
        TextView projectContainer = view.findViewById(R.id.project_container);
        projectContainer.setText(Utils.makeBulletList(this,
            Stream.of(Utils.getProjectDependencies(this))
                .map(project -> {
                  String name = project.get("name");
                  return String.format("%s, from %s, licenced under %s",
                      project.containsKey("extra_info") ?
                          String.format("%s (%s)", name, project.get("extra_info")) : name,
                      project.get("url"), project.get("licence"));
                }).collect(Collectors.toList()), R.drawable.ic_menu_forward));
        TextView additionalContainer = view.findViewById(R.id.additional_container);
        final List<CharSequence> lines = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.additional_credits)));
        lines.add(String.format("%s: %s", getString(R.string.translated_by), buildTranslationCredits()));
        additionalContainer.setText(Utils.makeBulletList(this,
            lines,
            R.drawable.ic_menu_forward));
        LinearLayout iconContainer = view.findViewById(R.id.additional_icons_container);
        final List<CharSequence> iconLines = Arrays.asList(getResources().getStringArray(R.array.additional_icon_credits));
        TypedArray ar = getResources().obtainTypedArray(R.array.additional_icon_credits_keys);
        int len = ar.length();
        final int height = UiUtils.dp2Px(32, getResources());
        final int drawablePadding = UiUtils.dp2Px(8, getResources());
        for (int i = 0; i < len; i++) {
          TextView textView = new TextView(this);
          textView.setGravity(Gravity.CENTER_VERTICAL);
          LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
          textView.setLayoutParams(layoutParams);
          textView.setCompoundDrawablePadding(drawablePadding);
          textView.setText(iconLines.get(i));
          UiUtils.setCompoundDrawablesCompatWithIntrinsicBounds(textView, ar.getResourceId(i, 0), 0, 0, 0);
          iconContainer.addView(textView);
        }
        ar.recycle();
        return builder.setTitle(R.string.pref_more_info_dialog_title)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .create();
    }
    return null;
  }

  private CharSequence buildTranslationCredits() {
    return Stream.of(Stream.of(getResources().getStringArray(R.array.pref_ui_language_values))
        .map(lang -> {
          final String[] parts = lang.split("-");
          return Pair.create(lang, getTranslatorsArrayResId(parts[0], parts.length == 2 ? parts[1].toLowerCase(Locale.ROOT) : null));
        })
        .filter(pair -> pair.second != 0)
        .map(pair -> Pair.create(pair.first, getResources().getStringArray(pair.second)))
        .flatMap(pair -> Stream.of(pair.second).map(name -> Pair.create(name, pair.first)))
        .collect(Collectors.groupingBy(pair -> pair.first, Collectors.mapping(pair -> pair.second, Collectors.toSet())))
        .entrySet())
        .sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
        .map(entry -> String.format("%s (%s)", entry.getKey(), Stream.of(entry.getValue()).collect(Collectors.joining(", "))))
        .collect(Collectors.joining(", "));
  }

  public int getTranslatorsArrayResId(String language, String country) {
    int result = 0;
    final String prefix = "translators_";
    if (!TextUtils.isEmpty(language)) {
      if (!TextUtils.isEmpty(country)) {
        result = getResources().getIdentifier(prefix + language + "_" + country,
            "array", getPackageName());
      }
      if (result == 0) {
        result = getResources().getIdentifier(prefix + language,
            "array", getPackageName());
      }
    }
    return result;
  }

  public void validateLicence() {
    startValidationTask(TaskExecutionFragment.TASK_VALIDATE_LICENCE, R.string.progress_validating_licence);
  }

  private void startValidationTask(int taskId, int progressResId) {
    startTaskExecution(taskId, new String[]{}, null, 0);
    showSnackbar(progressResId, Snackbar.LENGTH_INDEFINITE);
  }

  private Intent findDirPicker() {
    Intent intent = new Intent("com.estrongs.action.PICK_DIRECTORY ");
    intent.putExtra("com.estrongs.intent.extra.TITLE", "Select Directory");
    if (Utils.isIntentAvailable(this, intent)) {
      return intent;
    }
    return null;
  }

  @Override
  public void contribFeatureCalled(ContribFeature feature, Serializable tag) {
    if (feature == ContribFeature.CSV_IMPORT) {
      Intent i = new Intent(this, CsvImportActivity.class);
      startActivity(i);
    }
  }

  @Override
  public void contribFeatureNotCalled(ContribFeature feature) {

  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String permissions[], @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR:
        if (PermissionHelper.allGranted(grantResults)) {
          initialPrefToShow = PLANNER_CALENDAR_ID.getKey();
        }
    }
  }

  @Override
  protected void onResumeFragments() {
    super.onResumeFragments();
    if (initialPrefToShow != null) {
      getFragment().showPreference(initialPrefToShow);
      initialPrefToShow = null;
    }
  }

  @Override
  public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat,
                                         PreferenceScreen preferenceScreen) {
    final String key = preferenceScreen.getKey();
    if (key.equals(PERFORM_PROTECTION_SCREEN.getKey()) &&
        MyApplication.getInstance().isProtected()) {
      confirmCredentials(CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST, () -> startPerformProtectionScreen(), false);
      return true;
    }
    if (key.equals(UI_HOME_SCREEN_SHORTCUTS.getKey())) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        //TODO on O we will be able to pin the shortcuts
        showSnackbar(R.string.home_screen_shortcuts_nougate_info, Snackbar.LENGTH_LONG);
        return true;
      }
    }
    startPreferenceScreen(key);
    return true;
  }

  @Override
  public void onPostExecute(int taskId, Object o) {
    super.onPostExecute(taskId, o);
    if (taskId == TaskExecutionFragment.TASK_VALIDATE_LICENCE ||
        taskId == TaskExecutionFragment.TASK_REMOVE_LICENCE) {
      dismissSnackbar();
      if (o instanceof Result) {
        Result r = ((Result) o);
        showSnackbar(r.print(this), Snackbar.LENGTH_LONG);
        getFragment().setProtectionDependentsState();
        getFragment().configureContribPrefs();
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST) {
      if (resultCode == RESULT_OK) {
        startPerformProtectionScreen();
      }
    }
  }

  private void startPerformProtectionScreen() {
    startPreferenceScreen(PERFORM_PROTECTION_SCREEN.getKey());
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (super.dispatchCommand(command, tag)) {
      return true;
    }
    if (command == R.id.REMOVE_LICENCE_COMMAND) {
      startValidationTask(TaskExecutionFragment.TASK_REMOVE_LICENCE, R.string.progress_removing_licence);
      return true;
    }
    if (command == R.id.CHANGE_COMMAND) {
      getFragment().updateHomeCurrency((String) tag);
      return true;
    }
    return false;
  }

  private void startPreferenceScreen(String key) {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    SettingsFragment fragment = new SettingsFragment();
    Bundle args = new Bundle();
    args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key);
    fragment.setArguments(args);
    ft.replace(R.id.fragment_container, fragment, key);
    ft.addToBackStack(key);
    ft.commitAllowingStateLoss();
  }

}