package org.totschnig.myexpenses.fragment;

import static org.totschnig.myexpenses.activity.ConstantsKt.RESTORE_REQUEST;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.RESULT_RESTORE_OK;
import static org.totschnig.myexpenses.model.ContribFeature.CSV_IMPORT;
import static org.totschnig.myexpenses.preference.PrefKey.APP_DIR;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP_CLOUD;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_SWITCH;
import static org.totschnig.myexpenses.preference.PrefKey.CONTRIB_PURCHASE;
import static org.totschnig.myexpenses.preference.PrefKey.IMPORT_CSV;
import static org.totschnig.myexpenses.preference.PrefKey.LICENCE_EMAIL;
import static org.totschnig.myexpenses.preference.PrefKey.MORE_INFO_DIALOG;
import static org.totschnig.myexpenses.preference.PrefKey.NEW_LICENCE;
import static org.totschnig.myexpenses.preference.PrefKey.NEXT_REMINDER_RATE;
import static org.totschnig.myexpenses.preference.PrefKey.PERFORM_PROTECTION_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PERFORM_SHARE;
import static org.totschnig.myexpenses.preference.PrefKey.PERSONALIZED_AD_CONSENT;
import static org.totschnig.myexpenses.preference.PrefKey.PLANNER_CALENDAR_ID;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_DEVICE_LOCK_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.PURGE_BACKUP;
import static org.totschnig.myexpenses.preference.PrefKey.RATE;
import static org.totschnig.myexpenses.preference.PrefKey.RESTORE;
import static org.totschnig.myexpenses.preference.PrefKey.ROOT_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.SECURITY_QUESTION;
import static org.totschnig.myexpenses.preference.PrefKey.SEND_FEEDBACK;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;
import static org.totschnig.myexpenses.util.TextUtils.concatResStrings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.preference.CalendarListPreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.preference.FontSizeDialogFragmentCompat;
import org.totschnig.myexpenses.preference.FontSizeDialogPreference;
import org.totschnig.myexpenses.preference.LegacyPasswordPreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.preference.PopupMenuPreference;
import org.totschnig.myexpenses.preference.SecurityQuestionDialogFragmentCompat;
import org.totschnig.myexpenses.preference.SimplePasswordDialogFragmentCompat;
import org.totschnig.myexpenses.preference.SimplePasswordPreference;
import org.totschnig.myexpenses.preference.TimePreference;
import org.totschnig.myexpenses.preference.TimePreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;
import org.totschnig.myexpenses.util.licence.Package;
import org.totschnig.myexpenses.util.licence.ProfessionalPackage;

import java.util.Locale;

import javax.inject.Inject;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;

public class SettingsFragment extends BaseSettingsFragment implements
    SimpleInputDialog.OnDialogResultListener {

  private static final String DIALOG_VALIDATE_LICENCE = "validateLicence";
  private static final String DIALOG_MANAGE_LICENCE = "manageLicence";
  private static final String KEY_EMAIL = "email";
  private static final String KEY_KEY = "key";
  private long pickFolderRequestStart;
  private static final int PICK_FOLDER_REQUEST = 2;
  private static final int CONTRIB_PURCHASE_REQUEST = 3;

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
    boolean hasMasterSwitch = handleScreenWithMasterSwitch(PERFORM_SHARE) ||
            handleScreenWithMasterSwitch(AUTO_BACKUP) ||
            handleScreenWithMasterSwitch(AUTO_FILL_SWITCH) ||
            handleScreenWithMasterSwitch(PURGE_BACKUP);
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
  public boolean onPreferenceClick(@NonNull Preference preference) {
    trackPreferenceClick(preference);
    if (matches(preference, CONTRIB_PURCHASE)) {
      if (licenceHandler.isUpgradeable()) {
        Intent i = ContribInfoDialogActivity.getIntentFor(getPreferenceActivity(), null);
        if (DistributionHelper.isGithub()) {
          startActivityForResult(i, CONTRIB_PURCHASE_REQUEST);
        } else {
          startActivity(i);
        }
      } else {
        ProfessionalPackage[] proPackagesForExtendOrSwitch = licenceHandler.getProPackagesForExtendOrSwitch();
        if (proPackagesForExtendOrSwitch != null) {
          if (proPackagesForExtendOrSwitch.length > 1) {
            ((PopupMenuPreference) preference).showPopupMenu(item -> {
              contribBuyDo(proPackagesForExtendOrSwitch[item.getItemId()], false);
              return true;
            }, Stream.of(proPackagesForExtendOrSwitch).map(licenceHandler::getExtendOrSwitchMessage).toArray(String[]::new));
          } else {
            //Currently we assume that if we have only one item, we switch
            contribBuyDo(proPackagesForExtendOrSwitch[0], true);
          }
        }
      }
      return true;
    }
    if (matches(preference, SEND_FEEDBACK)) {
      getPreferenceActivity().dispatchCommand(R.id.FEEDBACK_COMMAND, null);
      return true;
    }
    if (matches(preference, RATE)) {
      prefHandler.putLong(NEXT_REMINDER_RATE, -1);
      getPreferenceActivity().dispatchCommand(R.id.RATE_COMMAND, null);
      return true;
    }
    if (matches(preference, MORE_INFO_DIALOG)) {
      getPreferenceActivity().showDialog(R.id.MORE_INFO_DIALOG);
      return true;
    }
    if (matches(preference, RESTORE)) {
      startActivityForResult(preference.getIntent(), RESTORE_REQUEST);
      return true;
    }
    if (matches(preference, APP_DIR)) {
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
      try {
        pickFolderRequestStart = System.currentTimeMillis();
        startActivityForResult(intent, PICK_FOLDER_REQUEST);
      } catch (ActivityNotFoundException e) {
        reportException(e);
      }
      return true;
    }
    if (handleContrib(IMPORT_CSV, CSV_IMPORT, preference)) return true;
    if (matches(preference, NEW_LICENCE)) {
      if (licenceHandler.hasValidKey()) {
        SimpleDialog.build()
            .title(R.string.licence_key)
            .msg(getKeyInfo())
            .pos(R.string.button_validate)
            .neg(R.string.menu_remove)
            .show(this, DIALOG_MANAGE_LICENCE);
      } else {
        String licenceKey = prefHandler.getString(NEW_LICENCE, "");
        String licenceEmail = prefHandler.getString(LICENCE_EMAIL, "");
        SimpleFormDialog.build()
            .title(R.string.pref_enter_licence_title)
            .fields(
                Input.email(KEY_EMAIL).required().text(licenceEmail),
                Input.plain(KEY_KEY).required().hint(R.string.licence_key).text(licenceKey)
            )
            .pos(R.string.button_validate)
            .neut()
            .show(this, DIALOG_VALIDATE_LICENCE);
      }
      return true;
    }
    if (matches(preference, PERSONALIZED_AD_CONSENT)) {
      getPreferenceActivity().checkGdprConsent(true);
      return true;
    }
    return false;
  }

  private void contribBuyDo(Package selectedPackage, boolean shouldReplaceExisting) {
    startActivity(ContribInfoDialogActivity.getIntentFor(getContext(), selectedPackage, shouldReplaceExisting));
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
        long pickFolderRequestDuration = System.currentTimeMillis() - pickFolderRequestStart;
        if (pickFolderRequestDuration < 250) {
          reportException(new IllegalStateException(String.format(Locale.ROOT, "PICK_FOLDER_REQUEST returned after %d millis with request code %d",
              pickFolderRequestDuration, requestCode)));
        }
      }
    }
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (DIALOG_VALIDATE_LICENCE.equals(dialogTag)) {
      if (which == BUTTON_POSITIVE) {
        prefHandler.putString(NEW_LICENCE, extras.getString(KEY_KEY).trim());
        prefHandler.putString(LICENCE_EMAIL, extras.getString(KEY_EMAIL).trim());
        getPreferenceActivity().validateLicence();
      }
    } else if (DIALOG_MANAGE_LICENCE.equals(dialogTag)) {
      switch (which) {
        case BUTTON_POSITIVE:
          getPreferenceActivity().validateLicence();
          break;
        case BUTTON_NEGATIVE:
          Bundle b = new Bundle();
          b.putInt(ConfirmationDialogFragment.KEY_TITLE,
              R.string.dialog_title_information);
          b.putString(ConfirmationDialogFragment.KEY_MESSAGE, getString(R.string.licence_removal_information, 5));
          b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.menu_remove);
          b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.REMOVE_LICENCE_COMMAND);
          ConfirmationDialogFragment.newInstance(b)
              .show(getParentFragmentManager(), "REMOVE_LICENCE");
          break;
      }
    }
    return true;
  }

}
