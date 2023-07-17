package org.totschnig.myexpenses.fragment.preferences

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.retrofit.ExchangeRateSource

class PreferencesExchangeRateFragment : BasePreferenceFragment() {

    override val preferencesResId = R.xml.preferences_exchange_rate

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        with(requirePreference<ListPreference>(PrefKey.EXCHANGE_RATE_PROVIDER)) {
            entries = ExchangeRateSource.values.map { it.host }.toTypedArray()
            entryValues = ExchangeRateSource.values.map { it.id }.toTypedArray()
        }
        arrayOf(ExchangeRateSource.OpenExchangeRates, ExchangeRateSource.CoinApi).forEach {
            requirePreference<Preference>(it.prefKey).summary =
                getString(R.string.pref_exchange_rates_api_key_summary, it.host)
        }
        configureExchangeRatesPreference(ExchangeRateSource.preferredSource(prefHandler))
    }

    fun configureExchangeRatesPreference(provider: ExchangeRateSource) {
        arrayOf(ExchangeRateSource.OpenExchangeRates, ExchangeRateSource.CoinApi).forEach {
            requirePreference<Preference>(it.prefKey).isVisible = provider == it
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
}