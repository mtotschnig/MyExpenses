package org.totschnig.fints

import org.kapott.hbci.structures.Konto
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.model2.Bank

fun Konto.toAccount(bank: Bank, openingBalance: Long) = Account(
    label = bank.bankName,
    accountNumber = dbNumber,
    currency = curr,
    type = AccountType.BANK,
    bankId = bank.id,
    openingBalance = openingBalance
)

val Konto.dbNumber: String
    get() = number + (subnumber?.let { "/$it" } ?: "")