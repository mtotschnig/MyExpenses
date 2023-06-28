package org.totschnig.myexpenses.fragment;

import static org.totschnig.myexpenses.activity.ConstantsKt.RESTORE_REQUEST;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.RESULT_RESTORE_OK;
import static org.totschnig.myexpenses.preference.PrefKey.APP_DIR;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP_CLOUD;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_SWITCH;
import static org.totschnig.myexpenses.preference.PrefKey.MANAGE_APP_DIR_FILES;
import static org.totschnig.myexpenses.preference.PrefKey.OPTIMIZE_PICTURE;
import static org.totschnig.myexpenses.preference.PrefKey.PERFORM_PROTECTION_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PERFORM_SHARE;
import static org.totschnig.myexpenses.preference.PrefKey.PLANNER_CALENDAR_ID;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_DEVICE_LOCK_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.PURGE_BACKUP;
import static org.totschnig.myexpenses.preference.PrefKey.ROOT_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.SECURITY_QUESTION;
import static org.totschnig.myexpenses.preference.PrefKey.UI_WEB;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;
import static org.totschnig.myexpenses.util.TextUtils.concatResStrings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.preference.CalendarListPreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.preference.FontSizeDialogFragmentCompat;
import org.totschnig.myexpenses.preference.FontSizeDialogPreference;
import org.totschnig.myexpenses.preference.LegacyPasswordPreferenceDialogFragmentCompat;
import androidx.preference.MultiSelectListPreferenceDialogFragment2;
import org.totschnig.myexpenses.preference.SecurityQuestionDialogFragmentCompat;
import org.totschnig.myexpenses.preference.SimplePasswordDialogFragmentCompat;
import org.totschnig.myexpenses.preference.SimplePasswordPreference;
import org.totschnig.myexpenses.preference.TimePreference;
import org.totschnig.myexpenses.preference.TimePreferenceDialogFragmentCompat;

import java.util.Locale;

public class SettingsFragment extends BaseSettingsFragment {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    final AppComponent appComponent = requireApplication().getAppComponent();
    appComponent.inject(this);
    super.onCreate(savedInstanceState);
    prefHandler.preparePreferenceFragment(this);
  }


  @Override
  public void onResume() {
    super.onResume();
    final MyPreferenceActivity activity = getPreferenceActivity();
    final ActionBar actionBar = activity.getSupportActionBar();
    PreferenceScreen screen = getPreferenceScreen();
    boolean isRoot = matches(screen, ROOT_SCREEN);
    CharSequence title = isRoot ?
        concatResStrings(activity, " ", R.string.app_name, R.string.menu_settings) :
        screen.getTitle();
    actionBar.setTitle(title);
    boolean hasMasterSwitch = handleScreenWithMasterSwitch(PERFORM_SHARE, true) ||
            handleScreenWithMasterSwitch(AUTO_BACKUP, true) ||
            handleScreenWithMasterSwitch(AUTO_FILL_SWITCH, true) ||
            handleScreenWithMasterSwitch(PURGE_BACKUP, true) ||
            handleScreenWithMasterSwitch(UI_WEB, false) ||
            handleScreenWithMasterSwitch(OPTIMIZE_PICTURE, true);
    if (!hasMasterSwitch) {
      actionBar.setCustomView(null);
    }
    if (isRoot) {
      requirePreference(PERFORM_PROTECTION_SCREEN).setSummary(getString(
          prefHandler.getBoolean(PROTECTION_LEGACY, false) ? R.string.pref_protection_password_title :
              prefHandler.getBoolean(PROTECTION_DEVICE_LOCK_SCREEN, false) ? R.string.pref_protection_device_lock_screen_title :
                  R.string.switch_off_text));
      configureContribPrefs();
      loadSyncAccountData();
    }
  }

  public void showPreference(String prefKey) {
    final Preference preference = findPreference(prefKey);
    if (preference != null) {
      onDisplayPreferenceDialog(preference);
    }
  }

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    DialogFragment fragment = null;
    String key = preference.getKey();
    if (matches(preference, PLANNER_CALENDAR_ID)) {
      if (CALENDAR.hasPermission(requireContext())) {
        fragment = CalendarListPreferenceDialogFragmentCompat.newInstance(key);
      } else {
        getPreferenceActivity().requestCalendarPermission();
        return;
      }
    } else if (preference instanceof FontSizeDialogPreference) {
      fragment = FontSizeDialogFragmentCompat.newInstance(key);
    } else if (preference instanceof TimePreference) {
      fragment = TimePreferenceDialogFragmentCompat.newInstance(key);
    } else if (matches(preference, PROTECTION_LEGACY)) {
      if (prefHandler.getBoolean(PROTECTION_DEVICE_LOCK_SCREEN, false)) {
        showOnlyOneProtectionWarning(false);
        return;
      } else {
        fragment = LegacyPasswordPreferenceDialogFragmentCompat.newInstance(key);
      }
    } else if (matches(preference, SECURITY_QUESTION)) {
      fragment = SecurityQuestionDialogFragmentCompat.newInstance(key);
    } else if (matches(preference, AUTO_BACKUP_CLOUD)) {
      if (((ListPreference) preference).getEntries().length == 1) {
        getPreferenceActivity().showSnackBar(R.string.no_sync_backends);
        return;
      }
    } else if (preference instanceof SimplePasswordPreference) {
      fragment = SimplePasswordDialogFragmentCompat.newInstance(key);
    } else if (matches(preference, MANAGE_APP_DIR_FILES)) {
      fragment = MultiSelectListPreferenceDialogFragment2.Companion.newInstance(key);
    }
    if (fragment != null) {
      fragment.setTargetFragment(this, 0);
      fragment.show(getParentFragmentManager(),
          "android.support.v7.preference.PreferenceFragment.DIALOG");
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode,
                               Intent intent) {
    if (requestCode == RESTORE_REQUEST && resultCode == RESULT_RESTORE_OK) {
      requireActivity().setResult(resultCode);
      requireActivity().finish();
    } else if (requestCode == PICK_FOLDER_REQUEST) {
      if (resultCode == Activity.RESULT_OK) {
        Uri dir = intent.getData();
        requireActivity().getContentResolver().takePersistableUriPermission(dir,
            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        prefHandler.putString(APP_DIR, dir.toString());
        loadAppDirSummary();
      } else {
        //we try to determine if we get here due to abnormal failure (observed on Xiaomi) of request, or if user canceled
        long pickFolderRequestDuration = System.currentTimeMillis() - getPickFolderRequestStart();
        if (pickFolderRequestDuration < 250) {
          reportException(new IllegalStateException(String.format(Locale.ROOT, "PICK_FOLDER_REQUEST returned after %d millis with request code %d",
              pickFolderRequestDuration, requestCode)));
        }
      }
    }
  }

}
