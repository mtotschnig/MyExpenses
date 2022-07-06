package org.totschnig.myexpenses.fragment

import android.app.KeyguardManager
import android.appwidget.AppWidgetProvider
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.icu.text.ListFormatter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils.isEmpty
import android.text.TextUtils.join
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
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
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.Input
import eltos.simpledialogfragment.form.SimpleFormDialog
import eltos.simpledialogfragment.list.CustomListDialog
import eltos.simpledialogfragment.list.SimpleListDialog
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.MyApplication.FEEDBACK_EMAIL
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ContribInfoDialogActivity
import org.totschnig.myexpenses.activity.Help
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.activity.RESTORE_REQUEST
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.HelpDialogFragment
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.exception.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.AccountPreference
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference.OnValidationErrorListener
import org.totschnig.myexpenses.preference.PopupMenuPreference
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.requireString
import org.totschnig.myexpenses.service.DailyScheduler
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.AppDirHelper.getContentUriForFile
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.enumValueOrNull
import org.totschnig.myexpenses.util.io.isConnectedWifi
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.licence.Package
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.util.setNightMode
import org.totschnig.myexpenses.util.tracking.Tracker
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.viewmodel.ShareViewModel
import org.totschnig.myexpenses.viewmodel.ShareViewModel.Companion.parseUri
import org.totschnig.myexpenses.viewmodel.WebUiViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.widget.AccountWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import org.totschnig.myexpenses.widget.WIDGET_CONTEXT_CHANGED
import org.totschnig.myexpenses.widget.updateWidgets
import timber.log.Timber
import java.io.File
import java.net.URI
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.LocalTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject

abstract class BaseSettingsFragment : PreferenceFragmentCompat(), OnValidationErrorListener,
    OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener, OnDialogResultListener {

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

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var crashHandler: CrashHandler

    private val webUiViewModel: WebUiViewModel by viewModels()
    private val currencyViewModel: CurrencyViewModel by viewModels()
    private val viewModel: SettingsViewModel by viewModels()

    //TODO: these settings need to be authoritatively stored in Database, instead of just mirrored
    private val storeInDatabaseChangeListener =
        Preference.OnPreferenceChangeListener { preference, newValue ->
            preferenceActivity.showSnackBarIndefinite(R.string.saving)
            viewModel.storeSetting(preference.key, newValue.toString())
                .observe(this@BaseSettingsFragment) { result ->
                    preferenceActivity.dismissSnackBar()
                    if ((!result)) preferenceActivity.showSnackBar("ERROR")
                }
            true
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.help, menu)
        menu.findItem(R.id.HELP_COMMAND).setShowAsAction(SHOW_AS_ACTION_ALWAYS)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.HELP_COMMAND) {
            when {
                matches(preferenceScreen, PrefKey.PERFORM_SHARE) -> {
                    preferenceActivity.startActionView("https://github.com/mtotschnig/MyExpenses/wiki/FAQ:-Data#what-are-the-different-share-options")
                }
                matches(preferenceScreen, PrefKey.UI_WEB) -> {
                    startActivity(Intent(requireContext(), Help::class.java).apply {
                        putExtra(HelpDialogFragment.KEY_CONTEXT, "WebUI")
                        putExtra(
                            HelpDialogFragment.KEY_TITLE,
                            getString(R.string.title_webui)
                        )
                    })
                }
            }
        }
        return true
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
        if(matches(preferenceScreen, PrefKey.UI_WEB)) {
            webUiViewModel.getServiceState().observe(this) { result ->
                preferenceActivity.supportActionBar?.let { actionBar ->
                    val switch = actionBar.customView as SwitchCompat
                    result.onSuccess { serverAddress ->
                        actionBar.subtitle = serverAddress
                        if (switch.isChecked && serverAddress == null) {
                            switch.isChecked = false
                        }
                    }.onFailure {
                        if (switch.isChecked) switch.isChecked = false
                        preferenceActivity.showSnackBar(it.safeMessage)
                    }
                }
            }
        }
        setHasOptionsMenu(matches(preferenceScreen, PrefKey.PERFORM_SHARE, PrefKey.UI_WEB))
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
        preferenceActivity.showSnackBar(messageResId)
    }

    val preferenceActivity get() = requireActivity() as MyPreferenceActivity

    private fun configureUninstallPrefs() {
        configureMultiSelectListPref(
            PrefKey.FEATURE_UNINSTALL_FEATURES,
            featureManager.installedFeatures().filterTo(HashSet()) { it != "drive" },
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

    private fun <T : Preference> findPreference(prefKey: PrefKey): T? =
        findPreference(prefHandler.getKey(prefKey))

    fun <T : Preference> requirePreference(prefKey: PrefKey): T {
        return findPreference(prefKey)
            ?: throw IllegalStateException("Preference not found")
    }

    private fun getLocaleArray() =
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
        preferenceActivity.ocrViewModel.configureOcrEnginePrefs(tesseract, mlkit)
    }

    fun requireApplication(): MyApplication {
        return (requireActivity().application as MyApplication)
    }

    fun handleContrib(prefKey: PrefKey, feature: ContribFeature, preference: Preference) =
        if (matches(preference, prefKey)) {
            if (licenceHandler.hasAccessTo(feature)) {
                preferenceActivity.contribFeatureCalled(feature, null)
            } else {
                preferenceActivity.showContribDialog(feature, null)
            }
            true
        } else false

    fun matches(preference: Preference, vararg prefKey: PrefKey) =
        prefKey.any { prefHandler.getKey(it) == preference.key }

    fun getKey(prefKey: PrefKey): String {
        return prefHandler.getKey(prefKey)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        when (key) {
            getKey(PrefKey.UI_LANGUAGE) -> {
                featureManager.requestLocale(preferenceActivity)
            }
            getKey(PrefKey.GROUP_MONTH_STARTS), getKey(PrefKey.GROUP_WEEK_STARTS), getKey(PrefKey.CRITERION_FUTURE) -> {
                preferenceActivity.rebuildDbConstants()
            }
            getKey(PrefKey.DB_SAFE_MODE) -> {
                preferenceActivity.rebuildAccountProjection()
            }
            getKey(PrefKey.UI_FONTSIZE) -> {
                updateAllWidgets()
                preferenceActivity.recreate()
            }
            getKey(PrefKey.PROTECTION_LEGACY), getKey(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN) -> {
                if (sharedPreferences.getBoolean(key, false)) {
                    preferenceActivity.showSnackBar(R.string.pref_protection_screenshot_information)
                    if (prefHandler.getBoolean(PrefKey.AUTO_BACKUP, false)) {
                        preferenceActivity.showUnencryptedBackupWarning()
                    }
                }
                setProtectionDependentsState()
                updateAllWidgets()
            }
            getKey(PrefKey.UI_THEME_KEY) -> {
                setNightMode(prefHandler, requireContext())
            }
            getKey(PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET) -> {
                //Log.d("DEBUG","shared preference changed: Account Widget");
                updateWidgetsForClass(AccountWidget::class.java)
            }
            getKey(PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET) -> {
                //Log.d("DEBUG","shared preference changed: Template Widget");
                updateWidgetsForClass(TemplateWidget::class.java)
            }
            getKey(PrefKey.AUTO_BACKUP) -> {
                if ((sharedPreferences.getBoolean(
                        key,
                        false
                    ) && ((prefHandler.getBoolean(
                        PrefKey.PROTECTION_LEGACY,
                        false
                    ) || prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false))))
                ) {
                    preferenceActivity.showUnencryptedBackupWarning()
                }
                DailyScheduler.updateAutoBackupAlarms(preferenceActivity)
            }
            getKey(PrefKey.AUTO_BACKUP_TIME) -> {
                DailyScheduler.updateAutoBackupAlarms(preferenceActivity)
            }
            getKey(PrefKey.SYNC_FREQUCENCY) -> {
                for (account in GenericAccountService.getAccounts(preferenceActivity)) {
                    GenericAccountService.addPeriodicSync(account, prefHandler)
                }
            }
            getKey(PrefKey.TRACKING) -> {
                preferenceActivity.setTrackingEnabled(sharedPreferences.getBoolean(key, false))
            }
            getKey(PrefKey.PLANNER_EXECUTION_TIME) -> {
                DailyScheduler.updatePlannerAlarms(preferenceActivity, false, false)
            }
            getKey(PrefKey.TESSERACT_LANGUAGE) -> {
                preferenceActivity.checkTessDataDownload()
            }
            getKey(PrefKey.OCR_ENGINE) -> {
                if (!featureManager.isFeatureInstalled(Feature.OCR, preferenceActivity)) {
                    featureManager.requestFeature(Feature.OCR, preferenceActivity)
                }
                configureOcrEnginePrefs()
            }
        }
    }

    override fun onPreferenceChange(pref: Preference, value: Any): Boolean {
        when {
            matches(pref, PrefKey.HOME_CURRENCY) -> {
                if (value != prefHandler.getString(PrefKey.HOME_CURRENCY, null)) {
                    MessageDialogFragment.newInstance(
                        getString(R.string.dialog_title_information),
                        concatResStrings(
                            requireContext(),
                            " ",
                            R.string.home_currency_change_warning,
                            R.string.continue_confirmation
                        ),
                        MessageDialogFragment.Button(
                            android.R.string.ok, R.id.CHANGE_COMMAND,
                            value as String
                        ),
                        null, MessageDialogFragment.noButton()
                    ).show(parentFragmentManager, "CONFIRM")
                }
                return false
            }
            matches(pref, PrefKey.SHARE_TARGET) -> {
                val target = value as String
                val uri: URI?
                if (target != "") {
                    uri = parseUri(target)
                    if (uri == null) {
                        preferenceActivity.showSnackBar(
                            getString(
                                R.string.ftp_uri_malformed,
                                target
                            )
                        )
                        return false
                    }
                    val scheme = uri.scheme
                    if (enumValueOrNull<ShareViewModel.Scheme>(scheme.uppercase()) == null) {
                        preferenceActivity.showSnackBar(
                            getString(
                                R.string.share_scheme_not_supported,
                                scheme
                            )
                        )
                        return false
                    }
                    val intent: Intent
                    if (scheme == "ftp") {
                        intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse(target)
                        if (!Utils.isIntentAvailable(requireActivity(), intent)) {
                            preferenceActivity.showDialog(R.id.FTP_DIALOG)
                        }
                    }
                }
            }
            matches(pref, PrefKey.CUSTOM_DECIMAL_FORMAT) -> {
                currencyFormatter.invalidateAll(requireContext().contentResolver)
            }
            matches(pref, PrefKey.EXCHANGE_RATE_PROVIDER) -> {
                configureOpenExchangeRatesPreference((value as String))
            }
            matches(pref, PrefKey.CRASHREPORT_USEREMAIL) -> {
                crashHandler.setUserEmail(value as String)
            }
            matches(pref, PrefKey.CRASHREPORT_ENABLED) -> {
                preferenceActivity.showSnackBar(R.string.app_restart_required)
            }
            matches(pref, PrefKey.OCR_DATE_FORMATS) -> {
                if (!isEmpty(value as String)) {
                    try {
                        for (line in value.lines()) {
                            LocalDate.now().format(DateTimeFormatter.ofPattern(line))
                        }
                    } catch (e: java.lang.Exception) {
                        preferenceActivity.showSnackBar(R.string.date_format_illegal)
                        return false
                    }
                }
            }
            matches(pref, PrefKey.OCR_TIME_FORMATS) -> {
                if (!isEmpty(value as String)) {
                    try {
                        for (line in value.lines()) {
                            LocalTime.now().format(DateTimeFormatter.ofPattern(line))
                        }
                    } catch (e: java.lang.Exception) {
                        preferenceActivity.showSnackBar(R.string.date_format_illegal)
                        return false
                    }
                }
            }
            matches(pref, PrefKey.PROTECTION_DEVICE_LOCK_SCREEN) -> {
                if (value as Boolean) {
                    if (!(requireContext().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardSecure) {
                        preferenceActivity.showDeviceLockScreenWarning()
                        return false
                    } else if (prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false)) {
                        showOnlyOneProtectionWarning(true)
                        return false
                    }
                }
                return true
            }
            matches(pref, PrefKey.UI_WEB) -> {
                return if (value as Boolean) {
                    if (!isConnectedWifi(requireContext())) {
                        preferenceActivity.showSnackBar(getString(R.string.no_network) + " (WIFI)")
                        return false
                    }
                    if (licenceHandler.hasAccessTo(ContribFeature.WEB_UI) && preferenceActivity.featureViewModel.isFeatureAvailable(
                            preferenceActivity,
                            Feature.WEBUI
                        )
                    ) {
                        true
                    } else {
                        preferenceActivity.contribFeatureRequested(ContribFeature.WEB_UI, null)
                        false
                    }
                } else {
                    true
                }
            }
        }
        return true
    }

    protected fun showOnlyOneProtectionWarning(legacyProtectionByPasswordIsActive: Boolean) {
        val lockScreen = getString(R.string.pref_protection_device_lock_screen_title)
        val passWord = getString(R.string.pref_protection_password_title)
        val formatArgs: Array<String> = if (legacyProtectionByPasswordIsActive) arrayOf(
            lockScreen,
            passWord
        ) else arrayOf(passWord, lockScreen)
        //noinspection StringFormatMatches
        preferenceActivity.showSnackBar(
            getString(
                R.string.pref_warning_only_one_protection,
                *formatArgs
            )
        )
    }

    private fun updateAllWidgets() {
        updateWidgetsForClass(AccountWidget::class.java)
        updateWidgetsForClass(TemplateWidget::class.java)
    }

    private fun updateWidgetsForClass(provider: Class<out AppWidgetProvider>) {
        updateWidgets(preferenceActivity, provider, WIDGET_CONTEXT_CHANGED)
    }

    fun setProtectionDependentsState() {
        if (matches(preferenceScreen, PrefKey.ROOT_SCREEN, PrefKey.PERFORM_PROTECTION_SCREEN)) {
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

    private fun getKeyInfo(): String {
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
                    it.summary = concatResStrings(
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
        findPreference<ListPreference>(PrefKey.HOME_CURRENCY)?.let {
            it.value = currencyCode
        } ?: run {
            prefHandler.putString(PrefKey.HOME_CURRENCY, currencyCode)
        }
        preferenceActivity.invalidateHomeCurrency()
        preferenceActivity.showSnackBarIndefinite(R.string.saving)
        viewModel.resetEquivalentAmounts().observe(this) { integer ->
            preferenceActivity.dismissSnackBar()
            if (integer != null) {
                preferenceActivity.showSnackBar(
                    String.format(
                        resources.configuration.locale,
                        "%s (%d)", getString(R.string.reset_equivalent_amounts_success), integer
                    )
                )
            } else {
                preferenceActivity.showSnackBar("Equivalent amount reset failed")
            }
        }
    }

    private fun trackPreferenceClick(preference: Preference) {
        val bundle = Bundle()
        bundle.putString(Tracker.EVENT_PARAM_ITEM_ID, preference.key)
        preferenceActivity.logEvent(Tracker.EVENT_PREFERENCE_CLICK, bundle)
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
    var pickFolderRequestStart: Long = 0

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        setListenerRecursive(
            preferenceScreen, if (getKey(PrefKey.UI_HOME_SCREEN_SHORTCUTS) == rootKey)
                homeScreenShortcutPrefClickHandler
            else
                this
        )
        unsetIconSpaceReservedRecursive(preferenceScreen)

        when (rootKey) {
            null -> { //ROOT screen
                requirePreference<Preference>(PrefKey.HOME_CURRENCY).onPreferenceChangeListener =
                    this
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

                val privacyCategory =
                    requirePreference<PreferenceCategory>(PrefKey.CATEGORY_PRIVACY)
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
            }
            getKey(PrefKey.UI_HOME_SCREEN_SHORTCUTS) -> {
                val shortcutSplitPref = requirePreference<Preference>(PrefKey.SHORTCUT_CREATE_SPLIT)
                shortcutSplitPref.isEnabled = licenceHandler.isContribEnabled
                shortcutSplitPref.summary = (getString(R.string.pref_shortcut_summary) + " " +
                        ContribFeature.SPLIT_TRANSACTION.buildRequiresString(requireActivity()))

            }
            getKey(PrefKey.PERFORM_PROTECTION_SCREEN) -> {
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
            }
            getKey(PrefKey.PERFORM_SHARE) -> {
                with(requirePreference<Preference>(PrefKey.SHARE_TARGET)) {
                    summary = getString(R.string.pref_share_target_summary) + " " +
                            ShareViewModel.Scheme.values().joinToString(
                                separator = ", ", prefix = "(", postfix = ")"
                            ) { it.name.lowercase() }
                    onPreferenceChangeListener = this@BaseSettingsFragment
                }
            }
            getKey(PrefKey.AUTO_BACKUP) -> {
                requirePreference<Preference>(PrefKey.AUTO_BACKUP_INFO).summary =
                    (getString(R.string.pref_auto_backup_summary) + " " +
                            ContribFeature.AUTO_BACKUP.buildRequiresString(requireActivity()))
            }
            getKey(PrefKey.GROUPING_START_SCREEN) -> {
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
            }
            getKey(PrefKey.CRASHREPORT_SCREEN) -> {
                requirePreference<Preference>(PrefKey.ACRA_INFO).summary = Utils.getTextWithAppName(
                    context,
                    R.string.crash_reports_user_info
                )
                requirePreference<Preference>(PrefKey.CRASHREPORT_ENABLED).onPreferenceChangeListener =
                    this
                requirePreference<Preference>(PrefKey.CRASHREPORT_USEREMAIL).onPreferenceChangeListener =
                    this
            }
            getKey(PrefKey.OCR) -> {
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
                    preferenceActivity.ocrViewModel.shouldShowEngineSelection()
                configureOcrEnginePrefs()
            }
            getKey(PrefKey.SYNC) -> {
                requirePreference<Preference>(PrefKey.MANAGE_SYNC_BACKENDS).summary = (getString(
                    R.string.pref_manage_sync_backends_summary,
                    BackendService.allAvailable(requireContext()).joinToString { it.label }
                ) +
                        " " + ContribFeature.SYNCHRONIZATION.buildRequiresString(requireActivity()))
                requirePreference<Preference>(PrefKey.SYNC_NOTIFICATION).onPreferenceChangeListener =
                    storeInDatabaseChangeListener
                requirePreference<Preference>(PrefKey.SYNC_WIFI_ONLY).onPreferenceChangeListener =
                    storeInDatabaseChangeListener
            }
            getKey(PrefKey.FEATURE_UNINSTALL) -> {
                configureUninstallPrefs()
            }
            getKey(PrefKey.EXCHANGE_RATES) -> {
                requirePreference<Preference>(PrefKey.EXCHANGE_RATE_PROVIDER).onPreferenceChangeListener =
                    this
                configureOpenExchangeRatesPreference(
                    prefHandler.requireString(
                        PrefKey.EXCHANGE_RATE_PROVIDER,
                        "EXCHANGE_RATE_HOST"
                    )
                )
            }
            getKey(PrefKey.DEBUG_SCREEN) -> {
                requirePreference<Preference>(PrefKey.CRASHLYTICS_USER_ID).let {
                    if (DistributionHelper.isGithub ||
                        !prefHandler.getBoolean(PrefKey.CRASHREPORT_ENABLED, false)
                    ) {
                        preferenceScreen.removePreference(it)
                    } else {
                        it.summary =
                            prefHandler.getString(PrefKey.CRASHLYTICS_USER_ID, null).toString()
                    }
                }
                viewModel.dataCorrupted().observe(this) {
                    if (it > 0) {
                        with(requirePreference<Preference>(PrefKey.DEBUG_REPAIR_987)) {
                            isVisible = true
                            title = "Repair Corrupted Data ($it)"
                        }
                    }
                }
            }
        }
    }

    fun repairBug987() {
        preferenceActivity.showSnackBar("Repair. Please wait ...", Snackbar.LENGTH_INDEFINITE)
        viewModel.repairBug987().observe(this) {
            preferenceActivity.dismissSnackBar()
            preferenceActivity.showSnackBar("$it split transactions have been repaired.")
        }
    }

    private fun getTranslatorsArrayResId(): Int {
        val locale = Locale.getDefault()
        val language = locale.language.lowercase(Locale.US)
        val country = locale.country.lowercase(Locale.US)
        return preferenceActivity.getTranslatorsArrayResId(language, country)
    }

    private fun configureOpenExchangeRatesPreference(provider: String) {
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
            action = "com.android.launcher.action.INSTALL_SHORTCUT"
        }

        if (Utils.isIntentReceiverAvailable(requireActivity(), intent)) {
            requireActivity().sendBroadcast(intent)
            preferenceActivity.showSnackBar(getString(R.string.pref_shortcut_added))
        } else {
            preferenceActivity.showSnackBar(getString(R.string.pref_shortcut_not_added))
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
            preferenceActivity.supportActionBar?.let { actionBar ->
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
                            preferenceActivity.showContribDialog(ContribFeature.AUTO_BACKUP, null)
                            if (ContribFeature.AUTO_BACKUP.usagesLeft(prefHandler) <= 0) {
                                buttonView.isChecked = false
                                return@setOnCheckedChangeListener
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

    fun reportException(e: Exception) {
        preferenceActivity.showSnackBar(e.safeMessage)
        CrashHandler.report(e)
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        trackPreferenceClick(preference)
        return when {
            matches(preference, PrefKey.CONTRIB_PURCHASE) -> {
                if (licenceHandler.isUpgradeable) {
                    val i = ContribInfoDialogActivity.getIntentFor(preferenceActivity, null)
                    if (DistributionHelper.isGithub) {
                        startActivityForResult(
                            i,
                            CONTRIB_PURCHASE_REQUEST
                        )
                    } else {
                        startActivity(i)
                    }
                } else {
                    val proPackagesForExtendOrSwitch = licenceHandler.proPackagesForExtendOrSwitch
                    if (proPackagesForExtendOrSwitch != null) {
                        if (proPackagesForExtendOrSwitch.size > 1) {
                            (preference as PopupMenuPreference).showPopupMenu(
                                { item ->
                                    contribBuyDo(
                                        proPackagesForExtendOrSwitch[item.itemId],
                                        false
                                    )
                                    true
                                },
                                *proPackagesForExtendOrSwitch.map(licenceHandler::getExtendOrSwitchMessage)
                                    .toTypedArray()
                            )
                        } else {
                            //Currently we assume that if we have only one item, we switch
                            contribBuyDo(proPackagesForExtendOrSwitch[0], true)
                        }
                    }
                }
                true
            }
            matches(preference, PrefKey.SEND_FEEDBACK) -> {
                preferenceActivity.dispatchCommand(R.id.FEEDBACK_COMMAND, null)
                true
            }
            matches(preference, PrefKey.DEBUG_LOG_SHARE) -> {
                viewModel.logData().observe(this) {
                    SimpleListDialog.build().choiceMode(CustomListDialog.MULTI_CHOICE)
                        .title(R.string.pref_debug_logging_share_summary)
                        .items(it)
                        .neg()
                        .pos(android.R.string.ok)
                        .show(this, DIALOG_SHARE_LOGS)
                }
                true
            }
            matches(preference, PrefKey.RATE) -> {
                prefHandler.putLong(PrefKey.NEXT_REMINDER_RATE, -1)
                preferenceActivity.dispatchCommand(R.id.RATE_COMMAND, null)
                true
            }
            matches(preference, PrefKey.MORE_INFO_DIALOG) -> {
                preferenceActivity.showDialog(R.id.MORE_INFO_DIALOG)
                true
            }
            matches(preference, PrefKey.RESTORE) -> {
                startActivityForResult(preference.intent, RESTORE_REQUEST)
                true
            }
            matches(preference, PrefKey.APP_DIR) -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                try {
                    pickFolderRequestStart = System.currentTimeMillis()
                    startActivityForResult(
                        intent,
                        PICK_FOLDER_REQUEST
                    )
                } catch (e: ActivityNotFoundException) {
                    reportException(e)
                }

                true
            }
            handleContrib(PrefKey.IMPORT_CSV, ContribFeature.CSV_IMPORT, preference) -> true
            matches(preference, PrefKey.NEW_LICENCE) -> {
                if (licenceHandler.hasValidKey()) {
                    SimpleDialog.build()
                        .title(R.string.licence_key)
                        .msg(getKeyInfo())
                        .pos(R.string.button_validate)
                        .neg(R.string.menu_remove)
                        .show(
                            this,
                            DIALOG_MANAGE_LICENCE
                        )
                } else {
                    val licenceKey = prefHandler.getString(PrefKey.NEW_LICENCE, "")
                    val licenceEmail = prefHandler.getString(PrefKey.LICENCE_EMAIL, "")
                    SimpleFormDialog.build()
                        .title(R.string.pref_enter_licence_title)
                        .fields(
                            Input.email(KEY_EMAIL)
                                .required().text(licenceEmail),
                            Input.plain(KEY_KEY)
                                .required().hint(R.string.licence_key).text(licenceKey)
                        )
                        .pos(R.string.button_validate)
                        .neut()
                        .show(
                            this,
                            DIALOG_VALIDATE_LICENCE
                        )
                }
                true
            }
            matches(preference, PrefKey.PERSONALIZED_AD_CONSENT) -> {
                preferenceActivity.checkGdprConsent(true)
                true
            }
            matches(preference, PrefKey.DEBUG_REPAIR_987) -> {
                ConfirmationDialogFragment.newInstance(Bundle().apply {
                    putString(
                        ConfirmationDialogFragment.KEY_TITLE_STRING,
                        "Repair Corrupted Data"
                    )
                    putString(
                        ConfirmationDialogFragment.KEY_MESSAGE,
                        "Please create a backup of the database before calling Repair."
                    )
                    putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.REPAIR_COMMAND)
                    putInt(
                        ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                        R.string.button_label_repair
                    )
                })
                    .show(parentFragmentManager, "Repair")
                true
            }
            matches(preference, PrefKey.EXCHANGE_RATES_CLEAR_CACHE) -> {
                viewModel.clearExchangeRateCache().observe(this) {
                    preferenceActivity.showSnackBar("${getString(R.string.clear_cache)} ($it)")
                }
                true
            }
            else -> false
        }
    }

    private fun contribBuyDo(selectedPackage: Package, shouldReplaceExisting: Boolean) {
        startActivity(
            ContribInfoDialogActivity.getIntentFor(
                context,
                selectedPackage,
                shouldReplaceExisting
            )
        )
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        when (dialogTag) {
            DIALOG_VALIDATE_LICENCE -> {
                if (which == OnDialogResultListener.BUTTON_POSITIVE) {
                    prefHandler.putString(
                        PrefKey.NEW_LICENCE,
                        extras.getString(KEY_KEY)!!.trim()
                    )
                    prefHandler.putString(
                        PrefKey.LICENCE_EMAIL,
                        extras.getString(KEY_EMAIL)!!.trim()
                    )
                    preferenceActivity.validateLicence()
                }
            }
            DIALOG_MANAGE_LICENCE -> {
                when (which) {
                    OnDialogResultListener.BUTTON_POSITIVE -> preferenceActivity.validateLicence()
                    OnDialogResultListener.BUTTON_NEGATIVE -> {
                        ConfirmationDialogFragment.newInstance(Bundle().apply {
                            putInt(
                                ConfirmationDialogFragment.KEY_TITLE,
                                R.string.dialog_title_information
                            )
                            putString(
                                ConfirmationDialogFragment.KEY_MESSAGE,
                                getString(R.string.licence_removal_information, 5)
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                                R.string.menu_remove
                            )
                            putInt(
                                ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                                R.id.REMOVE_LICENCE_COMMAND
                            )
                        })
                            .show(parentFragmentManager, "REMOVE_LICENCE")
                    }
                }
            }
            DIALOG_SHARE_LOGS -> {
                if (which == OnDialogResultListener.BUTTON_POSITIVE) {
                    val logDir = File(requireContext().getExternalFilesDir(null), "logs")
                    startActivity(Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, "[${getString(R.string.app_name)}]: Logs")
                        type = "text/plain"
                        val arrayList = ArrayList(
                            extras.getStringArrayList(SimpleListDialog.SELECTED_LABELS)!!.map {
                                getContentUriForFile(
                                    requireContext(),
                                    File(logDir, it)
                                )
                            })
                        Timber.d("ATTACHMENTS" + arrayList.joinToString())
                        putParcelableArrayListExtra(
                            Intent.EXTRA_STREAM,
                            arrayList
                        )
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    })
                }
            }
        }
        return true
    }

    companion object {
        const val DIALOG_VALIDATE_LICENCE = "validateLicence"
        const val DIALOG_MANAGE_LICENCE = "manageLicence"
        const val DIALOG_SHARE_LOGS = "shareLogs"
        const val KEY_EMAIL = "email"
        const val KEY_KEY = "key"
        const val PICK_FOLDER_REQUEST = 2
        private const val CONTRIB_PURCHASE_REQUEST = 3
    }
}