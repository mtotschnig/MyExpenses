package org.totschnig.myexpenses.next.data.balance

import java.time.LocalDate

/**
 * The balance is computed from the analysed transactions, so it is shown as
 * [org.totschnig.myexpenses.next.ledger.AnalysisState]
 */
data class BalanceOverview(val balance: MonthlyBalance, val today: LocalDate)
