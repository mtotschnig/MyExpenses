package org.totschnig.myexpenses.export.qif

import com.google.common.truth.Truth
import org.junit.Test
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.io.ImportTransaction
import java.math.BigDecimal
import java.util.Date

const val account1 = "Konto 1"
const val account2 = "Konto 2"

class QifReduceTransfersTest {
    @Test
    fun reduceTransferBetweenMainTransactions() {
        val now = Date(System.currentTimeMillis())
        val fromAccount = ImportAccount(memo = account1,
            transactions = listOf(
                ImportTransaction.Builder()
                    .toAccount(account2)
                    .date(now)
                    .amount(BigDecimal(-5)).build()
            ))
        val toAccount = ImportAccount(memo = account2,
            transactions = listOf(
                ImportTransaction.Builder()
                    .toAccount(account1)
                    .date(now)
                    .amount(BigDecimal(5)).build()
            ))
        val reduced = QifUtils.reduceTransfers(listOf(fromAccount, toAccount))
        Truth.assertThat(reduced[0].transactions.size).isEqualTo(0)
        Truth.assertThat(reduced[1].transactions.size).isEqualTo(1)
    }

    @Test
    fun reduceTransferWithSplitTransaction() {
        val now = Date(System.currentTimeMillis())
        val fromAccount = ImportAccount(memo = account1,
            transactions = listOf(
                ImportTransaction.Builder()
                    .toAccount(account2)
                    .date(now)
                    .amount(BigDecimal(-5))
                    .addSplit(ImportTransaction.Builder()
                        .toAccount(account2)
                        .date(now)
                        .amount(BigDecimal(-5)))
                    .build()
            ))
        val toAccount = ImportAccount(memo = account2,
            transactions = listOf(
                ImportTransaction.Builder()
                    .toAccount(account1)
                    .date(now)
                    .amount(BigDecimal(5)).build()
            ))
        val reduced = QifUtils.reduceTransfers(listOf(fromAccount, toAccount))
        Truth.assertThat(reduced[0].transactions.size).isEqualTo(1)
        Truth.assertThat(reduced[1].transactions.size).isEqualTo(0)
    }
}