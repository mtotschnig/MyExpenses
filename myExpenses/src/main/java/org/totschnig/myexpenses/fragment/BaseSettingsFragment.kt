package org.totschnig.myexpenses.fragment

import android.appwidget.AppWidgetProvider
import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.icu.text.ListFormatter
import android.os.Build
import android.os.Bundle
import android.text.TextUtils.isEmpty
import android.text.TextUtils.join
import androidx.annotation.DrawableRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreferenceCompat
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.exception.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.AccountPreference
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference.OnValidationErrorListener
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.requireString
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.service.DailyScheduler
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.ServiceLoader
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import org.totschnig.myexpenses.util.setNightMode
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.viewmodel.WebUiViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.widget.AccountWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import org.totschnig.myexpenses.widget.WIDGET_CONTEXT_CHANGED
import org.totschnig.myexpenses.widget.updateWidgets
import java.text.DateFormatSymbols
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject

abstract class BaseSettingsFragment : PreferenceFragmentCompat(), OnValidationErrorListener,
    OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    @Inject
    lateinit var featureManager: FeatureManager

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    @Inject
    lateinit var settings: SharedPreferences

    @Inject
    lateinit var licenceHandler: LicenceHandler

    @Inject
    lateinit var adHandlerFactory: AdHandlerFactory

    private val webUiViewModel: WebUiViewModel by viewModels()
    val currencyViewModel: CurrencyViewModel by viewModels()
    val viewModel: SettingsViewModel by viewModels()

    //TODO: these settings need to be authoritatively stored in Database, instead of just mirrored
    val storeInDatabaseChangeListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
            (activity as? MyPreferenceActivity)?.let { activity ->
                activity.showSnackbarIndefinite(R.string.saving)
                viewModel.storeSetting(preference.key, newValue.toString())
                    .observe(this@BaseSettingsFragment, { result ->
                        activity.dismissSnackbar()
                        if ((!result)) activity.showSnackbar("ERROR")
                    })
                true
            } ?: false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        with((requireActivity().application as MyApplication).appComponent) {
            inject(currencyViewModel)
            inject(viewModel)
            super.onCreate(savedInstanceState)
            inject(this@BaseSettingsFragment)
        }
        viewModel.appDirInfo.observe(this) { result ->
            val pref = requirePreference<Preference>(PrefKey.APP_DIR)
            result.onSuccess { appDirInfo ->
                pref.summary = if (appDirInfo.second) {
                    appDirInfo.first
                } else {
                    getString(R.string.app_dir_not_accessible, appDirInfo.first)
                }
            }.onFailure {
                pref.setSummary(
                    when (it) {
                        is ExternalStorageNotAvailableException -> R.string.external_storage_unavailable
                        else -> {
                            pref.isEnabled = false
                            R.string.io_error_appdir_null
                        }
                    }
                )
            }
        }
        webUiViewModel.getServiceState().observe(this) { result ->
            findPreference<SwitchPreferenceCompat>(PrefKey.UI_WEB)?.let { preference ->
                result.onSuccess { serverAddress ->
                    serverAddress?.let { preference.summaryOn = it }
                    if (preference.isChecked && serverAddress == null) {
                        preference.isChecked = false
                    }
                }.onFailure {
                    if (preference.isChecked) preference.isChecked = false
                    activity().showSnackbar(it.message ?: "ERROR")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (featureManager.isFeatureInstalled(Feature.WEBUI, requireContext())) {
            bindToWebUiService()
        }
    }

    fun bindToWebUiService() {
        webUiViewModel.bind(requireContext())
    }

    fun activateWebUi() {
        findPreference<SwitchPreferenceCompat>(PrefKey.UI_WEB)?.isChecked = true
    }

    override fun onStop() {
        super.onStop()
        if (featureManager.isFeatureInstalled(Feature.WEBUI, requireContext())) {
            webUiViewModel.unbind(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        settings.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        settings.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onValidationError(messageResId: Int) {
        activity().showSnackbar(messageResId)
    }

    fun activity() = activity as MyPreferenceActivity

    fun configureUninstallPrefs() {
        configureMultiSelectListPref(
            PrefKey.FEATURE_UNINSTALL_FEATURES, featureManager.installedFeatures(),
            featureManager::uninstallFeatures
        ) { module ->
            Feature.fromModuleName(module)?.let { getString(it.labelResId) } ?: module
        }
        configureMultiSelectListPref(
            PrefKey.FEATURE_UNINSTALL_LANGUAGES,
            featureManager.installedLanguages(),
            featureManager::uninstallLanguages
        ) { language ->
            Locale(language).let { it.getDisplayName(it) }
        }
    }

    private fun configureMultiSelectListPref(
        prefKey: PrefKey,
        entries: Set<String>,
        action: (Set<String>) -> Unit,
        prettyPrint: (String) -> String
    ) {
        (requirePreference(prefKey) as? MultiSelectListPreference)?.apply {
            if (entries.isEmpty()) {
                isEnabled = false
            } else {
                setOnPreferenceChangeListener { _, newValue ->
                    @Suppress("UNCHECKED_CAST")
                    (newValue as? Set<String>)?.let { action(it) }
                    false
                }
                setEntries(entries.map(prettyPrint).toTypedArray())
                entryValues = entries.toTypedArray()
            }
        }
    }

    fun <T : Preference> findPreference(prefKey: PrefKey): T? =
        findPreference(prefHandler.getKey(prefKey))

    fun <T : Preference> requirePreference(prefKey: PrefKey): T {
        return findPreference(prefKey)
            ?: throw IllegalStateException("Preference not found")
    }

    fun getLocaleArray() =
        requireContext().resources.getStringArray(R.array.pref_ui_language_values)
            .map(this::getLocaleDisplayName)
            .toTypedArray()

    private fun getLocaleDisplayName(localeString: CharSequence) =
        if (localeString == "default") {
            requireContext().getString(R.string.pref_ui_language_default)
        } else {
            val localeParts = localeString.split("-")
            val locale = if (localeParts.size == 2)
                Locale(localeParts[0], localeParts[1])
            else
                Locale(localeParts[0])
            locale.getDisplayName(locale)
        }

    fun configureOcrEnginePrefs() {
        val tesseract = requirePreference<ListPreference>(PrefKey.TESSERACT_LANGUAGE)
        val mlkit = requirePreference<ListPreference>(PrefKey.MLKIT_SCRIPT)
        activity().ocrViewModel.configureOcrEnginePrefs(tesseract, mlkit)
    }

    fun requireApplication(): MyApplication {
        return (requireActivity().application as MyApplication)
    }

    fun handleContrib(prefKey: PrefKey, feature: ContribFeature, preference: Preference) =
        if (matches(preference, prefKey)) {
            if (licenceHandler.hasAccessTo(feature)) {
                activity().contribFeatureCalled(feature, null)
            } else {
                activity().showContribDialog(feature, null)
            }
            true
        } else false

    fun matches(preference: Preference, prefKey: PrefKey) =
        prefHandler.getKey(prefKey) == preference.key

    fun getKey(prefKey: PrefKey): String {
        return prefHandler.getKey(prefKey)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        if (key == getKey(PrefKey.UI_LANGUAGE)) {
            featureManager.requestLocale(activity())
        } else if ((key == getKey(PrefKey.GROUP_MONTH_STARTS) ||
                    key == getKey(PrefKey.GROUP_WEEK_STARTS) || key == getKey(PrefKey.CRITERION_FUTURE))
        ) {
            activity().rebuildDbConstants()
        } else if (key == getKey(PrefKey.UI_FONTSIZE)) {
            updateAllWidgets()
            activity().recreate()
        } else if (key == getKey(PrefKey.PROTECTION_LEGACY) || key == getKey(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN)) {
            if (sharedPreferences.getBoolean(key, false)) {
                activity().showSnackbar(R.string.pref_protection_screenshot_information)
                if (prefHandler.getBoolean(PrefKey.AUTO_BACKUP, false)) {
                    activity().showUnencryptedBackupWarning()
                }
            }
            setProtectionDependentsState()
            updateAllWidgets()
        } else if (key == getKey(PrefKey.UI_THEME_KEY)) {
            setNightMode(prefHandler, requireContext())
        } else if (key == getKey(PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET)) {
            //Log.d("DEBUG","shared preference changed: Account Widget");
            updateWidgetsForClass(AccountWidget::class.java)
        } else if (key == getKey(PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET)) {
            //Log.d("DEBUG","shared preference changed: Template Widget");
            updateWidgetsForClass(TemplateWidget::class.java)
        } else if (key == getKey(PrefKey.AUTO_BACKUP)) {
            if ((sharedPreferences.getBoolean(
                    key,
                    false
                ) && ((prefHandler.getBoolean(
                    PrefKey.PROTECTION_LEGACY,
                    false
                ) || prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false))))
            ) {
                activity().showUnencryptedBackupWarning()
            }
            DailyScheduler.updateAutoBackupAlarms(activity())
        } else if (key == getKey(PrefKey.AUTO_BACKUP_TIME)) {
            DailyScheduler.updateAutoBackupAlarms(activity())
        } else if (key == getKey(PrefKey.SYNC_FREQUCENCY)) {
            for (account in GenericAccountService.getAccounts(activity())) {
                ContentResolver.addPeriodicSync(
                    account, TransactionProvider.AUTHORITY, Bundle.EMPTY,
                    prefHandler.getInt(
                        PrefKey.SYNC_FREQUCENCY,
                        GenericAccountService.DEFAULT_SYNC_FREQUENCY_HOURS
                    ).toLong() * GenericAccountService.HOUR_IN_SECONDS
                )
            }
        } else if (key == getKey(PrefKey.TRACKING)) {
            activity().setTrackingEnabled(sharedPreferences.getBoolean(key, false))
        } else if (key == getKey(PrefKey.PLANNER_EXECUTION_TIME)) {
            DailyScheduler.updatePlannerAlarms(activity(), false, false)
        } else if (key == getKey(PrefKey.TESSERACT_LANGUAGE)) {
            activity().checkTessDataDownload()
        } else if (key == getKey(PrefKey.OCR_ENGINE)) {
            if (!featureManager.isFeatureInstalled(Feature.OCR, activity())) {
                featureManager.requestFeature(Feature.OCR, activity())
            }
            configureOcrEnginePrefs()
        }
    }

    private fun updateAllWidgets() {
        updateWidgetsForClass(AccountWidget::class.java)
        updateWidgetsForClass(TemplateWidget::class.java)
    }

    private fun updateWidgetsForClass(provider: Class<out AppWidgetProvider>) {
        updateWidgets(activity(), provider, WIDGET_CONTEXT_CHANGED)
    }

    fun setProtectionDependentsState() {
        if (matches(preferenceScreen, PrefKey.ROOT_SCREEN) || matches(
                preferenceScreen,
                PrefKey.PERFORM_PROTECTION_SCREEN
            )
        ) {
            val isLegacy = prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false)
            val isProtected =
                isLegacy || prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)
            requirePreference<Preference>(PrefKey.SECURITY_QUESTION).isEnabled = isLegacy
            requirePreference<Preference>(PrefKey.PROTECTION_DELAY_SECONDS).isEnabled = isProtected
            requirePreference<Preference>(PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET).isEnabled =
                isProtected
            requirePreference<Preference>(PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET).isEnabled =
                isProtected
            requirePreference<Preference>(PrefKey.PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET).isEnabled =
                isProtected
        }
    }

    fun getKeyInfo(): String {
        return "${
            prefHandler.getString(
                PrefKey.LICENCE_EMAIL,
                ""
            )
        }: ${prefHandler.getString(PrefKey.NEW_LICENCE, "")}"
    }

    fun loadSyncAccountData() {
        requirePreference<AccountPreference>(PrefKey.AUTO_BACKUP_CLOUD).setData(requireContext())
    }

    fun configureContribPrefs() {
        if (!matches(preferenceScreen, PrefKey.ROOT_SCREEN)) {
            return
        }
        val contribPurchasePref = requirePreference<Preference>(PrefKey.CONTRIB_PURCHASE)
        val licenceKeyPref = findPreference<Preference>(PrefKey.NEW_LICENCE)
        if (licenceHandler.needsKeyEntry) {
            licenceKeyPref?.let {
                if (licenceHandler.hasValidKey()) {
                    it.title = getKeyInfo()
                    it.summary = TextUtils.concatResStrings(
                        requireActivity(), " / ",
                        R.string.button_validate, R.string.menu_remove
                    )
                } else {
                    it.setTitle(R.string.pref_enter_licence_title)
                    it.setSummary(R.string.pref_enter_licence_summary)
                }
            }
        } else {
            licenceKeyPref?.isVisible = false
        }
        val contribPurchaseTitle: String = licenceHandler.prettyPrintStatus(requireContext())
            ?: getString(R.string.pref_contrib_purchase_title) + (if (licenceHandler.doesUseIAP)
                " (${getString(R.string.pref_contrib_purchase_title_in_app)})" else "")
        var contribPurchaseSummary: String
        val licenceStatus = licenceHandler.licenceStatus
        if (licenceStatus == null && licenceHandler.addOnFeatures.isEmpty()) {
            contribPurchaseSummary = getString(R.string.pref_contrib_purchase_summary)
        } else {
            contribPurchaseSummary = if (licenceStatus?.isUpgradeable != false) {
                getString(R.string.pref_contrib_purchase_title_upgrade)
            } else {
                licenceHandler.getProLicenceAction(requireContext())
            }
            if (!isEmpty(contribPurchaseSummary)) {
                contribPurchaseSummary += "\n"
            }
            contribPurchaseSummary += getString(R.string.thank_you)
        }
        contribPurchasePref.summary = contribPurchaseSummary
        contribPurchasePref.title = contribPurchaseTitle
    }

    fun updateHomeCurrency(currencyCode: String) {
        (activity as? MyPreferenceActivity)?.let { activity ->
            findPreference<ListPreference>(PrefKey.HOME_CURRENCY)?.let {
                it.value = currencyCode
            } ?: run {
                prefHandler.putString(PrefKey.HOME_CURRENCY, currencyCode)
            }
            activity.invalidateHomeCurrency()
            activity.showSnackbarIndefinite(R.string.saving)
            viewModel.resetEquivalentAmounts().observe(this, { integer ->
                activity.dismissSnackbar()
                if (integer != null) {
                    activity.showSnackbar(
                        String.format(
                            resources.configuration.locale,
                            "%s (%d)", getString(R.string.reset_equivalent_amounts_success), integer
                        )
                    )
                } else {
                    activity.showSnackbar("Equivalent amount reset failed")
                }
            })
        }
    }

    fun trackPreferenceClick(preference: Preference) {
        val bundle = Bundle()
        bundle.putString(Tracker.EVENT_PARAM_ITEM_ID, preference.key)
        activity().logEvent(Tracker.EVENT_PREFERENCE_CLICK, bundle)
    }

    private fun setListenerRecursive(
        preferenceGroup: PreferenceGroup,
        listener: Preference.OnPreferenceClickListener
    ) {
        for (i in 0 until preferenceGroup.preferenceCount) {
            val preference = preferenceGroup.getPreference(i)
            if (preference is PreferenceCategory) {
                setListenerRecursive(preference, listener)
            } else {
                preference.onPreferenceClickListener = listener
            }
        }
    }

    private fun unsetIconSpaceReservedRecursive(preferenceGroup: PreferenceGroup) {
        for (i in 0 until preferenceGroup.preferenceCount) {
            val preference = preferenceGroup.getPreference(i)
            if (preference is PreferenceCategory) {
                unsetIconSpaceReservedRecursive(preference)
            }
            preference.isIconSpaceReserved = false
        }
    }

    private val homeScreenShortcutPrefClickHandler =
        Preference.OnPreferenceClickListener { preference: Preference ->
            trackPreferenceClick(preference)
            when {
                matches(preference, PrefKey.SHORTCUT_CREATE_TRANSACTION) -> {
                    addShortcut(
                        R.string.transaction, Transactions.TYPE_TRANSACTION,
                        getBitmapForShortcut(
                            R.drawable.shortcut_create_transaction_icon_lollipop
                        )
                    )
                    true
                }
                matches(preference, PrefKey.SHORTCUT_CREATE_TRANSFER) -> {
                    addShortcut(
                        R.string.transfer, Transactions.TYPE_TRANSFER,
                        getBitmapForShortcut(
                            R.drawable.shortcut_create_transfer_icon_lollipop
                        )
                    )
                    true
                }
                matches(preference, PrefKey.SHORTCUT_CREATE_SPLIT) -> {
                    addShortcut(
                        R.string.split_transaction, Transactions.TYPE_SPLIT,
                        getBitmapForShortcut(
                            R.drawable.shortcut_create_split_icon_lollipop
                        )
                    )
                    true
                }
                else -> false
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        setListenerRecursive(
            preferenceScreen, if (getKey(PrefKey.UI_HOME_SCREEN_SHORTCUTS) == rootKey)
                homeScreenShortcutPrefClickHandler
            else
                this
        )
        unsetIconSpaceReservedRecursive(preferenceScreen)

        if (rootKey == null) { //ROOT screen
            requirePreference<Preference>(PrefKey.HOME_CURRENCY).onPreferenceChangeListener = this
            requirePreference<Preference>(PrefKey.UI_WEB).onPreferenceChangeListener = this

            requirePreference<Preference>(PrefKey.RESTORE).title =
                getString(R.string.pref_restore_title) + " (ZIP)"

            this.requirePreference<LocalizedFormatEditTextPreference>(PrefKey.CUSTOM_DECIMAL_FORMAT).onValidationErrorListener =
                this

            this.requirePreference<LocalizedFormatEditTextPreference>(PrefKey.CUSTOM_DATE_FORMAT).onValidationErrorListener =
                this

            loadAppDirSummary()

            val qifPref = requirePreference<Preference>(PrefKey.IMPORT_QIF)
            qifPref.summary = getString(R.string.pref_import_summary, "QIF")
            qifPref.title = getString(R.string.pref_import_title, "QIF")
            val csvPref = requirePreference<Preference>(PrefKey.IMPORT_CSV)
            csvPref.summary = getString(R.string.pref_import_summary, "CSV")
            csvPref.title = getString(R.string.pref_import_title, "CSV")

            viewModel.hasStaleImages.observe(this) { result ->
                requirePreference<Preference>(PrefKey.MANAGE_STALE_IMAGES).isVisible = result
            }

            val privacyCategory = requirePreference<PreferenceCategory>(PrefKey.CATEGORY_PRIVACY)
            if (!DistributionHelper.distribution.supportsTrackingAndCrashReporting) {
                privacyCategory.removePreference(requirePreference(PrefKey.TRACKING))
                privacyCategory.removePreference(requirePreference(PrefKey.CRASHREPORT_SCREEN))
            }
            if (adHandlerFactory.isAdDisabled || !adHandlerFactory.isRequestLocationInEeaOrUnknown) {
                privacyCategory.removePreference(requirePreference(PrefKey.PERSONALIZED_AD_CONSENT))
            }
            if (privacyCategory.preferenceCount == 0) {
                preferenceScreen.removePreference(privacyCategory)
            }

            val languagePref = requirePreference<ListPreference>(PrefKey.UI_LANGUAGE)
            languagePref.entries = getLocaleArray()

            currencyViewModel.getCurrencies().observe(this) { currencies ->
                with(requirePreference<ListPreference>(PrefKey.HOME_CURRENCY)) {
                    entries = currencies.map(Currency::toString).toTypedArray()
                    entryValues = currencies.map { it.code }.toTypedArray()
                    isEnabled = true
                }
            }

            val translatorsArrayResId = getTranslatorsArrayResId()
            if (translatorsArrayResId != 0) {
                val translatorsArray = resources.getStringArray(translatorsArrayResId)
                val translators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    ListFormatter.getInstance().format(*translatorsArray) else join(
                    ", ",
                    translatorsArray
                )
                requirePreference<Preference>(PrefKey.TRANSLATION).summary =
                    "${getString(R.string.translated_by)}: $translators"
            }

            if (!featureManager.allowsUninstall()) {
                requirePreference<Preference>(PrefKey.FEATURE_UNINSTALL).isVisible = false
            }
            requirePreference<Preference>(PrefKey.AUTO_BACKUP_CLOUD).onPreferenceChangeListener =
                storeInDatabaseChangeListener

            requirePreference<Preference>(PrefKey.NEWS).title =
                "${getString(R.string.pref_news_title)} (Mastodon)"
        } else if (rootKey == getKey(PrefKey.UI_HOME_SCREEN_SHORTCUTS)) {
            val shortcutSplitPref = requirePreference<Preference>(PrefKey.SHORTCUT_CREATE_SPLIT)
            shortcutSplitPref.isEnabled = licenceHandler.isContribEnabled
            shortcutSplitPref.summary = (getString(R.string.pref_shortcut_summary) + " " +
                    ContribFeature.SPLIT_TRANSACTION.buildRequiresString(requireActivity()))

        } else if (rootKey == getKey(PrefKey.PERFORM_PROTECTION_SCREEN)) {
            setProtectionDependentsState()
            val preferenceLegacy = requirePreference<Preference>(PrefKey.PROTECTION_LEGACY)
            val preferenceSecurityQuestion =
                requirePreference<Preference>(PrefKey.SECURITY_QUESTION)
            val preferenceDeviceLock =
                requirePreference<Preference>(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN)
            val preferenceCategory = PreferenceCategory(requireContext())
            preferenceCategory.setTitle(R.string.feature_deprecated)
            preferenceScreen.addPreference(preferenceCategory)
            preferenceScreen.removePreference(preferenceLegacy)
            preferenceScreen.removePreference(preferenceSecurityQuestion)
            preferenceCategory.addPreference(preferenceLegacy)
            preferenceCategory.addPreference(preferenceSecurityQuestion)
            preferenceDeviceLock.onPreferenceChangeListener = this
        } else if (rootKey == getKey(PrefKey.PERFORM_SHARE)) {
            val sharePref = requirePreference<Preference>(PrefKey.SHARE_TARGET)

            sharePref.summary = (getString(R.string.pref_share_target_summary) + ":\n" +
                    "ftp: \"ftp://login:password@my.example.org:port/my/directory/\"\n" +
                    "mailto: \"mailto:john@my.example.com\"")
            sharePref.onPreferenceChangeListener = this
        } else if (rootKey == getKey(PrefKey.AUTO_BACKUP)) {
            requirePreference<Preference>(PrefKey.AUTO_BACKUP_INFO).summary =
                (getString(R.string.pref_auto_backup_summary) + " " +
                        ContribFeature.AUTO_BACKUP.buildRequiresString(requireActivity()))
        } else if (rootKey == getKey(PrefKey.GROUPING_START_SCREEN)) {
            var startPref = requirePreference<ListPreference>(PrefKey.GROUP_WEEK_STARTS)
            val locale = Locale.getDefault()
            val dfs = DateFormatSymbols(locale)
            val entries = arrayOfNulls<String>(7)
            System.arraycopy(dfs.weekdays, 1, entries, 0, 7)
            startPref.entries = entries
            startPref.entryValues = arrayOf(
                (Calendar.SUNDAY).toString(),
                (Calendar.MONDAY).toString(),
                (Calendar.TUESDAY).toString(),
                (Calendar.WEDNESDAY).toString(),
                (Calendar.THURSDAY).toString(),
                (Calendar.FRIDAY).toString(),
                (Calendar.SATURDAY).toString()
            )
            if (!prefHandler.isSet(PrefKey.GROUP_WEEK_STARTS)) {
                startPref.value = (Utils.getFirstDayOfWeek(locale)).toString()
            }

            startPref = requirePreference(PrefKey.GROUP_MONTH_STARTS)
            val daysEntries = arrayOfNulls<String>(31)
            val daysValues = arrayOfNulls<String>(31)
            for (i in 1..31) {
                daysEntries[i - 1] = Utils.toLocalizedString(i)
                daysValues[i - 1] = (i).toString()
            }
            startPref.entries = daysEntries
            startPref.entryValues = daysValues
        } else if (rootKey == getKey(PrefKey.CRASHREPORT_SCREEN)) {
            requirePreference<Preference>(PrefKey.ACRA_INFO).summary = Utils.getTextWithAppName(
                context,
                R.string.crash_reports_user_info
            )
            requirePreference<Preference>(PrefKey.CRASHREPORT_ENABLED).onPreferenceChangeListener =
                this
            requirePreference<Preference>(PrefKey.CRASHREPORT_USEREMAIL).onPreferenceChangeListener =
                this
        } else if (rootKey == getKey(PrefKey.OCR)) {
            if ("" == prefHandler.getString(PrefKey.OCR_TOTAL_INDICATORS, "")) {
                requirePreference<EditTextPreference>(PrefKey.OCR_TOTAL_INDICATORS).text =
                    getString(R.string.pref_ocr_total_indicators_default)
            }
            val ocrDatePref = requirePreference<EditTextPreference>(PrefKey.OCR_DATE_FORMATS)
            ocrDatePref.onPreferenceChangeListener = this
            if ("" == prefHandler.getString(PrefKey.OCR_DATE_FORMATS, "")) {
                val shortFormat = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                    FormatStyle.SHORT,
                    null,
                    IsoChronology.INSTANCE,
                    userLocaleProvider.systemLocale
                )
                val mediumFormat = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                    FormatStyle.MEDIUM,
                    null,
                    IsoChronology.INSTANCE,
                    userLocaleProvider.systemLocale
                )
                ocrDatePref.text = shortFormat + "\n" + mediumFormat
            }
            val ocrTimePref = requirePreference<EditTextPreference>(PrefKey.OCR_TIME_FORMATS)
            ocrTimePref.onPreferenceChangeListener = this
            if ("" == prefHandler.getString(PrefKey.OCR_TIME_FORMATS, "")) {
                val shortFormat = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                    null,
                    FormatStyle.SHORT,
                    IsoChronology.INSTANCE,
                    userLocaleProvider.systemLocale
                )
                val mediumFormat = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                    null,
                    FormatStyle.MEDIUM,
                    IsoChronology.INSTANCE,
                    userLocaleProvider.systemLocale
                )
                ocrTimePref.text = shortFormat + "\n" + mediumFormat
            }
            this.requirePreference<ListPreference>(PrefKey.OCR_ENGINE).isVisible =
                activity().ocrViewModel.shouldShowEngineSelection()
            configureOcrEnginePrefs()
        } else if (rootKey == getKey(PrefKey.SYNC)) {
            requirePreference<Preference>(PrefKey.MANAGE_SYNC_BACKENDS).summary = (getString(
                R.string.pref_manage_sync_backends_summary,
                ServiceLoader.load(context).map { it.label }.joinToString()
            ) +
                    " " + ContribFeature.SYNCHRONIZATION.buildRequiresString(requireActivity()))
            requirePreference<Preference>(PrefKey.SYNC_NOTIFICATION).onPreferenceChangeListener =
                storeInDatabaseChangeListener
            requirePreference<Preference>(PrefKey.SYNC_WIFI_ONLY).onPreferenceChangeListener =
                storeInDatabaseChangeListener
        } else if (rootKey == getKey(PrefKey.FEATURE_UNINSTALL)) {
            configureUninstallPrefs()
        } else if (rootKey == getKey(PrefKey.EXCHANGE_RATES)) {
            requirePreference<Preference>(PrefKey.EXCHANGE_RATE_PROVIDER).onPreferenceChangeListener =
                this
            configureOpenExchangeRatesPreference(
                prefHandler.requireString(
                    PrefKey.EXCHANGE_RATE_PROVIDER,
                    "EXCHANGE_RATE_HOST"
                )
            )
        }
    }

    private fun getTranslatorsArrayResId(): Int {
        val locale = Locale.getDefault()
        val language = locale.language.lowercase(Locale.US)
        val country = locale.country.lowercase(Locale.US)
        return activity().getTranslatorsArrayResId(language, country)
    }

    fun configureOpenExchangeRatesPreference(provider: String) {
        requirePreference<Preference>(PrefKey.OPEN_EXCHANGE_RATES_APP_ID).isEnabled =
            provider == "OPENEXCHANGERATES"
    }

    fun loadAppDirSummary() {
        viewModel.loadAppDirInfo()
    }

    private fun getBitmapForShortcut(@DrawableRes iconId: Int) = UiUtils.drawableToBitmap(
        ResourcesCompat.getDrawable(
            resources,
            iconId,
            null
        )!!
    )

    // credits Financisto
    // src/ru/orangesoftware/financisto/activity/PreferencesActivity.java
    private fun addShortcut(nameId: Int, operationType: Int, bitmap: Bitmap) {
        val shortcutIntent =
            ShortcutHelper.createIntentForNewTransaction(requireContext(), operationType)

        val intent = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(nameId))
            putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap)
            setAction("com.android.launcher.action.INSTALL_SHORTCUT")
        }

        if (Utils.isIntentReceiverAvailable(requireActivity(), intent)) {
            requireActivity().sendBroadcast(intent)
            activity().showSnackbar(getString(R.string.pref_shortcut_added))
        } else {
            activity().showSnackbar(getString(R.string.pref_shortcut_not_added))
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
    fun handleScreenWithMasterSwitch(prefKey: PrefKey): Boolean {
        if (matches(preferenceScreen, prefKey)) {
            activity().supportActionBar?.let { actionBar ->
                val status = prefHandler.getBoolean(prefKey, false)
                val actionBarSwitch = requireActivity().layoutInflater.inflate(
                    R.layout.pref_master_switch, null
                ) as SwitchCompat
                actionBar.setDisplayOptions(
                    ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM
                )
                actionBar.customView = actionBarSwitch
                actionBarSwitch.isChecked = status
                actionBarSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    //TODO factor out to call site
                    if (prefKey == PrefKey.AUTO_BACKUP) {
                        if (isChecked && !licenceHandler.hasAccessTo(ContribFeature.AUTO_BACKUP)) {
                            (activity as? MyPreferenceActivity)?.let {
                                it.showContribDialog(ContribFeature.AUTO_BACKUP, null)
                                if (ContribFeature.AUTO_BACKUP.usagesLeft(prefHandler) <= 0) {
                                    buttonView.isChecked = false
                                    return@setOnCheckedChangeListener
                                }
                            }
                        }
                    }
                    prefHandler.putBoolean(prefKey, isChecked)
                    updateDependents(isChecked)
                }
                updateDependents(status)
            }
            return true
        } else if (matches(preferenceScreen, PrefKey.ROOT_SCREEN)) {
            setOnOffSummary(prefKey)
        }
        return false
    }

    private fun setOnOffSummary(prefKey: PrefKey) {
        setOnOffSummary(prefKey, prefHandler.getBoolean(prefKey, false))
    }

    private fun setOnOffSummary(key: PrefKey, status: Boolean) {
        requirePreference<Preference>(key).summary = getString(
            if (status)
                R.string.switch_on_text
            else
                R.string.switch_off_text
        )
    }

    private fun updateDependents(enabled: Boolean) {
        for (i in 0 until preferenceScreen.preferenceCount) {
            preferenceScreen.getPreference(i).isEnabled = enabled
        }
    }
}