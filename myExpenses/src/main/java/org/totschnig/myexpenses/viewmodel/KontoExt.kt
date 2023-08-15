package org.totschnig.myexpenses.viewmodel

import org.kapott.hbci.structures.Konto
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model2.Account

fun Konto.toAccount(bank: Pair<Long, String>, openingBalance: Long) = Account(
    label = bank.second,
    accountNumber = dbNumber,
    currency = curr,
    type = AccountType.BANK,
    bankId = bank.first,
    openingBalance = openingBalance
)

val Konto.dbNumber: String
    get() = number + (subnumber?.let { "/$it" } ?: "")