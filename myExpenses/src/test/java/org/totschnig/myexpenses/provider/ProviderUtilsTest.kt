package org.totschnig.myexpenses.provider

import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.CurrencyFormatter
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ProviderUtilsTest {
    private val repository: Repository = Repository(
        ApplicationProvider.getApplicationContext<MyApplication>(),
        Mockito.mock(CurrencyContext::class.java),
        Mockito.mock(CurrencyFormatter::class.java),
        Mockito.mock(PrefHandler::class.java)
    )

    private lateinit var euroAccount: Account
    private lateinit var dollarAccount: Account

    @Before
    fun setupAccounts() {
        euroAccount = Account(
            "EUR-Account",
            CurrencyUnit(Currency.getInstance("EUR")),
            0L,
            null,
            AccountType.CASH,
            Account.DEFAULT_COLOR
        )
        euroAccount.save()
        dollarAccount = Account(
            "USD-Account",
            CurrencyUnit(Currency.getInstance("USD")),
            0L,
            null,
            AccountType.CASH,
            Account.DEFAULT_COLOR
        )
        dollarAccount.save()
    }

    private fun buildFromExtras(extras: Bundle) = ProviderUtils.buildFromExtras(repository, extras)

    @Test
    fun shouldPickAccountBasedOnCurrency() {
        var extras = Bundle()
        extras.putString(Transactions.CURRENCY, "EUR")
        var transaction = buildFromExtras(extras)
        assertEquals(euroAccount.id, transaction.accountId)
        extras = Bundle()
        extras.putString(Transactions.CURRENCY, "USD")
        transaction = buildFromExtras(extras)
        assertEquals(dollarAccount.id, transaction.accountId)
    }

    @Test
    fun shouldPickAccountBasedOnLabel() {
        var extras = Bundle()
        extras.putString(Transactions.ACCOUNT_LABEL, "EUR-Account")
        var transaction = buildFromExtras(extras)
        assertEquals(euroAccount.id, transaction.accountId)
        extras = Bundle()
        extras.putString(Transactions.ACCOUNT_LABEL, "USD-Account")
        transaction = buildFromExtras(extras)
        assertEquals(dollarAccount.id, transaction.accountId)
    }

    @Test
    fun shouldSetAmount() {
        val extras = Bundle()
        extras.putString(Transactions.CURRENCY, "EUR")
        extras.putLong(Transactions.AMOUNT_MICROS, 1230000)
        val transaction = buildFromExtras(extras)
        assertEquals(
            Money(CurrencyUnit(Currency.getInstance("EUR")), 123L),
            transaction.amount
        )
    }

    @Test
    fun shouldSetDate() {
        val extras = Bundle()
        val date = (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)) / 1000
        extras.putLong(Transactions.DATE, date)
        val transaction = buildFromExtras(extras)
        assertEquals(date, transaction.date)
    }

    @Test
    fun shouldSetPayee() {
        val extras = Bundle()
        val payee = "John Doe"
        extras.putString(Transactions.PAYEE_NAME, payee)
        val transaction = buildFromExtras(extras)
        assertEquals(payee, transaction.payee)
    }

    @Test
    fun shouldSetMainCategory() {
        val extras = Bundle()
        val category = "A"
        extras.putString(Transactions.CATEGORY_LABEL, category)
        val transaction = buildFromExtras(extras)
        assertEquals(repository.findCategory(category), transaction.catId)
        assertEquals(category, transaction.label)
    }

    @Test
    fun shouldSetSubCategory() {
        val extras = Bundle()
        val category = "B:C"
        extras.putString(Transactions.CATEGORY_LABEL, category)
        val transaction = buildFromExtras(extras)
        assertEquals(
            repository.findCategory("C", repository.findCategory("B")),
            transaction.catId
        )
        assertEquals(category, transaction.label)
    }

    @Test
    fun shouldSetComment() {
        val extras = Bundle()
        val comment = "A note"
        extras.putString(Transactions.COMMENT, comment)
        val transaction = buildFromExtras(extras)
        assertEquals(comment, transaction.comment)
    }

    @Test
    fun shouldSetMethod() {
        val extras = Bundle()
        val method = "CHEQUE"
        extras.putString(Transactions.METHOD_LABEL, method)
        val transaction = buildFromExtras(extras)
        assertEquals(PaymentMethod.find(method), transaction.methodId)
    }

    @Test
    fun shouldSetReferenceNumber() {
        val extras = Bundle()
        val number = "1"
        extras.putString(Transactions.REFERENCE_NUMBER, number)
        val transaction = buildFromExtras(extras)
        assertEquals(number, transaction.referenceNumber)
    }

    @Test
    fun shouldBuildTransfer() {
        val extras = Bundle()
        extras.putInt(Transactions.OPERATION_TYPE, Transactions.TYPE_TRANSFER)
        extras.putString(Transactions.ACCOUNT_LABEL, "EUR-Account")
        extras.putString(Transactions.TRANSFER_ACCOUNT_LABEL, "USD-Account")
        val transaction = buildFromExtras(extras)
        Assert.assertTrue(transaction is Transfer)
        assertEquals(dollarAccount.id, transaction.transferAccountId.toLong())
        assertEquals("USD-Account", transaction.label)
    }
}