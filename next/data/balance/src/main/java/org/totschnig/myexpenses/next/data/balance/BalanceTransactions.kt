package org.totschnig.myexpenses.next.data.balance

import org.totschnig.myexpenses.next.core.model.ledger.Ledger

/**
 * The transactions of [ledger] in [period], transfers included. Movements between own accounts
 * imported from the bank count like transfers in the app.
 */
fun balanceTransactions(ledger: Ledger, period: BalancePeriod) = ledger.transactions
    .filter { it.date in period }
    .map { BalanceTransaction(it.id, it.date, it.homeAmount, it.accountId, it.targetAccountId) }
