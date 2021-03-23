package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model2.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository(val contentResolver: ContentResolver, val currencyContext: CurrencyContext) {
    @Inject
    constructor(context: Context, currencyContext: CurrencyContext) : this(context.contentResolver, currencyContext)

    //Transaction
    fun createTransaction(transaction: Transaction) = with(transaction) {
        getCurrencyUnitForAccount(account)?.let { currencyUnit ->
            val ops = ArrayList<ContentProviderOperation>()
            ops.add(ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_URI).withValues(
                    ContentValues().apply {
                        put(DatabaseConstants.KEY_ACCOUNTID, account)
                        put(DatabaseConstants.KEY_AMOUNT, Money(currencyUnit, BigDecimal(amount.toString())).amountMinor)
                        val toEpochSecond = ZonedDateTime.of(date, LocalTime.now(), ZoneId.systemDefault()).toEpochSecond()
                        put(DatabaseConstants.KEY_DATE, toEpochSecond)
                        put(DatabaseConstants.KEY_VALUE_DATE, toEpochSecond)
                        put(DatabaseConstants.KEY_PAYEEID, findOrWritePayee(payee))
                        put(DatabaseConstants.KEY_CR_STATUS, CrStatus.UNRECONCILED.name)
                        category.takeIf { it > 0 }?.let { put(KEY_CATID, it) }
                        method.takeIf { it > 0 }?.let { put(DatabaseConstants.KEY_METHODID, it) }
                        put(DatabaseConstants.KEY_REFERENCE_NUMBER, number)
                        put(DatabaseConstants.KEY_COMMENT, comment)
                        put(DatabaseConstants.KEY_UUID, Model.generateUuid())
                    }
            ).build())
            for (tag in transaction.tags) {
                ops.add(ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_TAGS_URI)
                        .withValueBackReference(KEY_TRANSACTIONID, 0)
                        .withValue(DatabaseConstants.KEY_TAGID, tag).build())
            }
            contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)[0].uri?.let { ContentUris.parseId(it) }
        }
    }

    //Payee
    fun findOrWritePayeeInfo(payeeName: String, autoFill: Boolean) = findPayee(payeeName)?.let {
        Pair(it, if (autoFill) autoFill(it) else null)
    } ?: Pair(createPayee(payeeName)!!, null)

    private fun autoFill(payeeId: Long): AutoFillInfo? {
        return contentResolver.query(ContentUris.withAppendedId(TransactionProvider.AUTOFILL_URI, payeeId),
                arrayOf(KEY_CATID), null, null, null)?.use { cursor ->
            cursor.takeIf { it.moveToFirst() }?.let { DbUtils.getLongOrNull(it, 0)?.let { categoryId -> AutoFillInfo(categoryId) } }
        }
    }

    private fun findOrWritePayee(name: String) = findPayee(name) ?: createPayee(name)

    private fun findPayee(name: String) = contentResolver.query(TransactionProvider.PAYEES_URI,
            arrayOf(DatabaseConstants.KEY_ROWID),
            DatabaseConstants.KEY_PAYEE_NAME + " = ?",
            arrayOf(name.trim()), null)?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    }

    private fun createPayee(name: String) =
            contentResolver.insert(TransactionProvider.PAYEES_URI, ContentValues().apply {
                put(DatabaseConstants.KEY_PAYEE_NAME, name)
                put(DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED, Utils.normalize(name))
            })?.let { ContentUris.parseId(it) }

    //Account
    private fun getCurrencyUnitForAccount(accountId: Long) =
            contentResolver.query(ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
                    arrayOf(DatabaseConstants.KEY_CURRENCY), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }?.let { currencyContext[it] }

    //Transaction
    fun getUuidForTransaction(transactionId: Long) =
            contentResolver.query(ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, transactionId),
                    arrayOf(DatabaseConstants.KEY_UUID), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
}

data class AutoFillInfo(val categoryId: Long)