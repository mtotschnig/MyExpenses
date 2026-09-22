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
    openingBalance = openingBalance,
    color = bank.asWellKnown?.color ?: Account.DEFAULT_COLOR
)

fun Konto.getAsAttributes(gv: GV) = buildMap {
    name?.let { put(BankingAttribute.NAME, it) }
    number?.let { put(BankingAttribute.NUMBER, it) }
    subnumber?.let { put(BankingAttribute.SUBNUMBER, it) }
    iban?.let { put(BankingAttribute.IBAN, it) }
    bic?.let { put(BankingAttribute.BIC, it) }
    blz?.let { put(BankingAttribute.BLZ, it) }
    put(BankingAttribute.GESCHAEFTS_VORFALL, gv.name)
}

val Konto.dbNumber: String
    get() = number + (subnumber?.let { "/$it" } ?: "")

val Konto.kontoType
    get() = KontoType.find(acctype?.trim()?.ifEmpty { null }?.toInt())

val KontoType.isSupported: Boolean
    get() = when (this) {
        KontoType.WERTPAPIERDEPOT, KontoType.FONDSDEPOT -> false
        else -> true
    }

//https://github.com/willuhn/hibiscus/pull/158/changes
private val CREDIT_CARD_NUMBER_REGEX = Regex("[0-9]{12,19}")

val Konto.isCreditCardAccount: Boolean
    get() = kontoType == KontoType.KREDITKARTE ||
            number?.replace(" ", "")?.replace("-", "")?.matches(CREDIT_CARD_NUMBER_REGEX) == true