package org.totschnig.myexpenses.export.qif

import com.google.common.truth.Truth
import junit.framework.TestCase
import org.totschnig.myexpenses.export.qif.QifUtils.twoSidesOfTheSameTransfer
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.io.ImportTransaction
import java.math.BigDecimal
import java.util.Date

class QifUtilTwoSidesOfTheSameTransferTest : TestCase() {


    fun testShouldMatchTwoSidesOfSameTransfer() {
        val now = Date(System.currentTimeMillis())
        val fromAccount = ImportAccount(memo = "Konto 1")
        val toAccount = ImportAccount(memo = "Konto 2")
        val fromTransaction = ImportTransaction.Builder()
            .toAccount(toAccount.memo)
            .date(now)
            .amount(BigDecimal(5))
            .build()!!
        val toTransaction = ImportTransaction.Builder()
            .toAccount(fromAccount.memo)
            .date(now)
            .amount(BigDecimal(-5))
            .build()!!
        Truth.assertThat(twoSidesOfTheSameTransfer(
            fromAccount,
            fromTransaction,
            toAccount,
            toTransaction
        )).isTrue()
    }

    fun testShouldDistinguishNonMatchingDates() {
        val now = Date(System.currentTimeMillis())
        val fromAccount = ImportAccount(memo = "Konto 1")
        val toAccount = ImportAccount(memo = "Konto 2")
        val fromTransaction = ImportTransaction.Builder()
            .toAccount(toAccount.memo)
            .date(now)
            .amount(BigDecimal(5))
            .build()!!
        val toTransaction = ImportTransaction.Builder()
            .toAccount(fromAccount.memo)
            .date(Date(System.currentTimeMillis() - 100000))
            .amount(BigDecimal(-5))
            .build()!!
        Truth.assertThat(twoSidesOfTheSameTransfer(
            fromAccount,
            fromTransaction,
            toAccount,
            toTransaction
        )).isFalse()
    }

    fun testShouldDistinguishNonMatchingAccounts() {
        val now = Date(System.currentTimeMillis())
        val fromAccount = ImportAccount(memo = "Konto 1")
        val toAccount = ImportAccount(memo = "Konto 2")
        val fromTransaction = ImportTransaction.Builder()
            .toAccount(toAccount.memo)
            .date(now)
            .amount(BigDecimal(5))
            .build()!!
        val toTransaction = ImportTransaction.Builder()
            .toAccount("Konto 3")
            .date(now)
            .amount(BigDecimal(-5))
            .build()!!
        Truth.assertThat(twoSidesOfTheSameTransfer(
            fromAccount,
            fromTransaction,
            toAccount,
            toTransaction
        )).isFalse()
    }
}