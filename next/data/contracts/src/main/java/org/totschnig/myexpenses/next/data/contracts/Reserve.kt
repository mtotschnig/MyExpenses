package org.totschnig.myexpenses.next.data.contracts

import org.totschnig.myexpenses.next.core.model.ledger.Ledger

/**
 * Recognizes contracts that put money aside ("Rücklagen"), e.g. a building savings contract,
 * a savings plan or a standing order to a savings account. They are fixed outflows like other
 * contracts, but no costs: the money stays with the user.
 */
object Reserve {
    /**
     * Matched at the start of a word. Not just "spar", since "Sparkasse" and "Sparda-Bank" are banks.
     */
    private val PREFIXES = listOf(
        "bauspar", "sparplan", "sparkonto", "sparbuch", "sparvertrag", "sparrate", "sparbrief", "sparen",
        "tagesgeld", "festgeld", "depot", "fonds", "wertpapier", "vermögenswirksam", "rücklage", "altersvorsorge",
        "saving", "invest", "brokerage", "etf"
    )

    /** Matched as whole word only */
    private val WORDS = listOf("vl")

    private val NON_LETTERS = Regex("[^\\p{L}]+")

    /**
     * Suggests from the category, payee and purpose of the last payment whether a contract is a reserve.
     * Only debits can be reserves.
     */
    fun suggests(contract: Contract): Boolean {
        if (contract.isIncome) return false
        val last = contract.lastTransaction
        return listOfNotNull(last.categoryPath, last.payeeName, last.comment, contract.customName)
            .any { text ->
                text.lowercase().split(NON_LETTERS).any { word ->
                    word.isNotEmpty() && (PREFIXES.any { word.startsWith(it) } || word in WORDS)
                }
            }
    }
}

/**
 * Of the movements between own accounts, only money going from a daily account to another kind of
 * account (e.g. a savings account) can form a contract, a reserve. Moving money between daily
 * accounts, e.g. paying the credit card, and the receiving side of a movement are no contracts.
 * Other transactions are kept.
 */
fun List<ContractTransaction>.withoutMovementsOtherThanReserves(dailyAccountIds: Set<Long>) = filter { transaction ->
    val target = transaction.targetAccountId ?: return@filter true
    transaction.amount < 0 && transaction.accountId in dailyAccountIds && target !in dailyAccountIds
}

/**
 * The transactions of [ledger] contracts are detected from, amounts in the home currency.
 * Movements between own accounts are named after the account the money goes to, since their
 * payee is the user or missing.
 */
fun contractTransactions(ledger: Ledger): List<ContractTransaction> = ledger.transactions.map { transaction ->
    with(transaction) {
        ContractTransaction(
            id = id,
            date = date,
            amount = homeAmount,
            accountId = accountId,
            accountLabel = accountLabel,
            payeeId = payeeId,
            payeeName = targetAccountId?.let { ledger.ownAccounts.labels[it] }?.let { "→ $it" } ?: payeeName,
            comment = comment,
            categoryPath = categoryPath,
            categoryIcon = categoryIcon,
            templateId = templateId,
            targetAccountId = targetAccountId
        )
    }
}.withoutMovementsOtherThanReserves(ledger.ownAccounts.dailyIds)
