package org.totschnig.myexpenses.fragment

import android.appwidget.AppWidgetProvider
import android.content.ContentResolver
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.TextUtils.isEmpty
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.MyPreferenceActivity
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.LocalizedFormatEditTextPreference.OnValidationErrorListener
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.service.DailyScheduler
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import org.totschnig.myexpenses.util.setNightMode
import org.totschnig.myexpenses.viewmodel.WebUiViewModel
import org.totschnig.myexpenses.widget.AccountWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import org.totschnig.myexpenses.widget.WIDGET_CONTEXT_CHANGED
import org.totschnig.myexpenses.widget.updateWidgets
import java.util.*
import javax.inject.Inject

abstract class BaseSettingsFragment : PreferenceFragmentCompat(), OnValidationErrorListener,
        OnSharedPreferenceChangeListener {

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

    private lateinit var webUiViewModel: WebUiViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity().application as MyApplication).appComponent.inject(this)
        webUiViewModel = ViewModelProvider(this)[WebUiViewModel::class.java]
        webUiViewModel.getServiceState().observe(this) { serverAddress ->
            findPreference<SwitchPreferenceCompat>(PrefKey.UI_WEB)?.let { preference ->
                serverAddress?.let { preference.summaryOn = it }
                if (preference.isChecked && serverAddress == null) {
                    preference.isChecked = false
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
        configureMultiSelectListPref(PrefKey.FEATURE_UNINSTALL_FEATURES, featureManager.installedFeatures(),
                featureManager::uninstallFeatures) { module ->
            Feature.fromModuleName(module)?.let { getString(it.labelResId) } ?: module
        }
        configureMultiSelectListPref(PrefKey.FEATURE_UNINSTALL_LANGUAGES, featureManager.installedLanguages(), featureManager::uninstallLanguages) { language ->
            Locale(language).let { it.getDisplayName(it) }
        }
    }

    private fun configureMultiSelectListPref(prefKey: PrefKey, entries: Set<String>, action: (Set<String>) -> Unit, prettyPrint: (String) -> String) {
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

    fun configureTesseractLanguagePref() {
        requirePreference<ListPreference>(PrefKey.TESSERACT_LANGUAGE).let {
            activity().ocrViewModel.configureTesseractLanguagePref(it)
        }
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

    fun matches(preference: Preference, prefKey: PrefKey) = prefHandler.getKey(prefKey) == preference.key
    fun getKey(prefKey: PrefKey): String {
        return prefHandler.getKey(prefKey)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences,
                                           key: String) {
        if (key == getKey(PrefKey.UI_LANGUAGE)) {
            featureManager.requestLocale(activity())
        } else if ((key == getKey(PrefKey.GROUP_MONTH_STARTS) ||
                        key == getKey(PrefKey.GROUP_WEEK_STARTS) || key == getKey(PrefKey.CRITERION_FUTURE))) {
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
            if ((sharedPreferences.getBoolean(key, false) && ((prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false) || prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false))))) {
                activity().showUnencryptedBackupWarning()
            }
            DailyScheduler.updateAutoBackupAlarms(activity())
        } else if (key == getKey(PrefKey.AUTO_BACKUP_TIME)) {
            DailyScheduler.updateAutoBackupAlarms(activity())
        } else if (key == getKey(PrefKey.SYNC_FREQUCENCY)) {
            for (account in GenericAccountService.getAccounts(activity())) {
                ContentResolver.addPeriodicSync(account, TransactionProvider.AUTHORITY, Bundle.EMPTY,
                        prefHandler.getInt(PrefKey.SYNC_FREQUCENCY, GenericAccountService.DEFAULT_SYNC_FREQUENCY_HOURS).toLong() * GenericAccountService.HOUR_IN_SECONDS)
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
            configureTesseractLanguagePref()
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
        if (matches(preferenceScreen, PrefKey.ROOT_SCREEN) || matches(preferenceScreen, PrefKey.PERFORM_PROTECTION_SCREEN)) {
            val isLegacy = prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false)
            val isProtected = isLegacy || prefHandler.getBoolean(PrefKey.PROTECTION_DEVICE_LOCK_SCREEN, false)
            requirePreference<Preference>(PrefKey.SECURITY_QUESTION).isEnabled = isLegacy
            requirePreference<Preference>(PrefKey.PROTECTION_DELAY_SECONDS).isEnabled = isProtected
            requirePreference<Preference>(PrefKey.PROTECTION_ENABLE_ACCOUNT_WIDGET).isEnabled = isProtected
            requirePreference<Preference>(PrefKey.PROTECTION_ENABLE_TEMPLATE_WIDGET).isEnabled = isProtected
            requirePreference<Preference>(PrefKey.PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET).isEnabled = isProtected
        }
    }

    fun getKeyInfo(): String {
        return "${prefHandler.getString(PrefKey.LICENCE_EMAIL, "")}: ${prefHandler.getString(PrefKey.NEW_LICENCE, "")}"
    }

    open fun configureContribPrefs() {
        if (!matches(preferenceScreen, PrefKey.ROOT_SCREEN)) {
            return
        }
        val contribPurchasePref = requirePreference<Preference>(PrefKey.CONTRIB_PURCHASE)
        val licenceKeyPref = findPreference<Preference>(PrefKey.NEW_LICENCE)
        if (licenceHandler.needsKeyEntry) {
            licenceKeyPref?.let {
                if (licenceHandler.hasValidKey()) {
                    it.title = getKeyInfo()
                    it.summary = TextUtils.concatResStrings(activity, " / ",
                            R.string.button_validate, R.string.menu_remove)
                } else {
                    it.setTitle(R.string.pref_enter_licence_title)
                    it.setSummary(R.string.pref_enter_licence_summary)
                }
            }
        } else {
            licenceKeyPref?.isVisible = false
        }
        val contribPurchaseTitle: String = licenceHandler.prettyPrintStatus(requireContext()) ?:
        getString(R.string.pref_contrib_purchase_title) + (if (licenceHandler.doesUseIAP)
            " (${getString(R.string.pref_contrib_purchase_title_in_app)})" else "")
        var contribPurchaseSummary: String
        val licenceStatus = licenceHandler.licenceStatus
        if (licenceStatus == null && licenceHandler.addOnFeatures.isEmpty()) {
            contribPurchaseSummary = getString(R.string.pref_contrib_purchase_summary)
        } else {
            if (licenceStatus?.isUpgradeable != false) {
                contribPurchaseSummary = getString(R.string.pref_contrib_purchase_title_upgrade)
            } else {
                contribPurchaseSummary = licenceHandler.getProLicenceAction(requireContext())
            }
            if (!isEmpty(contribPurchaseSummary)) {
                contribPurchaseSummary += "\n"
            }
            contribPurchaseSummary += getString(R.string.thank_you)
        }
        contribPurchasePref.summary = contribPurchaseSummary
        contribPurchasePref.title = contribPurchaseTitle
    }
}