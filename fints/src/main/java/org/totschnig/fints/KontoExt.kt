package org.totschnig.fints

import org.kapott.hbci.structures.Konto
import org.totschnig.myexpenses.db2.BankingAttribute
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Bank

fun Konto.toAccount(bank: Bank, openingBalance: Long) = Account(
    label = bank.bankName,
    description = type,
    currency = curr,
    type = AccountType.BANK,
    bankId = bank.id,
    openingBalance = openingBalance
)

val Konto.asAttributes
    get() = buildMap {
        number?.let { put(BankingAttribute.NUMBER, it) }
        subnumber?.let { put(BankingAttribute.SUBNUMBER, it) }
        iban?.let { put(BankingAttribute.IBAN, it) }
    }


val Konto.dbNumber: String
    get() = number + (subnumber?.let { "/$it" } ?: "")