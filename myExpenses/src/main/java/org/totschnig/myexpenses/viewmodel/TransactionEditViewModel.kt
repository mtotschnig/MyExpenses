package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import app.cash.copper.flow.mapToList
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.exception.ExternalStorageNotAvailableException
import org.totschnig.myexpenses.exception.UnknownPictureSaveException
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.model.Plan.CalendarIntegrationNotAvailableException
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.KEY_DEBT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.*
import org.totschnig.myexpenses.provider.FULL_LABEL
import org.totschnig.myexpenses.provider.ProviderUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_ACCOUNTY_TYPE_LIST
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getStringIfExists
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.splitStringList
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

class TransactionEditViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    TagHandlingViewModel(application, savedStateHandle) {

    private val splitPartLoader = MutableStateFlow<Pair<Long, Boolean>?>(null)

    private var loadMethodJob: Job? = null

    private val _moveResult: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val moveResult: StateFlow<Boolean?> = _moveResult

    private val methods = MutableLiveData<List<PaymentMethod>>()
    fun getMethods(): LiveData<List<PaymentMethod>> = methods

    val accounts: Flow<List<Account>>
        get() = contentResolver.observeQuery(
            uri = TransactionProvider.ACCOUNTS_BASE_URI,
            projection = null,
            selection = "$KEY_SEALED = 0",
            selectionArgs = null,
            sortOrder = null,
            notifyForDescendants = false
        ).mapToList {
            buildAccount(it, currencyContext)
        }

    val templates: Flow<List<DataTemplate>>
        get() = contentResolver.observeQuery(
            uri = TransactionProvider.TEMPLATES_URI.buildUpon()
                .build(), projection = arrayOf(KEY_ROWID, KEY_TITLE),
            selection = "$KEY_PLANID is null AND $KEY_PARENTID is null AND $KEY_SEALED = 0",
            selectionArgs = null,
            sortOrder = Sort.preferredOrderByForTemplatesWithPlans(
                prefHandler,
                Sort.USAGES,
                collate
            ),
            notifyForDescendants = false
        ).mapToList { DataTemplate.fromCursor(it) }


    fun plan(planId: Long): LiveData<Plan?> = liveData(context = coroutineContext()) {
        emit(Plan.getInstanceFromDb(planId))
    }

    fun loadMethods(isIncome: Boolean, type: AccountType) {
        loadMethodJob?.cancel()
        viewModelScope.launch {
            contentResolver.observeQuery(
                TransactionProvider.METHODS_URI.buildUpon()
                    .appendPath(TransactionProvider.URI_SEGMENT_TYPE_FILTER)
                    .appendPath(if (isIncome) "1" else "-1")
                    .appendQueryParameter(QUERY_PARAMETER_ACCOUNTY_TYPE_LIST, type.name)
                    .build(), null, null, null, null, false
            )
                .mapToList { PaymentMethod.create(it) }
                .collect { methods.postValue(it) }
        }
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
        emit(if (result > 0 && !transaction.saveTags(tagsLiveData.value)) ERROR_WHILE_SAVING_TAGS else result)
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
            Account_model.loadTags(id, contentResolver)?.let { updateTags(it, false) }
        }
    }

    fun newTemplate(operationType: Int, accountId: Long, parentId: Long?): LiveData<Template?> =
        liveData(context = coroutineContext()) {
            emit(
                fallbackToLastUsed(accountId)?.let {
                    Template.getTypedNewInstance(operationType, it.first, it.second, true, parentId)
                }
            )
        }

    fun newTransaction(accountId: Long, parentId: Long?): LiveData<Transaction?> =
        liveData(context = coroutineContext()) {
            emit(
                fallbackToLastUsed(accountId)?.let {
                    Transaction.getNewInstance(it.first, it.second, parentId)
                }
            )
        }

    fun newTransfer(
        accountId: Long,
        transferAccountId: Long?,
        parentId: Long?
    ): LiveData<Transfer?> = liveData(context = coroutineContext()) {
        emit(
            fallbackToLastUsed(accountId)?.let {
                Transfer.getNewInstance(it.first, it.second, transferAccountId, parentId)
            }
        )
    }

    fun newSplit(accountId: Long): LiveData<SplitTransaction?> =
        liveData(context = coroutineContext()) {
            emit(
                fallbackToLastUsed(accountId)?.let {
                    SplitTransaction.getNewInstance(it.first, it.second, true)
                }
            )
        }

    private fun fallbackToLastUsed(accountId: Long) =
        accountId.takeIf { it != 0L }?.let { account ->
            repository.getCurrencyUnitForAccount(account)
                ?.let { currencyUnit -> account to currencyUnit }
        } ?: repository.getLastUsedOpenAccount()

    fun loadSplitParts(parentId: Long, parentIsTemplate: Boolean) {
        splitPartLoader.tryEmit(parentId to parentIsTemplate)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val splitParts = splitPartLoader.filterNotNull().flatMapLatest {
        val (parentId, parentIsTemplate) = it
            contentResolver.observeQuery(
                uri = if (parentIsTemplate) TransactionProvider.TEMPLATES_UNCOMMITTED_URI
                else TransactionProvider.UNCOMMITTED_URI,
                projection = listOfNotNull(
                    KEY_ROWID,
                    KEY_AMOUNT,
                    KEY_COMMENT,
                    FULL_LABEL,
                    KEY_TRANSFER_ACCOUNT,
                    if (parentIsTemplate) null else BaseTransactionProvider.DEBT_LABEL_EXPRESSION,
                    KEY_TAGLIST,
                    KEY_ICON
                ).toTypedArray(),
                selection = "$KEY_PARENTID = ?",
                selectionArgs = arrayOf(parentId.toString())
            ).mapToList { cursor ->
                SplitPart.fromCursor(cursor)
            }
        }

    fun moveUnCommittedSplitParts(transactionId: Long, accountId: Long, isTemplate: Boolean) {
        _moveResult.update {
            contentResolver.query(
                if (isTemplate) TransactionProvider.TEMPLATES_UNCOMMITTED_URI else TransactionProvider.UNCOMMITTED_URI,
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

    fun transaction(
        transactionId: Long,
        task: InstantiationTask,
        clone: Boolean,
        forEdit: Boolean,
        extras: Bundle?
    ): LiveData<Transaction?> = liveData(context = coroutineContext()) {
        when (task) {
            InstantiationTask.TEMPLATE -> Template.getInstanceFromDbWithTags(transactionId)
            InstantiationTask.TRANSACTION_FROM_TEMPLATE -> Transaction.getInstanceFromTemplateWithTags(
                transactionId
            )
            InstantiationTask.TRANSACTION -> Transaction.getInstanceFromDbWithTags(transactionId)
            InstantiationTask.FROM_INTENT_EXTRAS -> Pair(
                ProviderUtils.buildFromExtras(
                    repository,
                    extras!!
                ), emptyList()
            )
            InstantiationTask.TEMPLATE_FROM_TRANSACTION -> with(
                Transaction.getInstanceFromDb(
                    transactionId
                )
            ) {
                Pair(Template(this, payee ?: label), this.loadTags())
            }
        }?.also { pair ->
            if (forEdit) {
                pair.first.prepareForEdit(
                    clone,
                    clone && prefHandler.getBoolean(PrefKey.CLONE_WITH_CURRENT_DATE, true)
                )
            }
            emit(pair.first)
            pair.second?.takeIf { it.size > 0 }?.let { updateTags(it, false) }
        } ?: run {
            emit(null)
        }
    }

    data class SplitPart(
        override val id: Long,
        override val amountRaw: Long,
        override val comment: String?,
        override val label: String?,
        override val isTransfer: Boolean,
        override val debtLabel: String?,
        override val tagList: String?,
        override val icon: String?
    ) : SplitPartRVAdapter.ITransaction {
        companion object {
            fun fromCursor(cursor: Cursor) =
                SplitPart(
                    cursor.getLong(KEY_ROWID),
                    cursor.getLong(KEY_AMOUNT),
                    cursor.getStringOrNull(KEY_COMMENT),
                    cursor.getStringOrNull(KEY_LABEL),
                    cursor.getLongOrNull(KEY_TRANSFER_ACCOUNT) != null,
                    cursor.getStringIfExists(KEY_DEBT_LABEL),
                    cursor.splitStringList(KEY_TAGLIST).joinToString(),
                    cursor.getStringOrNull(KEY_ICON)
                )
        }
    }

    enum class InstantiationTask { TRANSACTION, TEMPLATE, TRANSACTION_FROM_TEMPLATE, FROM_INTENT_EXTRAS, TEMPLATE_FROM_TRANSACTION }
}


