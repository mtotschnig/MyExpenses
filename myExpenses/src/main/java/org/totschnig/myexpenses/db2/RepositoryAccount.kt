package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import androidx.core.database.getStringOrNull
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.withLimit
import org.totschnig.myexpenses.util.joinArrays

fun Repository.getCurrencyUnitForAccount(accountId: Long): CurrencyUnit? {
    require(accountId != 0L)
    return getCurrencyForAccount(accountId)?.let { currencyContext[it] }
}

fun Repository.getUuidForAccount(accountId: Long) = getStringValue(accountId, DatabaseConstants.KEY_UUID)
fun Repository.getCurrencyForAccount(accountId: Long) = getStringValue(accountId, DatabaseConstants.KEY_CURRENCY)
fun Repository.getLabelForAccount(accountId: Long) = getStringValue(accountId, DatabaseConstants.KEY_LABEL)

private fun Repository.getStringValue(accountId: Long, column: String): String? {
    require(accountId != 0L)
    return contentResolver.query(
        ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
        arrayOf(column), null, null, null
    )?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }
}

fun Repository.findAccountByUuid(uuid: String) = contentResolver.query(
    TransactionProvider.ACCOUNTS_URI,
    arrayOf(DatabaseConstants.KEY_ROWID),
    DatabaseConstants.KEY_UUID + " = ?",
    arrayOf(uuid),
    null
)?.use {
    if (it.moveToFirst()) it.getLong(0) else null
}

fun Repository.findAccountByUuidWithExtraColumn(uuid: String, extraColumn: String) = contentResolver.query(
    TransactionProvider.ACCOUNTS_URI,
    arrayOf(DatabaseConstants.KEY_ROWID, extraColumn),
    DatabaseConstants.KEY_UUID + " = ?",
    arrayOf(uuid),
    null
)?.use {
    if (it.moveToFirst()) it.getLong(0) to it.getStringOrNull(1) else null
}

fun Repository.getLastUsedOpenAccount() =
    contentResolver.query(
        TransactionProvider.ACCOUNTS_URI.withLimit(1),
        arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_CURRENCY),
        "$KEY_SEALED = 0",
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

fun Repository.updateAccount(accountId: Long, data: ContentValues) {
    contentResolver.update(
        ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
        data, null, null
    )
}

fun Repository.markAsExported(accountId: Long, filter: WhereFilter?) {
    val ops = buildList {
        val accountUri = TransactionProvider.ACCOUNTS_URI
        val debtUri = TransactionProvider.DEBTS_URI
        add(
            ContentProviderOperation.newUpdate(accountUri)
                .withValue(KEY_SEALED, -1)
                .withSelection("$KEY_SEALED = 1", null).build()
        )
        add(
            ContentProviderOperation.newUpdate(debtUri).withValue(KEY_SEALED, -1)
                .withSelection("$KEY_SEALED = 1", null).build()
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
                .withValue(KEY_SEALED, 1)
                .withSelection("$KEY_SEALED = -1", null).build()
        )
        add(
            ContentProviderOperation.newUpdate(debtUri).withValue(KEY_SEALED, 1)
                .withSelection("$KEY_SEALED = -1", null).build()
        )


    }
    contentResolver.applyBatch(TransactionProvider.AUTHORITY, ArrayList(ops))
}

/**
 * Looks for an account with a label, that is not sealed. WARNING: If several accounts have the same label, this
 * method fill return the first account retrieved in the cursor, order is undefined
 *
 * @param label label of the account we want to retrieve
 * @return id or null if not found
 */
fun Repository.findAnyOpenByLabel(label: String) = findAnyOpen(DatabaseConstants.KEY_LABEL, label)

/**
 * Returns the first account which uses the passed in currency, order is undefined
 *
 * @param currency ISO 4217 currency code
 * @return id or -1 if not found
 */
fun Repository.findAnyOpenByCurrency(currency: String) =
    findAnyOpen(DatabaseConstants.KEY_CURRENCY, currency)

fun Repository.findAnyOpen(column: String? = null, search: String? = null) = contentResolver.query(
    TransactionProvider.ACCOUNTS_URI,
    arrayOf(DatabaseConstants.KEY_ROWID),
    (if (column == null) "" else ("$column = ? AND  ")) + "$KEY_SEALED = 0",
    search?.let { arrayOf(it) },
    null
)?.use { if (it.moveToFirst()) it.getLong(0) else null }

fun updateTransferPeersForTransactionDelete(
    ops: java.util.ArrayList<ContentProviderOperation>,
    rowSelect: String,
    selectionArgs: Array<String>?
) {
    ops.add(
        ContentProviderOperation.newUpdate(TransactionProvider.ACCOUNTS_URI)
            .withValue(KEY_SEALED, -1)
            .withSelection("$KEY_SEALED = 1", null).build()
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
            .withValue(KEY_SEALED, 1)
            .withSelection("$KEY_SEALED = -1", null).build()
    )
}