package org.totschnig.myexpenses.provider

import android.os.Bundle
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.findCategory
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.model.Transfer
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ProviderUtilsTest: BaseTestWithRepository() {

    private var euroAccount: Long = 0
    private var dollarAccount: Long = 0

    @Before
    fun setupAccounts() {

        euroAccount = insertAccount(
            label = "EUR-Account",
            currency = "EUR"
        )
        dollarAccount = insertAccount(
            label = "USD-Account",
            currency = "USD"
        )
    }

    private fun buildFromExtras(extras: Bundle) = ProviderUtils.buildFromExtras(repository, extras)!!

    @Test
    fun shouldPickAccountBasedOnCurrency() {
        var extras = Bundle()
        extras.putString(Transactions.CURRENCY, "EUR")
        var transaction = buildFromExtras(extras)
        assertEquals(euroAccount, transaction.accountId)
        extras = Bundle()
        extras.putString(Transactions.CURRENCY, "USD")
        transaction = buildFromExtras(extras)
        assertEquals(dollarAccount, transaction.accountId)
    }

    @Test
    fun shouldPickAccountBasedOnLabel() {
        var extras = Bundle()
        extras.putString(Transactions.ACCOUNT_LABEL, "EUR-Account")
        var transaction = buildFromExtras(extras)
        assertEquals(euroAccount, transaction.accountId)
        extras = Bundle()
        extras.putString(Transactions.ACCOUNT_LABEL, "USD-Account")
        transaction = buildFromExtras(extras)
        assertEquals(dollarAccount, transaction.accountId)
    }

    @Test
    fun shouldSetAmount() {
        val extras = Bundle()
        extras.putString(Transactions.CURRENCY, "EUR")
        extras.putLong(Transactions.AMOUNT_MICROS, 1230000)
        val transaction = buildFromExtras(extras)
        assertEquals(
            123,
            transaction.amount.amountMinor
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
        assertEquals(payee, transaction.party?.name)
    }

    @Test
    fun shouldSetMainCategory() {
        val extras = Bundle()
        val category = "A"
        extras.putString(Transactions.CATEGORY_LABEL, category)
        val transaction = buildFromExtras(extras)
        assertEquals(repository.findCategory(category), transaction.catId)
        assertEquals(category, transaction.categoryPath)
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
        assertEquals(category, transaction.categoryPath)
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
        assertEquals(repository.findPaymentMethod(method), transaction.methodId)
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
        assertEquals(dollarAccount, transaction.transferAccountId.toLong())
    }
}