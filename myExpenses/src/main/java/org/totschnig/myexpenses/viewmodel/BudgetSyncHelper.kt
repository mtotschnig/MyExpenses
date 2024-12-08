package org.totschnig.myexpenses.viewmodel

import org.totschnig.myexpenses.preference.PrefHandler

const val KEY_BUDGET_IS_SYNCED = "budgetIsSynced_"
const val KEY_BUDGET_AGGREGATE_SYNC_ACCOUNT_NAME = "budgetAggregateSyncAccountName_"

fun setBudgetSynced(
    budgetId: Long,
    accountId: Long,
    prefHandler: PrefHandler,
    accountName: String,
)  {
    if (accountId > 0) prefHandler.putBoolean(KEY_BUDGET_IS_SYNCED + budgetId, true)
    else prefHandler.putString(KEY_BUDGET_AGGREGATE_SYNC_ACCOUNT_NAME + budgetId, accountName)
}