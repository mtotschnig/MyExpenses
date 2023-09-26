package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import androidx.core.database.getLongOrNull
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ATTACHMENT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_URI
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.AUTOFILL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DEBTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_CALLER_IS_IN_BULK
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_MARK_VOID
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTION_ATTACHMENT_SINGLE_URI
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.viewmodel.data.Debt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class Repository @Inject constructor(
    val context: Context,
    val currencyContext: CurrencyContext,
    val currencyFormatter: ICurrencyFormatter,
    val prefHandler: PrefHandler
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
        loadAttachmentIds(id).forEach {
            ops.add(
                ContentProviderOperation.newDelete(TRANSACTION_ATTACHMENT_SINGLE_URI(id, it))
                    .build()
            )
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
        return result.size == ops.size && result.last().count == 1
    }

    fun count(uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null): Int {
        return contentResolver.query(uri, arrayOf("count(*)"), selection, selectionArgs, null, null)
            ?.use {
                it.moveToFirst()
                it.getInt(0)
            } ?: 0
    }

    fun countTransactionsPerAccount(accountId: Long) =
        count(TRANSACTIONS_URI, "$KEY_ACCOUNTID = ?", arrayOf(accountId.toString()))

    /**
     * @return the number of transactions that have been created since creation of the db based on sqllite sequence
     */
    open fun getSequenceCount() = contentResolver.query(
        TransactionProvider.SQLITE_SEQUENCE_TRANSACTIONS_URI,
        null, null, null, null
    )?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    } ?: 0L
}

@JvmInline
value class AutoFillInfo(val categoryId: Long)