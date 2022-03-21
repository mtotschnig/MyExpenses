package org.totschnig.myexpenses.db2

import android.content.*
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model2.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.localDate2Epoch
import org.totschnig.myexpenses.viewmodel.data.Category2
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository(val contentResolver: ContentResolver, val currencyContext: CurrencyContext) {
    @Inject
    constructor(context: Context, currencyContext: CurrencyContext) : this(
        context.contentResolver,
        currencyContext
    )

    //Transaction
    fun createTransaction(transaction: Transaction) = with(transaction) {
        getCurrencyUnitForAccount(account)?.let { currencyUnit ->
            val ops = ArrayList<ContentProviderOperation>()
            ops.add(ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_URI)
                .withValues(
                    ContentValues().apply {
                        put(KEY_ACCOUNTID, account)
                        put(
                            KEY_AMOUNT,
                            Money(currencyUnit, BigDecimal(amount.toString())).amountMinor
                        )
                        val toEpochSecond = localDate2Epoch(date)
                        put(KEY_DATE, toEpochSecond)
                        put(KEY_VALUE_DATE, toEpochSecond)
                        put(KEY_PAYEEID, findOrWritePayee(payee))
                        put(KEY_CR_STATUS, CrStatus.UNRECONCILED.name)
                        category.takeIf { it > 0 }?.let { put(KEY_CATID, it) }
                        method.takeIf { it > 0 }?.let { put(KEY_METHODID, it) }
                        put(KEY_REFERENCE_NUMBER, number)
                        put(KEY_COMMENT, comment)
                        put(KEY_UUID, Model.generateUuid())
                    }
                ).build())
            for (tag in transaction.tags) {
                ops.add(
                    ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_TAGS_URI)
                        .withValueBackReference(KEY_TRANSACTIONID, 0)
                        .withValue(KEY_TAGID, tag).build()
                )
            }
            contentResolver.applyBatch(
                TransactionProvider.AUTHORITY,
                ops
            )[0].uri?.let { ContentUris.parseId(it) }
        }
    }

    //Payee
    fun findOrWritePayeeInfo(payeeName: String, autoFill: Boolean) = findPayee(payeeName)?.let {
        Pair(it, if (autoFill) autoFill(it) else null)
    } ?: Pair(createPayee(payeeName)!!, null)

    private fun autoFill(payeeId: Long): AutoFillInfo? {
        return contentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.AUTOFILL_URI, payeeId),
            arrayOf(KEY_CATID), null, null, null
        )?.use { cursor ->
            cursor.takeIf { it.moveToFirst() }?.let {
                DbUtils.getLongOrNull(it, 0)?.let { categoryId -> AutoFillInfo(categoryId) }
            }
        }
    }

    private fun findOrWritePayee(name: String) = findPayee(name) ?: createPayee(name)

    private fun findPayee(name: String) = contentResolver.query(
        TransactionProvider.PAYEES_URI,
        arrayOf(KEY_ROWID),
        KEY_PAYEE_NAME + " = ?",
        arrayOf(name.trim()), null
    )?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    }

    private fun createPayee(name: String) =
        contentResolver.insert(TransactionProvider.PAYEES_URI, ContentValues().apply {
            put(KEY_PAYEE_NAME, name.trim())
            put(KEY_PAYEE_NAME_NORMALIZED, Utils.normalize(name))
        })?.let { ContentUris.parseId(it) }

    //Account
    private fun getCurrencyUnitForAccount(accountId: Long) =
        contentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId),
            arrayOf(KEY_CURRENCY), null, null, null
        )?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }?.let { currencyContext[it] }

    //Transaction
    fun getUuidForTransaction(transactionId: Long) =
        contentResolver.query(
            ContentUris.withAppendedId(TransactionProvider.TRANSACTIONS_URI, transactionId),
            arrayOf(KEY_UUID), null, null, null
        )?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    fun saveDebt(debt: Debt) {
        if (debt.id == 0L) {
            contentResolver.insert(TransactionProvider.DEBTS_URI, debt.toContentValues())
        } else {
            contentResolver.update(
                ContentUris.withAppendedId(TransactionProvider.DEBTS_URI, debt.id),
                debt.toContentValues(), null, null
            )
        }
    }

    fun saveParty(id: Long, name: String) = Payee(id, name).save()

    fun saveCategory(category: Category2): Uri? {
        val initialValues = ContentValues().apply {
            put(KEY_LABEL, category.label)
            put(KEY_LABEL_NORMALIZED, Utils.normalize(category.label))
            category.color.takeIf { it != 0 }?.let {
                put(KEY_COLOR, it)
                put(KEY_ICON, category.icon)
            }
            if (category.id == 0L) {
                put(KEY_PARENTID, category.parentId)
            }
        }
        return try {
            if (category.id == 0L) {
                contentResolver.insert(TransactionProvider.CATEGORIES_URI, initialValues)
            } else {
                Category.CONTENT_URI.buildUpon().appendPath(category.id.toString()).build().let {
                    if (contentResolver.update(it, initialValues, null, null) == 0)
                        null else it
                }
            }
        } catch (e: SQLiteConstraintException) {
            null
        }
    }

    fun moveCategory(source: Long, target: Long?) = try {
        contentResolver.update(
            TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(source.toString())
                .build(),
            ContentValues().apply {
                put(KEY_PARENTID, target)
            },
            null,
            null
        ) > 0
    } catch (e: SQLiteConstraintException) {
        false
    }
}

data class AutoFillInfo(val categoryId: Long)