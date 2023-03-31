//Copyright (c) 2010 Denis Solonenko (Financisto)
//made available under the terms of the GNU Public License v2.0
//adapted to My Expenses by Michael Totschnig
package org.totschnig.myexpenses.export.qif

import org.totschnig.myexpenses.export.qif.QifUtils.trimFirstChar
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model2.Account
import java.io.IOException
import java.math.BigDecimal

class QifAccount {
    var type = ""
    @JvmField
    var memo = ""
    var desc = ""
    var openingBalance: BigDecimal? = null
    @JvmField
    var dbAccount: Account? = null
    @JvmField
    val transactions: ArrayList<QifTransaction> = ArrayList()
    fun toAccount(currency: CurrencyUnit): Account {
        return Account(
            label = memo,
            currency = currency.code,
            openingBalance = Money(currency, (if (openingBalance == null) BigDecimal.ZERO else openingBalance)!!).amountMinor,
            description = desc,
            type = AccountType.fromQifName(type)
        )
    }

    @Throws(IOException::class)
    fun readFrom(r: QifBufferedReader) {
        var line: String
        while (r.readLine().also { line = it } != null) {
            if (line.startsWith("^")) {
                break
            }
            if (line.startsWith("N")) {
                memo = trimFirstChar(line)
            } else if (line.startsWith("T")) {
                type = trimFirstChar(line)
            } else if (line.startsWith("D")) {
                desc = trimFirstChar(line)
            }
        }
    }

    companion object {
        fun fromAccount(account: Account): QifAccount {
            val qifAccount = QifAccount()
            qifAccount.type = account.type.toQifName()
            qifAccount.memo = account.label
            qifAccount.desc = account.description
            return qifAccount
        }
    }
}