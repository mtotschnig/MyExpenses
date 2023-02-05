package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.mapToOne
import app.cash.copper.flow.observeQuery
import com.squareup.sqlbrite3.BriteContentResolver
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.RenderType
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Account.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider.*
import org.totschnig.myexpenses.provider.checkForSealedDebt
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal
import org.totschnig.myexpenses.viewmodel.data.Budget
import org.totschnig.myexpenses.viewmodel.data.DateInfo2
import org.totschnig.myexpenses.viewmodel.data.Debt
import javax.inject.Inject
import kotlin.collections.set

const val KEY_ROW_IDS = "rowIds"

object AccountSealedException : IllegalStateException()

abstract class ContentResolvingAndroidViewModel(application: Application) :
    BaseViewModel(application) {
    @Inject
    lateinit var briteContentResolver: BriteContentResolver

    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    val collate: String
        get() = prefHandler.collate

    private val bulkDeleteStateInternal: MutableStateFlow<DeleteState?> = MutableStateFlow(null)
    val bulkDeleteState: StateFlow<DeleteState?> = bulkDeleteStateInternal

    fun bulkDeleteCompleteShown() {
        bulkDeleteStateInternal.update {
            null
        }
    }

    var disposable: Disposable? = null

    val contentResolver: ContentResolver
        get() = getApplication<MyApplication>().contentResolver

    val renderer: Flow<RenderType> by lazy {
        dataStore.data.map {
            if (it[prefHandler.getBooleanPreferencesKey(PrefKey.UI_ITEM_RENDERER_LEGACY)] == true)
                RenderType.Legacy else RenderType.New
        }
    }

    val withCategoryIcon: Flow<Boolean> by lazy {
        dataStore.data.map {
            it[prefHandler.getBooleanPreferencesKey(PrefKey.UI_ITEM_RENDERER_CATEGORY_ICON)] != false
        }
    }

    val dateInfo: Flow<DateInfo2> = flow {
        contentResolver.query(
            DUAL_URI,
            arrayOf(
                "${getThisYearOfWeekStart()} AS $KEY_THIS_YEAR_OF_WEEK_START",
                "${getThisYearOfMonthStart()} AS $KEY_THIS_YEAR_OF_MONTH_START",
                "$THIS_YEAR AS $KEY_THIS_YEAR",
                "${getThisMonth()} AS $KEY_THIS_MONTH",
                "${getThisWeek()} AS $KEY_THIS_WEEK",
                "$THIS_DAY AS $KEY_THIS_DAY"
            ),
            null, null, null, null
        )?.use { cursor ->
            cursor.moveToFirst()
            emit(DateInfo2.fromCursor(cursor))
        }
    }.flowOn(Dispatchers.IO)

    val budgetCreatorFunction: (Cursor) -> Budget = { cursor ->
        val currency = cursor.getString(KEY_CURRENCY)
        val currencyUnit = if (currency == AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE)
            Utils.getHomeCurrency() else currencyContext.get(currency)
        val budgetId = cursor.getLong(KEY_ROWID)
        val accountId = cursor.getLong(KEY_ACCOUNTID)
        val grouping = Grouping.valueOf(cursor.getString(KEY_GROUPING))
        Budget(
            id = budgetId,
            accountId = accountId,
            title = cursor.getString(KEY_TITLE),
            description = cursor.getString(KEY_DESCRIPTION),
            currency = currencyUnit,
            grouping = grouping,
            color = cursor.getInt(KEY_COLOR),
            start = cursor.getStringOrNull(KEY_START),
            end = cursor.getStringOrNull(KEY_END),
            accountName = cursor.getStringOrNull(KEY_ACCOUNT_LABEL),
            default = cursor.getBoolean(KEY_IS_DEFAULT)
        )
    }

    fun accountsMinimal(withHidden: Boolean = true) = contentResolver.observeQuery(
        ACCOUNTS_MINIMAL_URI, null,
        if (withHidden) null else "$KEY_HIDDEN = 0",
        null, null, false
    )
        .mapToList { cursor ->
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID))
            AccountMinimal(
                id,
                if (id == HOME_AGGREGATE_ID)
                    getString(R.string.grand_total)
                else
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_CURRENCY))
            )
        }

    fun account(accountId: Long, once: Boolean = false) = liveData(context = coroutineContext()) {
        val base =
            if (accountId > 0) ACCOUNTS_URI else ACCOUNTS_AGGREGATE_URI
        val flow = contentResolver.observeQuery(
            ContentUris.withAppendedId(base, accountId),
            Account.PROJECTION_BASE, null, null, null, true
        )
            .mapToOne { Account.fromCursor(it) }
        //.throttleFirst(100, TimeUnit.MILLISECONDS)
        (if (once) flow.take(1) else flow).collect {
            this.emit(it)
        }
    }

    override fun onCleared() {
        dispose()
    }

    fun dispose() {
        disposable?.let {
            if (!it.isDisposed) it.dispose()
        }
    }

    sealed class DeleteState {
        data class DeleteProgress(val count: Int, val total: Int) : DeleteState()
        data class DeleteComplete(val success: Int, val failure: Int) : DeleteState()
    }

    fun deleteTemplates(ids: LongArray, deletePlan: Boolean): LiveData<DeleteState.DeleteComplete> =
        liveData(context = coroutineContext()) {
            var success = 0
            var failure = 0
            ids.forEach {
                try {
                    Template.delete(it, deletePlan)
                    success++
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(e)
                    failure++
                }
            }
            emit(DeleteState.DeleteComplete(success, failure))
        }


    fun deleteTransactions(ids: LongArray, markAsVoid: Boolean) {
        viewModelScope.launch(context = coroutineContext()) {
            var success = 0
            var failure = 0
            ids.forEach {
                try {
                    if (repository.deleteTransaction(it, markAsVoid, true))
                        success++ else failure++
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(e)
                    failure++
                }
                bulkDeleteStateInternal.update {
                    DeleteState.DeleteProgress(success + failure, ids.size)
                }
            }
            contentResolver.notifyChange(TRANSACTIONS_URI, null, true)
            contentResolver.notifyChange(ACCOUNTS_URI, null, false)
            contentResolver.notifyChange(DEBTS_URI, null, false)
            contentResolver.notifyChange(UNCOMMITTED_URI, null, false)
            bulkDeleteStateInternal.update {
                DeleteState.DeleteComplete(success, failure)
            }
        }
    }

    internal fun deleteAccountsInternal(accountIds: LongArray) =
        if (contentResolver.query(
                TRANSACTIONS_URI,
                arrayOf("MAX($checkForSealedDebt)"),
                "$KEY_ACCOUNTID IN (${accountIds.joinToString()})",
                null,
                null
            )?.use {
                it.moveToFirst()
                it.getInt(0)
            } == 1
        ) {
            Result.failure(AccountSealedException)
        } else {
            val failures = mutableListOf<Exception>()
            for (accountId in accountIds) {
                try {
                    Account.delete(accountId)
                } catch (e: Exception) {
                    failures.add(e)
                }
            }
            if (failures.isEmpty())
                ResultUnit
            else
                Result.failure(CompositeException(failures))
        }

    /**
     * @param rowId For split transactions, we check if any of their children is linked to a debt,
     * in which case the parent should not be linkable to a debt, and we return an empty list
     */
    fun loadDebts(rowId: Long? = null, showAll: Boolean = false) = contentResolver.observeQuery(
            uri = with(DEBTS_URI.buildUpon()) {
                rowId?.let {
                    appendQueryParameter(KEY_TRANSACTIONID, rowId.toString())
                }
                build()
            },
            selection = if(showAll) null else  "$KEY_SEALED = 0 AND $KEY_AMOUNT-$KEY_SUM != 0",
            notifyForDescendants = true
        )
            .mapToList { Debt.fromCursor(it, currencyContext) }

    /**
     * deletes all expenses and updates account according to value of handleDelete
     *
     * @param filter        if not null only expenses matched by filter will be deleted
     * @param handleDelete  if equals [.EXPORT_HANDLE_DELETED_UPDATE_BALANCE] opening balance will
     * be adjusted to account for the deleted expenses,
     * if equals [.EXPORT_HANDLE_DELETED_CREATE_HELPER] a helper transaction
     * @param helperComment comment used for the helper transaction
     */
    fun reset(account: Account, filter: WhereFilter?, handleDelete: Int, helperComment: String?) {
        val ops = ArrayList<ContentProviderOperation>()
        var handleDeleteOperation: ContentProviderOperation? = null
        val sum = account.getTransactionSum(filter)
        if (handleDelete == Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE) {
            val currentBalance: Long = account.openingBalance.amountMinor + sum
            handleDeleteOperation = ContentProviderOperation.newUpdate(
                Account.CONTENT_URI.buildUpon().appendPath(account.id.toString()).build()
            )
                .withValue(KEY_OPENING_BALANCE, currentBalance)
                .build()
        } else if (handleDelete == Account.EXPORT_HANDLE_DELETED_CREATE_HELPER) {
            val helper = Transaction(account.id, Money(account.currencyUnit, sum))
            helper.comment = helperComment
            helper.status = STATUS_HELPER
            handleDeleteOperation = ContentProviderOperation.newInsert(Transaction.CONTENT_URI)
                .withValues(helper.buildInitialValues()).build()
        }
        val rowSelect = Account.buildTransactionRowSelect(filter)
        var selectionArgs: Array<String>? = arrayOf(account.id.toString())
        if (filter != null && !filter.isEmpty) {
            selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false))
        }
        account.updateTransferPeersForTransactionDelete(ops, rowSelect, selectionArgs)
        ops.add(
            ContentProviderOperation.newDelete(
                Transaction.CONTENT_URI
            )
                .withSelection(
                    "$KEY_ROWID IN ($rowSelect)",
                    selectionArgs
                )
                .build()
        )
        //needs to be last, otherwise helper transaction would be deleted
        if (handleDeleteOperation != null) ops.add(handleDeleteOperation)
        contentResolver.applyBatch(AUTHORITY, ops)
    }

/*    fun loadDebugDebts(count: Int = 10) {
        debts.postValue(List(
            count
        ) {
            Debt(
                it.toLong(),
                "Debt $it",
                "Description",
                1,
                5000,
                "EUR",
                System.currentTimeMillis() / 1000,
                "John doe",
                false,
                4123
            )
        })
    }*/

    companion object {
        fun <K, V> lazyMap(initializer: (K) -> V): Map<K, V> {
            val map = mutableMapOf<K, V>()
            return map.withDefault { key ->
                val newValue = initializer(key)
                map[key] = newValue
                return@withDefault newValue
            }
        }
    }
}