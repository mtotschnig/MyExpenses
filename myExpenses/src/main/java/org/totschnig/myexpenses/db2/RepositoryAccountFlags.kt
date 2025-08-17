package org.totschnig.myexpenses.db2

import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.provider.TransactionProvider

fun Repository.getAccountFlags(): Flow<List<AccountFlag>>  = contentResolver.observeQuery(
    TransactionProvider.ACCOUNT_FLAGS_URI,
    notifyForDescendants = true,
).mapToList {
    AccountFlag.fromCursor(it)
}