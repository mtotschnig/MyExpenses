package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.database.sqlite.SQLiteConstraintException
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
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Account.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.checkForSealedDebt
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal
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

    var disposable: Disposable? = null

    val contentResolver: ContentResolver
        get() = getApplication<MyApplication>().contentResolver

    private val debts = MutableLiveData<List<Debt>>()
    fun getDebts(): LiveData<List<Debt>> = debts

    protected fun accountsMinimal(withHidden: Boolean = true): LiveData<List<AccountMinimal>> {
        val liveData = MutableLiveData<List<AccountMinimal>>()
        disposable = briteContentResolver.createQuery(
            TransactionProvider.ACCOUNTS_MINIMAL_URI, null,
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
            .subscribe {
                liveData.postValue(it)
                dispose()
            }
        return liveData
    }

    fun account(accountId: Long, once: Boolean = false) = liveData(context = coroutineContext()) {
        val base =
            if (accountId > 0) TransactionProvider.ACCOUNTS_URI else TransactionProvider.ACCOUNTS_AGGREGATE_URI
        val flow = contentResolver.observeQuery(
            ContentUris.withAppendedId(base, accountId),
            Account.PROJECTION_BASE, null, null, null, true
        )
            .mapToOne { Account.fromCursor(it) }
        //.throttleFirst(100, TimeUnit.MILLISECONDS)
        (if (once) flow.take(1) else flow).collect {
            this.emit(it)
            onAccountLoaded(it)
        }
    }


    open fun onAccountLoaded(account: Account) {}

    override fun onCleared() {
        dispose()
    }

    fun dispose() {
        disposable?.let {
            if (!it.isDisposed) it.dispose()
        }
    }

    fun deleteTemplates(ids: LongArray, deletePlan: Boolean): LiveData<Int> =
        liveData(context = coroutineContext()) {
            emit(ids.sumBy {
                try {
                    Template.delete(it, deletePlan)
                    1
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(e)
                    0
                }
            })
        }


    fun deleteTransactions(ids: LongArray, markAsVoid: Boolean): LiveData<Int> =
        liveData(context = coroutineContext()) {
            emit(ids.sumBy {
                try {
                    Transaction.delete(it, markAsVoid)
                    1
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(e)
                    0
                }
            })
        }

    internal fun deleteAccountsInternal(accountIds: Array<Long>) =
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
    fun loadDebts(rowId: Long? = null) {
        viewModelScope.launch {
            contentResolver.observeQuery(
                uri = with(TransactionProvider.DEBTS_URI.buildUpon()) {
                    rowId?.let {
                        appendQueryParameter(KEY_TRANSACTIONID, rowId.toString())
                    }
                    build()
                },
                selection = "$KEY_SEALED = 0",
                notifyForDescendants = true
            )
                .mapToList { Debt.fromCursor(it, currencyContext) }
                .collect {
                    debts.postValue(it)
                }
        }
    }

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
        contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
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