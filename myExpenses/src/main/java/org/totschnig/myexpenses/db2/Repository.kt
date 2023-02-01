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
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Payee
import org.totschnig.myexpenses.model.Transaction.PROJECTION_EXTENDED
import org.totschnig.myexpenses.model2.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider.*
import org.totschnig.myexpenses.provider.appendBooleanQueryParameter
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.useAndMap
import org.totschnig.myexpenses.provider.withLimit
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.localDate2Epoch
import org.totschnig.myexpenses.util.localDateTime2Epoch
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel.Companion.prefNameForCriteria
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

    private fun Transaction.toContentValues() = ContentValues().apply {
        put(KEY_ACCOUNTID, account)
        put(
            KEY_AMOUNT,
            Money(getCurrencyUnitForAccount(account)!!, BigDecimal(amount.toString())).amountMinor
        )
        put(KEY_DATE, time?.let {
            localDateTime2Epoch(LocalDateTime.of(date, time))
        } ?: localDate2Epoch(date))
        put(KEY_VALUE_DATE, localDate2Epoch(valueDate))
        payee.takeIf { it.isNotEmpty() }?.let { put(KEY_PAYEEID, findOrWritePayee(it)) }
        put(KEY_CR_STATUS, CrStatus.UNRECONCILED.name)
        category?.takeIf { it > 0 }?.let { put(KEY_CATID, it) }
        method.takeIf { it > 0 }?.let { put(KEY_METHODID, it) }
        put(KEY_REFERENCE_NUMBER, number)
        put(KEY_COMMENT, comment)
        put(KEY_UUID, Model.generateUuid())
    }

    //Transaction
    fun updateTransaction(id: String, transaction: Transaction): Int {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(
            ContentProviderOperation.newUpdate(TRANSACTIONS_URI)
                .withValues(transaction.toContentValues())
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
            AUTHORITY,
            ops
        )
        return results[0].count ?: 0
    }

    fun createTransaction(transaction: Transaction): Long {
        val values = transaction.toContentValues().apply {
            put(KEY_UUID, Model.generateUuid())
        }
        val ops = ArrayList<ContentProviderOperation>().apply {
            add(
                ContentProviderOperation.newInsert(TRANSACTIONS_URI)
                    .withValues(values)
                    .build()
            )
            for (tag in transaction.tags) {
                add(
                    ContentProviderOperation.newInsert(TRANSACTIONS_TAGS_URI)
                        .withValueBackReference(KEY_TRANSACTIONID, 0)
                        .withValue(KEY_TAGID, tag).build()
                )
            }
        }
        return contentResolver.applyBatch(AUTHORITY, ops)[0].uri!!.let {
            ContentUris.parseId(it)
        }
    }

    fun loadTransactions(accountId: Long): List<Transaction> {
        val filter = FilterPersistence(
            prefHandler = prefHandler,
            keyTemplate = prefNameForCriteria(accountId),
            savedInstanceState = null,
            immediatePersist = false,
            restoreFromPreferences = true
        ).whereFilter.takeIf { !it.isEmpty }?.let {
            it.getSelectionForParents(VIEW_EXTENDED) to it.getSelectionArgs(false)
        }
        //noinspection Recycle
        return contentResolver.query(
            Account.extendedUriForTransactionList(true),
            PROJECTION_EXTENDED,
            "$KEY_ACCOUNTID = ? AND $KEY_PARENTID IS NULL ${
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
                    TRANSACTIONS_TAGS_URI,
                    arrayOf(KEY_ROWID),
                    "$KEY_TRANSACTIONID = ?",
                    arrayOf(cursor.getLong(KEY_ROWID).toString()),
                    null
                )?.useAndMap { it.getLong(0) } ?: emptyList()
            )
        }
    }

    //Payee
    fun findOrWritePayeeInfo(payeeName: String, autoFill: Boolean) = findPayee(payeeName)?.let {
        Pair(it, if (autoFill) autoFill(it) else null)
    } ?: Pair(createPayee(payeeName)!!, null)

    private fun autoFill(payeeId: Long): AutoFillInfo? {
        return contentResolver.query(
            ContentUris.withAppendedId(AUTOFILL_URI, payeeId),
            arrayOf(KEY_CATID), null, null, null
        )?.use { cursor ->
            cursor.takeIf { it.moveToFirst() }?.let {
                it.getLongOrNull(0)?.let { categoryId -> AutoFillInfo(categoryId) }
            }
        }
    }

    private fun findOrWritePayee(name: String) = findPayee(name) ?: createPayee(name)

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

    //Account
    fun getCurrencyUnitForAccount(accountId: Long): CurrencyUnit? {
        require(accountId != 0L)
        return contentResolver.query(
            ContentUris.withAppendedId(ACCOUNTS_URI, accountId),
            arrayOf(KEY_CURRENCY), null, null, null
        )?.use {
            if (it.moveToFirst()) currencyContext[it.getString(0)] else null
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

    fun getLastUsedOpenAccount() =
        contentResolver.query(
            ACCOUNTS_URI.withLimit(1), arrayOf(KEY_ROWID, KEY_CURRENCY), "$KEY_SEALED = 0", null, KEY_LAST_USED
        )?.use {
            if (it.moveToFirst()) it.getLong(0) to currencyContext.get(it.getString(1)) else null
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

    fun deleteTransaction(id: Long, markAsVoid: Boolean = false, inBulk: Boolean = false) =
        contentResolver.delete(
            ContentUris.withAppendedId(TRANSACTIONS_URI, id).buildUpon().apply {
                if (markAsVoid) appendBooleanQueryParameter(QUERY_PARAMETER_MARK_VOID)
                if (inBulk) appendBooleanQueryParameter(QUERY_PARAMETER_CALLER_IS_IN_BULK)
            }.build(),
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