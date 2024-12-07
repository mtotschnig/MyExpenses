package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.database.getLongOrNull
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_DEFAULT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.AUTOFILL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DEBTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_CALLER_IS_IN_BULK
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_MARK_VOID
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTION_ATTACHMENT_SINGLE_URI
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getEnum
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.Debt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class Repository @Inject constructor(
    val context: Context,
    val currencyContext: CurrencyContext,
    val currencyFormatter: ICurrencyFormatter,
    val prefHandler: PrefHandler,
    val dataStore: DataStore<Preferences>
) {
    companion object {
        const val UUID_SEPARATOR = ":"
    }

    val contentResolver: ContentResolver = context.contentResolver

    //Payee

    fun autoFill(payeeId: Long): AutoFillInfo? {
        return contentResolver.query(
            ContentUris.withAppendedId(AUTOFILL_URI, payeeId),
            arrayOf(KEY_CATID), null, null, null
        )?.use { cursor ->
            cursor.takeIf { it.moveToFirst() }?.let {
                it.getLongOrNull(0)?.let { categoryId -> AutoFillInfo(categoryId) }
            }
        }
    }

    //Transaction
    fun getUuidForTransaction(transactionId: Long) =
        contentResolver.query(
            ContentUris.withAppendedId(TRANSACTIONS_URI, transactionId),
            arrayOf(KEY_UUID), null, null, null
        )?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    fun saveDebt(debt: Debt) {
        if (debt.id == 0L) {
            contentResolver.insert(DEBTS_URI, debt.toContentValues())
        } else {
            contentResolver.update(
                ContentUris.withAppendedId(DEBTS_URI, debt.id),
                debt.toContentValues(), null, null
            )
        }
    }

    fun deleteTransaction(id: Long, markAsVoid: Boolean = false, inBulk: Boolean = false): Boolean {
        val ops = ArrayList<ContentProviderOperation>()
        if (!markAsVoid) {
            loadAttachmentIds(id).forEach {
                ops.add(
                    ContentProviderOperation.newDelete(TRANSACTION_ATTACHMENT_SINGLE_URI(id, it))
                        .build()
                )
            }
        }
        ops.add(
            ContentProviderOperation.newDelete(
                ContentUris.withAppendedId(TRANSACTIONS_URI, id).buildUpon().apply {
                    if (markAsVoid) appendBooleanQueryParameter(QUERY_PARAMETER_MARK_VOID)
                    if (inBulk) appendBooleanQueryParameter(QUERY_PARAMETER_CALLER_IS_IN_BULK)
                }.build()
            ).build()
        )
        val result = contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
        return result.size == ops.size && result.last().count!! > 0
    }

    fun count(uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null) =
        contentResolver.query(uri, arrayOf("count(*)"), selection, selectionArgs, null, null)
            ?.use {
                it.moveToFirst()
                it.getInt(0)
            } ?: 0

    /**
     * @return the number of transactions that have been created since creation of the db based on sqllite sequence
     */
    open fun getSequenceCount() = contentResolver.query(
        TransactionProvider.SQLITE_SEQUENCE_TRANSACTIONS_URI,
        null, null, null, null
    )?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    } ?: 0L

    val budgetCreatorFunction: (Cursor) -> Budget = { cursor ->
        with(cursor) {
            val grouping = getEnum(KEY_GROUPING, Grouping.NONE)
            Budget(
                id = getLong(KEY_ROWID),
                accountId = getLong(KEY_ACCOUNTID),
                title = getString(KEY_TITLE),
                description = getString(KEY_DESCRIPTION),
                currency = getString(KEY_CURRENCY),
                grouping = grouping,
                color = getInt(KEY_COLOR),
                start = if (grouping == Grouping.NONE) getString(KEY_START) else null,
                end = if (grouping == Grouping.NONE) getString(KEY_END) else null,
                accountName = getStringOrNull(KEY_ACCOUNT_LABEL),
                default = getBoolean(KEY_IS_DEFAULT),
                uuid = getString(KEY_UUID),
                syncAccountName = getStringOrNull(KEY_SYNC_ACCOUNT_NAME)
            )
        }
    }
}

@JvmInline
value class AutoFillInfo(val categoryId: Long)