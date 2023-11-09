package org.totschnig.myexpenses.fragment.preferences

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
    }

    fun updateHomeCurrency(currencyCode: String) {
        findPreference<ListPreference>(PrefKey.HOME_CURRENCY)?.let {
            it.value = currencyCode
        }
    }
}