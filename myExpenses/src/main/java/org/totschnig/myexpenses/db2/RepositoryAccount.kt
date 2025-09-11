package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import androidx.core.content.contentValuesOf
import androidx.core.database.getStringOrNull
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.flow.Flow
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CRITERION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LAST_USED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_EXPORTED
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.buildTransactionRowSelect
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.withLimit
import org.totschnig.myexpenses.util.joinArrays

fun Repository.getCurrencyUnitForAccount(accountId: Long) =
    getCurrencyForAccount(accountId)?.let { currencyContext[it] }

fun Repository.getUuidForAccount(accountId: Long) = getStringValue(accountId, KEY_UUID)
fun Repository.getCurrencyForAccount(accountId: Long) = getStringValue(accountId, KEY_CURRENCY)
fun Repository.getLabelForAccount(accountId: Long) = getStringValue(accountId, KEY_LABEL)

private fun Repository.getStringValue(accountId: Long, column: String): String? {
    require(accountId > 0L)
    return contentResolver.query(
        ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
        arrayOf(column), null, null, null
    )!!.use {
        if (it.moveToFirst()) it.getStringOrNull(0) else null
    }
}

fun Repository.getLastUsedOpenAccount() =
    contentResolver.query(
        TransactionProvider.ACCOUNTS_URI.withLimit(1),
        arrayOf(KEY_ROWID, KEY_CURRENCY),
        "$KEY_SEALED = 0",
        null,
        "$KEY_LAST_USED DESC"
    )?.use {
        if (it.moveToFirst()) it.getLong(0) to currencyContext[it.getString(1)] else null
    }


fun Repository.findAccountByUuid(uuid: String) = contentResolver.query(
    TransactionProvider.ACCOUNTS_URI,
    arrayOf(KEY_ROWID),
    "$KEY_UUID = ?",
    arrayOf(uuid),
    null
)?.use {
    if (it.moveToFirst()) it.getLong(0) else null
}

fun Repository.findAccountByUuidWithExtraColumn(uuid: String, extraColumn: String) =
    contentResolver.query(
        TransactionProvider.ACCOUNTS_URI,
        arrayOf(KEY_ROWID, extraColumn),
        "$KEY_UUID = ?",
        arrayOf(uuid),
        null
    )?.use {
        if (it.moveToFirst()) it.getLong(0) to it.getStringOrNull(1) else null
    }

fun Repository.loadAccount(accountId: Long): Account? {
    require(accountId > 0L)
    return contentResolver.query(
        ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
        Account.PROJECTION,
        null, null, null
    )?.use {
        if (it.moveToFirst()) {
            Account.fromCursor(it, AccountType.fromAccountCursor(it))
        } else null
    }
}

fun Repository.loadAccountFlow(accountId: Long): Flow<Account> {
    require(accountId > 0L)
    return contentResolver.observeQuery(
        ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
        Account.PROJECTION,
        null, null, null
    ).mapToOne {
        Account.fromCursor(it, AccountType.fromAccountCursor(it))
    }
}

fun Repository.loadAggregateAccountFlow(accountId: Long): Flow<Account> {
    require(accountId < 0L)
    return contentResolver.observeQuery(
        ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_AGGREGATE_URI, accountId),
        null, null, null, null
    ).mapToOne {
        Account(
            id = accountId,
            label = it.getString(KEY_LABEL),
            currency = it.getString(KEY_CURRENCY),
            openingBalance = it.getLong(KEY_OPENING_BALANCE),
            grouping = it.getEnum(KEY_GROUPING, Grouping.NONE),
            isSealed = it.getBoolean(KEY_SEALED),
            type = AccountType(name = "Aggregate")
        )
    }
}

fun Account.toContentValues() = ContentValues().apply {
    put(KEY_LABEL, label)
    put(KEY_OPENING_BALANCE, openingBalance)
    put(KEY_DESCRIPTION, description)
    put(KEY_CURRENCY, currency)
    put(KEY_TYPE, type.id)
    put(KEY_COLOR, color)
    put(KEY_SYNC_ACCOUNT_NAME, syncAccountName)
    if (criterion != null) {
        put(KEY_CRITERION, criterion)
    } else {
        putNull(KEY_CRITERION)
    }
    put(KEY_EXCLUDE_FROM_TOTALS, excludeFromTotals)
    bankId?.let {
        put(KEY_BANK_ID, it)
    }
    put(KEY_DYNAMIC, dynamicExchangeRates)
}

fun Repository.createAccount(account: Account): Account {
    val uuid = account.uuid ?: Model.generateUuid()
    val initialValues = account.toContentValues().apply {
        put(KEY_UUID, uuid)
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

fun Repository.updateAccount(accountId: Long, builder: ContentValues.() -> Unit) {
    updateAccount(accountId, ContentValues().also { it.builder() })
}

fun Repository.setGrouping(accountId: Long, grouping: Grouping) {
    contentResolver.update(
        ContentUris.withAppendedId(TransactionProvider.ACCOUNT_GROUPINGS_URI, accountId)
            .buildUpon()
            .appendPath(grouping.name).build(),
        null, null, null
    )
}

fun Repository.storeExchangeRate(
    accountId: Long,
    exchangeRate: Double,
    currency: String,
    homeCurrency: String
) {
    contentResolver.insert(
        buildExchangeRateUri(accountId, currency, homeCurrency),
        ContentValues().apply {
            put(KEY_EXCHANGE_RATE, exchangeRate)
        })
}

private fun buildExchangeRateUri(accountId: Long, currency: String, homeCurrency: String) =
    ContentUris.appendId(TransactionProvider.ACCOUNT_EXCHANGE_RATE_URI.buildUpon(), accountId)
        .appendEncodedPath(currency)
        .appendEncodedPath(homeCurrency).build()

/**
 * @return syncAccountName in case account was set up for synchronization in order to allow caller
 * to update AccountManager
 */
fun Repository.deleteAccount(accountId: Long): String? {
    val syncAccountName = getStringValue(accountId, KEY_SYNC_ACCOUNT_NAME)
    val ops = java.util.ArrayList<ContentProviderOperation>()
    val accountIdString = accountId.toString()
    updateTransferPeersForTransactionDelete(
        ops,
        buildTransactionRowSelect(null),
        arrayOf(accountIdString)
    )
    ops.add(
        ContentProviderOperation.newDelete(
            TransactionProvider.ACCOUNTS_URI.buildUpon().appendPath(accountIdString).build()
        ).build()
    )
    contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
    return syncAccountName
}

fun Repository.markAsExported(accountId: Long, filter: Criterion?) {
    val ops = buildList {
        val debtUri = TransactionProvider.DEBTS_URI
        add(
            ContentProviderOperation.newUpdate(debtUri).withValue(KEY_SEALED, -1)
                .withSelection("$KEY_SEALED = 1", null).build()
        )
        var selection =
            "$KEY_ACCOUNTID = ? AND $KEY_PARENTID is null AND $KEY_STATUS = ?"
        var selectionArgs: Array<String>? =
            arrayOf(accountId.toString(), STATUS_NONE.toString())
        if (filter != null) {
            selection += " AND " + filter.getSelectionForParents(TABLE_TRANSACTIONS)
            selectionArgs = joinArrays(selectionArgs, filter.getSelectionArgs(false))
        }
        add(
            ContentProviderOperation.newUpdate(Transaction.CONTENT_URI)
                .withValue(KEY_STATUS, STATUS_EXPORTED)
                .withSelection(selection, selectionArgs)
                .build()
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
fun Repository.findAnyOpenByLabel(label: String) = findAnyOpen("$TABLE_ACCOUNTS.$KEY_LABEL", label)

/**
 * Returns the first account which uses the passed in currency, order is undefined
 *
 * @param currency ISO 4217 currency code
 * @return id or -1 if not found
 */
fun Repository.findAnyOpenByCurrency(currency: String) =
    findAnyOpen(KEY_CURRENCY, currency)

fun Repository.findAnyOpen(column: String? = null, search: String? = null) = contentResolver.query(
    TransactionProvider.ACCOUNTS_URI,
    arrayOf(KEY_ROWID),
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
        putNull(KEY_TRANSFER_ACCOUNT)
        putNull(KEY_TRANSFER_PEER)
    }
    ops.add(
        ContentProviderOperation.newUpdate(TransactionProvider.TRANSACTIONS_URI)
            .withValues(args)
            .withSelection(
                "$KEY_TRANSFER_PEER IN ($rowSelect)",
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

fun Repository.countAccounts(selection: String? = null, selectionArgs: Array<String>? = null) =
    contentResolver.query(
        TransactionProvider.ACCOUNTS_URI, arrayOf("count(*)"),
        selection, selectionArgs, null
    )!!.use {
        it.moveToFirst()
        it.getInt(0)
    }

fun Repository.setAccountProperty(accountId: Long, column: String, value: Any?) {
    contentResolver.update(
        ContentUris.withAppendedId(ACCOUNTS_URI, accountId),
        contentValuesOf(
            column to value
        ),
        null,
        null
    )
}