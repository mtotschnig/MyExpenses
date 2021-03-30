package org.totschnig.myexpenses.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.icu.text.ListFormatter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.chrono.IsoChronology;
import org.threeten.bp.format.DateTimeFormatter;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity;
import org.totschnig.myexpenses.activity.FolderBrowser;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.feature.Feature;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.CalendarListPreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.preference.FontSizeDialogFragmentCompat;
import org.totschnig.myexpenses.preference.FontSizeDialogPreference;
import org.totschnig.myexpenses.preference.LegacyPasswordPreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference;
import org.totschnig.myexpenses.preference.PopupMenuPreference;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.preference.SecurityQuestionDialogFragmentCompat;
import org.totschnig.myexpenses.preference.SimplePasswordDialogFragmentCompat;
import org.totschnig.myexpenses.preference.SimplePasswordPreference;
import org.totschnig.myexpenses.preference.TimePreference;
import org.totschnig.myexpenses.preference.TimePreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.sync.ServiceLoader;
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;
import org.totschnig.myexpenses.util.ShareUtils;
import org.totschnig.myexpenses.util.ShortcutHelper;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.ads.AdHandlerFactory;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;
import org.totschnig.myexpenses.util.io.FileUtils;
import org.totschnig.myexpenses.util.io.NetworkUtilsKt;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.Package;
import org.totschnig.myexpenses.util.licence.ProfessionalPackage;
import org.totschnig.myexpenses.util.tracking.Tracker;
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.data.Currency;
import org.totschnig.myexpenses.widget.AbstractWidgetKt;

import java.net.URI;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;
import eltos.simpledialogfragment.input.SimpleInputDialog;

import static org.threeten.bp.format.DateTimeFormatterBuilder.getLocalizedDateTimePattern;
import static org.threeten.bp.format.FormatStyle.MEDIUM;
import static org.threeten.bp.format.FormatStyle.SHORT;
import static org.totschnig.myexpenses.activity.ConstantsKt.RESTORE_REQUEST;
import static org.totschnig.myexpenses.activity.ProtectedFragmentActivity.RESULT_RESTORE_OK;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.model.ContribFeature.CSV_IMPORT;
import static org.totschnig.myexpenses.preference.PrefKey.ACRA_INFO;
import static org.totschnig.myexpenses.preference.PrefKey.APP_DIR;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP_CLOUD;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP_INFO;
import static org.totschnig.myexpenses.preference.PrefKey.CATEGORY_PRIVACY;
import static org.totschnig.myexpenses.preference.PrefKey.CONTRIB_PURCHASE;
import static org.totschnig.myexpenses.preference.PrefKey.CRASHREPORT_ENABLED;
import static org.totschnig.myexpenses.preference.PrefKey.CRASHREPORT_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.CRASHREPORT_USEREMAIL;
import static org.totschnig.myexpenses.preference.PrefKey.CUSTOM_DATE_FORMAT;
import static org.totschnig.myexpenses.preference.PrefKey.CUSTOM_DECIMAL_FORMAT;
import static org.totschnig.myexpenses.preference.PrefKey.EXCHANGE_RATES;
import static org.totschnig.myexpenses.preference.PrefKey.EXCHANGE_RATE_PROVIDER;
import static org.totschnig.myexpenses.preference.PrefKey.FEATURE_UNINSTALL;
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
import static org.totschnig.myexpenses.preference.PrefKey.OCR;
import static org.totschnig.myexpenses.preference.PrefKey.OCR_DATE_FORMATS;
import static org.totschnig.myexpenses.preference.PrefKey.OCR_ENGINE;
import static org.totschnig.myexpenses.preference.PrefKey.OCR_TIME_FORMATS;
import static org.totschnig.myexpenses.preference.PrefKey.OCR_TOTAL_INDICATORS;
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
import static org.totschnig.myexpenses.preference.PrefKey.SHORTCUT_CREATE_SPLIT;
import static org.totschnig.myexpenses.preference.PrefKey.SHORTCUT_CREATE_TRANSACTION;
import static org.totschnig.myexpenses.preference.PrefKey.SHORTCUT_CREATE_TRANSFER;
import static org.totschnig.myexpenses.preference.PrefKey.SYNC;
import static org.totschnig.myexpenses.preference.PrefKey.SYNC_NOTIFICATION;
import static org.totschnig.myexpenses.preference.PrefKey.SYNC_WIFI_ONLY;
import static org.totschnig.myexpenses.preference.PrefKey.TRACKING;
import static org.totschnig.myexpenses.preference.PrefKey.TRANSLATION;
import static org.totschnig.myexpenses.preference.PrefKey.UI_HOME_SCREEN_SHORTCUTS;
import static org.totschnig.myexpenses.preference.PrefKey.UI_LANGUAGE;
import static org.totschnig.myexpenses.preference.PrefKey.UI_WEB;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;
import static org.totschnig.myexpenses.util.TextUtils.concatResStrings;

public class SettingsFragment extends BaseSettingsFragment implements
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
  @Inject
  AdHandlerFactory adHandlerFactory;
  @Inject
  CrashHandler crashHandler;
  @Inject
  CurrencyFormatter currencyFormatter;

  private CurrencyViewModel currencyViewModel;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    requireApplication().getAppComponent().inject(this);
    currencyViewModel = new ViewModelProvider(this).get(CurrencyViewModel.class);
    super.onCreate(savedInstanceState);
    prefHandler.preparePreferenceFragment(this);
  }

  private final Preference.OnPreferenceClickListener homeScreenShortcutPrefClickHandler =
      preference -> {
        trackPreferenceClick(preference);
        Bundle extras = new Bundle();
        extras.putBoolean(AbstractWidgetKt.EXTRA_START_FROM_WIDGET, true);
        extras.putBoolean(AbstractWidgetKt.EXTRA_START_FROM_WIDGET_DATA_ENTRY, true);
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

  //TODO: these settings need to be authoritatively stored in Database, instead of just mirrored
  private final Preference.OnPreferenceChangeListener storeInDatabaseChangeListener =
      (preference, newValue) -> {
        activity().startTaskExecution(TaskExecutionFragment.TASK_STORE_SETTING,
            new String[]{preference.getKey()}, newValue.toString(), R.string.progress_dialog_saving);
        return true;
      };

  private void trackPreferenceClick(Preference preference) {
    Bundle bundle = new Bundle();
    bundle.putString(Tracker.EVENT_PARAM_ITEM_ID, preference.getKey());
    activity().logEvent(Tracker.EVENT_PREFERENCE_CLICK, bundle);
  }

  private void setListenerRecursive(PreferenceGroup preferenceGroup, Preference.OnPreferenceClickListener listener) {
    for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
      final Preference preference = preferenceGroup.getPreference(i);
      if (preference instanceof PreferenceCategory) {
        setListenerRecursive(((PreferenceCategory) preference), listener);
      } else {
        preference.setOnPreferenceClickListener(listener);
      }
    }
  }


  private void unsetIconSpaceReservedRecursive(PreferenceGroup preferenceGroup) {
    for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
      final Preference preference = preferenceGroup.getPreference(i);
      if (preference instanceof PreferenceCategory) {
        unsetIconSpaceReservedRecursive(((PreferenceCategory) preference));
      }
      preference.setIconSpaceReserved(false);
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.preferences, rootKey);

    final PreferenceScreen preferenceScreen = getPreferenceScreen();
    setListenerRecursive(preferenceScreen, getKey(UI_HOME_SCREEN_SHORTCUTS).equals(rootKey) ?
        homeScreenShortcutPrefClickHandler : this);
    unsetIconSpaceReservedRecursive(preferenceScreen);

    if (rootKey == null) { //ROOT screen
      requirePreference(HOME_CURRENCY).setOnPreferenceChangeListener(this);
      requirePreference(UI_WEB).setOnPreferenceChangeListener(this);

      requirePreference(RESTORE).setTitle(getString(R.string.pref_restore_title) + " (ZIP)");

      Preference restoreLegacyPref = requirePreference(RESTORE_LEGACY);
      if (Utils.hasApiLevel(Build.VERSION_CODES.KITKAT)) {
        restoreLegacyPref.setVisible(false);
      } else {
        restoreLegacyPref.setTitle(getString(R.string.pref_restore_title) + " (" + getString(R.string.pref_restore_alternative) + ")");
      }

      this.<LocalizedFormatEditTextPreference>requirePreference(CUSTOM_DECIMAL_FORMAT).setOnValidationErrorListener(this);

      this.<LocalizedFormatEditTextPreference>requirePreference(CUSTOM_DATE_FORMAT).setOnValidationErrorListener(this);

      setAppDirSummary();

      Preference qifPref = requirePreference(IMPORT_QIF);
      qifPref.setSummary(getString(R.string.pref_import_summary, "QIF"));
      qifPref.setTitle(getString(R.string.pref_import_title, "QIF"));
      Preference csvPref = requirePreference(IMPORT_CSV);
      csvPref.setSummary(getString(R.string.pref_import_summary, "CSV"));
      csvPref.setTitle(getString(R.string.pref_import_title, "CSV"));

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
            requirePreference(MANAGE_STALE_IMAGES).setVisible(true);
        }
      }.execute();

      final PreferenceCategory privacyCategory = requirePreference(CATEGORY_PRIVACY);
      if (!DistributionHelper.getDistribution().getSupportsTrackingAndCrashReporting()) {
        privacyCategory.removePreference(requirePreference(TRACKING));
        privacyCategory.removePreference(requirePreference(CRASHREPORT_SCREEN));
      }
      if (adHandlerFactory.isAdDisabled() || !adHandlerFactory.isRequestLocationInEeaOrUnknown()) {
        privacyCategory.removePreference(requirePreference(PERSONALIZED_AD_CONSENT));
      }
      if (privacyCategory.getPreferenceCount() == 0) {
        preferenceScreen.removePreference(privacyCategory);
      }

      ListPreference languagePref = requirePreference(UI_LANGUAGE);
      if (Utils.hasApiLevel(Build.VERSION_CODES.JELLY_BEAN_MR1)) {
        languagePref.setEntries(getLocaleArray());
      } else {
        languagePref.setVisible(false);
      }

      currencyViewModel.getCurrencies().observe(this, currencies -> {
        ListPreference homeCurrencyPref = requirePreference(PrefKey.HOME_CURRENCY);
        homeCurrencyPref.setEntries(Stream.of(currencies).map(Currency::toString).toArray(CharSequence[]::new));
        homeCurrencyPref.setEntryValues(Stream.of(currencies).map(Currency::getCode).toArray(CharSequence[]::new));
        homeCurrencyPref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
      });

      final int translatorsArrayResId = getTranslatorsArrayResId();
      if (translatorsArrayResId != 0) {
        String[] translatorsArray = getResources().getStringArray(translatorsArrayResId);
        final String translators;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          //noinspection RedundantCast
          translators = ListFormatter.getInstance().format((Object[]) translatorsArray);
        } else {
          translators = TextUtils.join(", ", translatorsArray);
        }
        requirePreference(TRANSLATION).setSummary(String.format("%s: %s", getString(R.string.translated_by), translators));
      }

      if (!featureManager.allowsUninstall()) {
        requirePreference(FEATURE_UNINSTALL).setVisible(false);
      }
    }
    //SHORTCUTS screen
    else if (rootKey.equals(getKey(UI_HOME_SCREEN_SHORTCUTS))) {
      Preference shortcutSplitPref = requirePreference(SHORTCUT_CREATE_SPLIT);
      shortcutSplitPref.setEnabled(licenceHandler.isContribEnabled());
      shortcutSplitPref.setSummary(
          getString(R.string.pref_shortcut_summary) + " " +
              ContribFeature.SPLIT_TRANSACTION.buildRequiresString(requireActivity()));

    }
    //Password screen
    else if (rootKey.equals(getKey(PERFORM_PROTECTION_SCREEN))) {
      setProtectionDependentsState();
      Preference preferenceLegacy = requirePreference(PROTECTION_LEGACY);
      Preference preferenceSecurityQuestion = requirePreference(SECURITY_QUESTION);
      Preference preferenceDeviceLock = requirePreference(PROTECTION_DEVICE_LOCK_SCREEN);
      if (Utils.hasApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
        final PreferenceCategory preferenceCategory = new PreferenceCategory(requireContext());
        preferenceCategory.setTitle(R.string.feature_deprecated);
        preferenceScreen.addPreference(preferenceCategory);
        preferenceScreen.removePreference(preferenceLegacy);
        preferenceScreen.removePreference(preferenceSecurityQuestion);
        preferenceCategory.addPreference(preferenceLegacy);
        preferenceCategory.addPreference(preferenceSecurityQuestion);
        preferenceDeviceLock.setOnPreferenceChangeListener(this);
      } else {
        preferenceDeviceLock.setVisible(false);
      }
    }
    //SHARE screen
    else if (rootKey.equals(getKey(PERFORM_SHARE))) {
      Preference sharePref = requirePreference(SHARE_TARGET);
      //noinspection AuthLeak
      sharePref.setSummary(getString(R.string.pref_share_target_summary) + ":\n" +
          "ftp: \"ftp://login:password@my.example.org:port/my/directory/\"\n" +
          "mailto: \"mailto:john@my.example.com\"");
      sharePref.setOnPreferenceChangeListener(this);
    }
    //BACKUP screen
    else if (rootKey.equals(getKey(AUTO_BACKUP))) {
      requirePreference(AUTO_BACKUP_INFO).setSummary(getString(R.string.pref_auto_backup_summary) + " " +
          ContribFeature.AUTO_BACKUP.buildRequiresString(requireActivity()));
      requirePreference(AUTO_BACKUP_CLOUD).setOnPreferenceChangeListener(storeInDatabaseChangeListener);
    }
    //GROUP start screen
    else if (rootKey.equals(getKey(GROUPING_START_SCREEN))) {
      ListPreference startPref = requirePreference(GROUP_WEEK_STARTS);
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
      if (!prefHandler.isSet(GROUP_WEEK_STARTS)) {
        startPref.setValue(String.valueOf(Utils.getFirstDayOfWeek(locale)));
      }

      startPref = requirePreference(GROUP_MONTH_STARTS);
      String[] daysEntries = new String[31], daysValues = new String[31];
      for (int i = 1; i <= 31; i++) {
        daysEntries[i - 1] = Utils.toLocalizedString(i);
        daysValues[i - 1] = String.valueOf(i);
      }
      startPref.setEntries(daysEntries);
      startPref.setEntryValues(daysValues);
    } else if (rootKey.equals(getKey(CRASHREPORT_SCREEN))) {
      requirePreference(ACRA_INFO).setSummary(Utils.getTextWithAppName(getContext(), R.string.crash_reports_user_info));
      requirePreference(CRASHREPORT_ENABLED).setOnPreferenceChangeListener(this);
      requirePreference(CRASHREPORT_USEREMAIL).setOnPreferenceChangeListener(this);
    } else if (rootKey.equals(getKey(OCR))) {
      if ("".equals(prefHandler.getString(OCR_TOTAL_INDICATORS, ""))) {
        this.<EditTextPreference>requirePreference(OCR_TOTAL_INDICATORS).setText(getString(R.string.pref_ocr_total_indicators_default));
      }
      EditTextPreference ocrDatePref = requirePreference(OCR_DATE_FORMATS);
      ocrDatePref.setOnPreferenceChangeListener(this);
      if ("".equals(prefHandler.getString(OCR_DATE_FORMATS, ""))) {
        String shortFormat = getLocalizedDateTimePattern(SHORT, null, IsoChronology.INSTANCE, userLocaleProvider.getSystemLocale());
        String mediumFormat = getLocalizedDateTimePattern(MEDIUM, null, IsoChronology.INSTANCE, userLocaleProvider.getSystemLocale());
        ocrDatePref.setText(shortFormat + "\n" + mediumFormat);
      }
      EditTextPreference ocrTimePref = requirePreference(OCR_TIME_FORMATS);
      ocrTimePref.setOnPreferenceChangeListener(this);
      if ("".equals(prefHandler.getString(OCR_TIME_FORMATS, ""))) {
        String shortFormat = getLocalizedDateTimePattern(null, SHORT, IsoChronology.INSTANCE, userLocaleProvider.getSystemLocale());
        String mediumFormat = getLocalizedDateTimePattern(null, MEDIUM, IsoChronology.INSTANCE, userLocaleProvider.getSystemLocale());
        ocrTimePref.setText(shortFormat + "\n" + mediumFormat);
      }
      this.<ListPreference>requirePreference(OCR_ENGINE).setVisible(activity().ocrViewModel.shouldShowEngineSelection());
      configureTesseractLanguagePref();
    } else if (rootKey.equals(getKey(SYNC))) {
      requirePreference(MANAGE_SYNC_BACKENDS).setSummary(
          getString(R.string.pref_manage_sync_backends_summary,
              Stream.of(ServiceLoader.load(getContext()))
                  .map(SyncBackendProviderFactory::getLabel)
                  .collect(Collectors.joining(", "))) +
              " " + ContribFeature.SYNCHRONIZATION.buildRequiresString(requireActivity()));
      requirePreference(SYNC_NOTIFICATION).setOnPreferenceChangeListener(storeInDatabaseChangeListener);
      requirePreference(SYNC_WIFI_ONLY).setOnPreferenceChangeListener(storeInDatabaseChangeListener);
    } else if (rootKey.equals(getKey(FEATURE_UNINSTALL))) {
      configureUninstallPrefs();
    } else if (rootKey.equals(getKey(EXCHANGE_RATES))) {
      requirePreference(EXCHANGE_RATE_PROVIDER).setOnPreferenceChangeListener(this);
      configureOpenExchangeRatesPreference(prefHandler.getString(PrefKey.EXCHANGE_RATE_PROVIDER, "RATESAPI"));
    }
  }

  private int getTranslatorsArrayResId() {
    Locale locale = Locale.getDefault();
    String language = locale.getLanguage().toLowerCase(Locale.US);
    String country = locale.getCountry().toLowerCase(Locale.US);
    return activity().getTranslatorsArrayResId(language, country);
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
    }
  }

  /**
   * Configures the current screen with a Master Switch, if it has the given key
   * if we are on the root screen, the preference summary for the given key is updated with the
   * current value (On/Off)
   *
   * @param prefKey PrefKey of screen
   * @return true if we have handle the given key as a subScreen
   */
  private boolean handleScreenWithMasterSwitch(final PrefKey prefKey) {
    PreferenceScreen screen = getPreferenceScreen();
    final ActionBar actionBar = activity().getSupportActionBar();
    final boolean status = prefHandler.getBoolean(prefKey, false);
    if (matches(screen, prefKey)) {
      //noinspection InflateParams
      SwitchCompat actionBarSwitch = (SwitchCompat) requireActivity().getLayoutInflater().inflate(
          R.layout.pref_master_switch, null);
      actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
          ActionBar.DISPLAY_SHOW_CUSTOM);
      actionBar.setCustomView(actionBarSwitch);
      actionBarSwitch.setChecked(status);
      actionBarSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
        //TODO factor out to call site
        if (prefKey.equals(AUTO_BACKUP)) {
          if (isChecked && !licenceHandler.hasAccessTo(ContribFeature.AUTO_BACKUP)) {
            activity().showContribDialog(ContribFeature.AUTO_BACKUP, null);
            if (ContribFeature.AUTO_BACKUP.usagesLeft(prefHandler) <= 0) {
              buttonView.setChecked(false);
              return;
            }
          }
        }
        prefHandler.putBoolean(prefKey, isChecked);
        updateDependents(isChecked);
      });
      updateDependents(status);
      return true;
    } else if (matches(screen, ROOT_SCREEN)) {
      setOnOffSummary(prefKey);
    }
    return false;
  }

  private void setOnOffSummary(PrefKey prefKey) {
    setOnOffSummary(prefKey, prefHandler.getBoolean(prefKey, false));
  }

  private void setOnOffSummary(PrefKey key, boolean status) {
    requirePreference(key).setSummary(status ? getString(R.string.switch_on_text) :
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
    }
    else if (matches(pref, UI_WEB)) {
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

  private void configureOpenExchangeRatesPreference(String provider) {
    requirePreference(PrefKey.OPEN_EXCHANGE_RATES_APP_ID).setEnabled(provider.equals("OPENEXCHANGERATES"));
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

  private void setAppDirSummary() {
    Preference pref = requirePreference(APP_DIR);
    if (AppDirHelper.isExternalStorageAvailable()) {
      DocumentFile appDir = AppDirHelper.getAppDir(getActivity());
      if (appDir != null) {
        if (AppDirHelper.isWritableDirectory(appDir)) {
          pref.setSummary(FileUtils.getPath(getActivity(), appDir.getUri()));
        } else {
          pref.setSummary(getString(R.string.app_dir_not_accessible,
              FileUtils.getPath(requireApplication(), appDir.getUri())));
        }
      } else {
        pref.setSummary(R.string.io_error_appdir_null);
      }
    } else {
      pref.setSummary(R.string.external_storage_unavailable);
      pref.setEnabled(false);
    }
  }

  private Bitmap getBitmapForShortcut(int iconIdLegacy, int iconIdLollipop) {
    if (Utils.hasApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
      return UiUtils.drawableToBitmap(ResourcesCompat.getDrawable(getResources(), iconIdLollipop, null));
    } else {
      return UiUtils.getTintedBitmapForTheme(getActivity(), iconIdLegacy, R.style.DarkBackground);
    }
  }

  // credits Financisto
  // src/ru/orangesoftware/financisto/activity/PreferencesActivity.java
  private void addShortcut(int nameId, int operationType, Bitmap bitmap) {
    Intent shortcutIntent = ShortcutHelper.createIntentForNewTransaction(requireContext(), operationType);

    Intent intent = new Intent();
    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(nameId));
    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
    intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");

    if (Utils.isIntentReceiverAvailable(requireActivity(), intent)) {
      requireActivity().sendBroadcast(intent);
      activity().showSnackbar(getString(R.string.pref_shortcut_added));
    } else {
      activity().showSnackbar(getString(R.string.pref_shortcut_not_added));
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
        activity().showSnackbar(R.string.auto_backup_cloud_create_backend);
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
        setAppDirSummary();
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
      setAppDirSummary();
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

  public void updateHomeCurrency(String currencyCode) {
    final MyPreferenceActivity activity = ((MyPreferenceActivity) getActivity());
    if (activity != null) {
      final ListPreference preference = findPreference(HOME_CURRENCY);
      if (preference != null) {
        preference.setValue(currencyCode);
      } else {
        prefHandler.putString(HOME_CURRENCY, currencyCode);
      }
      activity.invalidateHomeCurrency();
      activity.startTaskExecution(TaskExecutionFragment.TASK_RESET_EQUIVALENT_AMOUNTS,
          null, null, R.string.progress_dialog_saving);
    }
  }
}
