package org.totschnig.myexpenses.sync

import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.os.RemoteException
import androidx.annotation.VisibleForTesting
import org.apache.commons.collections4.ListUtils
import org.totschnig.myexpenses.db2.CategoryHelper
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.ensureCategory
import org.totschnig.myexpenses.db2.extractTagIds
import org.totschnig.myexpenses.db2.findAccountByUuid
import org.totschnig.myexpenses.db2.findByAccountAndUuid
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.db2.writePaymentMethod
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model.saveTagLinks
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.NULL_CHANGE_INDICATOR
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.json.CategoryInfo
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.IOException

class SyncDelegate(
    val currencyContext: CurrencyContext,
    val featureManager: FeatureManager,
    val repository: Repository,
    val homeCurrency: CurrencyUnit,
    val resolver: (accountId: Long, transactionUUid: String) -> Long = repository.contentResolver::findByAccountAndUuid
) {

    private val categoryToId: MutableMap<String, Long> = HashMap()
    private val payeeToId: MutableMap<String, Long> = HashMap()
    private val methodToId: MutableMap<String, Long> = HashMap()
    private val tagToId: MutableMap<String, Long> = HashMap()
    private val accountUuidToId: MutableMap<String, Long> = HashMap()

    lateinit var account: Account

    @Throws(RemoteException::class, OperationApplicationException::class)
    fun writeRemoteChangesToDb(
        provider: ContentProviderClient,
        remoteChanges: List<TransactionChange>
    ) {
        if (remoteChanges.isNotEmpty()) {
            if (remoteChanges.size > SyncAdapter.BATCH_SIZE) {
                for (part in ListUtils.partition(remoteChanges, SyncAdapter.BATCH_SIZE)) {
                    writeRemoteChangesToDbPart(provider, part)
                }
            } else {
                writeRemoteChangesToDbPart(provider, remoteChanges)
            }
        }
    }

    @Throws(RemoteException::class, OperationApplicationException::class)
    private fun writeRemoteChangesToDbPart(
        provider: ContentProviderClient,
        remoteChanges: List<TransactionChange>
    ) {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(TransactionProvider.pauseChangeTrigger())
        remoteChanges.forEach { change: TransactionChange -> collectOperations(change, ops, -1) }
        ops.add(TransactionProvider.resumeChangeTrigger())
        val contentProviderResults = provider.applyBatch(ops)
        val opsSize = ops.size
        val resultsSize = contentProviderResults.size
        if (opsSize != resultsSize) {
            CrashHandler.report(
                Exception("applied $opsSize operations, received $resultsSize results"),
                SyncAdapter.TAG
            )
        }
    }

    /**
     * @param changeList
     * @return the same list with split parts moved as parts to their parents. If there are multiple parents
     * for the same uuid, the splits will appear under each of them
     */
    fun collectSplits(changeList: MutableList<TransactionChange>): List<TransactionChange> {
        val splitsPerUuid = HashMap<String, MutableList<TransactionChange>>()
        val i = changeList.iterator()
        while (i.hasNext()) {
            val change = i.next()
            change.parentUuid()?.let {
                ensureList(splitsPerUuid, it).add(change)
                i.remove()
            }
        }
        val splitsPerUuidFiltered = HashMap<String, List<TransactionChange>>()
        //When a split transaction is changed, we do not necessarily have an entry for the parent, so we
        //create one here
        splitsPerUuid.keys.forEach { uuid: String ->
            if (!changeList.any { change: TransactionChange -> change.uuid() == uuid }) {
                splitsPerUuid[uuid]?.let { list ->
                    changeList.add(
                        TransactionChange.builder().setType(TransactionChange.Type.updated)
                            .setTimeStamp(list[0].timeStamp()).setUuid(uuid).build()
                    )
                    splitsPerUuidFiltered[uuid] = filterDeleted(list, findDeletedUuids(list))
                }
            }
        }
        return changeList.map { change: TransactionChange ->
            if (splitsPerUuid.containsKey(change.uuid())) change.toBuilder()
                .setSplitPartsAndValidate(splitsPerUuid[change.uuid()]!!).build() else change
        }
    }

    private fun TransactionChange.Builder.setSplitPartsAndValidate(value: List<TransactionChange>): TransactionChange.Builder {
        return if (value.all { it.parentUuid() == uuid() }) {
            setSplitParts(value)
        } else {
            throw IllegalStateException("parts parentUuid does not match parents uuid")
        }
    }

    fun mergeChangeSets(
        first: List<TransactionChange>, second: List<TransactionChange>
    ): Pair<List<TransactionChange>, List<TransactionChange>> {

        //filter out changes made obsolete by later delete
        val deletedUuids = findDeletedUuids(first + second)
        var firstResult: List<TransactionChange> = filterDeleted(first, deletedUuids)
        var secondResult: List<TransactionChange> = filterDeleted(second, deletedUuids)

        //merge update changes
        val updatesPerUuid = HashMap<String, MutableList<TransactionChange>>()
        val mergesPerUuid = HashMap<String, TransactionChange>()
        (firstResult + secondResult)
            .filter { obj: TransactionChange -> obj.isCreateOrUpdate }
            .forEach { change: TransactionChange ->
                ensureList(updatesPerUuid, change.uuid()).add(
                    change
                )
            }
        val uuidsRequiringMerge = updatesPerUuid.keys
            .filter { uuid: String -> updatesPerUuid[uuid]!!.size > 1 }
        uuidsRequiringMerge.forEach { uuid: String ->
            mergesPerUuid[uuid] = mergeUpdates(updatesPerUuid[uuid]!!)
        }
        firstResult = mergeChanges(replaceByMerged(firstResult, mergesPerUuid))
        secondResult = mergeChanges(replaceByMerged(secondResult, mergesPerUuid))
        return firstResult to secondResult
    }

    private fun ensureList(
        map: HashMap<String, MutableList<TransactionChange>>,
        uuid: String
    ): MutableList<TransactionChange> {
        var changesForUuid = map[uuid]
        if (changesForUuid == null) {
            changesForUuid = ArrayList()
            map[uuid] = changesForUuid
        }
        return changesForUuid
    }

    private fun replaceByMerged(
        input: List<TransactionChange>,
        mergedMap: HashMap<String, TransactionChange>
    ): List<TransactionChange> {
        return input.map { change ->
            change.takeIf { it.isCreateOrUpdate }?.nullifyIfNeeded(mergedMap[change.uuid()]) ?: change
        }.distinct()
    }

    private fun TransactionChange.nullifyIfNeeded(from: TransactionChange?): TransactionChange {
        return if (from == null) this
        else toBuilder().apply {
            if (comment() != null && from.comment() != null && from.comment() != comment()) {
                setComment(null)
            }
            if (date() != null && from.date() != null && from.date() != date()) {
                setDate(null)
            }
            if (valueDate() != null && from.valueDate() != null && from.valueDate() != valueDate()) {
                setValueDate(null)
            }
            if (amount() != null && from.amount() != null && from.amount() != amount()) {
                setAmount(null)
            }
            if ((label() != null || categoryInfo() != null) &&
                (from.label() != null || from.categoryInfo() != null) &&
                (from.label() != label() || from.categoryInfo() != categoryInfo())
                ) {
                setLabel(null)
                setCategoryInfo(null)
            }
            if (payeeName() != null && from.payeeName() != null && from.payeeName() != payeeName()) {
                setPayeeName(null)
            }
            if (transferAccount() != null && from.transferAccount() != null && from.transferAccount() != transferAccount()) {
                setTransferAccount(null)
            }
            if (methodLabel() != null && from.methodLabel() != null && from.methodLabel() != methodLabel()) {
                setMethodLabel(null)
            }
            if (crStatus() != null && from.crStatus() != null && from.crStatus() != crStatus()) {
                setCrStatus(null)
            }
            if (referenceNumber() != null && from.referenceNumber() != null && from.referenceNumber() != referenceNumber()) {
                setReferenceNumber(null)
            }
            if ((pictureUri() != null || attachments() != null) &&
                (from.pictureUri() != null || from.attachments() != null) &&
                (from.pictureUri() != pictureUri() || from.attachments() != attachments())

                ) {
                setPictureUri(null)
                setAttachments(null)
            }
            if (splitParts() != null && from.splitParts() != null && from.splitParts() != splitParts()) {
                setSplitParts(null)
            }
            if (tags() != null && from.tags() != null && from.tags() != tags()) {
                setTags(null)
            }
        }.build()
    }

    private fun mergeChanges(input: List<TransactionChange>): List<TransactionChange> =
        input.groupBy(TransactionChange::uuid).map { entry -> mergeUpdates(entry.value) }

    @VisibleForTesting
    fun mergeUpdates(changeList: List<TransactionChange>): TransactionChange {
        check(changeList.isNotEmpty()) { "nothing to merge" }
        return changeList
            .sortedBy { obj: TransactionChange -> obj.timeStamp() }
            .reduce { initial: TransactionChange, change: TransactionChange ->
                mergeUpdate(
                    initial,
                    change
                )
            }
    }

    private fun mergeUpdate(
        initial: TransactionChange,
        change: TransactionChange
    ): TransactionChange {
        check(initial.uuid() == change.uuid()) { "Can only merge changes with same uuid" }
        if (initial.isDelete) return initial
        if (change.isDelete) return change
        val builder = initial.toBuilder()
        if (change.parentUuid() != null) {
            builder.setParentUuid(change.parentUuid())
        }
        if (change.comment() != null) {
            builder.setComment(change.comment())
        }
        if (change.date() != null) {
            builder.setDate(change.date())
        }
        if (change.valueDate() != null) {
            builder.setDate(change.valueDate())
        }
        if (change.amount() != null) {
            builder.setAmount(change.amount())
        }
        if (change.label() != null) {
            builder.setLabel(change.label())
        }
        if (change.payeeName() != null) {
            builder.setPayeeName(change.payeeName())
        }
        if (change.transferAccount() != null) {
            builder.setTransferAccount(change.transferAccount())
        }
        if (change.methodLabel() != null) {
            builder.setMethodLabel(change.methodLabel())
        }
        if (change.crStatus() != null) {
            builder.setCrStatus(change.crStatus())
        }
        if (change.referenceNumber() != null) {
            builder.setReferenceNumber(change.referenceNumber())
        }
        if (change.pictureUri() != null) {
            builder.setPictureUri(change.pictureUri())
        }
        if (change.splitParts() != null) {
            builder.setSplitParts(change.splitParts())
        }
        if (change.tags() != null) {
            builder.setTags(change.tags())
        }
        if (change.attachments() != null) {
            builder.setAttachments(change.attachments())
        }
        if (change.categoryInfo() != null) {
            builder.setCategoryInfo(change.categoryInfo())
        }
        return builder.setCurrentTimeStamp().build()
    }

    private fun saveAttachmentLinks(
        attachments: MutableList<String>?,
        transactionId: Long?,
        backReference: Int?
    ) = buildList {
        //we are not deleting attachments that might have been removed on another device, because
        //we would need to compare existing attachments with the new list, which would only be doable
        //via a method call but ContentProviderOperation.newCall is not available below API 30
        attachments?.forEach { uuid ->
            val insert =
                ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_ATTACHMENTS_URI)
                    .withValue(KEY_UUID, uuid)
            transactionId?.let {
                insert.withValue(KEY_TRANSACTIONID, it)
            } ?: backReference?.let {
                insert.withValueBackReference(KEY_TRANSACTIONID, it)
            } ?: throw IllegalArgumentException("neither id nor backReference provided")
            add(insert.build())
        }
    }

    @VisibleForTesting
    fun collectOperations(
        change: TransactionChange,
        ops: ArrayList<ContentProviderOperation>,
        parentOffset: Int
    ) {
        val uri = Transaction.CALLER_IS_SYNC_ADAPTER_URI
        var skipped = false
        val offset = ops.size
        var additionalOpsCount = 0
        val tagIds = change.tags()?.let { repository.extractTagIds(it, tagToId) }
        when (change.type()) {
            TransactionChange.Type.created -> {
                val transactionId = resolver(account.id, change.uuid())
                if (transactionId > -1) {
                    if (parentOffset > -1) {
                        //if we find a split part that already exists, we need to assume that it has already been synced
                        //by a previous sync of its transfer account, so all we do here is reparent it as child
                        //of the split transaction we currently ingest
                        ops.add(
                            ContentProviderOperation.newUpdate(uri)
                                .withValues(toContentValues(change))
                                .withSelection(
                                    "$KEY_ROWID = ?",
                                    arrayOf(transactionId.toString())
                                )
                                .withValueBackReference(KEY_PARENTID, parentOffset)
                                .build()
                        )
                        val tagOps = saveTagLinks(tagIds, transactionId, null, true)
                        ops.addAll(tagOps)
                        val attachmentOps =
                            saveAttachmentLinks(change.attachments(), transactionId, null)
                        ops.addAll(attachmentOps)
                        additionalOpsCount = tagOps.size + attachmentOps.size
                    } else {
                        skipped = true
                        SyncAdapter.log()
                            .i("Uuid found in changes already exists locally, likely a transfer implicitly created from its peer")
                    }
                } else {
                    ops.addAll(getContentProviderOperationsForCreate(change, offset, parentOffset))
                    val tagOps = saveTagLinks(tagIds, null, offset, true)
                    ops.addAll(tagOps)
                    val attachmentOps = saveAttachmentLinks(change.attachments(), null, offset)
                    ops.addAll(attachmentOps)
                    additionalOpsCount = tagOps.size + attachmentOps.size
                }
            }

            TransactionChange.Type.updated -> {
                val values: ContentValues = toContentValues(change)
                val transactionId = resolver(account.id, change.uuid())
                if (transactionId != -1L || parentOffset != -1) {
                    if (values.size() > 0 || parentOffset != -1) {
                        val builder = ContentProviderOperation.newUpdate(uri)
                        if (transactionId != -1L) {
                            builder.withSelection(
                                "$KEY_ROWID = ?",
                                arrayOf(transactionId.toString())
                            )
                            //Make sure we set the parent, in case the change is caused by "Transform to split"
                        } else {
                            builder.withSelection(
                                "$KEY_UUID = ?",
                                arrayOf(change.uuid())
                            )
                        }
                        if (parentOffset != -1) {
                            builder.withValueBackReference(KEY_PARENTID, parentOffset)
                        }
                        if (values.size() > 0) {
                            builder.withValues(values)
                        }
                        ops.add(builder.build())
                    }
                    val tagOps = saveTagLinks(tagIds, transactionId, null, true)
                    ops.addAll(tagOps)
                    val attachmentOps =
                        saveAttachmentLinks(change.attachments(), transactionId, null)
                    ops.addAll(attachmentOps)
                    additionalOpsCount = tagOps.size + attachmentOps.size
                }
            }

            TransactionChange.Type.deleted -> {
                val transactionId = resolver(account.id, change.uuid())
                if (transactionId != -1L) {
                    ops.add(
                        ContentProviderOperation.newDelete(
                            ContentUris.withAppendedId(
                                uri,
                                transactionId
                            )
                        )
                            .withSelection(
                                "$KEY_UUID = ? AND $KEY_ACCOUNTID = ?",
                                arrayOf(change.uuid(), account.id.toString())
                            )
                            .build()
                    )
                }
            }

            TransactionChange.Type.unsplit -> {
                ops.add(
                    ContentProviderOperation.newUpdate(
                        uri.buildUpon()
                            .appendPath(TransactionProvider.URI_SEGMENT_UNSPLIT).build()
                    )
                        .withValue(KEY_UUID, change.uuid())
                        .build()
                )
            }

            TransactionChange.Type.link -> {
                ops.add(
                    ContentProviderOperation.newUpdate(
                        uri.buildUpon()
                            .appendPath(TransactionProvider.URI_SEGMENT_LINK_TRANSFER)
                            .appendPath(change.uuid()).build()
                    )
                        .withValue(KEY_UUID, change.referenceNumber())
                        .build()
                )
            }

            else -> {}
        }
        if (change.isCreateOrUpdate && !skipped) {
            change.splitParts()?.let { splitParts ->
                val newParentOffset = ops.size - 1 - additionalOpsCount
                mergeChanges(
                    filterDeleted(
                        splitParts,
                        findDeletedUuids(splitParts)
                    )
                ).forEach { splitChange: TransactionChange ->
                    require(change.uuid() == splitChange.parentUuid())
                    //back reference is only used when we insert a new split,
                    //for updating an existing split we search for its _id via its uuid
                    collectOperations(
                        splitChange,
                        ops,
                        if (change.isCreate) newParentOffset else -1
                    )
                }
            }
        }
    }

    private fun findDeletedUuids(list: List<TransactionChange>): List<String> =
        list.filter { obj: TransactionChange -> obj.isDelete }
            .map { obj: TransactionChange -> obj.uuid() }

    private fun toContentValues(change: TransactionChange): ContentValues {
        val values = ContentValues()
        change.comment()?.let {
            if (it.isEmpty()) {
                values.putNull(KEY_COMMENT)
            } else {
                values.put(KEY_COMMENT, it)
            }
        }
        change.date()?.let { values.put(KEY_DATE, it) }
        change.valueDate()?.let { values.put(KEY_VALUE_DATE, it) }
        change.amount()?.let { values.put(KEY_AMOUNT, it) }
        if (change.categoryInfo()?.firstOrNull()?.uuid == NULL_CHANGE_INDICATOR) {
          values.putNull(KEY_CATID)
        } else {
            change.extractCatId()?.let { values.put(KEY_CATID, it) }
        }
        if (change.payeeName() == NULL_CHANGE_INDICATOR) {
            values.putNull(KEY_PAYEEID)
        } else {
            change.payeeName()?.let { name ->
                values.put(KEY_PAYEEID, extractParty(name))
            }
        }
        if (change.methodLabel()== NULL_CHANGE_INDICATOR) {
            values.putNull(KEY_METHODID)
        } else {
            change.methodLabel()?.let { label ->
                values.put(KEY_METHODID, extractMethodId(label))
            }
        }
        change.crStatus()?.let { values.put(KEY_CR_STATUS, it) }
        change.referenceNumber()?.let { values.put(KEY_REFERENCE_NUMBER, it) }
        if (change.originalAmount() != null && change.originalCurrency() != null) {
            if (change.originalAmount() == Long.MIN_VALUE) {
                values.putNull(KEY_ORIGINAL_AMOUNT)
                values.putNull(KEY_ORIGINAL_CURRENCY)
            } else {
                values.put(KEY_ORIGINAL_AMOUNT, change.originalAmount())
                values.put(KEY_ORIGINAL_CURRENCY, change.originalCurrency())
            }
        }
        if (change.equivalentAmount() != null && change.equivalentCurrency() != null) {
            if (change.equivalentAmount() == Long.MIN_VALUE || change.equivalentCurrency() != homeCurrency.code) {
                values.putNull(KEY_EQUIVALENT_AMOUNT)
            } else if (change.equivalentCurrency() == homeCurrency.code) {
                values.put(KEY_EQUIVALENT_AMOUNT, change.equivalentAmount())
            }
        }
        return values
    }

    private fun TransactionChange.extractCatId(): Long? {
        return label()?.let {
            CategoryHelper.insert(repository, it, categoryToId, false)
            categoryToId[it] ?: throw IOException("Saving category $it failed")
        } ?: categoryInfo()?.fold(null) { parentId: Long?, categoryInfo: CategoryInfo ->
            repository.ensureCategory(
                categoryInfo,
                parentId
            ).first
        }
    }

    private fun extractParty(party: String): Long =
        payeeToId[party] ?: repository.requireParty(party).also {
            payeeToId[party] = it
        }

    private fun extractMethodId(methodLabel: String): Long =
        methodToId[methodLabel] ?: (repository.findPaymentMethod(methodLabel).takeIf { it != -1L }
            ?: repository.writePaymentMethod(methodLabel, account.type)).also {
            methodToId[methodLabel] = it
        }

    private fun findTransferAccount(uuid: String): Long? =
        accountUuidToId[uuid] ?: repository.findAccountByUuid(uuid)?.also {
            accountUuidToId[uuid] = it
        }

    private fun getContentProviderOperationsForCreate(
        change: TransactionChange, offset: Int, parentOffset: Int
    ): ArrayList<ContentProviderOperation> {
        check(change.isCreate)
        val amount = change.amount() ?: 0L
        val money = Money(currencyContext[account.currency], amount)
        val t: Transaction = if (change.splitParts() != null) {
            SplitTransaction(account.id, money)
        } else {
            change.transferAccount()?.let { transferAccount ->
                //if the account exists locally and the peer has already been synced
                //we create a Transfer, the Transfer class will take care in buildSaveOperations
                //of linking them together
                findTransferAccount(transferAccount)?.takeIf { accountId ->
                    resolver(
                        accountId,
                        change.uuid()
                    ) != -1L
                }?.let { Transfer(account.id, money, it) }
            } ?: Transaction(account.id, money).apply {
                if (change.transferAccount() == null) {
                    if (change.categoryInfo()?.firstOrNull()?.uuid != NULL_CHANGE_INDICATOR)
                        catId = change.extractCatId()
                }
            }
        }
        t.uuid = change.uuid()
        change.comment()?.let { t.comment = it }
        change.date()?.let { t.date = it }
        t.valueDate = change.valueDate() ?: t.date
        change.payeeName()?.takeIf { it != NULL_CHANGE_INDICATOR }?.let { name ->
            t.payeeId = extractParty(name)
        }
        change.methodLabel()?.takeIf { it != NULL_CHANGE_INDICATOR }?.let { label ->
            t.methodId = extractMethodId(label)
        }
        change.crStatus()?.let { t.crStatus = CrStatus.valueOf(it) }
        t.referenceNumber = change.referenceNumber()
        if (parentOffset == -1) {
            change.parentUuid()?.let {
                val parentId = resolver(account.id, it)
                if (parentId == -1L) {
                    return ArrayList() //if we fail to link a split part to a parent, we need to ignore it
                }
                t.parentId = parentId
            }
        }
        change.originalAmount()?.let { originalAmount ->
            change.originalCurrency()?.let { originalCurrency ->
                t.originalAmount = Money(currencyContext[originalCurrency], originalAmount)
            }
        }
        change.equivalentAmount()?.let { equivalentAmount ->
            change.equivalentCurrency()?.let { equivalentCurrency ->
                with(homeCurrency) {
                    if (equivalentCurrency == code) {
                        t.equivalentAmount = Money(this, equivalentAmount)
                    }
                }
            }
        }
        return t.buildSaveOperations(repository.contentResolver, offset, parentOffset, true, false)
    }

    private fun filterDeleted(
        input: List<TransactionChange>,
        deletedUuids: List<String>
    ): List<TransactionChange> {
        return input.filter { change: TransactionChange ->
            change.isDelete || !deletedUuids.contains(
                change.uuid()
            )
        }
    }

    fun findMetadataChange(input: List<TransactionChange>) =
        input.findLast { value: TransactionChange -> value.type() == TransactionChange.Type.metadata }

    fun removeMetadataChange(input: List<TransactionChange>) =
        input.filter { value: TransactionChange -> value.type() != TransactionChange.Type.metadata }

    fun requireFeatureForAccount(context: Context, name: String): Feature? {
        BackendService.forAccount(name).getOrNull()?.feature?.let {
            if (!featureManager.isFeatureInstalled(it, context)) {
                featureManager.requestFeature(it, context)
                return it
            }
        }
        return null
    }
}