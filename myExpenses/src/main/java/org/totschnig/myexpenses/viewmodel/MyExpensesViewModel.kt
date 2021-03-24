package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.TransactionDatabase.SQLiteDowngradeFailedException
import org.totschnig.myexpenses.provider.TransactionDatabase.SQLiteUpgradeFailedException
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.ServiceLoader
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import javax.inject.Inject

const val ERROR_INIT_DOWNGRADE = -1
const val ERROR_INIT_UPGRADE = -2

class MyExpensesViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    @Inject
    lateinit var prefHandler: PrefHandler

    private val hasHiddenAccounts = MutableLiveData<Boolean>()

    fun getHasHiddenAccounts(): LiveData<Boolean> {
        return hasHiddenAccounts
    }

    init {
        (application as MyApplication).appComponent.inject(this)
    }

    fun initialize(): LiveData<Int> = liveData(context = coroutineContext()) {
        for (factory in ServiceLoader.load(getApplication())) {
            factory.init()
        }
        try {
            contentResolver.call(TransactionProvider.DUAL_URI, TransactionProvider.METHOD_INIT, null, null)
        } catch (e: SQLiteDowngradeFailedException) {
            CrashHandler.report(e)
            emit(ERROR_INIT_DOWNGRADE)
        } catch (e: SQLiteUpgradeFailedException) {
            CrashHandler.report(e)
            emit(ERROR_INIT_UPGRADE)
        }
        getApplication<MyApplication>().appComponent.licenceHandler().update()
        Account.updateTransferShortcut()
        emit(0)
    }

    fun loadHiddenAccountCount() {
        disposable = briteContentResolver.createQuery(TransactionProvider.ACCOUNTS_URI,
                arrayOf("count(*)"), "$KEY_HIDDEN = 1", null, null, false)
                .mapToOne { cursor -> cursor.getInt(0) > 0 }
                .subscribe { hasHiddenAccounts.postValue(it) }
    }

    fun persistGrouping(accountId: Long, grouping: Grouping) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (accountId == Account.HOME_AGGREGATE_ID) {
                    AggregateAccount.persistGroupingHomeAggregate(prefHandler, grouping)
                    contentResolver.notifyChange(TransactionProvider.ACCOUNTS_URI, null, false)
                } else {
                    contentResolver.update(ContentUris.withAppendedId(TransactionProvider.ACCOUNT_GROUPINGS_URI, accountId).buildUpon()
                            .appendPath(grouping.name).build(),
                            null, null, null)
                }
            }
        }
    }

    fun persistSortDirection(accountId: Long, sortDirection: SortDirection) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                contentResolver.update(ContentUris.withAppendedId(Account.CONTENT_URI, accountId).buildUpon()
                        .appendPath(TransactionProvider.URI_SEGMENT_SORT_DIRECTION)
                        .appendPath(sortDirection.name).build(),
                        null, null, null)
            }
        }
    }

    fun persistSortDirectionAggregate(currency: String, sortDirection: SortDirection) {
        AggregateAccount.persistSortDirectionAggregate(prefHandler, currency, sortDirection)
        contentResolver.notifyChange(TransactionProvider.ACCOUNTS_URI, null, false)
    }

    fun persistSortDirectionHomeAggregate(sortDirection: SortDirection) {
        AggregateAccount.persistSortDirectionHomeAggregate(prefHandler, sortDirection)
        contentResolver.notifyChange(TransactionProvider.ACCOUNTS_URI, null, false)
    }

    fun linkTransfer(itemIds: LongArray) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                contentResolver.update(TransactionProvider.TRANSACTIONS_URI.buildUpon()
                        .appendPath(TransactionProvider.URI_SEGMENT_LINK_TRANSFER)
                        .appendPath(repository.getUuidForTransaction(itemIds[0]))
                        .build(), ContentValues(1).apply {
                    put(KEY_UUID, repository.getUuidForTransaction(itemIds[1]))
                }, null, null)
            }
        }
    }
}
