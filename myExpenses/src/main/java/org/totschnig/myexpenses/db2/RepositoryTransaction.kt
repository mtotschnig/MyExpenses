package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.os.Bundle
import androidx.core.os.BundleCompat
import org.totschnig.myexpenses.dialog.ArchiveInfo
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction.CONTENT_URI
import org.totschnig.myexpenses.model.Transaction.generateUuid
import org.totschnig.myexpenses.model2.Transaction
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.uriBuilderForTransactionList
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_SEALED_ACCOUNT_WITH_TRANSFER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_SEALED_DEBT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
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
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.provider.withLimit
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
                put(KEY_UUID, generateUuid())
            })!!
    )
    contentResolver.saveTagsForTransaction(transaction.tags.toLongArray(), id)
    return id
}

suspend fun Repository.loadTransactions(accountId: Long, limit: Int? = 200): List<Transaction> {
    val filter = FilterPersistence(
        dataStore = dataStore,
        prefKey = MyExpensesViewModel.prefNameForCriteria(accountId),
    ).getValue()?.let {
        it.getSelectionForParents() to it.getSelectionArgs(false)
    }
    //noinspection Recycle
    return contentResolver.query(
        DataBaseAccount.uriForTransactionList(true).let {
            if (limit != null) it.withLimit(limit) else it
        },
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

fun Repository.getTransactionSum(account: DataBaseAccount, filter: Criterion? = null) =
    getTransactionSum(account.id, account.currency, filter)

fun Repository.getTransactionSum(
    id: Long,
    currency: String? = null,
    filter: Criterion? = null
): Long {
    var selection =
        "$KEY_ACCOUNTID = ? AND $WHERE_NOT_SPLIT_PART AND $WHERE_NOT_VOID"
    var selectionArgs: Array<String>? = arrayOf(id.toString())
    if (filter != null) {
        selection += " AND " + filter.getSelectionForParents()
        selectionArgs = joinArrays(selectionArgs, filter.getSelectionArgs(false))
    }
    return contentResolver.query(
        uriBuilderForTransactionList(id, currency, extended = false).build(),
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
    val ops = ArrayList<ContentProviderOperation>().apply {
        add(
            ContentProviderOperation.newAssertQuery(
                ContentUris.withAppendedId(TRANSACTIONS_URI, id)
            )
                .withSelection("$KEY_STATUS = $STATUS_ARCHIVE", null)
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

fun Repository.hasSealed(accountId: Long) = contentResolver.query(
    TRANSACTIONS_URI.buildUpon().appendQueryParameter(
        TransactionProvider.QUERY_PARAMETER_INCLUDE_ALL, "1"
    ).build(),
    arrayOf(
        KEY_HAS_SEALED_ACCOUNT_WITH_TRANSFER,
        KEY_HAS_SEALED_DEBT
    ),
    "$KEY_ACCOUNTID = ?",
    arrayOf(accountId.toString()),
    null
)!!.use {
    it.moveToFirst()
    it.getBoolean(0) to it.getBoolean(1)
}

fun Repository.getPayeeForTransaction(id: Long) = contentResolver.findBySelection(
    "$KEY_ROWID = ?",
    arrayOf(id.toString()),
    KEY_PAYEEID
)

private fun ContentResolver.findBySelection(
    selection: String,
    selectionArgs: Array<String>,
    column: String
) =
    query(
        CONTENT_URI,
        arrayOf(column),
        selection,
        selectionArgs,
        null
    )?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    } ?: -1

fun Repository.calculateSplitSummary(id: Long): List<Pair<String, String?>>? {
    return contentResolver.query(
        TransactionProvider.CATEGORIES_URI.buildUpon()
            .appendQueryParameter(KEY_TRANSACTIONID, id.toString()).build(),
        arrayOf(KEY_LABEL, KEY_ICON), null, null, null
    )
        ?.useAndMapToList {
            it.getString(KEY_LABEL) to it.getStringOrNull(KEY_ICON)
        }?.takeIf { it.isNotEmpty() }
}