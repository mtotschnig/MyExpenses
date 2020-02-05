package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import io.reactivex.disposables.CompositeDisposable
import org.totschnig.myexpenses.adapter.IAccount
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Plan.CalendarIntegrationNotAvailableException
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.model.Transaction.UnknownPictureSaveException
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_ACCOUNTY_TYPE_LIST
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import java.io.Serializable
import java.util.*

const val ERROR_UNKNOWN = -1L
const val ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE = -2L
const val ERROR_PICTURE_SAVE_UNKNOWN = -3L
const val ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE = -4L

class TransactionEditViewModel(application: Application) : TransactionViewModel(application) {

    val disposables = CompositeDisposable()
    private val methods = MutableLiveData<List<PaymentMethod>>()
    private val accounts = MutableLiveData<List<Account>>()

    fun getMethods(): LiveData<List<PaymentMethod>> {
        return methods
    }

    fun getAccounts(): LiveData<List<Account>> {
        return accounts
    }

    fun plan(planId: Long): LiveData<Plan?> = liveData(context = coroutineContext()) {
        emit(Plan.getInstanceFromDb(planId))
    }

    fun loadMethods(isIncome: Boolean, type: AccountType) {
        disposables.add(briteContentResolver.createQuery(TransactionProvider.METHODS_URI.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_TYPE_FILTER)
                .appendPath(if (isIncome) "1" else "-1")
                .appendQueryParameter(QUERY_PARAMETER_ACCOUNTY_TYPE_LIST, type.name)
                .build(), null, null, null, null, false)
                .mapToList { PaymentMethod.create(it) }
                .subscribe { methods.postValue(it) }
        )
    }

    fun loadAccounts(currencyContext: CurrencyContext) {
        disposables.add(briteContentResolver.createQuery(TransactionProvider.ACCOUNTS_BASE_URI, null, DatabaseConstants.KEY_SEALED + " = 0", null, null, false)
                .mapToList { buildAccount(it, currencyContext) }
                .subscribe { accounts.postValue(it) })
    }

    private fun buildAccount(cursor: Cursor, currencyContext: CurrencyContext): Account {
        val currency = currencyContext.get(cursor.getString(cursor.getColumnIndex(KEY_CURRENCY)))
        return Account(
                cursor.getLong(cursor.getColumnIndex(KEY_ROWID)),
                cursor.getString(cursor.getColumnIndex(KEY_LABEL)),
                currency,
                cursor.getInt(cursor.getColumnIndex(KEY_COLOR)),
                AccountType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE))),
                adjustExchangeRate(cursor.getDouble(cursor.getColumnIndex(KEY_EXCHANGE_RATE)), currency))
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun save(transaction: ITransaction): LiveData<Long> = liveData(context = coroutineContext()) {
        emit(
                try {
                    transaction.save(true)?.let { ContentUris.parseId(it) } ?: ERROR_UNKNOWN
                } catch (e: ExternalStorageNotAvailableException) {
                    ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE
                } catch (e: UnknownPictureSaveException) {
                    val customData = HashMap<String, String>()
                    customData["pictureUri"] = e.pictureUri.toString()
                    customData["homeUri"] = e.homeUri.toString()
                    CrashHandler.report(e, customData)
                    ERROR_PICTURE_SAVE_UNKNOWN
                } catch (e: CalendarIntegrationNotAvailableException) {
                    ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE
                } catch (e: Exception) {
                    CrashHandler.report(e)
                    ERROR_UNKNOWN
                })
    }

    fun cleanupSplit(id: Long, isTemplate: Boolean): LiveData<Unit> = liveData(context = coroutineContext()) {
        emit(
                if (isTemplate) Template.cleanupCanceledEdit(id) else SplitTransaction.cleanupCanceledEdit(id)
        )
    }

    data class Account(override val id: Long, val label: String, val currency: CurrencyUnit, val color: Int, val type: AccountType, val exchangeRate: Double) : IAccount, Serializable {
        override fun toString(): String {
            return label
        }
    }

    private fun adjustExchangeRate(raw: Double, currencyUnit: CurrencyUnit): Double {
        val minorUnitDelta: Int = currencyUnit.fractionDigits() - Utils.getHomeCurrency().fractionDigits()
        return raw * Math.pow(10.0, minorUnitDelta.toDouble())
    }
}


