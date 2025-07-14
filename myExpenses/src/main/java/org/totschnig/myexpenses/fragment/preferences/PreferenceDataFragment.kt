package org.totschnig.myexpenses.fragment.preferences

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.Keep
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.ListPreference.SimpleSummaryProvider
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceCategory
import androidx.preference.TwoStatePreference
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.PriceHistory
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.preference.DYNAMIC_EXCHANGE_RATES_DEFAULT_KEY
import org.totschnig.myexpenses.preference.PrefHandler.Companion.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX
import org.totschnig.myexpenses.preference.PrefHandler.Companion.SERVICE_DEACTIVATED
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DYNAMIC_CURRENCIES_URI
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency

@Keep
class PreferenceDataFragment : BasePreferenceFragment() {

    private val currencyViewModel: CurrencyViewModel by viewModels()

    override val preferencesResId = R.xml.preferences_data

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(currencyViewModel)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        requirePreference<Preference>(PrefKey.HOME_CURRENCY).onPreferenceChangeListener =
            OnPreferenceChangeListener { _, newValue ->
                val newHomeCurrency = newValue as String
                if (newHomeCurrency != prefHandler.getString(PrefKey.HOME_CURRENCY, null)) {
                    MessageDialogFragment.newInstance(
                        getString(R.string.pref_home_currency_title) + ": " + viewModel.currencyContext[newHomeCurrency].description ,
                        TextUtils.concatResStrings(
                            requireContext(),
                            R.string.recalculate_equivalent_amounts_warning,
                            R.string.continue_confirmation
                        ),
                        MessageDialogFragment.Button(
                            android.R.string.ok, R.id.CHANGE_COMMAND,
                            newHomeCurrency
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                currencyViewModel.usedCurrencies.collect { currencies ->
                    configureCurrenciesForAutomaticFXDownload(
                        ExchangeRateApi.configuredSources(prefHandler), currencies
                    )
                    with(requirePreference<PreferenceCategory>(PrefKey.CATEGORY_PRICES)) {
                        isVisible = currencies.isNotEmpty()
                        removeAll()
                        currencies.forEach {
                            Preference(requireContext()).apply {
                                title = viewModel.currencyContext[it.code].description
                                intent = Intent(requireContext(), PriceHistory::class.java).apply {
                                    putExtra(KEY_COMMODITY, it.code)
                                }
                                addPreference(this)
                            }
                        }
                    }
                }
            }
        }

        with(requirePreference<MultiSelectListPreference>(PrefKey.EXCHANGE_RATE_PROVIDER)) {
            entries = ExchangeRateApi.values.map { it.host }.toTypedArray()
            entryValues = ExchangeRateApi.values.map { it.name }.toTypedArray()
        }
        arrayOf(ExchangeRateApi.OpenExchangeRates, ExchangeRateApi.CoinApi).forEach {
            requirePreference<Preference>(it.prefKey).summary =
                getString(R.string.pref_exchange_rates_api_key_summary, it.host)
        }
        configureExchangeRatesPreference(ExchangeRateApi.configuredSources(prefHandler))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferenceDataStore.handleList(findPreference(DYNAMIC_EXCHANGE_RATES_DEFAULT_KEY)!!) {
                    requireContext().contentResolver.notifyChange(DYNAMIC_CURRENCIES_URI, null, false)
                    requireContext().contentResolver.notifyChange(ACCOUNTS_URI, null, false)
                }
            }
        }
    }

    private fun configureCurrenciesForAutomaticFXDownload(
        providers: Set<ExchangeRateApi>,
        currencies: List<CurrencyUnit>,
    ) {
        with(requirePreference<TwoStatePreference>(PrefKey.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD)) {
            isVisible = currencies.isNotEmpty()
            isEnabled = providers.isNotEmpty()
            summary = ContribFeature.AUTOMATIC_FX_DOWNLOAD.buildRequiresString(requireActivity())
            onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    preferenceActivity.contribFeatureRequested(ContribFeature.AUTOMATIC_FX_DOWNLOAD)
                    false
                } else true
            }
        }
        with(requirePreference<PreferenceCategory>(PrefKey.CATEGORY_CURRENCIES)) {
            buildList {
                for (i in 0 until preferenceCount) {
                    getPreference(i)
                        .takeIf {
                            it.key?.startsWith(AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX) == true
                        }
                        ?.let { add(it) }
                }
            }.forEach {
                removePreference(it)
            }
            currencies.forEach {
                ListPreference(requireContext()).apply {
                    key = "${AUTOMATIC_EXCHANGE_RATE_DOWNLOAD_PREF_KEY_PREFIX}${it.code}"
                    title = viewModel.currencyContext[it.code].description
                    entries = arrayOf(getString(R.string.disabled)) + providers.map { it.host }
                        .toTypedArray()
                    entryValues = arrayOf(SERVICE_DEACTIVATED) + providers.map { it.name }.toTypedArray()
                    setDefaultValue(SERVICE_DEACTIVATED)
                    setSummaryProvider(SimpleSummaryProvider.getInstance())
                    addPreference(this)
                    dependency =
                        prefHandler.getKey(PrefKey.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD)
                    onPreferenceChangeListener = automaticChangeRateCurrencyOnChangeListener
                }
            }
        }
    }

    val homeCurrency: String
        get() = currencyViewModel.currencyContext.homeCurrencyString

    val automaticChangeRateCurrencyOnChangeListener =
        OnPreferenceChangeListener { preference, newValue ->
            if (newValue == SERVICE_DEACTIVATED) return@OnPreferenceChangeListener true
            val source = ExchangeRateApi.getByName(newValue as String)
            if (source is ExchangeRateApi.SourceWithApiKey && source.getApiKey(prefHandler).isNullOrEmpty()) {
                preferenceActivity.showSnackBar(getString(R.string.pref_exchange_rates_api_key_summary, source.host))
                return@OnPreferenceChangeListener false
            }
            val currency = preference.key.substringAfterLast("_")
            checkIsExchangeRateSupported(
                source,
                currency
            ).also {
                if (!it) {
                    preferenceActivity.showSnackBar(getString(R.string.exchange_rate_not_supported, currency, homeCurrency))
                }
            }
        }

    private fun checkIsExchangeRateSupported(source: ExchangeRateApi, currency: String) =
        source.isSupported(currency, homeCurrency)

    private fun configureExchangeRatesPreference(providers: Set<ExchangeRateApi>) {
        arrayOf(ExchangeRateApi.OpenExchangeRates, ExchangeRateApi.CoinApi).forEach {
            requirePreference<Preference>(it.prefKey).isVisible = providers.contains(it)
        }
    }

    fun updateHomeCurrency(currencyCode: String) {
        requirePreference<ListPreference>(PrefKey.HOME_CURRENCY).value = currencyCode
    }

    fun activateAutomaticDownload() {
        requirePreference<TwoStatePreference>(PrefKey.AUTOMATIC_EXCHANGE_RATE_DOWNLOAD).isChecked = true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            getKey(PrefKey.EXCHANGE_RATE_PROVIDER) -> {
                val providers = ExchangeRateApi.configuredSources(
                    prefHandler.getStringSet(key)
                )
                configureExchangeRatesPreference(providers)
                configureCurrenciesForAutomaticFXDownload(
                    providers,
                    currencyViewModel.usedCurrencies.value
                )
            }
        }
    }
}