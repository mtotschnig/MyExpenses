package org.totschnig.myexpenses.next.core.model.ledger

/**
 * Own accounts as known from the account list
 *
 * @param dailyIds cash, bank and credit card accounts
 */
data class OwnAccounts(val dailyIds: Set<Long>, val labels: Map<Long, String>)

/**
 * IBANs are compared without spaces and case
 */
fun normalizeIban(iban: String) = iban.filterNot { it.isWhitespace() }.uppercase()

/**
 * The account a payment goes to, if it is a movement between own accounts: a transfer in the app,
 * or a payment whose counterpart has the IBAN of an own account, e.g. imported from the bank
 *
 * @param ownIbans normalized IBANs of own accounts to their id, see [normalizeIban]
 */
fun ownTransferTarget(accountId: Long, transferAccountId: Long?, counterpartIban: String?, ownIbans: Map<String, Long>) =
    transferAccountId ?: counterpartIban?.let { ownIbans[normalizeIban(it)] }?.takeIf { it != accountId }
