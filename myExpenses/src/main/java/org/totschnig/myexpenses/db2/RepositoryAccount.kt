package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.withLimit
import org.totschnig.myexpenses.util.joinArrays

fun Repository.getCurrencyUnitForAccount(accountId: Long): CurrencyUnit? {
    require(accountId != 0L)
    return contentResolver.query(
        ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
        arrayOf(DatabaseConstants.KEY_CURRENCY), null, null, null
    )?.use {
        if (it.moveToFirst()) currencyContext[it.getString(0)] else null
    }
}

fun Repository.getUuidForAccount(accountId: Long): String? {
    require(accountId != 0L)
    return contentResolver.query(
        ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
        arrayOf(DatabaseConstants.KEY_UUID), null, null, null
    )?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }
}

fun Repository.getLastUsedOpenAccount() =
    contentResolver.query(
        TransactionProvider.ACCOUNTS_URI.withLimit(1),
        arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_CURRENCY),
        "${DatabaseConstants.KEY_SEALED} = 0",
        null,
        DatabaseConstants.KEY_LAST_USED
    )?.use {
        if (it.moveToFirst()) it.getLong(0) to currencyContext.get(it.getString(1)) else null
    }

fun Repository.loadAccount(accountId: Long) = contentResolver.query(
    ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
    Account.PROJECTION,
    null, null, null
)!!.use {
    if (it.moveToFirst()) Account.fromCursor(it) else null
}

fun Account.toContentValues() = ContentValues().apply {
    put(DatabaseConstants.KEY_LABEL, label)
    put(DatabaseConstants.KEY_OPENING_BALANCE, openingBalance)
    put(DatabaseConstants.KEY_DESCRIPTION, description)
    put(DatabaseConstants.KEY_CURRENCY, currency)
    put(DatabaseConstants.KEY_TYPE, type.name)
    put(DatabaseConstants.KEY_COLOR, color)
    put(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME, syncAccountName)
    if (id == 0L) {
        put(DatabaseConstants.KEY_UUID, Model.generateUuid())
    }
    if (criterion != null) {
        put(DatabaseConstants.KEY_CRITERION, criterion)
    } else {
        putNull(DatabaseConstants.KEY_CRITERION)
    }
    put(DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS, excludeFromTotals)
}

fun Repository.createAccount(account: Account): Account {
    val uuid = Model.generateUuid()
    val initialValues = account.toContentValues().apply {
        put(DatabaseConstants.KEY_UUID, uuid)
    }
    val id = ContentUris.parseId(
        contentResolver.insert(
            TransactionProvider.ACCOUNTS_URI,
            initialValues
        )!!
    )
    return account.copy(id = id, uuid = uuid)
}

fun Repository.markAsExported(accountId: Long, filter: WhereFilter?) {
    val ops = buildList {
        val accountUri = TransactionProvider.ACCOUNTS_URI
        val debtUri = TransactionProvider.DEBTS_URI
        add(
            ContentProviderOperation.newUpdate(accountUri)
                .withValue(DatabaseConstants.KEY_SEALED, -1)
                .withSelection(DatabaseConstants.KEY_SEALED + " = 1", null).build()
        )
        add(
            ContentProviderOperation.newUpdate(debtUri).withValue(DatabaseConstants.KEY_SEALED, -1)
                .withSelection(DatabaseConstants.KEY_SEALED + " = 1", null).build()
        )
        var selection =
            DatabaseConstants.KEY_ACCOUNTID + " = ? AND " + DatabaseConstants.KEY_PARENTID + " is null AND " + DatabaseConstants.KEY_STATUS + " = ?"
        var selectionArgs: Array<String>? =
            arrayOf(accountId.toString(), DatabaseConstants.STATUS_NONE.toString())
        if (filter != null && !filter.isEmpty) {
            selection += " AND " + filter.getSelectionForParents(DatabaseConstants.TABLE_TRANSACTIONS)
            selectionArgs = joinArrays(selectionArgs, filter.getSelectionArgs(false))
        }
        add(
            ContentProviderOperation.newUpdate(Transaction.CONTENT_URI)
                .withValue(DatabaseConstants.KEY_STATUS, DatabaseConstants.STATUS_EXPORTED)
                .withSelection(selection, selectionArgs)
                .build()
        )
        add(
            ContentProviderOperation.newUpdate(accountUri)
                .withValue(DatabaseConstants.KEY_SEALED, 1)
                .withSelection(DatabaseConstants.KEY_SEALED + " = -1", null).build()
        )
        add(
            ContentProviderOperation.newUpdate(debtUri).withValue(DatabaseConstants.KEY_SEALED, 1)
                .withSelection(DatabaseConstants.KEY_SEALED + " = -1", null).build()
        )


    }
    contentResolver.applyBatch(TransactionProvider.AUTHORITY, ArrayList(ops))
}

fun updateTransferPeersForTransactionDelete(
    ops: java.util.ArrayList<ContentProviderOperation>,
    rowSelect: String,
    selectionArgs: Array<String>?
) {
    ops.add(
        ContentProviderOperation.newUpdate(TransactionProvider.ACCOUNTS_URI)
            .withValue(DatabaseConstants.KEY_SEALED, -1)
            .withSelection(DatabaseConstants.KEY_SEALED + " = 1", null).build()
    )
    val args = ContentValues().apply {
        putNull(DatabaseConstants.KEY_TRANSFER_ACCOUNT)
        putNull(DatabaseConstants.KEY_TRANSFER_PEER)
    }
    ops.add(
        ContentProviderOperation.newUpdate(TransactionProvider.TRANSACTIONS_URI)
            .withValues(args)
            .withSelection(
                DatabaseConstants.KEY_TRANSFER_PEER + " IN (" + rowSelect + ")",
                selectionArgs
            )
            .build()
    )
    ops.add(
        ContentProviderOperation.newUpdate(TransactionProvider.ACCOUNTS_URI)
            .withValue(DatabaseConstants.KEY_SEALED, 1)
            .withSelection(DatabaseConstants.KEY_SEALED + " = -1", null).build()
    )
}