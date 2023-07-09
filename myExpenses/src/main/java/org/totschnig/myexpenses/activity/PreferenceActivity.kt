package org.totschnig.myexpenses.activity

import android.appwidget.AppWidgetProvider
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.SettingsBinding
import org.totschnig.myexpenses.fragment.PreferenceDataFragment
import org.totschnig.myexpenses.fragment.TwoPanePreference
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.setNightMode
import org.totschnig.myexpenses.viewmodel.SettingsViewModel
import org.totschnig.myexpenses.widget.AccountWidget
import org.totschnig.myexpenses.widget.TemplateWidget
import org.totschnig.myexpenses.widget.WIDGET_CONTEXT_CHANGED
import org.totschnig.myexpenses.widget.updateWidgets

class PreferenceActivity : ProtectedFragmentActivity() {
    lateinit var binding: SettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainer.id, TwoPanePreference())
                .commit()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <F : Fragment?> getFragment(): F? =
        (binding.fragmentContainer.getFragment() as TwoPanePreference)
            .childFragmentManager
            .findFragmentById(androidx.preference.R.id.preferences_detail) as? F

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        return when (command) {

            R.id.CHANGE_COMMAND -> {
                val currencyCode = tag as String
                val dataFragment = getFragment<PreferenceDataFragment>()
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
    }
    private fun getKey(prefKey: PrefKey) = prefHandler.getKey(prefKey)

    private fun updateAllWidgets() {
        updateWidgetsForClass(AccountWidget::class.java)
        updateWidgetsForClass(TemplateWidget::class.java)
    }

    private fun updateWidgetsForClass(provider: Class<out AppWidgetProvider>) {
        updateWidgets(this, provider, WIDGET_CONTEXT_CHANGED)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        when(key) {
            getKey(PrefKey.UI_THEME_KEY) -> {
                setNightMode(prefHandler, this)
                updateAllWidgets()
            }
            getKey(PrefKey.UI_FONTSIZE) -> {
                updateAllWidgets()
                recreate()
            }
        }
    }
}