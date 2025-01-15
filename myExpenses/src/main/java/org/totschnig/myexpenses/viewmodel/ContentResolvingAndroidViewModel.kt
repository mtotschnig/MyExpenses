package org.totschnig.myexpenses.viewmodel

import android.accounts.AccountManager
import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.database.sqlite.SQLiteConstraintException
import android.os.Build
import android.text.TextUtils
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ColorSource
import org.totschnig.myexpenses.compose.RenderType
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.countAccounts
import org.totschnig.myexpenses.db2.deleteAccount
import org.totschnig.myexpenses.db2.getTransactionSum
import org.totschnig.myexpenses.db2.loadAccountFlow
import org.totschnig.myexpenses.db2.loadAggregateAccountFlow
import org.totschnig.myexpenses.db2.updateTransferPeersForTransactionDelete
import org.totschnig.myexpenses.dialog.select.SelectFromMappedTableDialogFragment
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.ACCOUNTS_MINIMAL_URI_WITH_AGGREGATES
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_HELPER
import org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_MINIMAL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.AUTHORITY
import org.totschnig.myexpenses.provider.TransactionProvider.DEBTS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.UNCOMMITTED_URI
import org.totschnig.myexpenses.provider.buildTransactionRowSelect
import org.totschnig.myexpenses.provider.checkForSealedDebt
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.joinArrays
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_CREATE_HELPER
import org.totschnig.myexpenses.viewmodel.ExportViewModel.Companion.EXPORT_HANDLE_DELETED_UPDATE_BALANCE
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal
import org.totschnig.myexpenses.viewmodel.data.DateInfo
import org.totschnig.myexpenses.viewmodel.data.DisplayDebt
import javax.inject.Inject
import kotlin.collections.set

const val KEY_ROW_IDS = "rowIds"

class AccountSealedException : IllegalStateException()

abstract class ContentResolvingAndroidViewModel(application: Application) :
    BaseViewModel(application) {

    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var licenceHandler: LicenceHandler

    val collate: String
        get() = prefHandler.collate

    private val bulkDeleteStateInternal: MutableStateFlow<DeleteState?> = MutableStateFlow(null)
    val bulkDeleteState: StateFlow<DeleteState?> = bulkDeleteStateInternal

    fun bulkDeleteCompleteShown() {
        bulkDeleteStateInternal.update {
            null
        }
    }

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

    val colorSource: Flow<ColorSource> by lazy {
        dataStore.data.map {
            enumValueOrDefault(
                it[prefHandler.getStringPreferencesKey(PrefKey.TRANSACTION_AMOUNT_COLOR_SOURCE)],
                ColorSource.TYPE
            )
        }
    }

    val dateInfo: Flow<DateInfo> = flow {
        emit(DateInfo.load(contentResolver))
    }.flowOn(Dispatchers.IO)

    fun accountsMinimal(query: String? = null, withAggregates: Boolean = true): Flow<List<AccountMinimal>> = contentResolver.observeQuery(
        if (withAggregates) ACCOUNTS_MINIMAL_URI_WITH_AGGREGATES else ACCOUNTS_MINIMAL_URI, null,
        query,
        null, null, false
    )
        .mapToList { cursor ->
            val id = cursor.getLong(KEY_ROWID)
            AccountMinimal(
                id,
                if (id == HOME_AGGREGATE_ID)
                    getString(R.string.grand_total)
                else
                    cursor.getString(KEY_LABEL),
                cursor.getString(KEY_CURRENCY),
                if (id < 0) null else AccountType.valueOf(cursor.getString(KEY_TYPE))
            )
        }

    fun account(accountId: Long): Flow<Account> = if (accountId > 0)
        repository.loadAccountFlow(accountId)
    else
        repository.loadAggregateAccountFlow(accountId)

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
                    Template.delete(contentResolver, it, deletePlan)
                    success++
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(contentResolver, e)
                    failure++
                }
            }
            emit(DeleteState.DeleteComplete(success, failure))
        }


    open fun deleteTransactions(ids: LongArray, markAsVoid: Boolean = false) {
        viewModelScope.launch(context = coroutineContext()) {
            var success = 0
            var failure = 0
            ids.forEach {
                try {
                    if (repository.deleteTransaction(it, markAsVoid, true))
                        success++ else failure++
                } catch (e: SQLiteConstraintException) {
                    CrashHandler.reportWithDbSchema(contentResolver, e)
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
            Result.failure(AccountSealedException())
        } else {
            val failures = mutableListOf<Exception>()
            for (accountId in accountIds) {
                try {
                    repository.deleteAccount(accountId)?.let {
                        val accountManager = AccountManager.get(getApplication())
                        val syncAccount = GenericAccountService.getAccount(it)
                        accountManager.setUserData(
                            syncAccount,
                            SyncAdapter.KEY_LAST_SYNCED_LOCAL(accountId), null
                        )
                        accountManager.setUserData(
                            syncAccount,
                            SyncAdapter.KEY_LAST_SYNCED_REMOTE(accountId), null
                        )
                    }
                } catch (e: Exception) {
                    CrashHandler.report(e)
                    failures.add(e)
                }
            }
            licenceHandler.updateNewAccountEnabled()
            updateTransferShortcut()
            if (failures.isEmpty())
                ResultUnit
            else
                Result.failure(Exception("${failures.size} exceptions occurred. "))
        }

    open fun updateTransferShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutHelper.configureTransferShortcut(
                getApplication(),
                isTransferEnabled
            )
        }
    }

    val isTransferEnabled
        get() = repository.countAccounts() > 1

    /**
     * @param rowId For split transactions, we check if any of their children is linked to a debt,
     * in which case the parent should not be linkable to a debt, and we return an empty list
     */
    fun loadDebts(
        rowId: Long? = null,
        showSealed: Boolean = false,
        showZero: Boolean = true,
        sortOrder: String? = null
    ) =
        contentResolver.observeQuery(
            uri = with(DEBTS_URI.buildUpon()) {
                rowId?.let {
                    appendQueryParameter(KEY_TRANSACTIONID, rowId.toString())
                }
                build()
            },
            selection = buildList {
                if (!showSealed) add("$KEY_SEALED = 0")
                if (!showZero) add("$KEY_AMOUNT-$KEY_SUM != 0")
            }.joinToString(separator = " AND "),
            sortOrder = sortOrder,
            notifyForDescendants = true
        )
            .mapToList { DisplayDebt.fromCursor(it, currencyContext) }

    /**
     * deletes all expenses and updates account according to value of handleDelete
     *
     * @param filter        if not null only expenses matched by filter will be deleted
     * @param handleDelete  if equals [EXPORT_HANDLE_DELETED_UPDATE_BALANCE] opening balance will
     * be adjusted to account for the deleted expenses,
     * if equals [EXPORT_HANDLE_DELETED_CREATE_HELPER] a helper transaction
     * @param helperComment comment used for the helper transaction
     */
    fun reset(
        account: Account,
        filter: Criterion?,
        handleDelete: Int,
        helperComment: String?
    ) {
        val ops = ArrayList<ContentProviderOperation>()
        var handleDeleteOperation: ContentProviderOperation? = null
        val sum = repository.getTransactionSum(account, filter)
        if (handleDelete == EXPORT_HANDLE_DELETED_UPDATE_BALANCE) {
            val currentBalance: Long = account.openingBalance + sum
            handleDeleteOperation = ContentProviderOperation.newUpdate(
                ACCOUNTS_URI.buildUpon().appendPath(account.id.toString()).build()
            )
                .withValue(KEY_OPENING_BALANCE, currentBalance)
                .build()
        } else if (handleDelete == EXPORT_HANDLE_DELETED_CREATE_HELPER) {
            val helper = Transaction(account.id, Money(currencyContext[account.currency], sum))
            helper.comment = helperComment
            helper.status = STATUS_HELPER
            handleDeleteOperation = ContentProviderOperation.newInsert(Transaction.CONTENT_URI)
                .withValues(helper.buildInitialValues(contentResolver)).build()
        }
        val rowSelect = buildTransactionRowSelect(filter)
        var selectionArgs: Array<String>? = arrayOf(account.id.toString())
        if (filter != null) {
            selectionArgs = joinArrays(selectionArgs, filter.getSelectionArgs(false))
        }
        updateTransferPeersForTransactionDelete(ops, rowSelect, selectionArgs)
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

    fun childCount(transactionId : Long) = repository.count(TRANSACTIONS_URI,
        "$KEY_PARENTID = ?", arrayOf(transactionId.toString())
    )

    fun joinQueryAndAccountFilter(
        filter: String?,
        accountId: Long?,
        filterColumn: String,
        linkColumn: String,
        tableName: String,
    ): Pair<String?, Array<String>?> {
        val filterSelection =
            if (TextUtils.isEmpty(filter)) null else "$filterColumn LIKE ?"
        val filterSelectionArgs: Array<String>? = if (TextUtils.isEmpty(filter)) null else
            arrayOf("%${Utils.escapeSqlLikeExpression(Utils.normalize(filter))}%")
        val accountSelection = if (accountId == null) null else
            StringBuilder("exists (SELECT 1 from $TABLE_TRANSACTIONS WHERE $linkColumn = $tableName.$KEY_ROWID").apply {
                SelectFromMappedTableDialogFragment.accountSelection(accountId)?.let {
                    append(" AND ")
                    append(it)
                }
                append(")")
            }
        val accountSelectionArgs: Array<String>? =
            if (accountId == null) null else SelectFromMappedTableDialogFragment.accountSelectionArgs(
                accountId
            )

        val selection = buildString {
            filterSelection?.let { append(it) }
            accountSelection?.let {
                if (isNotEmpty()) append(" AND ")
                append(it)
            }
        }.takeIf { it.isNotEmpty() }
        return selection to joinArrays(filterSelectionArgs, accountSelectionArgs)
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