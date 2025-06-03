package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMODITY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SOURCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLocalDate
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.viewmodel.data.FullPrice
import timber.log.Timber


class PriceCalculationViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
    ContentResolvingAndroidViewModel(application) {

    suspend fun reCalculatePrices(newHomeCurrency: String) = withContext(coroutineContext()) {

        var count = 0
        contentResolver.query(
            TransactionProvider.PRICES_URI,
            arrayOf(KEY_DATE, KEY_SOURCE, KEY_VALUE, KEY_CURRENCY, KEY_COMMODITY),
            null,
            null,
            null
        )
            ?.useAndMapToList {
                FullPrice(
                    date = it.getLocalDate(0),
                    source = ExchangeRateSource.getByName(it.getString(1)),
                    value = it.getDouble(2),
                    currency = it.getString(3),
                    commodity = it.getString(4)
                )
            }
            ?.groupBy { it.date }
            ?.forEach { (date, prices) ->
                Timber.d("Date %s", date)
                val existingPrices = mutableListOf<Pair<String, Double>>()
                existingPrices.addAll(prices.filter { it.currency == newHomeCurrency }
                    .map { it.commodity to it.value })
                Timber.d("Existing prices %s", existingPrices.joinToString())
                val pricesNotCalculated =
                    prices.filter { it.source != ExchangeRateSource.Calculation }
                // Go through all prices that we can just invert
                pricesNotCalculated.forEach { price ->
                    if (price.commodity == newHomeCurrency && existingPrices.none { it.first == price.currency }) {
                        val inverseValue = 1 / price.value
                        Timber.d(
                            "calculating %s:%s (from inverse): %f",
                            price.currency,
                            newHomeCurrency,
                            inverseValue
                        )
                        repository.savePrice(
                            base = newHomeCurrency,
                            commodity = price.currency,
                            date = date,
                            source = ExchangeRateSource.Calculation,
                            value = inverseValue,
                            updateEquivalentAmount = false
                        )
                        existingPrices.add(price.currency to inverseValue)
                        count++
                    }
                }
                pricesNotCalculated.forEach { price ->
                    if (price.commodity != newHomeCurrency && existingPrices.none { it.first == price.commodity }) {
                        existingPrices.find { it.first == price.currency }?.let { reference ->
                            val value = reference.second * price.value
                            //noinspection TimberArgCount
                            Timber.d(
                                "calculating %1\$s:%2\$s from (%1\$s:%3\$s and %3\$s:%2\$s): %4\$f",
                                price.commodity,
                                newHomeCurrency,
                                price.currency,
                                value
                            )
                            repository.savePrice(
                                base = newHomeCurrency,
                                commodity = price.commodity,
                                date = date,
                                source = ExchangeRateSource.Calculation,
                                value = value,
                                updateEquivalentAmount = false
                            )
                            existingPrices.add(price.commodity to value)
                            count++
                        }
                    }
                }
            }
        count
    }

    suspend fun reCalculateEquivalentAmounts(
        newHomeCurrency: String = currencyContext.homeCurrencyString,
        accountId: Long? = null,
    ): Pair<Int, Int> =
        withContext(coroutineContext()) {
            contentResolver.call(
                TransactionProvider.DUAL_URI,
                TransactionProvider.METHOD_RECALCULATE_EQUIVALENT_AMOUNTS, null,
                Bundle(1).apply {
                    putString(KEY_CURRENCY, newHomeCurrency)
                    accountId?.let { putLong(KEY_ACCOUNTID, it) }
                }
            )!!.getSerializable(TransactionProvider.KEY_RESULT) as Pair<Int, Int>
        }
}