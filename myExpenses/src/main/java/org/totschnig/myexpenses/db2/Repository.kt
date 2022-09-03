package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Payee
import org.totschnig.myexpenses.model.Transaction.PROJECTION_EXTENDED
import org.totschnig.myexpenses.model2.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.CATEGORIES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_TAGS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.localDate2Epoch
import org.totschnig.myexpenses.util.localDateTime2Epoch
import org.totschnig.myexpenses.viewmodel.TransactionListViewModel.Companion.prefNameForCriteria
import org.totschnig.myexpenses.viewmodel.data.Category
import org.totschnig.myexpenses.viewmodel.data.Debt
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    val context: Context,
    val currencyContext: CurrencyContext,
    val currencyFormatter: CurrencyFormatter,
    val prefHandler: PrefHandler
) {

    val contentResolver: ContentResolver = context.contentResolver

    fun Transaction.toContentValues() = getCurrencyUnitForAccount(account)?.let { currencyUnit ->
        ContentValues().apply {
            put(KEY_ACCOUNTID, account)
            put(
                KEY_AMOUNT,
                Money(currencyUnit, BigDecimal(amount.toString())).amountMinor
            )
            put(KEY_DATE, time?.let {
                localDateTime2Epoch(LocalDateTime.of(date, time))
            } ?: localDate2Epoch(date))
            put(KEY_VALUE_DATE, localDate2Epoch(valueDate))
            payee.takeIf { it.isNotEmpty() }?.let {  put(KEY_PAYEEID, findOrWritePayee(it)) }
            put(KEY_CR_STATUS, CrStatus.UNRECONCILED.name)
            category?.takeIf { it > 0 }?.let { put(KEY_CATID, it) }
            method.takeIf { it > 0 }?.let { put(KEY_METHODID, it) }
            put(KEY_REFERENCE_NUMBER, number)
            put(KEY_COMMENT, comment)
            put(KEY_UUID, Model.generateUuid())
        }
    }

    //Transaction
    fun updateTransaction(id: String, transaction: Transaction) = transaction.toContentValues()?.let {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(
            ContentProviderOperation.newUpdate(TRANSACTIONS_URI).withValues(it)
                .withSelection("$KEY_ROWID = ?", arrayOf(id))
                .build()
        )
        ops.add(
            ContentProviderOperation.newDelete(TRANSACTIONS_TAGS_URI)
                .withSelection("$KEY_TRANSACTIONID = ?", arrayOf(id))
                .build()
        )
        for (tag in transaction.tags) {
            ops.add(
                ContentProviderOperation.newInsert(TRANSACTIONS_TAGS_URI)
                    .withValue(KEY_TRANSACTIONID, id)
                    .withValue(KEY_TAGID, tag).build()
            )
        }
        val results = contentResolver.applyBatch(
            TransactionProvider.AUTHORITY,
            ops
        )
        results[0].count
    }

    fun createTransaction(transaction: Transaction) = transaction.toContentValues()?.let { values ->
        val ops = ArrayList<ContentProviderOperation>()
        values.put(KEY_UUID, Model.generateUuid())
        ops.add(
            ContentProviderOperation.newInsert(TRANSACTIONS_URI)
                .withValues(values)
                .build()
        )
        for (tag in transaction.tags) {
            ops.add(
                ContentProviderOperation.newInsert(TRANSACTIONS_TAGS_URI)
                    .withValueBackReference(KEY_TRANSACTIONID, 0)
                    .withValue(KEY_TAGID, tag).build()
            )
        }
        contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)[0].uri?.let {
            ContentUris.parseId(it)
        }
    }

    fun loadTransactions(accountId: Long): List<Transaction> =
        getCurrencyUnitForAccount(accountId)?.let { currencyUnit ->
            val filter = FilterPersistence(
                    prefHandler = prefHandler,
                    keyTemplate = prefNameForCriteria(accountId),
                    savedInstanceState = null,
                    immediatePersist = false,
                    restoreFromPreferences = true
                ).whereFilter.takeIf { !it.isEmpty }?.let {
                    it.getSelectionForParents(VIEW_EXTENDED) to it.getSelectionArgs(false)
            }
            contentResolver.query(
                Account.extendedUriForTransactionList(true),
                PROJECTION_EXTENDED,
                "$KEY_ACCOUNTID = ? AND $KEY_PARENTID IS NULL ${filter?.first?.takeIf { it != "" }?.let { "AND $it" } ?: ""}",
                filter?.let { arrayOf(accountId.toString(), *it.second) } ?: arrayOf(accountId.toString()),
                null
            )?.use { cursor ->
                cursor.asSequence.map { cursor ->
                    Transaction.fromCursor(
                        context,
                        cursor,
                        accountId,
                        currencyUnit,
                        currencyFormatter,
                        Utils.ensureDateFormatWithShortYear(context)
                    ).copy(
                        tags = contentResolver.query(
                            TRANSACTIONS_TAGS_URI,
                            arrayOf(KEY_ROWID),
                            "$KEY_TRANSACTIONID = ?",
                            arrayOf(cursor.getLong(KEY_ROWID).toString()),
                            null
                        )?.use { tagCursor ->
                            tagCursor.asSequence.map { it.getLong(0) }.toList()
                        } ?: emptyList()
                    )
                }.toList()
            }
        } ?: emptyList()

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
        "$KEY_PAYEE_NAME = ?",
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
            ContentUris.withAppendedId(TRANSACTIONS_URI, transactionId),
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

    fun saveCategory(category: Category): Uri? {
        val initialValues = ContentValues().apply {
            put(KEY_LABEL, category.label.trim())
            put(KEY_LABEL_NORMALIZED, Utils.normalize(category.label))
            category.color.takeIf { it != 0 }?.let {
                put(KEY_COLOR, it)
            }
            put(KEY_ICON, category.icon)
            if (category.id == 0L) {
                put(KEY_PARENTID, category.parentId)
            }
        }
        return try {
            if (category.id == 0L) {
                contentResolver.insert(CATEGORIES_URI, initialValues)
            } else {
                CATEGORIES_URI.buildUpon().appendPath(category.id.toString()).build().let {
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
            CATEGORIES_URI.buildUpon().appendPath(source.toString())
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

    fun deleteTransaction(id: Long) = contentResolver.delete(
        ContentUris.withAppendedId(TRANSACTIONS_URI, id),
        null,
        null
    ) > 0

    fun deleteCategory(id: Long) = contentResolver.delete(
        ContentUris.withAppendedId(CATEGORIES_URI, id),
        null,
        null
    ) > 0

    fun updateCategoryColor(id: Long, color: Int?) = contentResolver.update(
        ContentUris.withAppendedId(CATEGORIES_URI, id),
        ContentValues().apply {
            put(KEY_COLOR, color)
        }, null, null
    ) == 1

    /**
     * Looks for a cat with a label under a given parent
     *
     * @return id or -1 if not found
     */
    fun findCategory(label: String, parentId: Long? = null): Long {
        val stripped = label.trim()
        val (parentSelection, parentSelectionArgs) = if (parentId == null) {
            "$KEY_PARENTID is null" to emptyArray()
        } else {
            "$KEY_PARENTID = ?" to arrayOf(parentId.toString())
        }
        return contentResolver.query(
            CATEGORIES_URI,
            arrayOf(KEY_ROWID),
            "$KEY_LABEL = ? AND $parentSelection",
            arrayOf(stripped) + parentSelectionArgs,
            null
        )?.use {
            if (it.count == 0) -1 else {
                it.moveToFirst()
                it.getLong(0)
            }
        } ?: -1
    }

    @VisibleForTesting
    fun loadCategory(id: Long): Category? = contentResolver.query(
        CATEGORIES_URI,
        arrayOf(KEY_PARENTID, KEY_LABEL, KEY_COLOR, KEY_ICON),
        "$KEY_ROWID = ?",
        arrayOf(id.toString()),
        null
    )?.use {
        if (it.moveToFirst())
            Category(
                id = id,
                parentId = it.getLongOrNull(0),
                label = it.getString(1),
                color = it.getIntOrNull(2),
                icon = it.getString(3)
            ) else null
    }

    fun count(uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null): Int {
        return contentResolver.query(uri, arrayOf("count(*)"), selection, selectionArgs, null, null)
            ?.use {
                it.moveToFirst()
                it.getInt(0)
            } ?: 0
    }
}

data class AutoFillInfo(val categoryId: Long)