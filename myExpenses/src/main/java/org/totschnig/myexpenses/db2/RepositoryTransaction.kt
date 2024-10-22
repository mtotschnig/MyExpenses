package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.os.Bundle
import androidx.core.os.BundleCompat
import org.totschnig.myexpenses.dialog.ArchiveInfo
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model2.Transaction
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT_PART
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_VOID
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_ARCHIVE
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_CAN_BE_ARCHIVED
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_TAGS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_UNARCHIVE
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.joinArrays
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

private fun Repository.toContentValues(transaction: Transaction) = with(transaction) {
    ContentValues().apply {
        put(KEY_ACCOUNTID, account)
        put(
            KEY_AMOUNT,
            Money(
                getCurrencyUnitForAccount(account)!!,
                BigDecimal(amount.toString())
            ).amountMinor
        )
        put(KEY_DATE, time?.let {
            LocalDateTime.of(date, time).toEpoch()
        } ?: date.toEpoch())
        put(KEY_VALUE_DATE, valueDate.toEpoch())
        party.takeIf { it > 0 }?.let { put(KEY_PAYEEID, it) }
        put(KEY_CR_STATUS, CrStatus.UNRECONCILED.name)
        category?.takeIf { it > 0 }?.let { put(KEY_CATID, it) }
        method.takeIf { it > 0 }?.let { put(KEY_METHODID, it) }
        put(KEY_REFERENCE_NUMBER, number)
        put(KEY_COMMENT, comment)
    }
}

fun Repository.updateTransaction(id: Long, transaction: Transaction): Boolean {
    if (contentResolver.update(
            ContentUris.withAppendedId(TRANSACTIONS_URI, id),
            toContentValues(transaction),
            null, null
        ) != 1
    ) return false
    contentResolver.saveTagsForTransaction(transaction.tags.toLongArray(), id)
    return true
}

fun Repository.createTransaction(transaction: Transaction): Long {
    val id = ContentUris.parseId(
        contentResolver.insert(
            TRANSACTIONS_URI,
            toContentValues(transaction).apply {
                put(KEY_UUID, Model.generateUuid())
            })!!
    )
    contentResolver.saveTagsForTransaction(transaction.tags.toLongArray(), id)
    return id
}

fun Repository.loadTransactions(accountId: Long): List<Transaction> {
    val filter = FilterPersistence(
        prefHandler = prefHandler,
        keyTemplate = MyExpensesViewModel.prefNameForCriteria(accountId),
        savedInstanceState = null,
        immediatePersist = false,
        restoreFromPreferences = true
    ).whereFilter.takeIf { !it.isEmpty }?.let {
        it.getSelectionForParents() to it.getSelectionArgs(false)
    }
    //noinspection Recycle
    return contentResolver.query(
        DataBaseAccount.uriForTransactionList(true),
        DatabaseConstants.getProjectionExtended(),
        "$KEY_ACCOUNTID = ? AND $KEY_PARENTID IS NULL ${
            filter?.first?.takeIf { it != "" }?.let { "AND $it" } ?: ""
        }",
        filter?.let { arrayOf(accountId.toString(), *it.second) }
            ?: arrayOf(accountId.toString()),
        null
    )!!.useAndMapToList { cursor ->
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
                TRANSACTIONS_TAGS_URI,
                arrayOf(KEY_ROWID),
                "$KEY_TRANSACTIONID = ?",
                arrayOf(cursor.getLong(KEY_ROWID).toString()),
                null
            )?.useAndMapToList { it.getLong(0) } ?: emptyList()
        )
    }

}

fun Repository.getTransactionSum(account: DataBaseAccount, filter: WhereFilter? = null): Long {
    var selection =
        "$KEY_ACCOUNTID = ? AND $WHERE_NOT_SPLIT_PART AND $WHERE_NOT_VOID"
    var selectionArgs: Array<String>? = arrayOf(account.id.toString())
    if (filter != null && !filter.isEmpty) {
        selection += " AND " + filter.getSelectionForParents()
        selectionArgs = joinArrays(selectionArgs, filter.getSelectionArgs(false))
    }
    return contentResolver.query(
        account.uriForTransactionList(extended = false),
        arrayOf("${DbUtils.aggregateFunction(prefHandler)}($KEY_AMOUNT)"),
        selection,
        selectionArgs,
        null
    )!!.use {
        it.moveToFirst()
        it.getLong(0)
    }
}

fun Repository.archive(
    accountId: Long,
    range: Pair<LocalDate, LocalDate>
) = contentResolver.call(TransactionProvider.DUAL_URI, METHOD_ARCHIVE, null, Bundle().apply {
    putLong(KEY_ACCOUNTID, accountId)
    putSerializable(KEY_START, range.first)
    putSerializable(KEY_END, range.second)
})!!.getLong(KEY_TRANSACTIONID)

fun Repository.unarchive(id: Long) {
    val ops = java.util.ArrayList<ContentProviderOperation>().apply {
        add(
            ContentProviderOperation.newAssertQuery(TRANSACTIONS_URI)
                .withSelection(
                    "$KEY_ROWID = ? AND $KEY_STATUS == $STATUS_ARCHIVE",
                    arrayOf(id.toString())
                )
                .withExpectedCount(1).build()
        )
        add(
            ContentProviderOperation.newUpdate(
                TRANSACTIONS_URI.buildUpon().appendPath(URI_SEGMENT_UNARCHIVE).build()
            )
                .withValue(KEY_ROWID, id)
                .build()
        )
    }
    val result = contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
    val affectedRows = result[1].count
    if (affectedRows != 1) {
        CrashHandler.report(Exception("Unarchive returned $affectedRows affected rows"))
    }
}

fun Repository.canBeArchived(
    accountId: Long,
    range: Pair<LocalDate, LocalDate>
) = BundleCompat.getParcelable(
    contentResolver.call(
        TransactionProvider.DUAL_URI,
        METHOD_CAN_BE_ARCHIVED,
        null,
        Bundle().apply {
            putLong(KEY_ACCOUNTID, accountId)
            putSerializable(KEY_START, range.first)
            putSerializable(KEY_END, range.second)
        })!!, KEY_RESULT, ArchiveInfo::class.java
)!!

fun Repository.countTransactionsPerAccount(
    accountId: Long
) = count(
    TRANSACTIONS_URI,
    "$KEY_ACCOUNTID = ? AND $KEY_PARENTID is null",
    arrayOf(accountId.toString())
)

fun ContentResolver.findByAccountAndUuid(accountId: Long, uuid: String) = findBySelection(
    "$KEY_UUID = ? AND $KEY_ACCOUNTID = ?",
    arrayOf(uuid, accountId.toString()),
    KEY_ROWID
)

fun Repository.hasParent(id: Long) = contentResolver.findBySelection(
    "$KEY_ROWID = ?",
    arrayOf(id.toString()),
    KEY_PARENTID
) != 0L

private fun ContentResolver.findBySelection(
    selection: String,
    selectionArgs: Array<String>,
    column: String
) =
    query(
        org.totschnig.myexpenses.model.Transaction.CONTENT_URI,
        arrayOf(column),
        selection,
        selectionArgs,
        null
    )?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    } ?: -1


fun Repository.deleteTemplate(id: Long) {
    contentResolver.delete(ContentUris.withAppendedId(TransactionProvider.TEMPLATES_URI, id), null, null)
}