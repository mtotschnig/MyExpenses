package org.totschnig.myexpenses.activity

import android.app.Dialog
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.SettingsBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.feature.BankingFeature
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.fragment.TwoPanePreference
import org.totschnig.myexpenses.fragment.TwoPanePreference.Companion.KEY_INITIAL_SCREEN
import org.totschnig.myexpenses.fragment.preferences.*
import org.totschnig.myexpenses.fragment.preferences.PreferencesExportFragment.Companion.KEY_CHECKED_FILES
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.service.AutoBackupWorker
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.PermissionHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.setNightMode
import org.totschnig.myexpenses.viewmodel.LicenceValidationViewModel
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.widget.AccountWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import org.totschnig.myexpenses.widget.WIDGET_CONTEXT_CHANGED
import org.totschnig.myexpenses.widget.updateWidgets
import java.io.Serializable

class PreferenceActivity : ProtectedFragmentActivity(), ContribIFace {
    lateinit var binding: SettingsBinding
    val viewModel: SettingsViewModel?
        get() = twoPanePreference.getDetailFragment<BasePreferenceFragment>()
            ?.viewModel

    private val bankingFeature: BankingFeature
        get() = requireApplication().appComponent.bankingFeature() ?: object : BankingFeature {}

    private val licenceValidationViewModel: LicenceValidationViewModel by viewModels()

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

    private fun observeLicenceApiResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                licenceValidationViewModel.result.collect { result ->
                    result?.let {
                        twoPanePreference.getDetailFragment<PreferencesContribFragment>()
                            ?.configureContribPrefs()
                        showDismissibleSnackBar(it, dismissCallback)
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
        injector.inject(licenceValidationViewModel)
        super.onCreate(savedInstanceState)
        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        title = getString(R.string.menu_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    binding.fragmentContainer.id,
                    TwoPanePreference.newInstance(intent.getStringExtra(KEY_INITIAL_SCREEN))
                )
                .commit()
        }
        observeLicenceApiResult()
    }

    override fun setTitle(title: CharSequence?) {
        supportActionBar!!.title = title
    }

    override fun onCreateOptionsMenu(menu: Menu) = false

    val twoPanePreference: TwoPanePreference
        get() = binding.fragmentContainer.getFragment()

    override fun doHome() {
        if (!twoPanePreference.doHome()) super.doHome()
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) true
        else when (command) {

            R.id.CHANGE_COMMAND -> {
                val currencyCode = tag as String
                val dataFragment: PreferenceDataFragment? = twoPanePreference.getDetailFragment()
                if (dataFragment != null) {
                    dataFragment.updateHomeCurrency(currencyCode)
                } else {
                    prefHandler.putString(PrefKey.HOME_CURRENCY, currencyCode)
                }
                requireApplication().invalidateHomeCurrency(currencyCode)
                showSnackBarIndefinite(R.string.saving)
                viewModel?.resetEquivalentAmounts()?.observe(this) { integer ->
                    dismissSnackBar()
                    if (integer != null) {
                        showSnackBar(
                            String.format(
                                getLocale(),
                                "%s (%d)",
                                getString(R.string.reset_equivalent_amounts_success),
                                integer
                            )
                        )
                    } else {
                        showSnackBar("Equivalent amount reset failed")
                    }
                }
                true
            }

            R.id.REMOVE_LICENCE_COMMAND -> {
                showSnackBarIndefinite(R.string.progress_removing_licence)
                licenceValidationViewModel.removeLicence()
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
            getKey(PrefKey.UI_THEME_KEY) -> {
                setNightMode(prefHandler, this)
                updateAllWidgets()
            }

            getKey(PrefKey.UI_FONTSIZE) -> {
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
                    GenericAccountService.addPeriodicSync(account, prefHandler)
                }

            getKey(PrefKey.PROTECTION_LEGACY), getKey(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN) -> {
                if (sharedPreferences.getBoolean(key, false)) {
                    showSnackBar(R.string.pref_protection_screenshot_information)
                    if (prefHandler.getBoolean(PrefKey.AUTO_BACKUP, false)) {
                        showUnencryptedBackupWarning()
                    }
                }
                updateAllWidgets()
            }

            getKey(PrefKey.CUSTOM_DECIMAL_FORMAT) -> {
                currencyFormatter.invalidateAll(contentResolver)
            }

            getKey(PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET) -> {
                updateWidgetsForClass(AccountWidget::class.java)
            }

            getKey(PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET) -> {
                updateWidgetsForClass(TemplateWidget::class.java)
            }

            getKey(PrefKey.PLANNER_EXECUTION_TIME) -> {
                enqueuePlanner(false)
            }
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
            Feature.OCR, Feature.MLKIT, Feature.TESSERACT -> {
                twoPanePreference.getDetailFragment<PreferencesOcrFragment>()?.configureOcrEnginePrefs()
            }
            Feature.WEBUI -> {
                twoPanePreference.getDetailFragment<PreferencesWebUiFragment>()?.bindToWebUiService()
                activateWebUi()
            }
            Feature.FINTS -> startBanking()
            else -> {}
        }
    }

    private fun showUnencryptedBackupWarning() {
        if (prefHandler.getString(PrefKey.EXPORT_PASSWORD, null) == null) showMessage(
            unencryptedBackupWarning
        )
    }

    override fun onPositive(args: Bundle, checked: Boolean) {
        super.onPositive(args, checked)
        if (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE) == R.id.DELETE_FILES_COMMAND) {
            viewModel
                ?.deleteAppFiles(args.getStringArray(KEY_CHECKED_FILES)!!)
                ?.observe(this) {
                    showSnackBar(resources.getQuantityString(R.plurals.delete_success, it, it))
                }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        super.onPermissionsGranted(requestCode, perms)
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_NOTIFICATIONS_WEBUI) {
            activateWebUi()
        }
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_WRITE_CALENDAR) {
            initialPrefToShow = prefHandler.getKey(PrefKey.PLANNER_CALENDAR_ID)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        super.onPermissionsDenied(requestCode, perms)
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_NOTIFICATIONS_WEBUI) {
            activateWebUi()
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        initialPrefToShow?.let {
            twoPanePreference.getDetailFragment<BasePreferenceFragment>()?.showPreference(it)
            initialPrefToShow = null
        }
    }

    fun activateWebUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(this).areNotificationsEnabled()
        ) {
            requestNotificationPermission(PermissionHelper.PERMISSIONS_REQUEST_NOTIFICATIONS_WEBUI)
        } else {
            prefHandler.putBoolean(PrefKey.UI_WEB, true)
        }
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        when (feature) {
            ContribFeature.CSV_IMPORT -> {
                startActivity(Intent(this, CsvImportActivity::class.java))
            }

            ContribFeature.WEB_UI -> {
                if (featureViewModel.isFeatureAvailable(this, Feature.WEBUI)) {
                    activateWebUi()
                } else {
                    featureViewModel.requestFeature(this, Feature.WEBUI)
                }
            }
            ContribFeature.BANKING -> {
                if (featureViewModel.isFeatureAvailable(this, Feature.FINTS)) {
                    startBanking()
                } else {
                    featureViewModel.requestFeature(this, Feature.FINTS)
                }
            }

            else -> {}
        }
    }

    private fun startBanking() {
        bankingFeature.startBankingList(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                twoPanePreference.startPerformProtection()
            }
        }
    }

    fun protectionCheck(pref: Preference) =
        if (pref.key == prefHandler.getKey(PrefKey.CATEGORY_SECURITY) &&
            (application as MyApplication).isProtected
        ) {
            confirmCredentials(
                CONFIRM_DEVICE_CREDENTIALS_MANAGE_PROTECTION_SETTINGS_REQUEST,
                { twoPanePreference.startPerformProtection() },
                false
            )
            false
        } else true

    fun getTranslatorsArrayResId(language: String, country: String?): Int {
        var result = 0
        val prefix = "translators_"
        if (!TextUtils.isEmpty(language)) {
            if (!TextUtils.isEmpty(country)) {
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

    companion object {
        fun getIntent(context: Context, initialScreen: String? = null) =
            Intent(context, PreferenceActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(KEY_INITIAL_SCREEN, initialScreen)
    }
}