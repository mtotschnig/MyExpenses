package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.os.Bundle
import androidx.core.content.contentValuesOf
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_FLAG_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORTED_IDS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VISIBLE
import org.totschnig.myexpenses.provider.DatabaseConstants.METHOD_FLAG_SORT
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNT_FLAGS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNT_TYPES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DUAL_URI
import org.totschnig.myexpenses.provider.withAppendedId

fun Repository.getAccountFlags(): Flow<List<AccountFlag>> = contentResolver.observeQuery(
    ACCOUNT_FLAGS_URI,
    notifyForDescendants = true,
    sortOrder = "$KEY_FLAG_SORT_KEY DESC"
).mapToList {
    AccountFlag.fromCursor(it)
}

fun Repository.updateAccountFlag(accountFlag: AccountFlag) {
    require(accountFlag.id > 0)
    contentResolver.update(
        ACCOUNT_FLAGS_URI.withAppendedId(accountFlag.id),
        accountFlag.asContentValues,
        null,
        null
    )
}

fun Repository.setAccountFlagVisible(accountFlagId: Long, visible: Boolean) {
    contentResolver.update(
        ACCOUNT_FLAGS_URI.withAppendedId(accountFlagId),
        contentValuesOf(
            KEY_VISIBLE to visible
        ),
        null,
        null
    )
}

fun Repository.addAccountFlag(accountFlag: AccountFlag): AccountFlag {
    val id = ContentUris.parseId(
        contentResolver.insert(
            ACCOUNT_FLAGS_URI,
            accountFlag.asContentValues
        )!!
    )
    return accountFlag.copy(id = id)
}

fun Repository.deleteAccountFlag(accountFlagId: Long) {
    contentResolver.delete(
        ACCOUNT_FLAGS_URI.withAppendedId(accountFlagId),
        null,
        null
    )
}

fun Repository.saveAccountFlagOrder(sortedIds: LongArray) {
    contentResolver.call(DUAL_URI, METHOD_FLAG_SORT, null, Bundle().apply {
        putLongArray(KEY_SORTED_IDS, sortedIds)
    })
}

fun Repository.saveSelectedAccountsForFlag(
    accountFlagId: Long,
    flaggedAccounts: Set<Long>
) {
    contentResolver.update(
        ACCOUNTS_URI,
        contentValuesOf(
            KEY_FLAG to accountFlagId,
        ),
        "$KEY_ROWID IN (${flaggedAccounts.joinToString(",") { "?" }})",
        flaggedAccounts.map { it.toString() }.toTypedArray()
    )
}

fun Repository.findAccountFlag(name: String): AccountFlag? = contentResolver.query(
    ACCOUNT_FLAGS_URI,
    null,
    "$KEY_FLAG_LABEL = ?",
    arrayOf(name),
    null
)?.use {
    if (it.moveToFirst()) {
        AccountFlag.fromCursor(it)
    } else null
}