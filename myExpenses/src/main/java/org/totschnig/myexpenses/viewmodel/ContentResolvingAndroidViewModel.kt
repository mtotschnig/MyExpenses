package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.squareup.sqlbrite3.BriteContentResolver
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineDispatcher
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.Account.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.ui.ContextHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val KEY_ROW_IDS = "rowIds"

abstract class ContentResolvingAndroidViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var briteContentResolver: BriteContentResolver

    @Inject
    lateinit var coroutineDispatcher: CoroutineDispatcher

    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var prefHandler: PrefHandler

    var disposable: Disposable? = null

    val contentResolver: ContentResolver
        get() = getApplication<MyApplication>().contentResolver

    private var accountDisposable: Disposable? = null

    val localizedContext: Context
        get() = with(getApplication<MyApplication>()) {
            ContextHelper.wrap(this, appComponent.userLocaleProvider().getUserPreferredLocale())
        }

    val accountsMinimal: LiveData<List<AccountMinimal>> by lazy {
        val liveData = MutableLiveData<List<AccountMinimal>>()
        disposable = briteContentResolver.createQuery(TransactionProvider.ACCOUNTS_MINIMAL_URI, null, null, null, null, false)
                .mapToList { cursor ->
                    val id = cursor.getLong(cursor.getColumnIndex(KEY_ROWID))
                    AccountMinimal(id,
                            if (id == HOME_AGGREGATE_ID)
                                getApplication<MyApplication>().getString(R.string.grand_total)
                            else
                                cursor.getString(cursor.getColumnIndex(KEY_LABEL)),
                            cursor.getString(cursor.getColumnIndex(KEY_CURRENCY)))
                }
                .subscribe {
                    liveData.postValue(it)
                    dispose()
                }
        return@lazy liveData
    }

    private val accountLiveData: Map<Long, LiveData<Account>> = lazyMap { accountId ->
        val liveData = MutableLiveData<Account>()
        disposeAccount()
        val base = if (accountId > 0) TransactionProvider.ACCOUNTS_URI else TransactionProvider.ACCOUNTS_AGGREGATE_URI
        accountDisposable = briteContentResolver.createQuery(ContentUris.withAppendedId(base, accountId),
                Account.PROJECTION_BASE, null, null, null, true)
                .mapToOne { Account.fromCursor(it) }
                .throttleFirst(100, TimeUnit.MILLISECONDS)
                .subscribe {
                    liveData.postValue(it)
                    onAccountLoaded(it)
                }
        return@lazyMap liveData
    }

    fun account(accountId: Long): LiveData<Account> = accountLiveData.getValue(accountId)

    open fun onAccountLoaded(account: Account) {}
    open fun onAccountsLoaded() {}

    override fun onCleared() {
        dispose()
        disposeAccount()
    }

    private fun disposeAccount() {
        accountDisposable?.let {
            if (!it.isDisposed) it.dispose()
        }
    }

    fun dispose() {
        disposable?.let {
            if (!it.isDisposed) it.dispose()
        }
    }

    protected fun coroutineContext() = viewModelScope.coroutineContext + coroutineDispatcher

    fun deleteTemplates(ids: LongArray, deletePlan: Boolean): LiveData<Int> = liveData(context = coroutineContext()) {
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


    fun deleteTransactions(ids: LongArray, markAsVoid: Boolean): LiveData<Int> = liveData(context = coroutineContext()) {
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