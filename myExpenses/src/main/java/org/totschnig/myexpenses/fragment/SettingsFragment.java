package org.totschnig.myexpenses.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.annimon.stream.Stream;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.activity.FolderBrowser;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.feature.Feature;
import org.totschnig.myexpenses.model.ContribFeature;
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
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.ShareUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;
import org.totschnig.myexpenses.util.io.NetworkUtilsKt;
import org.totschnig.myexpenses.util.licence.Package;
import org.totschnig.myexpenses.util.licence.ProfessionalPackage;

import java.net.URI;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;

import static org.totschnig.myexpenses.activity.ConstantsKt.RESTORE_REQUEST;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.RESULT_RESTORE_OK;
import static org.totschnig.myexpenses.model.ContribFeature.CSV_IMPORT;
import static org.totschnig.myexpenses.preference.PrefKey.APP_DIR;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP_CLOUD;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_SWITCH;
import static org.totschnig.myexpenses.preference.PrefKey.CONTRIB_PURCHASE;
import static org.totschnig.myexpenses.preference.PrefKey.CRASHREPORT_ENABLED;
import static org.totschnig.myexpenses.preference.PrefKey.CRASHREPORT_USEREMAIL;
import static org.totschnig.myexpenses.preference.PrefKey.CUSTOM_DECIMAL_FORMAT;
import static org.totschnig.myexpenses.preference.PrefKey.EXCHANGE_RATE_PROVIDER;
import static org.totschnig.myexpenses.preference.PrefKey.HOME_CURRENCY;
import static org.totschnig.myexpenses.preference.PrefKey.IMPORT_CSV;
import static org.totschnig.myexpenses.preference.PrefKey.LICENCE_EMAIL;
import static org.totschnig.myexpenses.preference.PrefKey.MORE_INFO_DIALOG;
import static org.totschnig.myexpenses.preference.PrefKey.NEW_LICENCE;
import static org.totschnig.myexpenses.preference.PrefKey.NEXT_REMINDER_RATE;
import static org.totschnig.myexpenses.preference.PrefKey.OCR_DATE_FORMATS;
import static org.totschnig.myexpenses.preference.PrefKey.OCR_TIME_FORMATS;
import static org.totschnig.myexpenses.preference.PrefKey.PERFORM_PROTECTION_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PERFORM_SHARE;
import static org.totschnig.myexpenses.preference.PrefKey.PERSONALIZED_AD_CONSENT;
import static org.totschnig.myexpenses.preference.PrefKey.PLANNER_CALENDAR_ID;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_DEVICE_LOCK_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.RATE;
import static org.totschnig.myexpenses.preference.PrefKey.RESTORE;
import static org.totschnig.myexpenses.preference.PrefKey.RESTORE_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.ROOT_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.SECURITY_QUESTION;
import static org.totschnig.myexpenses.preference.PrefKey.SEND_FEEDBACK;
import static org.totschnig.myexpenses.preference.PrefKey.SHARE_TARGET;
import static org.totschnig.myexpenses.preference.PrefKey.UI_WEB;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;
import static org.totschnig.myexpenses.util.TextUtils.concatResStrings;

public class SettingsFragment extends BaseSettingsFragment implements
    SimpleInputDialog.OnDialogResultListener {

  private static final String DIALOG_VALIDATE_LICENCE = "validateLicence";
  private static final String DIALOG_MANAGE_LICENCE = "manageLicence";
  private static final String KEY_EMAIL = "email";
  private static final String KEY_KEY = "key";
  private long pickFolderRequestStart;
  private static final int PICK_FOLDER_REQUEST = 2;
  private static final int CONTRIB_PURCHASE_REQUEST = 3;
  private static final int PICK_FOLDER_REQUEST_LEGACY = 4;

  @Inject
  CrashHandler crashHandler;
  @Inject
  CurrencyFormatter currencyFormatter;

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
    final MyPreferenceActivity activity = activity();
    final ActionBar actionBar = activity.getSupportActionBar();
    PreferenceScreen screen = getPreferenceScreen();
    boolean isRoot = matches(screen, ROOT_SCREEN);
    CharSequence title = isRoot ?
        concatResStrings(activity, " ", R.string.app_name, R.string.menu_settings) :
        screen.getTitle();
    actionBar.setTitle(title);
    boolean hasMasterSwitch = handleScreenWithMasterSwitch(PERFORM_SHARE);
    hasMasterSwitch = handleScreenWithMasterSwitch(AUTO_BACKUP) || hasMasterSwitch;
    hasMasterSwitch = handleScreenWithMasterSwitch(AUTO_FILL_SWITCH) || hasMasterSwitch;
    if (!hasMasterSwitch) {
      actionBar.setCustomView(null);
    }
    if (isRoot) {
      requirePreference(PERFORM_PROTECTION_SCREEN).setSummary(getString(
          prefHandler.getBoolean(PROTECTION_LEGACY, false) ? R.string.pref_protection_password_title :
              prefHandler.getBoolean(PROTECTION_DEVICE_LOCK_SCREEN, false) ? R.string.pref_protection_device_lock_screen_title :
                  R.string.switch_off_text));
      Preference preference = requirePreference(PLANNER_CALENDAR_ID);
      if (activity.isCalendarPermissionPermanentlyDeclined()) {
        preference.setSummary(Utils.getTextWithAppName(getContext(),
            R.string.calendar_permission_required));
      } else {
        preference.setSummary(R.string.pref_planning_calendar_summary);
      }
      configureContribPrefs();
      loadSyncAccountData();
    }
  }

  public void showPreference(String prefKey) {
    final Preference preference = findPreference(prefKey);
    if (preference != null) {
      //noinspection RestrictedApi
      preference.performClick();
    }
  }

  @Override
  public boolean onPreferenceChange(Preference pref, Object value) {
    if (matches(pref, HOME_CURRENCY)) {
      if (!value.equals(prefHandler.getString(HOME_CURRENCY, null))) {
        MessageDialogFragment.newInstance(getString(R.string.dialog_title_information),
            concatResStrings(getContext(), " ", R.string.home_currency_change_warning, R.string.continue_confirmation),
            new MessageDialogFragment.Button(android.R.string.ok, R.id.CHANGE_COMMAND, ((String) value)),
            null, MessageDialogFragment.noButton()).show(getParentFragmentManager(), "CONFIRM");
      }
      return false;
    }
    if (matches(pref, SHARE_TARGET)) {
      String target = (String) value;
      URI uri;
      if (!target.equals("")) {
        uri = ShareUtils.parseUri(target);
        if (uri == null) {
          activity().showSnackbar(getString(R.string.ftp_uri_malformed, target));
          return false;
        }
        String scheme = uri.getScheme();
        if (!(scheme.equals("ftp") || scheme.equals("mailto"))) {
          activity().showSnackbar(getString(R.string.share_scheme_not_supported, scheme));
          return false;
        }
        Intent intent;
        if (scheme.equals("ftp")) {
          intent = new Intent(Intent.ACTION_SENDTO);
          intent.setData(android.net.Uri.parse(target));
          if (!Utils.isIntentAvailable(requireActivity(), intent)) {
            getActivity().showDialog(R.id.FTP_DIALOG);
          }
        }
      }
    } else if (matches(pref, CUSTOM_DECIMAL_FORMAT)) {
      currencyFormatter.invalidateAll(requireContext().getContentResolver());
    } else if (matches(pref, EXCHANGE_RATE_PROVIDER)) {
      configureOpenExchangeRatesPreference((String) value);
    } else if (matches(pref, CRASHREPORT_USEREMAIL)) {
      crashHandler.setUserEmail((String) value);
    } else if (matches(pref, CRASHREPORT_ENABLED)) {
      activity().showSnackbar(R.string.app_restart_required);
    } else if (matches(pref, OCR_DATE_FORMATS)) {
      if (!TextUtils.isEmpty((String) value)) {
        try {
          for (String line : kotlin.text.StringsKt.lines(((String) value))) {
            LocalDate.now().format(DateTimeFormatter.ofPattern(line));
          }
        } catch (Exception e) {
          activity().showSnackbar(R.string.date_format_illegal);
          return false;
        }
      }
    } else if (matches(pref, OCR_TIME_FORMATS)) {
      if (!TextUtils.isEmpty((String) value)) {
        try {
          for (String line : kotlin.text.StringsKt.lines(((String) value))) {
            LocalTime.now().format(DateTimeFormatter.ofPattern(line));
          }
        } catch (Exception e) {
          activity().showSnackbar(R.string.date_format_illegal);
          return false;
        }
      }
    } else if (matches(pref, PROTECTION_DEVICE_LOCK_SCREEN)) {
      if (Utils.hasApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
        if (((Boolean) value)) {
          if (!((KeyguardManager) requireContext().getSystemService(Context.KEYGUARD_SERVICE)).isKeyguardSecure()) {
            activity().showDeviceLockScreenWarning();
            return false;
          } else if (prefHandler.getBoolean(PROTECTION_LEGACY, false)) {
            showOnlyOneProtectionWarning(true);
            return false;
          }
        }
      }
      return true;
    } else if (matches(pref, UI_WEB)) {
      if ((Boolean) value) {
        if (!NetworkUtilsKt.isNetworkConnected(requireContext())) {
          activity().showSnackbar(R.string.no_network);
          return false;
        }
        if (licenceHandler.hasAccessTo(ContribFeature.WEB_UI) && activity().featureViewModel.isFeatureAvailable(activity(), Feature.WEBUI)) {
          return true;
        } else {
          activity().contribFeatureRequested(ContribFeature.WEB_UI, null);
          return false;
        }
      } else {
        return true;
      }
    }
    return true;
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    trackPreferenceClick(preference);
    if (matches(preference, CONTRIB_PURCHASE)) {
      if (licenceHandler.isUpgradeable()) {
        Intent i = ContribInfoDialogActivity.getIntentFor(getActivity(), null);
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
      activity().dispatchCommand(R.id.FEEDBACK_COMMAND, null);
      return true;
    }
    if (matches(preference, RATE)) {
      prefHandler.putLong(NEXT_REMINDER_RATE, -1);
      activity().dispatchCommand(R.id.RATE_COMMAND, null);
      return true;
    }
    if (matches(preference, MORE_INFO_DIALOG)) {
      getActivity().showDialog(R.id.MORE_INFO_DIALOG);
      return true;
    }
    if (matches(preference, RESTORE) || matches(preference, RESTORE_LEGACY)) {
      startActivityForResult(preference.getIntent(), RESTORE_REQUEST);
      return true;
    }
    if (matches(preference, APP_DIR)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        //noinspection InlinedApi
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        try {
          pickFolderRequestStart = System.currentTimeMillis();
          startActivityForResult(intent, PICK_FOLDER_REQUEST);
          return true;
        } catch (ActivityNotFoundException e) {
          CrashHandler.report(e);
          //fallback to FolderBrowser
        }
      }
      startLegacyFolderRequest(AppDirHelper.getAppDir(getActivity()));
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
      activity().checkGdprConsent(true);
      return true;
    }
    return false;
  }

  private void showOnlyOneProtectionWarning(boolean legacyProtectionByPasswordIsActive) {
    String lockScreen = getString(R.string.pref_protection_device_lock_screen_title);
    String passWord = getString(R.string.pref_protection_password_title);
    Object[] formatArgs = legacyProtectionByPasswordIsActive ? new String[]{lockScreen, passWord} : new String[]{passWord, lockScreen};
    //noinspection StringFormatMatches
    activity().showSnackbar(getString(R.string.pref_warning_only_one_protection, formatArgs));
  }

  private void contribBuyDo(Package selectedPackage, boolean shouldReplaceExisting) {
    startActivity(ContribInfoDialogActivity.getIntentFor(getContext(), selectedPackage, shouldReplaceExisting));
  }

  private void startLegacyFolderRequest(@NonNull DocumentFile appDir) {
    Intent intent;
    intent = new Intent(getActivity(), FolderBrowser.class);
    intent.putExtra(FolderBrowser.PATH, appDir.getUri().getPath());
    startActivityForResult(intent, PICK_FOLDER_REQUEST_LEGACY);
  }

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    DialogFragment fragment = null;
    String key = preference.getKey();
    if (matches(preference, PLANNER_CALENDAR_ID)) {
      if (CALENDAR.hasPermission(getContext())) {
        fragment = CalendarListPreferenceDialogFragmentCompat.newInstance(key);
      } else {
        activity().requestCalendarPermission();
        return;
      }
    } else if (preference instanceof FontSizeDialogPreference) {
      fragment = FontSizeDialogFragmentCompat.newInstance(key);
    } else if (preference instanceof TimePreference) {
      fragment = TimePreferenceDialogFragmentCompat.newInstance(key);
    } else if (matches(preference, PROTECTION_LEGACY)) {
      if (Utils.hasApiLevel(Build.VERSION_CODES.LOLLIPOP) && prefHandler.getBoolean(PROTECTION_DEVICE_LOCK_SCREEN, false)) {
        showOnlyOneProtectionWarning(false);
        return;
      } else {
        fragment = LegacyPasswordPreferenceDialogFragmentCompat.newInstance(key);
      }
    } else if (matches(preference, SECURITY_QUESTION)) {
      fragment = SecurityQuestionDialogFragmentCompat.newInstance(key);
    } else if (matches(preference, AUTO_BACKUP_CLOUD)) {
      if (((ListPreference) preference).getEntries().length == 1) {
        activity().showSnackbar(R.string.no_sync_backends);
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

  @TargetApi(Build.VERSION_CODES.KITKAT)
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
          CrashHandler.report(String.format(Locale.ROOT, "PICK_FOLDER_REQUEST returned after %d millis with request code %d",
              pickFolderRequestDuration, requestCode));
          DocumentFile appDir = AppDirHelper.getAppDir(getActivity());
          if (appDir != null) {
            startLegacyFolderRequest(appDir);
          }
        }
      }
    } else if (requestCode == PICK_FOLDER_REQUEST_LEGACY && resultCode == Activity.RESULT_OK) {
      loadAppDirSummary();
    }
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (DIALOG_VALIDATE_LICENCE.equals(dialogTag)) {
      if (which == BUTTON_POSITIVE) {
        prefHandler.putString(NEW_LICENCE, extras.getString(KEY_KEY).trim());
        prefHandler.putString(LICENCE_EMAIL, extras.getString(KEY_EMAIL).trim());
        activity().validateLicence();
      }
    } else if (DIALOG_MANAGE_LICENCE.equals(dialogTag)) {
      switch (which) {
        case BUTTON_POSITIVE:
          activity().validateLicence();
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
