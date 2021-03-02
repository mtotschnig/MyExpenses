package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import io.reactivex.disposables.CompositeDisposable
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Plan.CalendarIntegrationNotAvailableException
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transaction.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.model.Transaction.UnknownPictureSaveException
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_ACCOUNTY_TYPE_LIST
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.util.*
import kotlin.math.pow
import org.totschnig.myexpenses.viewmodel.data.Template as DataTemplate

const val ERROR_UNKNOWN = -1L
const val ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE = -2L
const val ERROR_PICTURE_SAVE_UNKNOWN = -3L
const val ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE = -4L
const val ERROR_WHILE_SAVING_TAGS = -5L

class TransactionEditViewModel(application: Application) : TransactionViewModel(application) {

    private val disposables = CompositeDisposable()
    //TODO move to lazyMap
    private val methods = MutableLiveData<List<PaymentMethod>>()

    private val accounts by lazy {
        val liveData = MutableLiveData<List<Account>>()
        disposables.add(briteContentResolver.createQuery(TransactionProvider.ACCOUNTS_BASE_URI, null, "$KEY_SEALED = 0", null, null, false)
                .mapToList { buildAccount(it, currencyContext) }
                .subscribe { liveData.postValue(it) })
        return@lazy liveData
    }

    private val templates by lazy {
        val liveData = MutableLiveData<List<DataTemplate>>()
        disposables.add(briteContentResolver.createQuery(TransactionProvider.TEMPLATES_URI.buildUpon()
                .build(), arrayOf(KEY_ROWID, KEY_TITLE),
                "$KEY_PLANID is null AND $KEY_PARENTID is null AND $KEY_SEALED = 0",
                null,
                Sort.preferredOrderByForTemplatesWithPlans(prefHandler, Sort.USAGES),
                false)
                .mapToList { DataTemplate.fromCursor(it) }
                .subscribe { liveData.postValue(it) }
        )
        return@lazy liveData
    }

    fun getMethods(): LiveData<List<PaymentMethod>> = methods

    fun getAccounts(): LiveData<List<Account>> = accounts

    fun getTemplates(): LiveData<List<DataTemplate>> = templates

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
        val result = try {
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
        }
        emit(if (result > 0 && !transaction.saveTags(tags.value, getApplication<Application>().contentResolver)) ERROR_WHILE_SAVING_TAGS else result)
    }

    fun cleanupSplit(id: Long, isTemplate: Boolean): LiveData<Unit> = liveData(context = coroutineContext()) {
        emit(
                if (isTemplate) Template.cleanupCanceledEdit(id) else SplitTransaction.cleanupCanceledEdit(id)
        )
    }

    private fun adjustExchangeRate(raw: Double, currencyUnit: CurrencyUnit): Double {
        val minorUnitDelta: Int = currencyUnit.fractionDigits - Utils.getHomeCurrency().fractionDigits
        return raw * 10.0.pow(minorUnitDelta.toDouble())
    }

    fun updateTags(it: MutableList<Tag>) {
        tags.postValue(it)
    }

    fun removeTag(tag: Tag) {
        tags.value?.remove(tag)
    }

    fun removeTags(tagIds: LongArray) {
        tags.value?.let { tags.postValue(it.filter { tag -> !tagIds.contains(tag.id) }.toMutableList()) }
    }

    fun newTemplate(operationType: Int, accountId: Long, parentId: Long?): LiveData<Template?> = liveData(context = coroutineContext()) {
        emit(Template.getTypedNewInstance(operationType, accountId, true, parentId))
    }

    fun newTransaction(accountId: Long, parentId: Long?): LiveData<Transaction?> = liveData(context = coroutineContext()) {
        emit(Transaction.getNewInstance(accountId, parentId))
    }

    fun newTransfer(accountId: Long, transferAccountId: Long?, parentId: Long?): LiveData<Transfer?> = liveData(context = coroutineContext()) {
        emit(Transfer.getNewInstance(accountId, transferAccountId, parentId))
    }

    fun newSplit(accountId: Long): LiveData<SplitTransaction?> = liveData(context = coroutineContext()) {
        emit(SplitTransaction.getNewInstance(accountId))
    }
}


