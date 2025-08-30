package org.totschnig.myexpenses.io

import okhttp3.internal.toImmutableList
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus.Companion.fromQifName
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PREDEFINED_NAME_CASH
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model2.Account
import java.math.BigDecimal
import java.util.Date

data class ImportAccount(
    val type: String? = PREDEFINED_NAME_CASH,
    val memo: String  = "",
    val desc: String = "",
    val openingBalance: BigDecimal = BigDecimal.ZERO,
    val transactions: List<ImportTransaction> = mutableListOf()
) {
    fun toAccount(currency: CurrencyUnit, type: AccountType): Account {
        return Account(
            label = memo,
            currency = currency.code,
            openingBalance = Money(currency, openingBalance).amountMinor,
            description = desc,
            type = type
        )
    }

    class Builder {
        var type: String? = null
            private set
        var memo: String? = null
            private set
        private var desc: String? = null
        private var openingBalance: BigDecimal? = null
        private var transactions: MutableList<ImportTransaction.Builder> = mutableListOf()

        fun type(type: String) = apply {
            this.type = AccountType.qif2Internal(type)
        }

        fun memo(memo: String) = apply { this.memo = memo }
        fun desc(desc: String) = apply { this.desc = desc }
        fun openingBalance(openingBalance: BigDecimal) = apply { this.openingBalance = openingBalance }
        fun addTransaction(transaction: ImportTransaction.Builder) = apply { transactions.add(transaction) }
        fun build() = ImportAccount(
            type = type,
            memo = memo ?: "",
            desc = desc ?: "",
            openingBalance = openingBalance ?: BigDecimal.ZERO,
            transactions = transactions.mapNotNull { it.build() }
        )
    }

}

data class ImportTransaction(
    val date: Date,
    val valueDAte: Date?,
    val amount: BigDecimal,
    val payee: String?,
    val memo: String?,
    val category: String?,
    val categoryClass: String?,
    val toAccount: String?,
    val toAmount: BigDecimal?,
    val status: String?,
    val number: String?,
    val method: String?,
    val tags : List<String>?,
    val splits: List<ImportTransaction>?
) {
    class Builder {
        private var date: Date = Date()
        private var valueDate: Date? = null
        var amount: BigDecimal? = null
        var payee: String? = null
        private var memo: String? = null
        var category: String? = null
        var categoryClass: String? = null
        var toAccount: String? = null
        var toAmount: BigDecimal? = null
        private var status: String? = null
        private var number: String? = null
        private var method: String? = null
        var tags: MutableList<String> = mutableListOf()
        var splits: MutableList<Builder> = mutableListOf()

        fun date(date: Date) = apply { this.date = date }

        fun valueDate(valueDate: Date) = apply { this.valueDate = valueDate }
        fun amount(amount: BigDecimal) = apply { this.amount = amount }
        fun payee(payee: String) = apply { this.payee = payee }
        fun memo(memo: String) = apply { this.memo = memo }
        fun category(category: String) = apply { this.category = category }
        fun categoryClass(categoryClass: String) = apply { this.categoryClass = categoryClass }
        fun toAccount(toAccount: String) = apply { this.toAccount = toAccount }
        fun status(status: String) = apply { this.status = status }
        fun number(number: String) = apply { this.number = number }

        fun method(method: String) = apply { this.method = method }
        fun addSplit(split: Builder) = apply { splits.add(split.date(date)) }

        fun addTags(tagCollection: Collection<String>) = apply { tags.addAll(tagCollection) }

        val isOpeningBalance get() = payee == "Opening Balance"

        fun build(): ImportTransaction? = amount?.let {
            ImportTransaction(
                date,
                valueDate,
                it,
                payee,
                memo,
                category,
                categoryClass,
                toAccount,
                toAmount,
                status,
                number,
                method,
                if (tags.isEmpty()) null else tags.toImmutableList(),
                if (splits.isEmpty()) null else splits.mapNotNull { split -> split.build() }
            )
        }
    }

    val isSplit get() = splits != null

    fun toTransaction(a: Account, currencyUnit: CurrencyUnit): Transaction {
        val t: Transaction
        val m = Money(currencyUnit, amount)
        t = if (isSplit) {
            SplitTransaction(a.id, m)
        } else if (isTransfer) {
            Transfer(a.id, m)
        } else {
            Transaction(a.id, m)
        }
        t.setDate(date)
        t.comment = memo
        t.crStatus = fromQifName(status)
        t.referenceNumber = number
        return t
    }

    val isTransfer get() = !isSplit && toAccount != null
}