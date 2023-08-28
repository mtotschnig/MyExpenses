package org.totschnig.myexpenses.db2

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.core.database.getLongOrNull
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Payee
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.TransactionProvider.AUTOFILL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DEBTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.PAYEES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_CALLER_IS_IN_BULK
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_MARK_VOID
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.data.Debt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class Repository @Inject constructor(
    val context: Context,
    val currencyContext: CurrencyContext,
    val currencyFormatter: CurrencyFormatter,
    val prefHandler: PrefHandler
) {
    companion object {
        const val UUID_SEPARATOR = ":"
    }

    val contentResolver: ContentResolver = context.contentResolver

    //Payee
    fun findOrWritePayeeInfo(payeeName: String, autoFill: Boolean) = findPayee(payeeName)?.let {
        Pair(it, if (autoFill) autoFill(it) else null)
    } ?: Pair(createPayee(payeeName)!!, null)

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

    internal fun findOrWritePayee(name: String) = findPayee(name) ?: createPayee(name)

    private fun findPayee(name: String) = contentResolver.query(
        PAYEES_URI,
        arrayOf(KEY_ROWID),
        "$KEY_PAYEE_NAME = ?",
        arrayOf(name.trim()), null
    )?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    }

    private fun createPayee(name: String) =
        contentResolver.insert(PAYEES_URI, ContentValues().apply {
            put(KEY_PAYEE_NAME, name.trim())
            put(KEY_PAYEE_NAME_NORMALIZED, Utils.normalize(name))
        })?.let { ContentUris.parseId(it) }

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

    fun deleteTransaction(id: Long, markAsVoid: Boolean = false, inBulk: Boolean = false) =
        contentResolver.delete(
            ContentUris.withAppendedId(TRANSACTIONS_URI, id).buildUpon().apply {
                if (markAsVoid) appendBooleanQueryParameter(QUERY_PARAMETER_MARK_VOID)
                if (inBulk) appendBooleanQueryParameter(QUERY_PARAMETER_CALLER_IS_IN_BULK)
            }.build(),
            null,
            null
        ) > 0

    fun count(uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null): Int {
        return contentResolver.query(uri, arrayOf("count(*)"), selection, selectionArgs, null, null)
            ?.use {
                it.moveToFirst()
                it.getInt(0)
            } ?: 0
    }
}

@JvmInline
value class AutoFillInfo(val categoryId: Long)