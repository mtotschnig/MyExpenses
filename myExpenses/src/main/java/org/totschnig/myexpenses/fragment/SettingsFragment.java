package org.totschnig.myexpenses.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.CommonCommands;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.activity.FolderBrowser;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.CurrencyEnum;
import org.totschnig.myexpenses.preference.CalendarListPreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.preference.FontSizeDialogFragmentCompat;
import org.totschnig.myexpenses.preference.FontSizeDialogPreference;
import org.totschnig.myexpenses.preference.PasswordPreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.preference.PopupMenuPreference;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.preference.SecurityQuestionDialogFragmentCompat;
import org.totschnig.myexpenses.preference.TimePreference;
import org.totschnig.myexpenses.preference.TimePreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.PreferenceDividerItemDecoration;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.ShareUtils;
import org.totschnig.myexpenses.util.ShortcutHelper;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.io.FileUtils;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;
import org.totschnig.myexpenses.util.licence.Package;
import org.totschnig.myexpenses.widget.AbstractWidget;

import java.net.URI;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;

import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.RESTORE_REQUEST;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.RESULT_RESTORE_OK;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.preference.PrefKey.APP_DIR;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP_INFO;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP_CLOUD;
import static org.totschnig.myexpenses.preference.PrefKey.CATEGORY_ADVANCED;
import static org.totschnig.myexpenses.preference.PrefKey.CATEGORY_CONTRIB;
import static org.totschnig.myexpenses.preference.PrefKey.CATEGORY_MANAGE;
import static org.totschnig.myexpenses.preference.PrefKey.CONTRIB_PURCHASE;
import static org.totschnig.myexpenses.preference.PrefKey.CUSTOM_DECIMAL_FORMAT;
import static org.totschnig.myexpenses.preference.PrefKey.DEBUG_ADS;
import static org.totschnig.myexpenses.preference.PrefKey.DEBUG_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.GROUPING_START_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.GROUP_MONTH_STARTS;
import static org.totschnig.myexpenses.preference.PrefKey.GROUP_WEEK_STARTS;
import static org.totschnig.myexpenses.preference.PrefKey.HOME_CURRENCY;
import static org.totschnig.myexpenses.preference.PrefKey.IMPORT_CSV;
import static org.totschnig.myexpenses.preference.PrefKey.IMPORT_QIF;
import static org.totschnig.myexpenses.preference.PrefKey.LICENCE_EMAIL;
import static org.totschnig.myexpenses.preference.PrefKey.MANAGE_STALE_IMAGES;
import static org.totschnig.myexpenses.preference.PrefKey.MANAGE_SYNC_BACKENDS;
import static org.totschnig.myexpenses.preference.PrefKey.MORE_INFO_DIALOG;
import static org.totschnig.myexpenses.preference.PrefKey.NEW_LICENCE;
import static org.totschnig.myexpenses.preference.PrefKey.NEXT_REMINDER_RATE;
import static org.totschnig.myexpenses.preference.PrefKey.PERFORM_PROTECTION_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PERFORM_SHARE;
import static org.totschnig.myexpenses.preference.PrefKey.PLANNER_CALENDAR_ID;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_DELAY_SECONDS;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_DEVICE_LOCK_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET;
import static org.totschnig.myexpenses.preference.PrefKey.PROTECTION_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.RATE;
import static org.totschnig.myexpenses.preference.PrefKey.RESTORE;
import static org.totschnig.myexpenses.preference.PrefKey.RESTORE_LEGACY;
import static org.totschnig.myexpenses.preference.PrefKey.ROOT_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.SECURITY_QUESTION;
import static org.totschnig.myexpenses.preference.PrefKey.SEND_FEEDBACK;
import static org.totschnig.myexpenses.preference.PrefKey.SHARE_TARGET;
import static org.totschnig.myexpenses.preference.PrefKey.SHORTCUT_CREATE_SPLIT;
import static org.totschnig.myexpenses.preference.PrefKey.SHORTCUT_CREATE_TRANSACTION;
import static org.totschnig.myexpenses.preference.PrefKey.SHORTCUT_CREATE_TRANSFER;
import static org.totschnig.myexpenses.preference.PrefKey.SYNC_NOTIFICATION;
import static org.totschnig.myexpenses.preference.PrefKey.TRACKING;
import static org.totschnig.myexpenses.preference.PrefKey.UI_HOME_SCREEN_SHORTCUTS;
import static org.totschnig.myexpenses.preference.PrefKey.UI_LANGUAGE;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

public class SettingsFragment extends PreferenceFragmentCompat implements
    Preference.OnPreferenceChangeListener,
    Preference.OnPreferenceClickListener,
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
  LicenceHandler licenceHandler;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    MyApplication.getInstance().getAppComponent().inject(this);
    super.onCreate(savedInstanceState);
    if (MyApplication.isInstrumentationTest()) {
      getPreferenceManager().setSharedPreferencesName(MyApplication.getTestId());
    }
  }

  private Preference.OnPreferenceClickListener homeScreenShortcutPrefClickHandler =
      preference -> {
        Bundle extras = new Bundle();
        extras.putBoolean(AbstractWidget.EXTRA_START_FROM_WIDGET, true);
        extras.putBoolean(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, true);
        int nameId = 0, operationType = 0;
        Bitmap bitmap = null;
        if (matches(preference, SHORTCUT_CREATE_TRANSACTION)) {
          nameId = R.string.transaction;
          bitmap = getBitmapForShortcut(R.drawable.shortcut_create_transaction_icon,
              R.drawable.shortcut_create_transaction_icon_lollipop);
          operationType = TYPE_TRANSACTION;
        }
        if (matches(preference, SHORTCUT_CREATE_TRANSFER)) {
          nameId = R.string.transfer;
          bitmap = getBitmapForShortcut(R.drawable.shortcut_create_transfer_icon,
              R.drawable.shortcut_create_transfer_icon_lollipop);
          operationType = TYPE_TRANSFER;
        }
        if (matches(preference, SHORTCUT_CREATE_SPLIT)) {
          nameId = R.string.split_transaction;
          bitmap = getBitmapForShortcut(R.drawable.shortcut_create_split_icon,
              R.drawable.shortcut_create_split_icon_lollipop);
          operationType = TYPE_SPLIT;
        }
        if (nameId != 0) {
          addShortcut(nameId, operationType, bitmap);
          return true;
        }
        return false;
      };

  private Preference.OnPreferenceChangeListener storeInDatabaseChangeListener =
      (preference, newValue) -> {
        activity().startTaskExecution(TaskExecutionFragment.TASK_STORE_SETTING,
            new String[]{preference.getKey()}, newValue.toString(), R.string.progress_dialog_saving);
        return true;
      };

  private Preference findPreference(PrefKey prefKey) {
    return findPreference(prefKey.getKey());
  }

  private boolean matches(Preference preference, PrefKey prefKey) {
    return preference.getKey().equals(prefKey.getKey());
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.preferences, rootKey);
    Preference pref;

    if (rootKey == null) {//ROOT screen
      findPreference(HOME_CURRENCY).setOnPreferenceChangeListener(this);
      findPreference(SEND_FEEDBACK).setOnPreferenceClickListener(this);
      findPreference(MORE_INFO_DIALOG).setOnPreferenceClickListener(this);

      pref = findPreference(RESTORE);
      pref.setTitle(getString(R.string.pref_restore_title) + " (ZIP)");
      pref.setOnPreferenceClickListener(this);

      pref = findPreference(RESTORE_LEGACY);
      pref.setTitle(getString(R.string.pref_restore_title) + " (" + getString(R.string.pref_restore_alternative) + ")");
      pref.setOnPreferenceClickListener(this);

      findPreference(RATE).setOnPreferenceClickListener(this);

      pref = findPreference(CUSTOM_DECIMAL_FORMAT);
      pref.setOnPreferenceChangeListener(this);
      if (CUSTOM_DECIMAL_FORMAT.getString("").equals("")) {
        setDefaultNumberFormat(((EditTextPreference) pref));
      }

      findPreference(APP_DIR).setOnPreferenceClickListener(this);
      setAppDirSummary();

      final PreferenceCategory categoryManage =
          ((PreferenceCategory) findPreference(CATEGORY_MANAGE));
      final Preference prefStaleImages = findPreference(MANAGE_STALE_IMAGES);
      categoryManage.removePreference(prefStaleImages);

      pref = findPreference(IMPORT_QIF);
      pref.setSummary(getString(R.string.pref_import_summary, "QIF"));
      pref.setTitle(getString(R.string.pref_import_title, "QIF"));
      pref = findPreference(IMPORT_CSV);
      pref.setSummary(getString(R.string.pref_import_summary, "CSV"));
      pref.setTitle(getString(R.string.pref_import_title, "CSV"));
      pref.setOnPreferenceClickListener(this);

      findPreference(MANAGE_SYNC_BACKENDS).setSummary(
          getString(R.string.pref_manage_sync_backends_summary,
              Stream.of(ServiceLoader.load(getContext()))
                  .map(SyncBackendProviderFactory::getLabel)
                  .collect(Collectors.joining(", "))) +
              " " + ContribFeature.SYNCHRONIZATION.buildRequiresString(getActivity()));

      new AsyncTask<Void, Void, Boolean>() {
        @Override
        protected Boolean doInBackground(Void... params) {
          if (getActivity() == null) return false;
          Cursor c = getActivity().getContentResolver().query(
              TransactionProvider.STALE_IMAGES_URI,
              new String[]{"count(*)"},
              null, null, null);
          if (c == null)
            return false;
          boolean hasImages = false;
          if (c.moveToFirst() && c.getInt(0) > 0)
            hasImages = true;
          c.close();
          return hasImages;
        }

        @Override
        protected void onPostExecute(Boolean result) {
          if (getActivity() != null && !getActivity().isFinishing() && result)
            categoryManage.addPreference(prefStaleImages);
        }
      }.execute();

      pref = findPreference(TRACKING);
      try {
        Class.forName("org.totschnig.myexpenses.util.tracking.PlatformTracker");
      } catch (ClassNotFoundException e) {
        ((PreferenceCategory) findPreference(CATEGORY_ADVANCED)).removePreference(pref);
      }

      ListPreference languagePref = ((ListPreference) findPreference(UI_LANGUAGE));
      languagePref.setEntries(getLocaleArray(getContext()));

      findPreference(SYNC_NOTIFICATION).setOnPreferenceChangeListener(storeInDatabaseChangeListener);

      findPreference(getString(R.string.pref_follow_gplus_key)).setTitle(
          Utils.getTextWithAppName(getContext(), R.string.pref_follow_gplus_title));

      ListPreference homeCurrencyPref = (ListPreference) findPreference(PrefKey.HOME_CURRENCY);
      CurrencyEnum[] currencies = CurrencyEnum.sortedValues();
      homeCurrencyPref.setEntries(Stream.of(currencies).map(CurrencyEnum::toString).toArray(CharSequence[]::new));
      homeCurrencyPref.setEntryValues(Stream.of(currencies).map(CurrencyEnum::name).toArray(CharSequence[]::new));
    }
    //SHORTCUTS screen
    else if (rootKey.equals(UI_HOME_SCREEN_SHORTCUTS.getKey())) {
      findPreference(SHORTCUT_CREATE_TRANSACTION)
          .setOnPreferenceClickListener(homeScreenShortcutPrefClickHandler);
      findPreference(SHORTCUT_CREATE_TRANSFER)
          .setOnPreferenceClickListener(homeScreenShortcutPrefClickHandler);
      pref = findPreference(SHORTCUT_CREATE_SPLIT);
      pref.setOnPreferenceClickListener(homeScreenShortcutPrefClickHandler);
      pref.setEnabled(licenceHandler.isContribEnabled());
      pref.setSummary(
          getString(R.string.pref_shortcut_summary) + " " +
              ContribFeature.SPLIT_TRANSACTION.buildRequiresString(getActivity()));

    }
    //Password screen
    else if (rootKey.equals(PERFORM_PROTECTION_SCREEN.getKey())) {
      setProtectionDependentsState();
      findPreference(SECURITY_QUESTION).setSummary(
          getString(R.string.pref_security_question_summary) + " " +
              ContribFeature.SECURITY_QUESTION.buildRequiresString(getActivity()));

      Preference preference = findPreference(PROTECTION_DEVICE_LOCK_SCREEN);
      if (Utils.hasApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
        preference.setOnPreferenceClickListener(this);
        findPreference(PROTECTION_LEGACY)
            .setTitle(String.format("%s (%s)", getString(R.string.pref_protection_password_title), getString(R.string.feature_deprecated)));
      } else {
        getPreferenceScreen().removePreference(preference);
      }
    }
    //SHARE screen
    else if (rootKey.equals(PERFORM_SHARE.getKey())) {
      pref = findPreference(SHARE_TARGET);
      //noinspection AuthLeak
      pref.setSummary(getString(R.string.pref_share_target_summary) + ":\n" +
          "ftp: \"ftp://login:password@my.example.org:port/my/directory/\"\n" +
          "mailto: \"mailto:john@my.example.com\"");
      pref.setOnPreferenceChangeListener(this);
    }
    //BACKUP screen
    else if (rootKey.equals(AUTO_BACKUP.getKey())) {
      pref = findPreference(AUTO_BACKUP_INFO);
      String summary = getString(R.string.pref_auto_backup_summary) + " " +
          ContribFeature.AUTO_BACKUP.buildRequiresString(getActivity());
      pref.setSummary(summary);
    }
    //GROUP start screen
    else if (rootKey.equals(GROUPING_START_SCREEN.getKey())) {
      ListPreference startPref = (ListPreference) findPreference(GROUP_WEEK_STARTS);
      final Locale locale = Locale.getDefault();
      DateFormatSymbols dfs = new DateFormatSymbols(locale);
      String[] entries = new String[7];
      System.arraycopy(dfs.getWeekdays(), 1, entries, 0, 7);
      startPref.setEntries(entries);
      startPref.setEntryValues(new String[]{
          String.valueOf(Calendar.SUNDAY),
          String.valueOf(Calendar.MONDAY),
          String.valueOf(Calendar.TUESDAY),
          String.valueOf(Calendar.WEDNESDAY),
          String.valueOf(Calendar.THURSDAY),
          String.valueOf(Calendar.FRIDAY),
          String.valueOf(Calendar.SATURDAY),
      });
      if (!GROUP_WEEK_STARTS.isSet()) {
        startPref.setValue(String.valueOf(Utils.getFirstDayOfWeek(locale)));
      }

      startPref = (ListPreference) findPreference(GROUP_MONTH_STARTS);
      String[] daysEntries = new String[31], daysValues = new String[31];
      for (int i = 1; i <= 31; i++) {
        daysEntries[i - 1] = Utils.toLocalizedString(i);
        daysValues[i - 1] = String.valueOf(i);
      }
      startPref.setEntries(daysEntries);
      startPref.setEntryValues(daysValues);
    } else if (rootKey.equals(DEBUG_SCREEN.getKey())) {
      if (!licenceHandler.isContribEnabled() && !BuildConfig.DEBUG) {
        getPreferenceScreen().removePreference(findPreference(DEBUG_ADS));
      }
    }
  }

  public static String[] getLocaleArray(Context context) {
    return Stream.of(context.getResources().getStringArray(R.array.pref_ui_language_values))
        .map(localeString -> getLocaleDisplayName(context, localeString)).toArray(size -> new String[size]);
  }

  private static CharSequence getLocaleDisplayName(Context context, CharSequence localeString) {
    if (localeString.equals("default")) {
      return context.getString(R.string.pref_ui_language_default);
    } else {
      String[] localeParts = localeString.toString().split("-");
      Locale locale = localeParts.length == 2 ?
          new Locale(localeParts[0], localeParts[1]) : new Locale(localeParts[0]);
      return locale.getDisplayName(locale);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    final MyPreferenceActivity activity = activity();
    final ActionBar actionBar = activity.getSupportActionBar();
    PreferenceScreen screen = getPreferenceScreen();
    boolean isRoot = matches(screen, ROOT_SCREEN);
    CharSequence title = isRoot ?
        Utils.concatResStrings(activity, " ", R.string.app_name, R.string.menu_settings) :
        screen.getTitle();
    actionBar.setTitle(title);
    boolean hasMasterSwitch = handleScreenWithMasterSwitch(PERFORM_SHARE);
    hasMasterSwitch = handleScreenWithMasterSwitch(AUTO_BACKUP) || hasMasterSwitch;
    if (!hasMasterSwitch) {
      actionBar.setCustomView(null);
    }
    if (isRoot) {
      findPreference(PERFORM_PROTECTION_SCREEN).setSummary(getString(
          PROTECTION_LEGACY.getBoolean(false) ? R.string.pref_protection_password_title :
              PROTECTION_DEVICE_LOCK_SCREEN.getBoolean(false) ? R.string.pref_protection_device_lock_screen_title :
                  R.string.switch_off_text));
      Preference preference = findPreference(PLANNER_CALENDAR_ID);
      if (preference != null) {
        if (activity.isCalendarPermissionPermanentlyDeclined()) {
          preference.setSummary(Utils.getTextWithAppName(getContext(),
              R.string.calendar_permission_required));
        } else {
          preference.setSummary(R.string.pref_planning_calendar_summary);
        }
      }
      configureContribPrefs();
    }
  }

  /**
   * Configures the current screen with a Master Switch, if it has the given key
   * if we are on the root screen, the preference summary for the given key is updated with the
   * current value (On/Off)
   *
   * @param prefKey
   * @return true if we have handle the given key as a subscreen
   */
  private boolean handleScreenWithMasterSwitch(final PrefKey prefKey) {
    PreferenceScreen screen = getPreferenceScreen();
    final ActionBar actionBar = activity().getSupportActionBar();
    final boolean status = prefKey.getBoolean(false);
    if (matches(screen, prefKey)) {
      //noinspection InflateParams
      SwitchCompat actionBarSwitch = (SwitchCompat) getActivity().getLayoutInflater().inflate(
          R.layout.pref_master_switch, null);
      actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
          ActionBar.DISPLAY_SHOW_CUSTOM);
      actionBar.setCustomView(actionBarSwitch);
      actionBarSwitch.setChecked(status);
      actionBarSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          if (prefKey.equals(AUTO_BACKUP)) {
            if (isChecked && !ContribFeature.AUTO_BACKUP.hasAccess()) {
              CommonCommands.showContribDialog(getActivity(), ContribFeature.AUTO_BACKUP, null);
              if (ContribFeature.AUTO_BACKUP.usagesLeft() <= 0) {
                buttonView.setChecked(false);
                return;
              }
            }
          }
          prefKey.putBoolean(isChecked);
          updateDependents(isChecked);
        }
      });
      updateDependents(status);
      return true;
    } else if (matches(screen, ROOT_SCREEN)) {
      setOnOffSummary(prefKey);
    }
    return false;
  }

  private void setOnOffSummary(PrefKey prefKey) {
    setOnOffSummary(prefKey, prefKey.getBoolean(false));
  }

  private void setOnOffSummary(PrefKey key, boolean status) {
    findPreference(key).setSummary(status ? getString(R.string.switch_on_text) :
        getString(R.string.switch_off_text));
  }

  private void updateDependents(boolean enabled) {
    int count = getPreferenceScreen().getPreferenceCount();
    for (int i = 0; i < count; ++i) {
      Preference pref = getPreferenceScreen().getPreference(i);
      pref.setEnabled(enabled);
    }
  }

  public void showPreference(String prefKey) {
    final Preference preference = findPreference(prefKey);
    if (preference != null) {
      //noinspection RestrictedApi
      preference.performClick();
    }
  }

  public void configureContribPrefs() {
    Preference contribPurchasePref = findPreference(CONTRIB_PURCHASE),
        licenceKeyPref = findPreference(NEW_LICENCE);
    if (DistribHelper.isGithub()) {
      licenceKeyPref.setOnPreferenceClickListener(this);
    } else if (licenceKeyPref != null) {
      ((PreferenceCategory) findPreference(CATEGORY_CONTRIB)).removePreference(licenceKeyPref);
    }
    String contribPurchaseTitle, contribPurchaseSummary;
    LicenceStatus licenceStatus = licenceHandler.getLicenceStatus();
    if (licenceStatus == null) {
      int baseTitle = R.string.pref_contrib_purchase_title;
      contribPurchaseTitle = getString(baseTitle);
      if (!DistribHelper.isGithub()) {
        contribPurchaseTitle += " (" + getString(R.string.pref_contrib_purchase_title_in_app) + ")";
      }
      contribPurchaseSummary = getString(R.string.pref_contrib_purchase_summary);
    } else {
      contribPurchaseTitle = getString(R.string.licence_status) + ": " + getString(licenceStatus.getResId());
      if (licenceHandler.hasLegacyLicence()) {
        contribPurchaseSummary = Utils.getTextWithAppName(getContext(), R.string.licence_migration_info).toString();
      } else if (licenceStatus.isUpgradeable()) {
        contribPurchaseSummary = getString(R.string.pref_contrib_purchase_title_upgrade);
      } else {
        contribPurchaseSummary = licenceHandler.getProLicenceAction(getContext());
        String proLicenceStatus = licenceHandler.getProLicenceStatus(getContext());
        if (!TextUtils.isEmpty(proLicenceStatus)) {
          contribPurchaseTitle += String.format(" (%s)", proLicenceStatus);
        }
      }
      if (!TextUtils.isEmpty(contribPurchaseSummary)) {
        contribPurchaseSummary += "\n";
      }
      contribPurchaseSummary += getString(R.string.thank_you);
    }
    contribPurchasePref.setOnPreferenceClickListener(this);
    contribPurchasePref.setSummary(contribPurchaseSummary);
    contribPurchasePref.setTitle(contribPurchaseTitle);
  }

  public void setProtectionDependentsState() {
    boolean isLegacy = PROTECTION_LEGACY.getBoolean(false);
    boolean isProtected = isLegacy || PROTECTION_DEVICE_LOCK_SCREEN.getBoolean(false);
    findPreference(SECURITY_QUESTION).setEnabled(licenceHandler.isContribEnabled() && isLegacy);
    findPreference(PROTECTION_DELAY_SECONDS).setEnabled(isProtected);
    findPreference(PROTECTION_ENABLE_ACCOUNT_WIDGET).setEnabled(isProtected);
    findPreference(PROTECTION_ENABLE_TEMPLATE_WIDGET).setEnabled(isProtected);
    findPreference(PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET).setEnabled(isProtected);
  }

  @Override
  public boolean onPreferenceChange(Preference pref, Object value) {
    if (matches(pref, HOME_CURRENCY)) {
      if (!value.equals(HOME_CURRENCY.getString(null))) {
        MessageDialogFragment.newInstance(R.string.dialog_title_information,
            R.string.home_currency_change_warning,
            new MessageDialogFragment.Button(android.R.string.ok, R.id.CHANGE_COMMAND, ((String) value)),
            null, MessageDialogFragment.Button.noButton()).show(getFragmentManager(), "CONFIRM");
      }
      return false;
    }
    if (matches(pref, SHARE_TARGET)) {
      String target = (String) value;
      URI uri;
      if (!target.equals("")) {
        uri = ShareUtils.parseUri(target);
        if (uri == null) {
          activity().showSnackbar(getString(R.string.ftp_uri_malformed, target), Snackbar.LENGTH_LONG);
          return false;
        }
        String scheme = uri.getScheme();
        if (!(scheme.equals("ftp") || scheme.equals("mailto"))) {
          activity().showSnackbar(getString(R.string.share_scheme_not_supported, scheme), Snackbar.LENGTH_LONG);
          return false;
        }
        Intent intent;
        if (scheme.equals("ftp")) {
          intent = new Intent(Intent.ACTION_SENDTO);
          intent.setData(android.net.Uri.parse(target));
          if (!Utils.isIntentAvailable(getActivity(), intent)) {
            getActivity().showDialog(R.id.FTP_DIALOG);
          }
        }
      }
    } else if (matches(pref, CUSTOM_DECIMAL_FORMAT)) {
      if (TextUtils.isEmpty((String) value)) {
        CurrencyFormatter.instance().invalidateAll();
        return true;
      }
      try {
        DecimalFormat nf = new DecimalFormat();
        nf.applyLocalizedPattern(((String) value));
        CurrencyFormatter.instance().invalidateAll();
      } catch (IllegalArgumentException e) {
        activity().showSnackbar(R.string.number_format_illegal, Snackbar.LENGTH_LONG);
        return false;
      }
    }
    return true;
  }

  private void setDefaultNumberFormat(EditTextPreference pref) {
    String pattern = ((DecimalFormat) NumberFormat.getCurrencyInstance()).toLocalizedPattern();
    //Log.d(MyApplication.TAG,pattern);
    pref.setText(pattern);
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    if (matches(preference, CONTRIB_PURCHASE)) {
      if (licenceHandler.hasLegacyLicence()) {
        CommonCommands.dispatchCommand(activity(), R.id.REQUEST_LICENCE_MIGRATION_COMMAND, null);
      } else if (licenceHandler.isUpgradeable()) {
        Intent i = ContribInfoDialogActivity.getIntentFor(getActivity(), null);
        if (DistribHelper.isGithub()) {
          startActivityForResult(i, CONTRIB_PURCHASE_REQUEST);
        } else {
          startActivity(i);
        }
      } else {
        Package[] proPackagesForExtendOrSwitch = licenceHandler.getProPackagesForExtendOrSwitch();
        if (proPackagesForExtendOrSwitch != null) {
          if (proPackagesForExtendOrSwitch.length > 1) {
            ((PopupMenuPreference) preference).showPopupMenu(item -> {
              contribBuyDo(proPackagesForExtendOrSwitch[item.getItemId()], false);
              return true;
            }, Stream.of(proPackagesForExtendOrSwitch).map(licenceHandler::getExtendOrSwitchMessage).toArray(size -> new String[size]));
          } else {
            //Currently we assume that if we have only one item, we switch
            contribBuyDo(proPackagesForExtendOrSwitch[0], true);
          }
        }
      }
      return true;
    }
    if (matches(preference, SEND_FEEDBACK)) {
      CommonCommands.dispatchCommand(activity(), R.id.FEEDBACK_COMMAND, null);
      return true;
    }
    if (matches(preference, RATE)) {
      NEXT_REMINDER_RATE.putLong(-1);
      CommonCommands.dispatchCommand(activity(), R.id.RATE_COMMAND, null);
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
      DocumentFile appDir = AppDirHelper.getAppDir(getActivity());
      if (appDir == null) {
        preference.setSummary(R.string.external_storage_unavailable);
        preference.setEnabled(false);
      } else {
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
        startLegacyFolderRequest(appDir);
      }
      return true;
    }
    if (matches(preference, IMPORT_CSV)) {
      if (ContribFeature.CSV_IMPORT.hasAccess()) {
        activity().contribFeatureCalled(ContribFeature.CSV_IMPORT, null);
      } else {
        CommonCommands.showContribDialog(getActivity(), ContribFeature.CSV_IMPORT, null);
      }
      return true;
    }
    if (matches(preference, NEW_LICENCE)) {
      String licenceKey = NEW_LICENCE.getString("");
      String licenceEmail = LICENCE_EMAIL.getString("");
      if (licenceHandler.isContribEnabled() && !licenceHandler.hasLegacyLicence()) {
        SimpleDialog.build()
            .title(R.string.licence_key)
            .msg(String.format("%s: %s", licenceEmail, licenceKey))
            .pos(R.string.button_validate)
            .neg(R.string.menu_remove)
            .show(this, DIALOG_MANAGE_LICENCE);

      } else {
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
    if (matches(preference, PROTECTION_DEVICE_LOCK_SCREEN)) {
      if (Utils.hasApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
        SwitchPreferenceCompat switchPreferenceCompat = (SwitchPreferenceCompat) preference;
        if (switchPreferenceCompat.isChecked()) {
          if (!((KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE)).isKeyguardSecure()) {
            activity().showDeviceLockScreenWarning();
            switchPreferenceCompat.setChecked(false);
          } else if (PROTECTION_LEGACY.getBoolean(false)) {
            showOnlyOneProtectionWarning(true);
            switchPreferenceCompat.setChecked(false);
          }
        }
      }
      return true;
    }
    return false;
  }

  private void showOnlyOneProtectionWarning(boolean legacyProtectionByPasswordIsActive) {
    String lockScreen = getString(R.string.pref_protection_device_lock_screen_title);
    String passWord = getString(R.string.pref_protection_password_title);
    Object[] formatArgs = legacyProtectionByPasswordIsActive ? new String[]{lockScreen, passWord} : new String[]{passWord, lockScreen};
    //noinspection StringFormatMatches
    activity().showSnackbar(getString(R.string.pref_warning_only_one_protection, formatArgs), Snackbar.LENGTH_LONG);
  }

  private void contribBuyDo(Package selectedPackage, boolean shouldReplaceExisting) {
    startActivity(ContribInfoDialogActivity.getIntentFor(getContext(), selectedPackage, shouldReplaceExisting));
  }

  protected void startLegacyFolderRequest(@NonNull  DocumentFile appDir) {
    Intent intent;
    intent = new Intent(getActivity(), FolderBrowser.class);
    intent.putExtra(FolderBrowser.PATH, appDir.getUri().getPath());
    startActivityForResult(intent, PICK_FOLDER_REQUEST_LEGACY);
  }

  private void setAppDirSummary() {
    Preference pref = findPreference(APP_DIR);
    if (AppDirHelper.isExternalStorageAvailable()) {
      DocumentFile appDir = AppDirHelper.getAppDir(getActivity());
      if (appDir != null) {
        if (AppDirHelper.isWritableDirectory(appDir)) {
          pref.setSummary(FileUtils.getPath(getActivity(), appDir.getUri()));
        } else {
          pref.setSummary(getString(R.string.app_dir_not_accessible,
              FileUtils.getPath(MyApplication.getInstance(), appDir.getUri())));
        }
      } else {
        pref.setSummary(R.string.io_error_appdir_null);
      }
    } else {
      pref.setSummary(R.string.external_storage_unavailable);
      pref.setEnabled(false);
    }
  }

  private Bitmap getBitmapForShortcut(int iconIdLegacy, int iconIdLolipop) {
    if (Utils.hasApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
      return UiUtils.drawableToBitmap(getResources().getDrawable(iconIdLolipop));
    } else {
      return UiUtils.getTintedBitmapForTheme(getActivity(), iconIdLegacy, R.style.ThemeDark);
    }
  }

  // credits Financisto
  // src/ru/orangesoftware/financisto/activity/PreferencesActivity.java
  private void addShortcut(int nameId, int operationType, Bitmap bitmap) {
    Intent shortcutIntent = ShortcutHelper.createIntentForNewTransaction(getContext(), operationType);

    Intent intent = new Intent();
    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(nameId));
    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
    intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");

    if (Utils.isIntentReceiverAvailable(getActivity(), intent)) {
      getActivity().sendBroadcast(intent);
      activity().showSnackbar(getString(R.string.pref_shortcut_added), Snackbar.LENGTH_LONG);
    } else {
      activity().showSnackbar(getString(R.string.pref_shortcut_not_added), Snackbar.LENGTH_LONG);
    }
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
      if (Utils.hasApiLevel(Build.VERSION_CODES.LOLLIPOP) && PROTECTION_DEVICE_LOCK_SCREEN.getBoolean(false)) {
        showOnlyOneProtectionWarning(false);
        return;
      } else {
        fragment = PasswordPreferenceDialogFragmentCompat.newInstance(key);
      }
    } else if (matches(preference, SECURITY_QUESTION)) {
      fragment = SecurityQuestionDialogFragmentCompat.newInstance(key);
    } else if (matches(preference, AUTO_BACKUP_CLOUD)) {
      if (((ListPreference) preference).getEntries().length == 1) {
        activity().showSnackbar(R.string.auto_backup_cloud_create_backend, Snackbar.LENGTH_LONG);
        return;
      }
    }
    if (fragment != null) {
      fragment.setTargetFragment(this, 0);
      fragment.show(getFragmentManager(),
          "android.support.v7.preference.PreferenceFragment.DIALOG");
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  @Override
  public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
    RecyclerView result = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
    result.addItemDecoration(
        new PreferenceDividerItemDecoration(getActivity())
    );
    return result;
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  @Override
  public void onActivityResult(int requestCode, int resultCode,
                               Intent intent) {
    if (requestCode == RESTORE_REQUEST && resultCode == RESULT_RESTORE_OK) {
      getActivity().setResult(resultCode);
      getActivity().finish();
    } else if (requestCode == PICK_FOLDER_REQUEST) {
      if (resultCode == Activity.RESULT_OK) {
        Uri dir = intent.getData();
        getActivity().getContentResolver().takePersistableUriPermission(dir,
            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        APP_DIR.putString(intent.getData().toString());
        setAppDirSummary();
      } else {
        //we try to determine if we get here due to abnormal failure (observed on Xiaomi) of request, or if user canceled
        long pickFolderRequestDuration = System.currentTimeMillis() - pickFolderRequestStart;
        if (pickFolderRequestDuration < 250) {
          //String error = String.format(Locale.ROOT, "PICK_FOLDER_REQUEST returned after %d millis with request code %d",
          //    pickFolderRequestDuration, requestCode);
          //AcraHelper.report(new Exception(error));
          DocumentFile appDir = AppDirHelper.getAppDir(getActivity());
          if (appDir != null) {
            startLegacyFolderRequest(appDir);
          }
        }
      }
    } else if (requestCode == PICK_FOLDER_REQUEST_LEGACY && resultCode == Activity.RESULT_OK) {
      setAppDirSummary();
    }
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (DIALOG_VALIDATE_LICENCE.equals(dialogTag)) {
      if (which == BUTTON_POSITIVE) {
        NEW_LICENCE.putString(extras.getString(KEY_KEY));
        LICENCE_EMAIL.putString(extras.getString(KEY_EMAIL));
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
              .show(getFragmentManager(), "REMOVE_LICENCE");
          break;
      }
    }
    return true;
  }
  
  private final MyPreferenceActivity activity() {
    return (MyPreferenceActivity) super.getActivity();
  }

  public void updateHomeCurrency(String currencyCode) {
    ((ListPreference) findPreference(HOME_CURRENCY)).setValue(currencyCode);
    Account.invalidateHomeAccount();
    activity().startTaskExecution(TaskExecutionFragment.TASK_RESET_EQUIVALENT_AMOUNTS,
        null, null, R.string.progress_dialog_saving);
  }
}
