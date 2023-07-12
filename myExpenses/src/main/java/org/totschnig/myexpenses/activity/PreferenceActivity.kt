package org.totschnig.myexpenses.activity

import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.SettingsBinding
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.fragment.BaseSettingsFragment
import org.totschnig.myexpenses.fragment.preferences.PreferenceDataFragment
import org.totschnig.myexpenses.fragment.TwoPanePreference
import org.totschnig.myexpenses.fragment.preferences.PreferencesExchangeRateFragment
import org.totschnig.myexpenses.fragment.preferences.PreferencesOcrFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.service.AutoBackupWorker
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.setNightMode
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.widget.AccountWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import org.totschnig.myexpenses.widget.WIDGET_CONTEXT_CHANGED
import org.totschnig.myexpenses.widget.updateWidgets
import java.io.Serializable

class PreferenceActivity : ProtectedFragmentActivity(), ContribIFace {
    lateinit var binding: SettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        title = getString(R.string.menu_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainer.id, TwoPanePreference())
                .commit()
        }
    }

    override fun setTitle(title: CharSequence?) {
        supportActionBar!!.title = title
    }

    override fun onCreateOptionsMenu(menu: Menu) = false

    private val twoPanePreference: TwoPanePreference
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
                viewModel.resetEquivalentAmounts().observe(this) { integer ->
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
                if ((sharedPreferences.getBoolean(key, false) &&
                            ((prefHandler.getBoolean(PrefKey.PROTECTION_LEGACY, false) ||
                                    prefHandler.getBoolean(
                                        PrefKey.PROTECTION_DEVICE_LOCK_SCREEN,
                                        false
                                    ))
                                    )
                            )
                ) {
                    showUnencryptedBackupWarning()
                }
                AutoBackupWorker.enqueueOrCancel(this, prefHandler)
            }

            getKey(PrefKey.EXCHANGE_RATE_PROVIDER) -> {
                twoPanePreference.getDetailFragment<PreferencesExchangeRateFragment>()
                    ?.configureExchangeRatesPreference(
                        ExchangeRateSource.preferredSource(
                            sharedPreferences.getString(key, null)
                        )
                    )
            }
            getKey(PrefKey.OCR_ENGINE) -> {
                checkOcrFeature()
                twoPanePreference.getDetailFragment<PreferencesOcrFragment>()
                    ?.configureOcrEnginePrefs()
            }
            getKey(PrefKey.TESSERACT_LANGUAGE) -> {
                checkTessDataDownload()
            }
            getKey(PrefKey.MLKIT_SCRIPT) -> {
                checkOcrFeature()
            }
            getKey(PrefKey.SYNC_FREQUCENCY) -> {
                for (account in GenericAccountService.getAccounts(this)) {
                    GenericAccountService.addPeriodicSync(account, prefHandler)
                }
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
        if (feature in listOf(Feature.OCR, Feature.MLKIT, Feature.TESSERACT)) {
            twoPanePreference.getDetailFragment<PreferencesOcrFragment>()?.configureOcrEnginePrefs()
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
            viewModel.deleteAppFiles(args.getStringArray(BaseSettingsFragment.KEY_CHECKED_FILES)!!)
                .observe(this) {
                    showSnackBar(resources.getQuantityString(R.plurals.delete_success, it, it))
                }
        }
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (feature === ContribFeature.CSV_IMPORT) {
            val i = Intent(this, CsvImportActivity::class.java)
            startActivity(i)
        }
        /*        if (feature === ContribFeature.WEB_UI) {
                    if (featureViewModel.isFeatureAvailable(this, Feature.WEBUI)) {
                        activateWebUi()
                    } else {
                        featureViewModel.requestFeature(this, Feature.WEBUI)
                    }
                }*/
    }
}