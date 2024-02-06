package org.totschnig.myexpenses.fragment.preferences

import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.Keep
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency

@Keep
class PreferenceDataFragment: BasePreferenceFragment() {

    private val currencyViewModel: CurrencyViewModel by viewModels()

    override val preferencesResId = R.xml.preferences_data

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(currencyViewModel)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        requirePreference<Preference>(PrefKey.HOME_CURRENCY).onPreferenceChangeListener  =
            OnPreferenceChangeListener { _, newValue ->
                if (newValue != prefHandler.getString(PrefKey.HOME_CURRENCY, null)) {
                MessageDialogFragment.newInstance(
                    getString(R.string.dialog_title_information),
                    TextUtils.concatResStrings(
                        requireContext(),
                        " ",
                        R.string.home_currency_change_warning,
                        R.string.continue_confirmation
                    ),
                    MessageDialogFragment.Button(
                        android.R.string.ok, R.id.CHANGE_COMMAND,
                        newValue as String
                    ),
                    null, MessageDialogFragment.noButton()
                ).show(parentFragmentManager, "CONFIRM")
            }
                false
            }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                currencyViewModel.currencies.collect { currencies ->
                    with(requirePreference<ListPreference>(PrefKey.HOME_CURRENCY)) {
                        entries = currencies.map(Currency::toString).toTypedArray()
                        entryValues = currencies.map { it.code }.toTypedArray()
                        isEnabled = true
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasStaleImages.collect { result ->
                    requirePreference<Preference>(PrefKey.MANAGE_STALE_IMAGES).isVisible =
                        result
                }
            }
        }
        with(requirePreference<ListPreference>(PrefKey.EXCHANGE_RATE_PROVIDER)) {
            entries = ExchangeRateSource.values.map { it.host }.toTypedArray()
            entryValues = ExchangeRateSource.values.map { it.id }.toTypedArray()
        }
        arrayOf(ExchangeRateSource.OpenExchangeRates, ExchangeRateSource.CoinApi).forEach {
            requirePreference<Preference>(it.prefKey).summary =
                getString(R.string.pref_exchange_rates_api_key_summary, it.host)
        }
        configureExchangeRatesPreference(ExchangeRateSource.preferredSource(prefHandler))
        requirePreference<Preference>(PrefKey.EXCHANGE_RATES_CLEAR_CACHE).title =
            "${getString(R.string.clear_cache)} (${getString(R.string.pref_category_exchange_rates)})"
    }

    private fun configureExchangeRatesPreference(provider: ExchangeRateSource) {
        arrayOf(ExchangeRateSource.OpenExchangeRates, ExchangeRateSource.CoinApi).forEach {
            requirePreference<Preference>(it.prefKey).isVisible = provider == it
        }
    }

    fun updateHomeCurrency(currencyCode: String) {
        findPreference<ListPreference>(PrefKey.HOME_CURRENCY)?.let {
            it.value = currencyCode
        }
    }

    override fun onPreferenceTreeClick(preference: Preference) = when {
        super.onPreferenceTreeClick(preference) -> true
        matches(preference, PrefKey.EXCHANGE_RATES_CLEAR_CACHE) -> {
            viewModel.clearExchangeRateCache().observe(this) {
                preferenceActivity.showSnackBar("${getString(R.string.clear_cache)} ($it)")
            }
            true
        }

        else -> false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            getKey(PrefKey.EXCHANGE_RATE_PROVIDER) -> {
                configureExchangeRatesPreference(
                    ExchangeRateSource.preferredSource(
                        sharedPreferences.getString(key, null)
                    )
                )
            }
        }
    }
}