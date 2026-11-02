package org.totschnig.myexpenses.activity

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.progress.NewProgressDialogFragment
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.TransactionProvider.DYNAMIC_CURRENCIES_URI
import org.totschnig.myexpenses.util.TextUtils
import org.totschnig.myexpenses.viewmodel.ModalProgressViewModel
import org.totschnig.myexpenses.viewmodel.PriceCalculationViewModel

/**
 * Makes [currencyCode] the home currency: calculates prices and equivalent amounts anew, while a
 * progress dialog tells about it
 *
 * @param store stores the new home currency, e.g. through the preference of the screen shown
 */
fun BaseActivity.changeHomeCurrency(
    currencyCode: String,
    progressViewModel: ModalProgressViewModel,
    priceCalculationViewModel: PriceCalculationViewModel,
    store: () -> Unit = { prefHandler.putString(PrefKey.HOME_CURRENCY, currencyCode) },
) {
    supportFragmentManager.beginTransaction()
        .add(
            NewProgressDialogFragment.newInstance(
                getString(R.string.pref_home_currency_title) + ": " + currencyContext[currencyCode].description
            ).apply { isCancelable = false },
            BaseActivity.PROGRESS_TAG
        )
        .commitNow()
    lifecycleScope.launch {
        progressViewModel.appendToMessage(
            TextUtils.concatResStrings(
                this@changeHomeCurrency,
                ": ",
                R.string.progress_recalculating,
                R.string.price_history
            )
        )
        val updatedPrices = priceCalculationViewModel.reCalculatePrices(currencyCode)
        progressViewModel.appendToMessage(updatedPrices.toString())
        progressViewModel.appendToMessage(
            TextUtils.concatResStrings(
                this@changeHomeCurrency,
                ": ",
                R.string.progress_recalculating,
                R.string.equivalent_amount_plural
            )
        )
        val updatedTransactions =
            priceCalculationViewModel.reCalculateEquivalentAmounts(currencyCode)
        progressViewModel.appendToMessage(updatedTransactions.let { (it.first + it.second) }
            .toString())
        store()
        requireApplication().invalidateHomeCurrency()
        contentResolver.notifyChange(DYNAMIC_CURRENCIES_URI, null, false)

        progressViewModel.appendToMessage(getString(R.string.done_label))
        progressViewModel.onTaskCompleted(emptyList())
    }
}
