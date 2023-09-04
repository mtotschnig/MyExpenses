package org.totschnig.myexpenses.export.qif

import com.google.common.truth.Truth
import org.junit.Test
import java.math.BigDecimal
import java.util.Date

class QifReduceTransfersTest {
    @Test
    fun reduceTransferBetweenMainTransactions() {
        val now = Date(System.currentTimeMillis())
        val fromAccount = QifAccount().apply {
            memo = "Konto 1"
        }
        val toAccount = QifAccount().apply {
            memo = "Konto 2"
        }
        val fromTransaction = QifTransaction().apply {
            this.toAccount = toAccount.memo
            date = now
            amount = BigDecimal(-5)
        }
        fromAccount.transactions.add(fromTransaction)
        val toTransaction = QifTransaction().apply {
            this.toAccount = fromAccount.memo
            date = now
            amount = BigDecimal(5)
        }
        toAccount.transactions.add(toTransaction)
        val map = mapOf (
            "Konto 1" to fromAccount,
            "Konto 2" to toAccount
        )
        QifUtils.reduceTransfers(listOf(fromAccount, toAccount), map)
        Truth.assertThat(fromAccount.transactions.size).isEqualTo(1)
        Truth.assertThat(toAccount.transactions.size).isEqualTo(0)
    }

    @Test
    fun reduceTransferWithSplitTransaction() {
        val now = Date(System.currentTimeMillis())
        val fromAccount = QifAccount().apply {
            memo = "Konto 1"
        }
        val toAccount = QifAccount().apply {
            memo = "Konto 2"
        }
        val fromTransaction = QifTransaction().apply {
            this.toAccount = toAccount.memo
            date = now
            amount = BigDecimal(-5)
            addSplit(QifTransaction().apply {
                this.toAccount = toAccount.memo
                date = now
                amount = BigDecimal(-5)
            })
        }
        fromAccount.transactions.add(fromTransaction)
        val toTransaction = QifTransaction().apply {
            this.toAccount = fromAccount.memo
            date = now
            amount = BigDecimal(5)
        }
        toAccount.transactions.add(toTransaction)
        val map = mapOf (
            "Konto 1" to fromAccount,
            "Konto 2" to toAccount
        )
        QifUtils.reduceTransfers(listOf(fromAccount, toAccount), map)
        Truth.assertThat(fromAccount.transactions.size).isEqualTo(1)
        Truth.assertThat(toAccount.transactions.size).isEqualTo(0)
    }
}