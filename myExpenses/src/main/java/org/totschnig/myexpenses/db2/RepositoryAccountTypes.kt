package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.os.Bundle
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORTED_IDS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE_SORT_KEY
import org.totschnig.myexpenses.provider.DatabaseConstants.METHOD_TYPE_SORT
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTTYPES_METHODS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNT_TYPES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DUAL_URI
import org.totschnig.myexpenses.provider.withAppendedId

fun Repository.getAccountTypes(): Flow<List<AccountType>> = contentResolver.observeQuery(
    ACCOUNT_TYPES_URI,
    sortOrder = "$KEY_TYPE_SORT_KEY DESC",
    notifyForDescendants = true,
).mapToList {
    AccountType.fromCursor(it)
}

fun Repository.loadAccountType(id: Long): AccountType = contentResolver.query(
    ACCOUNT_TYPES_URI.withAppendedId(id),
    null,
    null,
    null,
    null
)!!.use {
    it.moveToFirst()
    AccountType.fromCursor(it)
}

fun Repository.addAccountType(accountType: AccountType): AccountType {
    val id = ContentUris.parseId(
        contentResolver.insert(
            ACCOUNT_TYPES_URI,
            accountType.asContentValues
        )!!
    )
    return accountType.copy(id = id)
}

fun Repository.updateAccountType(accountType: AccountType) {
    require(accountType.id > 0)
    contentResolver.update(
        ACCOUNT_TYPES_URI.withAppendedId(accountType.id),
        accountType.asContentValues,
        null,
        null
    )
}

fun Repository.deleteAccountType(accountTypeId: Long) {
    contentResolver.delete(
        ACCOUNTTYPES_METHODS_URI,
        "$KEY_TYPE = ?",
        arrayOf(accountTypeId.toString())
    )
    contentResolver.delete(
        ACCOUNT_TYPES_URI.withAppendedId(accountTypeId),
        null,
        null
    )
}

fun Repository.findAccountType(name: String): AccountType? = contentResolver.query(
    ACCOUNT_TYPES_URI,
null,
"$KEY_LABEL = ?",
arrayOf(name),
null
)?.use {
    if (it.moveToFirst()) {
        AccountType.fromCursor(it)
    } else null
}

fun Repository.saveAccountTypeOrder(sortedIds: LongArray) {
    contentResolver.call(DUAL_URI, METHOD_TYPE_SORT, null, Bundle().apply {
        putLongArray(KEY_SORTED_IDS, sortedIds)
    })
}

fun Repository.requireAccountTypeForSync(name: String): AccountType = findAccountType(name) ?:
AccountType.initialAccountTypes.firstOrNull { it.nameForSyncLegacy == name }?.let {
    findAccountType(it.name)
} ?:  addAccountType(AccountType(name =name))