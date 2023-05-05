package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model2.Transaction
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.useAndMap
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.joinArrays
import org.totschnig.myexpenses.util.localDate2Epoch
import org.totschnig.myexpenses.util.localDateTime2Epoch
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import java.math.BigDecimal
import java.time.LocalDateTime

private fun Repository.toContentValues(transaction: Transaction) = with(transaction) {
    ContentValues().apply {
        put(DatabaseConstants.KEY_ACCOUNTID, account)
        put(
            DatabaseConstants.KEY_AMOUNT,
            Money(
                getCurrencyUnitForAccount(account)!!,
                BigDecimal(amount.toString())
            ).amountMinor
        )
        put(DatabaseConstants.KEY_DATE, time?.let {
            localDateTime2Epoch(LocalDateTime.of(date, time))
        } ?: localDate2Epoch(date))
        put(DatabaseConstants.KEY_VALUE_DATE, localDate2Epoch(valueDate))
        payee.takeIf { it.isNotEmpty() }
            ?.let { put(DatabaseConstants.KEY_PAYEEID, findOrWritePayee(it)) }
        put(DatabaseConstants.KEY_CR_STATUS, CrStatus.UNRECONCILED.name)
        category?.takeIf { it > 0 }?.let { put(DatabaseConstants.KEY_CATID, it) }
        method.takeIf { it > 0 }?.let { put(DatabaseConstants.KEY_METHODID, it) }
        put(DatabaseConstants.KEY_REFERENCE_NUMBER, number)
        put(DatabaseConstants.KEY_COMMENT, comment)
    }
}

fun Repository.updateTransaction(id: String, transaction: Transaction): Int {
    val ops = ArrayList<ContentProviderOperation>()
    ops.add(
        ContentProviderOperation.newUpdate(
            TransactionProvider.TRANSACTIONS_URI.buildUpon().appendEncodedPath(id).build())
            .withValues(toContentValues(transaction))
            .build()
    )
    ops.add(
        ContentProviderOperation.newDelete(TransactionProvider.TRANSACTIONS_TAGS_URI)
            .withSelection("${DatabaseConstants.KEY_TRANSACTIONID} = ?", arrayOf(id))
            .build()
    )
    for (tag in transaction.tags) {
        ops.add(
            ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_TAGS_URI)
                .withValue(DatabaseConstants.KEY_TRANSACTIONID, id)
                .withValue(DatabaseConstants.KEY_TAGID, tag).build()
        )
    }
    val results = contentResolver.applyBatch(
        TransactionProvider.AUTHORITY,
        ops
    )
    return results[0].count ?: 0
}

fun Repository.createTransaction(transaction: Transaction): Long {
    val values = toContentValues(transaction).apply {
        put(DatabaseConstants.KEY_UUID, Model.generateUuid())
    }
    val ops = ArrayList<ContentProviderOperation>().apply {
        add(
            ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_URI)
                .withValues(values)
                .build()
        )
        for (tag in transaction.tags) {
            add(
                ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_TAGS_URI)
                    .withValueBackReference(DatabaseConstants.KEY_TRANSACTIONID, 0)
                    .withValue(DatabaseConstants.KEY_TAGID, tag).build()
            )
        }
    }
    return contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)[0].uri!!.let {
        ContentUris.parseId(it)
    }
}

fun Repository.loadTransactions(accountId: Long): List<Transaction> {
    val filter = FilterPersistence(
        prefHandler = prefHandler,
        keyTemplate = MyExpensesViewModel.prefNameForCriteria(accountId),
        savedInstanceState = null,
        immediatePersist = false,
        restoreFromPreferences = true
    ).whereFilter.takeIf { !it.isEmpty }?.let {
        it.getSelectionForParents(DatabaseConstants.VIEW_EXTENDED) to it.getSelectionArgs(false)
    }
    //noinspection Recycle
    return contentResolver.query(
        DataBaseAccount.uriForTransactionList(true),
        DatabaseConstants.getProjectionExtended(),
        "${DatabaseConstants.KEY_ACCOUNTID} = ? AND ${DatabaseConstants.KEY_PARENTID} IS NULL ${
            filter?.first?.takeIf { it != "" }?.let { "AND $it" } ?: ""
        }",
        filter?.let { arrayOf(accountId.toString(), *it.second) }
            ?: arrayOf(accountId.toString()),
        null
    )!!.useAndMap { cursor ->
        Transaction.fromCursor(
            context,
            cursor,
            accountId,
            getCurrencyUnitForAccount(accountId)!!,
            currencyFormatter,
            Utils.ensureDateFormatWithShortYear(context)
        ).copy(
            //noinspection Recycle
            tags = contentResolver.query(
                TransactionProvider.TRANSACTIONS_TAGS_URI,
                arrayOf(DatabaseConstants.KEY_ROWID),
                "${DatabaseConstants.KEY_TRANSACTIONID} = ?",
                arrayOf(cursor.getLong(DatabaseConstants.KEY_ROWID).toString()),
                null
            )?.useAndMap { it.getLong(0) } ?: emptyList()
        )
    }

}

fun Repository.getTransactionSum(accountId: Long, filter: WhereFilter? = null): Long {
    var selection =
        "${DatabaseConstants.KEY_ACCOUNTID} = ? AND ${DatabaseConstants.WHERE_NOT_SPLIT_PART} AND ${DatabaseConstants.WHERE_NOT_VOID}"
    var selectionArgs: Array<String>? = arrayOf(accountId.toString())
    if (filter != null && !filter.isEmpty) {
        selection += " AND " + filter.getSelectionForParents(DatabaseConstants.VIEW_COMMITTED)
        selectionArgs = joinArrays(selectionArgs, filter.getSelectionArgs(false))
    }
    return contentResolver.query(
        TransactionProvider.TRANSACTIONS_URI,
        arrayOf("sum(${DatabaseConstants.KEY_AMOUNT})"),
        selection,
        selectionArgs,
        null
    )!!.use {
        it.moveToFirst()
        it.getLong(0)
    }
}
