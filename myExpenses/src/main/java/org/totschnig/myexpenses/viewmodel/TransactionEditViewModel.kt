package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutManagerCompat.FLAG_MATCH_PINNED
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
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.db2.addAttachments
import org.totschnig.myexpenses.db2.deleteAttachments
import org.totschnig.myexpenses.db2.getCategoryPath
import org.totschnig.myexpenses.db2.getCurrencyUnitForAccount
import org.totschnig.myexpenses.db2.getLastUsedOpenAccount
import org.totschnig.myexpenses.db2.loadActiveTagsForAccount
import org.totschnig.myexpenses.db2.loadAttachments
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.db2.savePrice
import org.totschnig.myexpenses.exception.UnknownPictureSaveException
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.ITransaction
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Sort
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Template
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants.CATEGORY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.CAT_AS_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENT_BALANCE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DYNAMIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PATH
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED
import org.totschnig.myexpenses.provider.PlannerUtils
import org.totschnig.myexpenses.provider.ProviderUtils
import org.totschnig.myexpenses.provider.TRANSFER_ACCOUNT_LABEL
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_FULL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_ACCOUNT_TYPE_LIST
import org.totschnig.myexpenses.provider.fileName
import org.totschnig.myexpenses.provider.filter.KEY_CRITERION
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongIfExists
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringIfExists
import org.totschnig.myexpenses.provider.isDebugAsset
import org.totschnig.myexpenses.retrofit.ExchangeRateSource
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.util.ImageOptimizer
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.asExtension
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.io.getFileExtension
import org.totschnig.myexpenses.util.io.getNameWithoutExtension
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import org.totschnig.myexpenses.viewmodel.data.SplitPart
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import org.totschnig.myexpenses.viewmodel.data.Template as DataTemplate

class TransactionEditViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    TagHandlingViewModel(application, savedStateHandle) {

    @Inject
    lateinit var plannerUtils: PlannerUtils

    private val splitPartLoader = MutableStateFlow<Pair<Long, Boolean>?>(null)

    private var loadMethodJob: Job? = null

    private val _moveResult: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val moveResult: StateFlow<Boolean?> = _moveResult

    private val _autoFillData: MutableStateFlow<AutoFillData?> = MutableStateFlow(null)
    val autoFillData: StateFlow<AutoFillData?> = _autoFillData

    private val methods = MutableLiveData<List<PaymentMethod>>()
    fun getMethods(): LiveData<List<PaymentMethod>> = methods

    val accounts: Flow<List<Account>>
        get() = contentResolver.observeQuery(
            uri = ACCOUNTS_FULL_URI,
            selection = "$KEY_SEALED = 0"
        ).mapToList {
            buildAccount(it, currencyContext)
        }

    val templates: Flow<List<DataTemplate>>
        get() = contentResolver.observeQuery(
            uri = TransactionProvider.TEMPLATES_URI.buildUpon()
                .build(), projection = arrayOf(KEY_ROWID, KEY_TITLE),
            selection = "$KEY_PLANID is null AND $KEY_PARENTID is null AND $KEY_SEALED = 0",
            sortOrder = Sort.preferredOrderByForTemplatesWithPlans(
                prefHandler,
                Sort.USAGES,
                collate
            )
        ).mapToList { DataTemplate.fromCursor(it) }


    fun plan(planId: Long): LiveData<Plan?> = liveData(context = coroutineContext()) {
        emit(Plan.getInstanceFromDb(contentResolver, planId))
    }

    fun loadMethods(isIncome: Boolean, type: AccountType) {
        loadMethodJob?.cancel()
        loadMethodJob = viewModelScope.launch {
            contentResolver.observeQuery(
                TransactionProvider.METHODS_URI.buildUpon()
                    .appendPath(TransactionProvider.URI_SEGMENT_TYPE_FILTER)
                    .appendPath(if (isIncome) "1" else "-1")
                    .appendQueryParameter(QUERY_PARAMETER_ACCOUNT_TYPE_LIST, type.id.toString())
                    .build(), null, null, null, null, false
            )
                .mapToList { PaymentMethod.create(it) }
                .collect {
                    methods.postValue(it)
                }
        }
    }

    private fun buildAccount(cursor: Cursor, currencyContext: CurrencyContext): Account {
        val currency =
            currencyContext[cursor.getString(KEY_CURRENCY)]
        return Account(
            id = cursor.getLong(KEY_ROWID),
            label = cursor.getString(KEY_LABEL),
            currency,
            color = cursor.getInt(KEY_COLOR),
            type = AccountType.fromAccountCursor(cursor),
            criterion = cursor.getLongOrNull(KEY_CRITERION),
            isDynamic = cursor.getBoolean(KEY_DYNAMIC),
            flag = AccountFlag.fromAccountCursor(cursor),
            currentBalance = cursor.getLong(KEY_CURRENT_BALANCE)
        )
    }

    fun save(transaction: ITransaction, userSetExchangeRate: BigDecimal?): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            emit(runCatching {
                transaction.party?.let {
                    if (it.id == null) {
                        transaction.party = DisplayParty(repository.requireParty(it.name), it.name)
                    }
                }

                transaction.save(contentResolver, plannerUtils, true) ?: throw Throwable("Error while saving transaction")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && transaction is Template && transaction.id != 0L) {
                    if (
                        ShortcutManagerCompat.getShortcuts(getApplication(), FLAG_MATCH_PINNED)
                            .any {
                                it.id == ShortcutHelper.idTemplate(transaction.id)
                            }
                    ) {
                        ShortcutManagerCompat.updateShortcuts(
                            getApplication(),
                            listOf(
                                ShortcutHelper.buildTemplateShortcut(
                                    getApplication(),
                                    TemplateInfo.fromTemplate(transaction)
                                )
                            )
                        )
                    }
                }
                tagsLiveData.value?.let { transaction.saveTags(contentResolver, it) }
                (originalUris - attachmentUris.value.toSet()).takeIf { it.isNotEmpty() }?.let {
                    repository.deleteAttachments(transaction.id, it)
                }

                val attachments =
                    (attachmentUris.value - originalUris.toSet()).map(::prepareUriForSave)
                repository.addAttachments(transaction.id, attachments)
                (transaction as? Transfer)?.transferPeer?.let {
                    repository.addAttachments(
                        it,
                        attachments
                    )
                }
                val date = epoch2LocalDate(transaction.date)
                if (date <= LocalDate.now()) {
                    userSetExchangeRate?.let {
                        repository.savePrice(
                            currencyContext.homeCurrencyString,
                            transaction.amount.currencyUnit.code,
                            date,
                            ExchangeRateSource.User,
                            it.toDouble()
                        )
                    }
                }
                Unit
            })
        }

    private val shouldCopyExternalUris
        get() = prefHandler.getBoolean(PrefKey.COPY_ATTACHMENT, true)

    private fun prepareUriForSave(uri: Uri): Uri {
        val pictureUriBase: String = PictureDirHelper.getPictureUriBase(false, getApplication())
        return if (uri.toString().startsWith(pictureUriBase) || uri.isDebugAsset) uri
        else {

            val pictureUriTemp = PictureDirHelper.getPictureUriBase(true, getApplication())
            val isInTempFolder = uri.toString().startsWith(pictureUriTemp)
            val isExternal = !isInTempFolder

            if (isExternal && !shouldCopyExternalUris) uri else {

                val result = if (contentResolver.getType(uri)?.startsWith("image") == true &&
                    prefHandler.getBoolean(PrefKey.OPTIMIZE_PICTURE, true)
                ) {
                    val format = prefHandler.enumValueOrDefault(
                        PrefKey.OPTIMIZE_PICTURE_FORMAT,
                        Bitmap.CompressFormat.WEBP
                    )

                    val homeUri = PictureDirHelper.getOutputMediaUri(
                        false,
                        getApplication(),
                        extension = format.asExtension
                    )
                    try {

                        val maxSize =
                            prefHandler.getInt(PrefKey.OPTIMIZE_PICTURE_MAX_SIZE, 1000)
                        val quality = prefHandler.getInt(PrefKey.OPTIMIZE_PICTURE_QUALITY, 80)
                            .coerceAtLeast(0).coerceAtMost(100)
                        ImageOptimizer.optimize(
                            contentResolver,
                            uri,
                            homeUri,
                            format,
                            maxSize,
                            maxSize,
                            quality
                        )
                    } catch (e: IOException) {
                        throw UnknownPictureSaveException(uri, homeUri, e)
                    }
                    homeUri
                } else {
                    val fileName = uri.fileName(getApplication())
                    val homeUri = PictureDirHelper.getOutputMediaUri(
                        false,
                        getApplication(),
                        fileName = getNameWithoutExtension(fileName),
                        extension = getFileExtension(fileName)
                    )
                    FileCopyUtils.copy(contentResolver, uri, homeUri)
                    homeUri
                }
                if (isInTempFolder) {
                    contentResolver.delete(uri, null, null)
                }
                result
            }
        }
    }

    fun cleanupSplit(id: Long, isTemplate: Boolean): LiveData<Unit> =
        liveData(context = coroutineContext()) {
            emit(
                if (isTemplate) Template.cleanupCanceledEdit(contentResolver, id) else
                    SplitTransaction.cleanupCanceledEdit(contentResolver, id)
            )
        }

    fun loadActiveTags(id: Long) = viewModelScope.launch(coroutineContext()) {
        if (!userHasUpdatedTags) {
            updateTags(repository.loadActiveTagsForAccount(id), false)
        }
    }

    private fun Transaction.applyDefaultTransferCategory() {
        if (isTransfer) {
            prefHandler.defaultTransferCategory?.let { id ->
                repository.getCategoryPath(id)?.let { path ->
                    catId = id
                    categoryPath = path
                }
            }
        }
    }

    suspend fun newTemplate(
        operationType: Int,
        parentId: Long?,
    ): Template? = withContext(coroutineContext()) {
        repository.getLastUsedOpenAccount()?.let {
            Template.getTypedNewInstance(
                contentResolver,
                operationType,
                it.first,
                it.second,
                true,
                parentId
            )?.apply {
                applyDefaultTransferCategory()
            }
        }
    }

    private suspend fun ensureLoadData(
        accountId: Long,
        currencyUnit: CurrencyUnit?,
    ): Pair<Long, CurrencyUnit>? {
        return if (accountId > 0 && currencyUnit != null)
            accountId to currencyUnit
        else withContext(coroutineContext()) {
            (if (accountId > 0) {
                repository.getCurrencyUnitForAccount(accountId)?.let {
                    accountId to it
                }
            } else null) ?: repository.getLastUsedOpenAccount()
        }
    }

    suspend fun newTransaction(
        accountId: Long,
        currencyUnit: CurrencyUnit?,
        parentId: Long?,
    ): Transaction? = ensureLoadData(accountId, currencyUnit)?.let { (accountId, currencyUnit) ->
        Transaction.getNewInstance(accountId, currencyUnit, parentId)
    }

    suspend fun newTransfer(
        accountId: Long,
        currencyUnit: CurrencyUnit?,
        transferAccountId: Long?,
        parentId: Long?,
    ): Transfer? = ensureLoadData(accountId, currencyUnit)?.let { (accountId, currencyUnit) ->
        Transfer.getNewInstance(accountId, currencyUnit, transferAccountId, parentId).apply {
            applyDefaultTransferCategory()
        }
    }

    suspend fun newSplit(accountId: Long, currencyUnit: CurrencyUnit?): SplitTransaction? =
        ensureLoadData(accountId, currencyUnit)?.let { (accountId, currencyUnit) ->
            SplitTransaction.getNewInstance(contentResolver, accountId, currencyUnit, true)
        }

    fun loadSplitParts(parentId: Long, parentIsTemplate: Boolean) {
        splitPartLoader.tryEmit(parentId to parentIsTemplate)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val splitParts = splitPartLoader.filterNotNull().flatMapLatest { (parentId, parentIsTemplate) ->
        contentResolver.observeQuery(
            uri = if (parentIsTemplate) TransactionProvider.TEMPLATES_UNCOMMITTED_URI
            else TransactionProvider.UNCOMMITTED_URI,
            projection = listOfNotNull(
                KEY_ROWID,
                KEY_AMOUNT,
                KEY_COMMENT,
                KEY_PATH,
                KEY_TRANSFER_ACCOUNT,
                TRANSFER_ACCOUNT_LABEL,
                if (parentIsTemplate) null else BaseTransactionProvider.DEBT_LABEL_EXPRESSION,
                KEY_TAGLIST,
                KEY_ICON
            ).toTypedArray(),
            selection = "$KEY_PARENTID = ?",
            selectionArgs = arrayOf(parentId.toString())
        ).mapToList { SplitPart.fromCursor(it, contentResolver) }
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
            } == true
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
        extras: Bundle?,
    ): LiveData<Transaction?> = liveData(context = coroutineContext()) {
        when (task) {
            InstantiationTask.TEMPLATE -> Template.getInstanceFromDbWithTags(
                contentResolver,
                transactionId
            )

            InstantiationTask.TRANSACTION_FROM_TEMPLATE ->
                Transaction.getInstanceFromTemplateWithTags(contentResolver, transactionId)?.let {
                    it.first to it.second
                }

            InstantiationTask.TRANSACTION -> Transaction.getInstanceFromDbWithTags(
                contentResolver,
                transactionId,
                currencyContext.homeCurrencyUnit
            )

            InstantiationTask.FROM_INTENT_EXTRAS -> Pair(
                ProviderUtils.buildFromExtras(
                    repository,
                    extras!!
                )!!, emptyList()
            )

            InstantiationTask.TEMPLATE_FROM_TRANSACTION -> with(
                Transaction.getInstanceFromDb(
                    contentResolver, transactionId, currencyContext.homeCurrencyUnit
                )
            ) {
                Pair(
                    Template(contentResolver, this, party?.name ?: categoryPath),
                    this.loadTags(contentResolver)
                )
            }
        }?.also { pair ->
            if (forEdit) {
                pair.first.prepareForEdit(
                    contentResolver,
                    clone,
                    clone && prefHandler.getBoolean(PrefKey.CLONE_WITH_CURRENT_DATE, true)
                )
            }
            emit(pair.first)
            pair.second?.takeIf { it.isNotEmpty() }?.let { updateTags(it, false) }
            if (task == InstantiationTask.TRANSACTION) {
                val uriList = repository.loadAttachments(transactionId)
                //If we clone a transaction the attachments need to be considered new for the clone in order to get saved
                if (clone) {
                    addAttachmentUris(*uriList.toTypedArray())
                } else {
                    originalUris = ArrayList(uriList)
                }
            }
        } ?: run {
            emit(null)
        }
    }

    companion object {
        private const val KEY_ATTACHMENT_URIS = "attachmentUris"
        private const val KEY_ORIGINAL_URIS = "originalUris"
    }

    fun startAutoFill(id: Long, overridePreferences: Boolean, autoFillAccountFromExtra: Boolean) {
        val dataToLoad = buildList {

            val autoFillAccountFromPreference =
                prefHandler.getString(PrefKey.AUTO_FILL_ACCOUNT, "aggregate")
            val mayLoadAccount =
                overridePreferences && autoFillAccountFromExtra || autoFillAccountFromPreference == "always" ||
                        autoFillAccountFromPreference == "aggregate" && autoFillAccountFromExtra
            if (overridePreferences || prefHandler.getBoolean(PrefKey.AUTO_FILL_AMOUNT, true)) {
                add(KEY_CURRENCY)
                add(KEY_AMOUNT)
            }
            if (overridePreferences || prefHandler.getBoolean(
                    PrefKey.AUTO_FILL_CATEGORY,
                    true
                )
            ) {
                add(KEY_CATID)
                add(CAT_AS_LABEL)
                add(CATEGORY_ICON)
            }
            if (overridePreferences || prefHandler.getBoolean(
                    PrefKey.AUTO_FILL_COMMENT,
                    true
                )
            ) {
                add(KEY_COMMENT)
            }
            if (overridePreferences || prefHandler.getBoolean(PrefKey.AUTO_FILL_METHOD, true)) {
                add(KEY_METHODID)
            }
            if (overridePreferences || prefHandler.getBoolean(PrefKey.AUTO_FILL_DEBT, true)) {
                add(KEY_DEBT_ID)
            }
            if (mayLoadAccount) {
                add(KEY_ACCOUNTID)
            }
        }
        viewModelScope.launch(coroutineContext()) {
            contentResolver.query(
                ContentUris.withAppendedId(TransactionProvider.AUTOFILL_URI, id),
                dataToLoad.toTypedArray(), null, null, null
            )?.use {
                if (it.moveToFirst()) {
                    _autoFillData.tryEmit(AutoFillData.fromCursor(it, currencyContext))
                }
            }
        }
    }

    fun autoFillDone() {
        _autoFillData.tryEmit(null)
    }

    private var originalUris: ArrayList<Uri>
        get() = savedStateHandle[KEY_ORIGINAL_URIS] ?: ArrayList()
        set(value) {
            savedStateHandle[KEY_ORIGINAL_URIS] = value
            addAttachmentUris(*value.toTypedArray())
        }

    val attachmentUris: StateFlow<ArrayList<Uri>> =
        savedStateHandle.getStateFlow(KEY_ATTACHMENT_URIS, ArrayList())

    fun addAttachmentUris(vararg uris: Uri) {
        savedStateHandle[KEY_ATTACHMENT_URIS] = ArrayList(mutableSetOf<Uri>().apply {
            addAll(attachmentUris.value)
            addAll(uris)
        })
    }

    fun removeAttachmentUri(uri: Uri) {
        savedStateHandle[KEY_ATTACHMENT_URIS] = ArrayList<Uri>().apply {
            addAll(attachmentUris.value.filterNot { it == uri })
        }
    }

    data class AutoFillData(
        val catId: Long?,
        val label: String?,
        val icon: String?,
        val comment: String?,
        val amount: Money?,
        val methodId: Long?,
        val accountId: Long?,
        val debtId: Long?,
    ) {
        companion object {
            fun fromCursor(cursor: Cursor, currencyContext: CurrencyContext) = AutoFillData(
                catId = cursor.getLongIfExists(KEY_CATID),
                label = cursor.getStringIfExists(KEY_LABEL),
                icon = cursor.getStringIfExists(KEY_ICON),
                comment = cursor.getStringIfExists(KEY_COMMENT),
                amount = cursor.getLongIfExists(KEY_AMOUNT)?.let {
                    Money(currencyContext[cursor.getString(KEY_CURRENCY)], it)
                },
                methodId = cursor.getLongIfExists(KEY_METHODID),
                accountId = cursor.getLongIfExists(KEY_ACCOUNTID),
                debtId = cursor.getLongIfExists(KEY_DEBT_ID)
            )
        }
    }

    enum class InstantiationTask { TRANSACTION, TEMPLATE, TRANSACTION_FROM_TEMPLATE, FROM_INTENT_EXTRAS, TEMPLATE_FROM_TRANSACTION }
}


