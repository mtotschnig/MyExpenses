package org.totschnig.myexpenses.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import com.evernote.android.state.State
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.SortUtilityDialogFragment
import org.totschnig.myexpenses.dialog.progress.NewProgressDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.fragment.TwoPanePreference
import org.totschnig.myexpenses.fragment.TwoPanePreference.Companion.KEY_INITIAL_SCREEN
import org.totschnig.myexpenses.fragment.preferences.BasePreferenceFragment
import org.totschnig.myexpenses.fragment.preferences.PreferenceDataFragment
import org.totschnig.myexpenses.fragment.preferences.PreferencesAdvancedFragment
import org.totschnig.myexpenses.fragment.preferences.PreferencesBackupRestoreFragment
import org.totschnig.myexpenses.fragment.preferences.PreferencesBackupRestoreFragment.Companion.KEY_CHECKED_FILES
import org.totschnig.myexpenses.fragment.preferences.PreferencesOcrFragment
import org.totschnig.myexpenses.fragment.preferences.PreferencesWebUiFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DYNAMIC_CURRENCIES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.service.AutoBackupWorker
import org.totschnig.myexpenses.service.DailyExchangeRateDownloadService
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.LazyFontSelector
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.config.Configurator
import org.totschnig.myexpenses.util.config.Configurator.Configuration.USE_SET_DECOR_PADDING_WORKAROUND
import org.totschnig.myexpenses.util.config.get
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.ui.setNightMode
import org.totschnig.myexpenses.viewmodel.LicenceValidationViewModel
import org.totschnig.myexpenses.viewmodel.ModalProgressViewModel
import org.totschnig.myexpenses.viewmodel.PriceCalculationViewModel
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.viewmodel.SyncViewModel
import org.totschnig.myexpenses.widget.AccountWidget
import org.totschnig.myexpenses.widget.BudgetWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import org.totschnig.myexpenses.widget.WIDGET_CONTEXT_CHANGED
import org.totschnig.myexpenses.widget.updateWidgets
import timber.log.Timber
import java.io.Serializable
import javax.inject.Inject

class PreferenceActivity : SyncBackendSetupActivity(), ContribIFace, SortUtilityDialogFragment.OnConfirmListener {

    @Inject
    lateinit var configurator: Configurator

    @State
    var resultCode: Int = RESULT_OK

    val viewModel: SettingsViewModel?
        get() = twoPanePreference.getDetailFragment<BasePreferenceFragment>()
            ?.viewModel

    private val licenceValidationViewModel: LicenceValidationViewModel by viewModels()

    private val progressViewModel: ModalProgressViewModel by viewModels()

    private val priceCalculationViewModel: PriceCalculationViewModel by viewModels()

    private val dismissCallback = object : Snackbar.Callback() {
        override fun onDismissed(
            transientBottomBar: Snackbar,
            event: Int
        ) {
            if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION)
                licenceValidationViewModel.messageShown()
        }
    }

    private var initialPrefToShow: String? = null

    val calledFromSystemSettings
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                intent.action == Intent.ACTION_APPLICATION_PREFERENCES

    private fun observeLicenceApiResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                licenceValidationViewModel.result.collect { result ->
                    result?.let {
                        showDismissibleSnackBar(it.second, dismissCallback)
                    }
                }
            }
        }
    }

    fun validateLicence() {
        showSnackBarIndefinite(R.string.progress_validating_licence)
        licenceValidationViewModel.validateLicence()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (configurator[USE_SET_DECOR_PADDING_WORKAROUND, false]) {
            Timber.i("Using DECOR_PADDING_WORKAROUND")
            window.decorView
        }
        super.onCreate(savedInstanceState)
        injector.inject(licenceValidationViewModel)
        injector.inject(priceCalculationViewModel)
        setupWithFragment(savedInstanceState == null, false) {
            TwoPanePreference.newInstance(intent.getStringExtra(KEY_INITIAL_SCREEN))
        }
        setupToolbar()
        title = getString(R.string.settings_label)
        observeLicenceApiResult()
        if (calledFromSystemSettings) {
            // Get the Application instance and call the method
            (applicationContext as? MyApplication)?.signalInitialLaunchForSystemPreferences()
        }
    }

    override fun injectDependencies() {
        injector.inject(this)
    }

    override fun setTitle(title: CharSequence?) {
        supportActionBar!!.title = title
    }

    override fun onCreateOptionsMenu(menu: Menu) = false

    val twoPanePreference: TwoPanePreference
        get() = supportFragmentManager.findFragmentById(R.id.fragment_container) as TwoPanePreference

    override fun doHome() {
        if (!twoPanePreference.doHome()) {
            setResult(resultCode)
            finish()
        }
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) true
        else when (command) {

            R.id.CHANGE_COMMAND -> {
                val currencyCode = tag as String
                supportFragmentManager.beginTransaction()
                    .add(
                        NewProgressDialogFragment.newInstance(
                            getString(R.string.pref_home_currency_title) + ": " + currencyContext[currencyCode].description
                        ).apply { isCancelable = false },
                        PROGRESS_TAG
                    )
                    .commitNow()
                lifecycleScope.launch {
                    progressViewModel.appendToMessage(TextUtils.concatResStrings(this@PreferenceActivity, ": ", R.string.progress_recalculating, R.string.price_history))
                    val updatedPrices = priceCalculationViewModel.reCalculatePrices(currencyCode)
                    progressViewModel.appendToMessage(updatedPrices.toString())
                    progressViewModel.appendToMessage(TextUtils.concatResStrings(this@PreferenceActivity, ": ", R.string.progress_recalculating, R.string.equivalent_amount_plural))
                    val updatedTransactions = priceCalculationViewModel.reCalculateEquivalentAmounts(currencyCode)
                    progressViewModel.appendToMessage(updatedTransactions.let { (it.first + it.second) }.toString() )
                    val dataFragment: PreferenceDataFragment? = twoPanePreference.getDetailFragment()
                    if (dataFragment != null) {
                        dataFragment.updateHomeCurrency(currencyCode)
                    } else {
                        prefHandler.putString(PrefKey.HOME_CURRENCY, currencyCode)
                    }
                    requireApplication().invalidateHomeCurrency()
                    contentResolver.notifyChange(DYNAMIC_CURRENCIES_URI, null, false)

                    progressViewModel.appendToMessage(getString(R.string.done_label))
                    progressViewModel.onTaskCompleted(emptyList())
                }
                true
            }

            R.id.REMOVE_LICENCE_COMMAND -> {
                showSnackBarIndefinite(R.string.progress_removing_licence)
                licenceValidationViewModel.removeLicence()
                true
            }

            R.id.DELETE_CALENDAR_COMMAND -> {
                //noinspection MissingPermission
                viewModel?.deleteLocalCalendar()?.observe(this) {
                    when (it) {
                        1 -> twoPanePreference.getDetailFragment<PreferencesAdvancedFragment>()
                            ?.configureDeleteCalendarPreference(false)

                        else -> {
                            val message = if (it == 0) "Deletion of local calendar failed" else
                                "PANIC: DeleteLocalCalendar returned $it"
                            CrashHandler.report(Exception(message))
                            showSnackBar(message)
                        }
                    }
                }
                true
            }

            else -> false
        }

    private fun getKey(prefKey: PrefKey) = prefHandler.getKey(prefKey)

    private fun updateAllWidgets() {
        updateWidgetsForClass(AccountWidget::class.java)
        updateWidgetsForClass(TemplateWidget::class.java)
    }

    private fun updateWidgetsForClass(provider: Class<out AppWidgetProvider>) {
        updateWidgets(this, provider, WIDGET_CONTEXT_CHANGED)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        when (key) {
            getKey(PrefKey.UI_THEME) -> {
                setNightMode(prefHandler, this)
                updateAllWidgets()
            }

            getKey(PrefKey.UI_FONT_SIZE) -> {
                updateAllWidgets()
                recreate()
            }

            getKey(PrefKey.AUTO_BACKUP) -> {
                val autoBackup = sharedPreferences.getBoolean(key, false)
                if (autoBackup &&
                    (prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false) ||
                            prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)
                            )
                ) {
                    showUnencryptedBackupWarning()
                }
                if (autoBackup) {
                    checkNotificationPermissionForAutoBackup()
                }
                AutoBackupWorker.enqueueOrCancel(this, prefHandler)
            }

            getKey(PrefKey.AUTO_BACKUP_TIME) -> AutoBackupWorker.enqueueOrCancel(this, prefHandler)

            getKey(PrefKey.TRACKING) ->
                tracker.setEnabled(sharedPreferences.getBoolean(key, false))

            getKey(PrefKey.CRASHREPORT_USEREMAIL) ->
                crashHandler.setUserEmail(sharedPreferences.getString(key, null))

            getKey(PrefKey.CRASHREPORT_ENABLED) -> {
                crashHandler.setEnabled(sharedPreferences.getBoolean(key, false))
                showSnackBar(R.string.app_restart_required)
            }

            getKey(PrefKey.OCR_ENGINE) -> checkOcrFeature()

            getKey(PrefKey.TESSERACT_LANGUAGE) -> checkTessDataDownload()

            getKey(PrefKey.MLKIT_SCRIPT) -> checkOcrFeature()

            getKey(PrefKey.SYNC_FREQUCENCY) ->
                for (account in GenericAccountService.getAccounts(this)) {
                    GenericAccountService.configureAutomaticAndPeriod(account, prefHandler)
                }

            getKey(PrefKey.PROTECTION_LEGACY), getKey(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN) -> {
                if (sharedPreferences.getBoolean(key, false)) {
                    if (prefHandler.getBoolean(PrefKey.AUTO_BACKUP, false)) {
                        showUnencryptedBackupWarning()
                    }
                }
                updateAllWidgets()
            }

            getKey(PrefKey.CUSTOM_DECIMAL_FORMAT) -> currencyFormatter.invalidate(contentResolver)

            getKey(PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET) ->
                updateWidgetsForClass(AccountWidget::class.java)

            getKey(PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET) ->
                updateWidgetsForClass(TemplateWidget::class.java)

            getKey(PrefKey.PROTECTION_ENABLE_BUDGET_WIDGET) ->
                updateWidgetsForClass(BudgetWidget::class.java)

            getKey(PrefKey.PLANNER_EXECUTION_TIME) -> enqueuePlanner(false)

            getKey(PrefKey.UNMAPPED_TRANSACTION_AS_TRANSFER) -> {
                contentResolver.notifyChange(TRANSACTIONS_URI, null, false)
                contentResolver.notifyChange(ACCOUNTS_URI, null, false)
            }

            getKey(PrefKey.PRINT_FONT_SIZE) -> LazyFontSelector.FontType.clearCache()

            getKey(PrefKey.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD) -> DailyExchangeRateDownloadService.enqueueOrCancel(this, prefHandler)
        }
    }

    /**
     * checks and requests OCR + engine + (script for mlkit)
     */
    private fun checkOcrFeature() {
        if (!featureManager.isFeatureInstalled(Feature.OCR, this)) {
            featureManager.requestFeature(Feature.OCR, this)
        }
    }

    override fun onFeatureAvailable(feature: Feature) {
        super.onFeatureAvailable(feature)
        when (feature) {
            Feature.OCR, Feature.MLKIT, Feature.TESSERACT ->
                twoPanePreference.getDetailFragment<PreferencesOcrFragment>()
                    ?.configureOcrEnginePrefs()

            Feature.WEBUI ->
                twoPanePreference.getDetailFragment<PreferencesWebUiFragment>()
                    ?.bindToWebUiService()

            else -> {}
        }
    }

    private fun showUnencryptedBackupWarning() {
        if (prefHandler.getString(PrefKey.EXPORT_PASSWORD, null) == null) showMessage(
            unencryptedBackupWarning
        )
    }

    fun onStartWebUi() {
        if (licenceHandler.hasAccessTo(ContribFeature.WEB_UI) &&
            featureViewModel.isFeatureAvailable(this, Feature.WEBUI)
        ) {
            activateWebUi()
        } else {
            contribFeatureRequested(ContribFeature.WEB_UI)
        }
    }

    override fun onPositive(args: Bundle, checked: Boolean) {
        super.onPositive(args, checked)
        when (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
            R.id.DELETE_FILES_COMMAND -> twoPanePreference.headerFragment.viewModel
                .deleteAppFiles(args.getStringArray(KEY_CHECKED_FILES)!!)
                .observe(this) {
                    showSnackBar(resources.getQuantityString(R.plurals.delete_success, it, it))
                }
            R.id.WEB_UI_COMMAND -> onStartWebUi()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        super.onPermissionsGranted(requestCode, perms)
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR) {
            initialPrefToShow = prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID)
        }
    }

    override fun onReceiveSyncAccountData(data: SyncViewModel.SyncAccountData) {
        twoPanePreference.getDetailFragment<PreferencesBackupRestoreFragment>()
            ?.loadSyncAccountData(data.accountName)
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        initialPrefToShow?.let {
            twoPanePreference.getDetailFragment<BasePreferenceFragment>()?.showPreference(it)
            initialPrefToShow = null
        }
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        when (feature) {
            ContribFeature.CSV_IMPORT -> {
                startActivity(Intent(this, CsvImportActivity::class.java))
            }
            ContribFeature.AUTOMATIC_FX_DOWNLOAD -> {
                twoPanePreference.getDetailFragment<PreferenceDataFragment>()?.activateAutomaticDownload()
            }

            else -> super.contribFeatureCalled(feature, tag)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                twoPanePreference.startPerformProtection()
                requireApplication().unlock()
            }
        }
        if (requestCode == CONTRIB_REQUEST && resultCode == RESULT_FIRST_USER) {
            validateLicence()
        }
    }

    fun protectionCheck(pref: Preference) =
        if (pref.key == prefHandler.getKey(PrefKey.CATEGORY_SECURITY) && prefHandler.isProtected) {
            confirmCredentials(
                CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST,
                { twoPanePreference.startPerformProtection() },
                shouldHideWindow = false,
                shouldLock = false
            )
            false
        } else true

    @SuppressLint("DiscouragedApi")
    fun getTranslatorsArrayResId(language: String, country: String?): Int {
        var result = 0
        val prefix = "translators_"
        if (language.isEmpty()) {
            if (!country.isNullOrEmpty()) {
                result = resources.getIdentifier(
                    prefix + language + "_" + country,
                    "array", packageName
                )
            }
            if (result == 0) {
                result = resources.getIdentifier(
                    prefix + language,
                    "array", packageName
                )
            }
        }
        return result
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateDialog(id: Int): Dialog? = when (id) {
        R.id.FTP_DIALOG -> DialogUtils.sendWithFTPDialog(this)
        else -> {
            CrashHandler.report(IllegalStateException("onCreateDialog called with $id"))
            super.onCreateDialog(id)
        }
    }

    override fun onWebUiActivated() {
        super.onWebUiActivated()
        resultCode = RESULT_INVALIDATE_OPTIONS_MENU
    }

    override val createAccountTaskShouldQueryRemoteAccounts = false
    override val offerEncryption = false

    override fun onSortOrderConfirmed(sortedIds: LongArray) {
        viewModel?.sortAccounts(sortedIds)
    }

    companion object {
        fun getIntent(context: Context, initialScreen: String? = null) =
            Intent(context, PreferenceActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(KEY_INITIAL_SCREEN, initialScreen)
    }
}