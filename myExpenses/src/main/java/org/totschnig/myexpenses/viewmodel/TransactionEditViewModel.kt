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
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.db2.addAttachments
import org.totschnig.myexpenses.db2.deleteAttachments
import org.totschnig.myexpenses.db2.getCurrencyUnitForAccount
import org.totschnig.myexpenses.db2.getLastUsedOpenAccount
import org.totschnig.myexpenses.db2.loadActiveTagsForAccount
import org.totschnig.myexpenses.db2.loadAttachments
import org.totschnig.myexpenses.exception.UnknownPictureSaveException
import org.totschnig.myexpenses.model.*
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.BaseTransactionProvider.Companion.KEY_DEBT_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.CATEGORY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.CAT_AS_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCHANGE_RATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PLANID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SEALED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TITLE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_UNCOMMITTED
import org.totschnig.myexpenses.provider.FULL_LABEL
import org.totschnig.myexpenses.provider.ProviderUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_ACCOUNTY_TYPE_LIST
import org.totschnig.myexpenses.provider.fileName
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongIfExists
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringIfExists
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.splitStringList
import org.totschnig.myexpenses.util.ImageOptimizer
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.ShortcutHelper
import org.totschnig.myexpenses.util.asExtension
import org.totschnig.myexpenses.util.io.FileCopyUtils
import org.totschnig.myexpenses.util.io.getFileExtension
import org.totschnig.myexpenses.util.io.getNameWithoutExtension
import org.totschnig.myexpenses.viewmodel.data.Account
import org.totschnig.myexpenses.viewmodel.data.PaymentMethod
import timber.log.Timber
import java.io.IOException
import kotlin.math.pow
import org.totschnig.myexpenses.viewmodel.data.Template as DataTemplate

class TransactionEditViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    TagHandlingViewModel(application, savedStateHandle) {

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
        emit(Plan.getInstanceFromDb(contentResolver, planId))
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

    fun save(transaction: ITransaction): LiveData<Result<Long>> =
        liveData(context = coroutineContext()) {
            emit(kotlin.runCatching {
                val existingTemplateMaybeUpdateShortcut =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && transaction is Template && transaction.id != 0L
                val result =
                    transaction.save(contentResolver, true)?.let { ContentUris.parseId(it) }
                        ?: throw Throwable("Error while saving transaction")
                if (existingTemplateMaybeUpdateShortcut) {
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
                                    TemplateInfo.fromTemplate(transaction as Template)
                                )
                            )
                        )
                    }
                }
                if (!transaction.saveTags(
                        contentResolver,
                        tagsLiveData.value
                    )
                ) throw Throwable("Error while saving tags")
                (originalUris - attachmentUris.value.toSet()).takeIf { it.isNotEmpty() }?.let {
                    repository.deleteAttachments(transaction.id, it)
                }
                repository.addAttachments(
                    transaction.id,
                    (attachmentUris.value - originalUris.toSet()).map(::prepareUriForSave)
                )
                result
            })
        }

    private val shouldCopyExternalUris
        get() = prefHandler.getBoolean(PrefKey.COPY_ATTACHMENT, true)

    private fun prepareUriForSave(uri: Uri): Uri {
        val pictureUriBase: String = PictureDirHelper.getPictureUriBase(false, getApplication())
        return if (uri.toString().startsWith(pictureUriBase)) {
            Timber.d("nothing todo: Internal")
            uri
        } else {

            val pictureUriTemp = PictureDirHelper.getPictureUriBase(true, getApplication())
            val isInTempFolder = uri.toString().startsWith(pictureUriTemp)
            val isExternal = !isInTempFolder

            if (isExternal && !shouldCopyExternalUris) uri else {

                val type = contentResolver.getType(uri)

                val result = if (type!!.startsWith("image") && prefHandler.getBoolean(
                        PrefKey.OPTIMIZE_PICTURE,
                        true
                    )
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

                        val maxSize = prefHandler.getInt(PrefKey.OPTIMIZE_PICTURE_MAX_SIZE, 1000)
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

    private fun adjustExchangeRate(raw: Double, currencyUnit: CurrencyUnit): Double {
        val minorUnitDelta: Int =
            currencyUnit.fractionDigits - homeCurrencyProvider.homeCurrencyUnit.fractionDigits
        return raw * 10.0.pow(minorUnitDelta.toDouble())
    }

    fun loadActiveTags(id: Long) = viewModelScope.launch(coroutineContext()) {
        if (!userHasUpdatedTags) {
            updateTags(repository.loadActiveTagsForAccount(id), false)
        }
    }

    suspend fun newTemplate(
        operationType: Int,
        parentId: Long?
    ): Template? = withContext(coroutineContext()) {
        repository.getLastUsedOpenAccount()?.let {
            Template.getTypedNewInstance(
                contentResolver,
                operationType,
                it.first,
                it.second,
                true,
                parentId
            )
        }
    }

    private suspend fun ensureLoadData(
        accountId: Long,
        currencyUnit: CurrencyUnit?
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
        parentId: Long?
    ): Transaction? = ensureLoadData(accountId, currencyUnit)?.let {
        Transaction.getNewInstance(it.first, it.second, parentId)
    }

    suspend fun newTransfer(
        accountId: Long,
        currencyUnit: CurrencyUnit?,
        transferAccountId: Long?,
        parentId: Long?
    ): Transfer? = ensureLoadData(accountId, currencyUnit)?.let {
        Transfer.getNewInstance(it.first, it.second, transferAccountId, parentId)
    }

    suspend fun newSplit(accountId: Long, currencyUnit: CurrencyUnit?): SplitTransaction? =
        ensureLoadData(accountId, currencyUnit)?.let {
            SplitTransaction.getNewInstance(contentResolver, it.first, it.second, true)
        }

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
            InstantiationTask.TEMPLATE -> Template.getInstanceFromDbWithTags(
                contentResolver,
                transactionId
            )

            InstantiationTask.TRANSACTION_FROM_TEMPLATE ->
                Transaction.getInstanceFromTemplateWithTags(contentResolver, transactionId)

            InstantiationTask.TRANSACTION -> Transaction.getInstanceFromDbWithTags(
                contentResolver,
                transactionId,
                homeCurrencyProvider.homeCurrencyUnit
            )

            InstantiationTask.FROM_INTENT_EXTRAS -> Pair(
                ProviderUtils.buildFromExtras(
                    repository,
                    extras!!
                )!!, emptyList()
            )

            InstantiationTask.TEMPLATE_FROM_TRANSACTION -> with(
                Transaction.getInstanceFromDb(
                    contentResolver, transactionId, homeCurrencyProvider.homeCurrencyUnit
                )
            ) {
                Pair(
                    Template(contentResolver, this, payee ?: label),
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
            pair.second?.takeIf { it.size > 0 }?.let { updateTags(it, false) }
            if (task == InstantiationTask.TRANSACTION) {
                originalUris = repository.loadAttachments(transactionId)
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
        val debtId: Long?
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


