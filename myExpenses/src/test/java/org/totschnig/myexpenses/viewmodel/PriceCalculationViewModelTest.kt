package org.totschnig.myexpenses.viewmodel

import android.content.ContentUris
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.db2.storeExchangeRate
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.PRICES_URI
import org.totschnig.myexpenses.provider.requireString
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.retrofit.ExchangeRateApi
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class PriceCalculationViewModelTest: BaseTestWithRepository() {

    private fun buildViewModel() = PriceCalculationViewModel(
            application,
            SavedStateHandle()
        ).also {
            application.injector.inject(it)
        }

    @Test
    fun reCalculatePrices() = runTest {
        //preparePrices
        val testCurrencies = listOf("SAR", "KGS", "KZT", "USD")
        val testPrices = mapOf(
            "2025-01-01" to listOf(0.98, 23.0, 140.0, 3.67),
            "2025-01-06" to listOf(0.978, 24.0, 142.0, 3.661)
        )
        val initialHomeCurrency = "AED"
        testPrices.forEach { (date, prices) ->
            val localDate = LocalDate.parse(date)
            prices.forEachIndexed { index, price ->
                repository.savePrice(initialHomeCurrency, testCurrencies[index], localDate,
                    ExchangeRateApi.Frankfurter, price)
            }
        }

        val viewModel = buildViewModel()
        var newPrices = viewModel.recalculateAndLoad("SAR")
        var newPrices1 = newPrices["2025-01-01"]!!
        var newPrices2 = newPrices["2025-01-06"]!!
        newPrices1.verify("AED", 1.02040816326531)
        newPrices1.verify("KGS", 23.469387755102)
        newPrices1.verify("KZT", 142.85714285714)
        newPrices1.verify("USD", 3.74489795918367)
        newPrices2.verify("AED", 1.02249488752556)
        newPrices2.verify("KGS", 24.5398773006135)
        newPrices2.verify("KZT",145.19427402863)
        newPrices2.verify("USD", 3.74335378323108)

        newPrices = viewModel.recalculateAndLoad("KGS")
        newPrices1 = newPrices["2025-01-01"]!!
        newPrices2 = newPrices["2025-01-06"]!!
        newPrices1.verify("AED", 0.0434782608695652)
        newPrices1.verify("SAR", 0.0426086956521739)
        newPrices1.verify("KZT", 6.08695652173913)
        newPrices1.verify("USD", 0.159565217391304)
        newPrices2.verify("AED", 0.0416666666666667)
        newPrices2.verify("SAR", 0.04075)
        newPrices2.verify("KZT",5.91666666666667)
        newPrices2.verify("USD", 0.152541666666667)

        newPrices = viewModel.recalculateAndLoad("KZT")
        newPrices1 = newPrices["2025-01-01"]!!
        newPrices2 = newPrices["2025-01-06"]!!
        newPrices1.verify("AED", 0.00714285714285714)
        newPrices1.verify("SAR", 0.007)
        newPrices1.verify("KGS", 0.164285714285714)
        newPrices1.verify("USD", 0.0262142857142857)
        newPrices2.verify("AED", 0.00704225352112676)
        newPrices2.verify("SAR", 0.00688732394366197)
        newPrices2.verify("KGS",0.169014084507042)
        newPrices2.verify("USD", 0.0257816901408451)

        newPrices = viewModel.recalculateAndLoad("USD")
        newPrices1 = newPrices["2025-01-01"]!!
        newPrices2 = newPrices["2025-01-06"]!!
        newPrices1.verify("AED", 0.272479564032698)
        newPrices1.verify("SAR", 0.267029972752044)
        newPrices1.verify("KGS", 6.26702997275204)
        newPrices1.verify("KZT", 38.1471389645777)
        newPrices2.verify("AED", 0.273149412728763)
        newPrices2.verify("SAR", 0.26714012564873)
        newPrices2.verify("KGS",6.5555859054903)
        newPrices2.verify("KZT", 38.7872166074843)
     }

    @Test
    fun reCalculateEquivalentAmountsWithoutPrice() = runTest {
        val viewModel = buildViewModel()
        val accountId = insertAccount("Test account", currency = "EUR", dynamic = true)
        val transactionId = insertTransaction(accountId, 100)
        viewModel.recalculateAmountsAndAssert(
            "USD",
            transactionExpectedAmounts = listOf(transactionId.first to null),
            accountExpectedRates = listOf(accountId to null)
        )
    }

    @Test
    fun reCalculateEquivalentAmountsWithStatic() = runTest {
        val viewModel = buildViewModel()
        val accountId = insertAccount("Test account", currency = "EUR", dynamic = false)
        val transactionId = insertTransaction(accountId, 100)
        repository.savePrice("USD", "EUR", LocalDate.now().minusDays(1),
            ExchangeRateApi.Frankfurter, 1.5)
        prefHandler.putString(PrefKey.HOME_CURRENCY, "USD")
        viewModel.recalculateAmountsAndAssert(
            "USD",
            transactionExpectedAmounts = listOf(transactionId.first to null),
            accountExpectedRates = listOf(accountId to 1.5)
        )
    }

    @Test
    fun reCalculateEquivalentAmountsWithApplicableDateInThePast() = runTest {
        val viewModel = buildViewModel()
        val accountId = insertAccount("Test account", currency = "EUR", dynamic = true)
        val transactionId = insertTransaction(accountId, 100)
        repository.savePrice("USD", "EUR", LocalDate.now().minusDays(1),
            ExchangeRateApi.Frankfurter, 1.5)
        viewModel.recalculateAmountsAndAssert(
            "USD",
            transactionExpectedAmounts = listOf(transactionId.first to 150),
            accountExpectedRates = listOf(accountId to 1.5)
        )
    }

    @Test
    fun reCalculateEquivalentAmountsWithNoApplicableDateInThePast() = runTest {
        val viewModel = buildViewModel()
        val accountId = insertAccount("Test account", currency = "EUR", dynamic = true)
        val transactionId = insertTransaction(accountId, 100, date = LocalDateTime.now().minusDays(1))
        repository.savePrice("USD", "EUR", LocalDate.now(),
            ExchangeRateApi.Frankfurter, 1.5)
        viewModel.recalculateAmountsAndAssert(
            "USD",
            transactionExpectedAmounts = listOf(transactionId.first to null),
            accountExpectedRates = listOf(accountId to null)
        )
    }

    @Test
    fun recalculateEquivalentAmountsAll() = runTest {
        val viewModel = buildViewModel()
        val accountId = insertAccount("Test account", currency = "EUR", dynamic = true)
        repository.storeExchangeRate(accountId, 1.6, "EUR", "USD")
        val transactionId = insertTransaction(accountId, 100, equivalentAmount = 160)
        repository.savePrice("USD", "EUR", LocalDate.now().minusDays(1),
            ExchangeRateApi.Frankfurter, 1.5)
        viewModel.recalculateAmountsAndAssert(
            "USD",
            transactionExpectedAmounts = listOf(transactionId.first to 150),
            accountExpectedRates = listOf(accountId to 1.5)
        )
    }

    @Test
    fun recalculateEquivalentAmountsForAllAccounts() = runTest {
        val viewModel = buildViewModel()
        val account1Id = insertAccount("Test account 1", currency = "EUR", dynamic = true)
        val transaction1Id = insertTransaction(account1Id, 100)
        val account2Id = insertAccount("Test account 2", currency = "EUR", dynamic = true)
        val transaction2Id = insertTransaction(account2Id, 100)
        repository.savePrice("USD", "EUR", LocalDate.now().minusDays(1),
            ExchangeRateApi.Frankfurter, 1.5)
        viewModel.recalculateAmountsAndAssert(
            "USD",
            transactionExpectedAmounts = listOf(transaction1Id.first to 150, transaction2Id.first to 150),
            accountExpectedRates = listOf(account1Id to 1.5, account2Id to 1.5)
        )
    }

    @Test
    fun recalculateEquivalentAmountsForOneAccount() = runTest {
        val viewModel = buildViewModel()
        val account1Id = insertAccount("Test account 1", currency = "EUR", dynamic = true)
        val transaction1Id = insertTransaction(account1Id, 100)
        val account2Id = insertAccount("Test account 2", currency = "EUR", dynamic = true)
        val transaction2Id = insertTransaction(account2Id, 100)
        repository.savePrice("USD", "EUR", LocalDate.now().minusDays(1),
            ExchangeRateApi.Frankfurter, 1.5)
        viewModel.recalculateAmountsAndAssert(
            "USD",
            onlyAccount = account1Id,
            transactionExpectedAmounts = listOf(transaction1Id.first to 150, transaction2Id.first to null),
            accountExpectedRates = listOf(account1Id to 1.5)
        )
    }

    @Test
    fun recalculateEquivalentAmountsRounding() = runTest {
        val viewModel = buildViewModel()
        val accountId = insertAccount("Test account 1", currency = "EUR", dynamic = true)
        val transactionId = insertTransaction(accountId, 100)
        repository.savePrice("USD", "EUR", LocalDate.now().minusDays(1),
            ExchangeRateApi.Frankfurter, 1.666666666)
        viewModel.recalculateAmountsAndAssert(
            "USD",
            transactionExpectedAmounts = listOf(transactionId.first to 167),
            accountExpectedRates = listOf(accountId to 1.666666666)
        )
    }


    @Test
    fun recalculateAccountExchangeRateWithoutTransaction() = runTest {
        val viewModel = buildViewModel()
        val accountId = insertAccount("Test account", currency = "EUR", dynamic = true)
        repository.savePrice("USD", "EUR", LocalDate.now(),
            ExchangeRateApi.Frankfurter, 1.5)
        viewModel.recalculateAmountsAndAssert(
            "USD",
            transactionExpectedAmounts = emptyList(),
            accountExpectedRates = listOf(accountId to 1.5)
        )
    }

    @Test
    fun recalculateAccountExchangeRateWithTransaction() = runTest {
        val viewModel = buildViewModel()
        val accountId = insertAccount("Test account", currency = "EUR", dynamic = true)
        val transactionId = insertTransaction(accountId, 100, date = LocalDateTime.now().minusDays(1), equivalentAmount = 190)
        repository.savePrice("USD", "EUR", LocalDate.now(),
            ExchangeRateApi.Frankfurter, 1.5)
        repository.savePrice("USD", "EUR", LocalDate.now().minusDays(1),
            ExchangeRateApi.Frankfurter, 1.6)
        viewModel.recalculateAmountsAndAssert(
            "USD",
            transactionExpectedAmounts = listOf(transactionId.first to 160),
            accountExpectedRates = listOf(accountId to 1.6)
        )
    }


    private suspend fun PriceCalculationViewModel.recalculateAmountsAndAssert(
        newHomeCurrency: String,
        onlyAccount: Long? = null,
        transactionExpectedAmounts: List<Pair<Long, Long?>>,
        accountExpectedRates: List<Pair<Long, Double?>> = emptyList(),
        expectedResultCountTransactions: Int = transactionExpectedAmounts.count { it.second != null },
        expectedResultCountAccounts: Int = accountExpectedRates.count { it.second != null }
    ) {
        prefHandler.putString(PrefKey.HOME_CURRENCY, newHomeCurrency)
        val result = reCalculateEquivalentAmounts(newHomeCurrency, onlyAccount)
        assertThat(result.first).isEqualTo(expectedResultCountTransactions)
        assertThat(result.second).isEqualTo(expectedResultCountAccounts)
        transactionExpectedAmounts.forEach { (transactionId, expectedEquivalentAmount) ->
            contentResolver.query(
                ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, transactionId),
                arrayOf(KEY_EQUIVALENT_AMOUNT),
                null, null, null
            ).useAndAssert {
                movesToFirst()
                if (expectedEquivalentAmount == null) {
                    isNull(0)
                } else {
                    hasLong(0, expectedEquivalentAmount)
                }
            }
        }
        accountExpectedRates.forEach { (accountId, expectedRate) ->
            contentResolver.query(
                ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
                arrayOf(KEY_EXCHANGE_RATE),
                null, null, null
            ).useAndAssert {
                movesToFirst()
                if (expectedRate == null) {
                    isNull(0)
                } else {
                    hasDouble(0, expectedRate)
                }
            }

        }
    }

    private suspend fun PriceCalculationViewModel.recalculateAndLoad(newHomeCurrency: String): Map<String, List<Pair<String, Double>>> {
        reCalculatePrices(newHomeCurrency)
        return contentResolver.query(
            PRICES_URI,
            arrayOf(DatabaseConstants.KEY_DATE, DatabaseConstants.KEY_COMMODITY, DatabaseConstants.KEY_VALUE),
            "${DatabaseConstants.KEY_CURRENCY} = ?",
            arrayOf(newHomeCurrency),
            null
        )!!.useAndMapToList { Triple(it.requireString(0), it.requireString(1), it.getDouble(2)) }
            .groupBy({ it.first}, { it.second to it.third })
    }

    private fun List<Pair<String, Double>>.verify(currency: String, value: Double) {
        assertThat(this.first { it.first == currency }.second).isWithin(0.00000000001).of(value)
    }

}