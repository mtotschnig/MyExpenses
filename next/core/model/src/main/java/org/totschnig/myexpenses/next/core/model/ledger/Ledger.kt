package org.totschnig.myexpenses.next.core.model.ledger

import java.time.LocalDate

/**
 * The transactions of the last [HISTORY_MONTHS] months, read once for all analyses of the new UI
 * (e.g. contracts and the balance). Each analysis derives the facts it needs from them,
 * see the README of this package. Equal transactions give an equal ledger, so that the analyses
 * are not computed again when the transactions are read again unchanged.
 */
data class Ledger(
    val transactions: List<LedgerTransaction>,
    val ownAccounts: OwnAccounts,
) {
    companion object {
        /** A bit more than two years, so that yearly contracts have been debited at least twice */
        const val HISTORY_MONTHS = 26L
    }
}

/**
 * A debit or credit as all analyses read it. Split transactions are taken as a whole, since the
 * payee is stored with the parent; their [parts] tell how the amount is shared among categories.
 */
data class LedgerTransaction(
    val id: Long,
    val date: LocalDate,
    /** In minor units of the account currency; debits are negative */
    val amount: Long,
    /** [amount] in minor units of the home currency, so that amounts of all accounts can be summed up */
    val homeAmount: Long = amount,
    val accountId: Long,
    val accountLabel: String? = null,
    val currency: String? = null,
    /** The currency actually paid in, e.g. abroad, if one was entered */
    val originalCurrency: String? = null,
    val payeeId: Long? = null,
    val payeeName: String? = null,
    val comment: String? = null,
    val categoryId: Long? = null,
    val categoryPath: String? = null,
    val categoryIcon: String? = null,
    val templateId: Long? = null,
    /** The other account of a transfer entered in the app */
    val transferAccountId: Long? = null,
    /**
     * For a movement between own accounts: the account the money goes to. Set for transfers in the
     * app and for payments whose counterpart has the IBAN of an own account, see [ownTransferTarget].
     */
    val targetAccountId: Long? = null,
    /** Split into [parts], its own category is the one that marks split transactions */
    val isSplit: Boolean = false,
    /** In a sealed account or of a sealed debt: changes are rejected by the database */
    val isSealed: Boolean = false,
    val tagIds: Set<Long> = emptySet(),
    /**
     * The parts of a split transaction, empty for other transactions. Only analyses by category
     * use them, all others take the transaction as a whole, so nothing is counted twice.
     */
    val parts: List<LedgerPart> = emptyList(),
) {
    /**
     * How the amount is shared among categories: the [parts] of a split transaction, otherwise the
     * transaction itself as the only part
     */
    val categoryParts: List<LedgerPart>
        get() = parts.ifEmpty { listOf(LedgerPart(amount, homeAmount, categoryId, transferAccountId)) }

    /** Paid in another currency than the one of the account */
    val isForeign: Boolean
        get() = originalCurrency != null && originalCurrency != currency
}

/**
 * One part of a split transaction, with its own category, or a transfer
 *
 * @param amount in minor units of the account currency, debits are negative
 * @param homeAmount in minor units of the home currency
 */
data class LedgerPart(
    val amount: Long,
    val homeAmount: Long = amount,
    val categoryId: Long? = null,
    val transferAccountId: Long? = null,
)
