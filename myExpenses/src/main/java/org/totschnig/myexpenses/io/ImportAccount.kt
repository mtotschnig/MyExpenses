package org.totschnig.myexpenses.io

import okhttp3.internal.toImmutableList
import org.totschnig.myexpenses.export.qif.QifBufferedReader
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.export.qif.QifUtils.isTransferCategory
import org.totschnig.myexpenses.export.qif.QifUtils.parseDate
import org.totschnig.myexpenses.export.qif.QifUtils.parseMoney
import org.totschnig.myexpenses.export.qif.QifUtils.trimFirstChar
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus.Companion.fromQifName
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model2.Account
import java.math.BigDecimal
import java.util.Date

data class ImportAccount(
    val type: String = "",
    val memo: String  = "",
    val desc: String = "",
    val openingBalance: BigDecimal = BigDecimal.ZERO,
    val transactions: List<ImportTransaction> = mutableListOf()
) {
    fun toAccount(currency: CurrencyUnit): Account {
        return Account(
            label = memo,
            currency = currency.code,
            openingBalance = Money(currency, openingBalance).amountMinor,
            description = desc,
            type = AccountType.fromQifName(type)
        )
    }

    class Builder {
        var type: String? = null
            private set
        var memo: String? = null
            private set
        var desc: String? = null
            private set
        var openingBalance: BigDecimal? = null
            private set
        private var transactions: MutableList<ImportTransaction> = mutableListOf()

        fun type(type: String) = apply { this.type = type }
        fun memo(memo: String) = apply { this.memo = memo }
        fun desc(desc: String) = apply { this.desc = desc }
        fun openingBalance(openingBalance: BigDecimal) = apply { this.openingBalance = openingBalance }
        fun addTransaction(transaction: ImportTransaction) = apply { transactions.add(transaction) }
        fun build() = ImportAccount(type ?: "", memo ?: "", desc ?: "", openingBalance ?: BigDecimal.ZERO, transactions.toImmutableList())
    }

    companion object {
        fun readFrom(r: QifBufferedReader): Builder {
            val builder = Builder()
            var line : String?
            do {
                line = r.readLine() ?: break
                if (line.startsWith("^")) {
                    break
                }
                if (line.startsWith("N")) {
                    builder.memo(trimFirstChar(line))
                } else if (line.startsWith("T")) {
                    builder.type(trimFirstChar(line))
                } else if (line.startsWith("D")) {
                    builder.desc(trimFirstChar(line))
                }

            } while (true)
            return builder
        }
    }
}

data class ImportTransaction constructor(
    val date: Date?,
    val amount: BigDecimal,
    val payee: String?,
    val memo: String?,
    val category: String?,
    val categoryClass: String?,
    val toAccount: String?,
    val status: String?,
    val number: String?,
    val splits: List<ImportTransaction>?
) {
    class Builder {
        private lateinit var date: Date
        private lateinit var amount: BigDecimal
        private var payee: String? = null
        private var memo: String? = null
        private var category: String? = null
        private var categoryClass: String? = null
        private var toAccount: String? = null
        private var status: String? = null
        private var number: String? = null
        private var splits: MutableList<ImportTransaction> = mutableListOf()

        fun date(date: Date) = apply { this.date = date }
        fun amount(amount: BigDecimal) = apply { this.amount = amount }
        fun payee(payee: String) = apply { this.payee = payee }
        fun memo(memo: String) = apply { this.memo = memo }
        fun category(category: String) = apply { this.category = category }
        fun categoryClass(categoryClass: String) = apply { this.categoryClass = categoryClass }
        fun toAccount(toAccount: String) = apply { this.toAccount = toAccount }
        fun status(status: String) = apply { this.status = status }
        fun number(number: String) = apply { this.number = number }
        fun addSplit(split: Builder) = apply { splits.add(split.date(date).build()) }
        fun build() = ImportTransaction(
            date,
            amount,
            payee,
            memo,
            category,
            categoryClass,
            toAccount,
            status,
            number,
            if (splits.isEmpty()) null else splits.toImmutableList()
        )

    }

    val isSplit get() = splits != null

    val isOpeningBalance get() = payee == "Opening Balance"

    companion object {
        fun readFrom(r: QifBufferedReader, dateFormat: QifDateFormat, currency: CurrencyUnit): ImportTransaction {
            val builder = Builder()
            var split: Builder? = null
            val splits = mutableListOf<Builder>()
            var line : String?
            do {
                line = r.readLine() ?: break
                if (line.startsWith("^")) {
                    break
                }
                if (line.startsWith("D")) {
                    builder.date(parseDate(trimFirstChar(line), dateFormat))
                } else if (line.startsWith("T")) {
                    builder.amount(parseMoney(trimFirstChar(line), currency))
                } else if (line.startsWith("P")) {
                    builder.payee(trimFirstChar(line))
                } else if (line.startsWith("M")) {
                    builder.memo(trimFirstChar(line))
                } else if (line.startsWith("C")) {
                    builder.status(trimFirstChar(line))
                } else if (line.startsWith("N")) {
                    builder.number(trimFirstChar(line))
                } else if (line.startsWith("L")) {
                    parseCategory(builder, line)
                } else if (line.startsWith("S")) {
                    split?.let { splits.add(it) }
                    split = Builder()
                    parseCategory(split, line)
                } else if (line.startsWith("$")) {
                    split?.amount(parseMoney(trimFirstChar(line), currency))
                } else if (line.startsWith("E")) {
                    split?.memo(trimFirstChar(line))
                }
            } while (true)
            split?.let { splits.add(it) }
            splits.forEach {
                builder.addSplit(it)
            }
            return builder.build()
        }

        private fun parseCategory(builder: Builder, line: String) {
            var category = trimFirstChar(line)
            val i = category.indexOf('/')
            if (i != -1) {
                builder.categoryClass(category.substring(i + 1))
                category = category.substring(0, i)
            }
            if (isTransferCategory(category)) {
                builder.toAccount(category.substring(1, category.length - 1))
            } else {
                builder.category(category)
            }
        }
    }

    fun toTransaction(a: Account, currencyUnit: CurrencyUnit?): Transaction {
        val t: Transaction
        val m = Money(currencyUnit!!, amount)
        t = if (isSplit) {
            SplitTransaction(a.id, m)
        } else if (isTransfer) {
            Transfer(a.id, m)
        } else {
            Transaction(a.id, m)
        }
        if (date != null) {
            t.setDate(date)
        }
        t.comment = memo
        t.crStatus = fromQifName(status)
        t.referenceNumber = number
        return t
    }

    val isTransfer get() = !isSplit && toAccount != null
}