package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getStringOrNull
import androidx.lifecycle.*
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.exception.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.exception.UnknownPictureSaveException
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Plan.CalendarIntegrationNotAvailableException
import org.totschnig.myexpenses.provider.*
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_ACCOUNTY_TYPE_LIST
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import kotlin.collections.set
import kotlin.math.pow
import org.totschnig.myexpenses.model.Account as Account_model
import org.totschnig.myexpenses.viewmodel.data.Template as DataTemplate

const val ERROR_UNKNOWN = -1L
const val ERROR_EXTERNAL_STORAGE_NOT_AVAILABLE = -2L
const val ERROR_PICTURE_SAVE_UNKNOWN = -3L
const val ERROR_CALENDAR_INTEGRATION_NOT_AVAILABLE = -4L
const val ERROR_WHILE_SAVING_TAGS = -5L

class TransactionEditViewModel(application: Application) : TransactionViewModel(application) {

    private val disposables = CompositeDisposable()

    private val splitParts = MutableLiveData<List<SplitPart>>()
    private var loadJob: Job? = null
    fun getSplitParts(): LiveData<List<SplitPart>> = splitParts

    private val _moveResult: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val moveResult: StateFlow<Boolean?> = _moveResult

    //TODO move to lazyMap
    private val methods = MutableLiveData<List<PaymentMethod>>()

    private val accounts by lazy {
        val liveData = MutableLiveData<List<Account>>()
        disposables.add(briteContentResolver.createQuery(
            TransactionProvider.ACCOUNTS_BASE_URI,
            null,
            "$KEY_SEALED = 0",
            null,
            null,
            false
        )
            .mapToList {
                buildAccount(it, currencyContext)
            }
            .subscribe {
                liveData.postValue(it)
            }
        )
        return@lazy liveData
    }

    private val templates by lazy {
        val liveData = MutableLiveData<List<DataTemplate>>()
        disposables.add(briteContentResolver.createQuery(
            TransactionProvider.TEMPLATES_URI.buildUpon()
                .build(), arrayOf(KEY_ROWID, KEY_TITLE),
            "$KEY_PLANID is null AND $KEY_PARENTID is null AND $KEY_SEALED = 0",
            null,
            Sort.preferredOrderByForTemplatesWithPlans(prefHandler, Sort.USAGES),
            false
        )
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
        disposables.add(briteContentResolver.createQuery(
            TransactionProvider.METHODS_URI.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_TYPE_FILTER)
                .appendPath(if (isIncome) "1" else "-1")
                .appendQueryParameter(QUERY_PARAMETER_ACCOUNTY_TYPE_LIST, type.name)
                .build(), null, null, null, null, false
        )
            .mapToList { PaymentMethod.create(it) }
            .subscribe { methods.postValue(it) }
        )
    }

    private fun buildAccount(cursor: Cursor, currencyContext: CurrencyContext): Account {
        val currency =
            currencyContext.get(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CURRENCY)))
        return Account(
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID)),
            cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL)),
            currency,
            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COLOR)),
            AccountType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TYPE))),
            adjustExchangeRate(
                cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_EXCHANGE_RATE)),
                currency
            )
        )
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
        emit(if (result > 0 && !transaction.saveTags(tags.value)) ERROR_WHILE_SAVING_TAGS else result)
    }

    fun cleanupSplit(id: Long, isTemplate: Boolean): LiveData<Unit> =
        liveData(context = coroutineContext()) {
            emit(
                if (isTemplate) Template.cleanupCanceledEdit(id) else SplitTransaction.cleanupCanceledEdit(
                    id
                )
            )
        }

    private fun adjustExchangeRate(raw: Double, currencyUnit: CurrencyUnit): Double {
        val minorUnitDelta: Int =
            currencyUnit.fractionDigits - Utils.getHomeCurrency().fractionDigits
        return raw * 10.0.pow(minorUnitDelta.toDouble())
    }

    fun loadActiveTags(id: Long) = viewModelScope.launch(coroutineContext()) {
        if (!userHasUpdatedTags) {
            Account_model.loadTags(id)?.let { updateTags(it, false) }
        }
    }

    fun newTemplate(operationType: Int, accountId: Long, parentId: Long?): LiveData<Template?> =
        liveData(context = coroutineContext()) {
            emit(Template.getTypedNewInstance(operationType, accountId, true, parentId))
        }

    fun newTransaction(accountId: Long, parentId: Long?): LiveData<Transaction?> =
        liveData(context = coroutineContext()) {
            emit(Transaction.getNewInstance(accountId, parentId))
        }

    fun newTransfer(
        accountId: Long,
        transferAccountId: Long?,
        parentId: Long?
    ): LiveData<Transfer?> = liveData(context = coroutineContext()) {
        emit(Transfer.getNewInstance(accountId, transferAccountId, parentId))
    }

    fun newSplit(accountId: Long): LiveData<SplitTransaction?> =
        liveData(context = coroutineContext()) {
            emit(SplitTransaction.getNewInstance(accountId))
        }

    fun loadSplitParts(parentId: Long, parentIsTemplate: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            contentResolver.observeQuery(
                uri = if (parentIsTemplate) TransactionProvider.TEMPLATES_UNCOMMITTED_URI
                else TransactionProvider.UNCOMMITTED_URI,
                projection = arrayOf(
                    KEY_ROWID,
                    KEY_AMOUNT,
                    KEY_COMMENT,
                    FULL_LABEL,
                    KEY_TRANSFER_ACCOUNT,
                    if (parentIsTemplate) "null" else BaseTransactionProvider.DEBT_LABEL_EXPRESSION,
                    KEY_TAGLIST
                ),
                selection = "$KEY_PARENTID = ?",
                selectionArgs = arrayOf(parentId.toString())
            ).cancellable().mapToList {
                SplitPart.fromCursor(it)
            }.collect { splitParts.postValue(it) }
        }
    }

    fun moveUnCommittedSplitParts(transactionId: Long, accountId: Long, isTemplate: Boolean) {
        _moveResult.update {
            contentResolver.query(
                if (isTemplate)  TransactionProvider.TEMPLATES_UNCOMMITTED_URI else TransactionProvider.UNCOMMITTED_URI,
                arrayOf("count(*)"),
                "$KEY_PARENTID = ? AND $KEY_TRANSFER_ACCOUNT  = ?",
                arrayOf(transactionId.toString(), accountId.toString()),
                null
            )?.use {
                if (it.moveToFirst() && it.getInt(0) == 0) {
                    val values = ContentValues()
                    values.put(KEY_ACCOUNTID, accountId)
                    contentResolver.update(
                        if (isTemplate) TransactionProvider.TEMPLATES_URI else TransactionProvider.TRANSACTIONS_URI,
                        values,
                        "$KEY_PARENTID = ? AND $KEY_STATUS = $STATUS_UNCOMMITTED",
                        arrayOf(transactionId.toString())
                    )
                    true
                } else false
            } ?: false
        }
    }

    fun moveResultProcessed() {
        _moveResult.update {
            null
        }
    }

    data class SplitPart(
        override val id: Long,
        override val amountRaw: Long,
        override val comment: String?,
        override val label: String?,
        override val isTransfer: Boolean,
        override val debtLabel: String?,
        override val tagList: String?
    ) : SplitPartRVAdapter.ITransaction {
        companion object {
            fun fromCursor(cursor: Cursor) =
                SplitPart(
                    cursor.getLong(0),
                    cursor.getLong(1),
                    cursor.getStringOrNull(2),
                    cursor.getStringOrNull(3),
                    DbUtils.getLongOrNull(cursor, 4) != null,
                    cursor.getStringOrNull(5),
                    cursor.getString(6)
                )
        }
    }
}


