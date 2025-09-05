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
import org.totschnig.myexpenses.db2.ensureCategoryPath
import org.totschnig.myexpenses.db2.extractTagIds
import org.totschnig.myexpenses.db2.extractTagIdsV2
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
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
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
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.NULL_CHANGE_INDICATOR
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.fromSyncAdapter
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.ui.DisplayParty
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
        remoteChanges.forEach { change: TransactionChange -> collectOperations(change, ops) }
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

        //When a split transaction is changed, we do not necessarily have an entry for the parent, so we
        //create one here
        splitsPerUuid.keys.forEach { uuid: String ->
            if (!changeList.any { change: TransactionChange -> change.uuid() == uuid }) {
                splitsPerUuid[uuid]?.let { list ->
                    changeList.add(
                        TransactionChange.builder().setType(TransactionChange.Type.updated)
                            .setTimeStamp(list[0].timeStamp()).setUuid(uuid).build()
                    )
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
            change.takeIf { it.isCreateOrUpdate }?.nullifyIfNeeded(mergedMap[change.uuid()])
                ?: change
        }.distinct()
    }

    /**
     * For all fields in a give change log entry, we compare if the final merged change has a different
     * non-null value, in which case we set the field to null. This prevents a value that has already
     * been overwritten from being written again either remotely or locally
     */
    private fun TransactionChange.nullifyIfNeeded(merged: TransactionChange?): TransactionChange {
        return if (merged == null) this
        else toBuilder().apply {
            if (comment() != null && merged.comment() != null && merged.comment() != comment()) {
                setComment(null)
            }
            if (date() != null && merged.date() != null && merged.date() != date()) {
                setDate(null)
            }
            if (valueDate() != null && merged.valueDate() != null && merged.valueDate() != valueDate()) {
                setValueDate(null)
            }
            if (amount() != null && merged.amount() != null && merged.amount() != amount()) {
                setAmount(null)
            }
            if ((label() != null || categoryInfo() != null) &&
                (merged.label() != null || merged.categoryInfo() != null) &&
                (merged.label() != label() || merged.categoryInfo() != categoryInfo())
            ) {
                setLabel(null)
                setCategoryInfo(null)
            }
            if (payeeName() != null && merged.payeeName() != null && merged.payeeName() != payeeName()) {
                setPayeeName(null)
            }
            if (transferAccount() != null && merged.transferAccount() != null && merged.transferAccount() != transferAccount()) {
                setTransferAccount(null)
            }
            if (methodLabel() != null && merged.methodLabel() != null && merged.methodLabel() != methodLabel()) {
                setMethodLabel(null)
            }
            if (crStatus() != null && merged.crStatus() != null && merged.crStatus() != crStatus()) {
                setCrStatus(null)
            }

            if (status() != null && merged.status() != null && merged.status() != status()) {
                setStatus(null)
            }
            if (referenceNumber() != null && merged.referenceNumber() != null && merged.referenceNumber() != referenceNumber()) {
                setReferenceNumber(null)
            }
            if ((pictureUri() != null || attachments() != null) &&
                (merged.pictureUri() != null || merged.attachments() != null) &&
                (merged.pictureUri() != pictureUri() || merged.attachments() != attachments())

            ) {
                setPictureUri(null)
                setAttachments(null)
            }
            if (splitParts() != null && merged.splitParts() != null && merged.splitParts() != splitParts()) {
                setSplitParts(null)
            }
            if ((tags() != null || tagsV2() != null) &&
                (merged.tags() != null || merged.tagsV2() != null) &&
                (merged.tags() != tags() || merged.tagsV2() != merged.tagsV2())
            ) {
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
            .sortedBy { if (it.isCreate) 0L else it.timeStamp() }
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
        if (change.status() != null) {
            builder.setStatus(change.status())
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
        if (change.tagsV2() != null) {
            builder.setTagsV2(change.tagsV2())
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
        attachments: Set<String>,
        transactionId: Long?,
        backReference: Int?
    ) = buildList {
        attachments.forEach { uuid ->
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

    private fun saveTagLinks(
        tagIds: List<Long>,
        transactionId: Long?,
        backReference: Int? = null
    ) = buildList {
        val uri = TransactionProvider.TRANSACTIONS_TAGS_URI.fromSyncAdapter()
        transactionId?.let {
            add(
                ContentProviderOperation.newDelete(uri)
                    .withSelection("$KEY_TRANSACTIONID = ?", arrayOf(it.toString())).build()
            )
        }
        tagIds.forEach { tagId ->
            val insert =
                ContentProviderOperation.newInsert(uri)
                    .withValue(DatabaseConstants.KEY_TAGID, tagId)
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
        parentOffset: Int = -1
    ) {
        val uri = Transaction.CALLER_IS_SYNC_ADAPTER_URI
        var skipped = false
        val offset = ops.size
        var additionalOpsCount = 0
        val tagIds: List<Long>? = buildList {
            change.tags()?.let { addAll(repository.extractTagIds(it, tagToId)) }
            change.tagsV2()?.let { addAll(repository.extractTagIdsV2(it, tagToId)) }
        }.takeIf { it.isNotEmpty() }
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
                        val tagOpsSize = tagIds
                            ?.let { saveTagLinks(it, transactionId) }
                            ?.also { ops.addAll(it) }
                            ?.size
                            ?: 0
                        val attachmentOpsSize = change.attachments()
                            ?.let { saveAttachmentLinks(it, transactionId, null) }
                            ?.also { ops.addAll(it) }
                            ?.size
                            ?: 0
                        additionalOpsCount = tagOpsSize + attachmentOpsSize
                    } else {
                        skipped = true
                        SyncAdapter.log()
                            .i("Uuid found in changes already exists locally, likely a transfer implicitly created from its peer")
                    }
                } else {
                    ops.addAll(getContentProviderOperationsForCreate(change, offset, parentOffset))
                    val tagOpsSize = tagIds
                        ?.let { saveTagLinks(it, null, offset) }
                        ?.also { ops.addAll(it) }
                        ?.size
                        ?: 0
                    val attachmentOpsSize = change.attachments()
                        ?.let { saveAttachmentLinks(it, null, offset) }
                        ?.also { ops.addAll(it) }
                        ?.size
                        ?: 0
                    additionalOpsCount = tagOpsSize + attachmentOpsSize
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
                    val tagOpsSize = tagIds
                        ?.let { list ->
                            saveTagLinks(
                                list,
                                transactionId.takeIf { it != -1L },
                                parentOffset.takeIf { it != -1 })
                        }
                        ?.also { ops.addAll(it) }
                        ?.size
                        ?: 0
                    val attachmentOpsSize = change.attachments()
                        ?.let { set ->
                            saveAttachmentLinks(
                                set,
                                transactionId.takeIf { it != -1L },
                                parentOffset.takeIf { it != -1 }
                            )
                        }
                        ?.also { ops.addAll(it) }
                        ?.size
                        ?: 0
                    additionalOpsCount = tagOpsSize + attachmentOpsSize
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

            TransactionChange.Type.unarchive -> {
                ops.add(
                    ContentProviderOperation.newAssertQuery(TRANSACTIONS_URI)
                        .withSelection(
                            "$KEY_UUID = ? AND $KEY_STATUS == $STATUS_ARCHIVE",
                            arrayOf(change.uuid())
                        )
                        .withExpectedCount(1).build()
                )
                ops.add(
                    ContentProviderOperation.newUpdate(
                        uri.buildUpon()
                            .appendPath(TransactionProvider.URI_SEGMENT_UNARCHIVE).build()
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
                extractParty(name)?.let {
                    values.put(KEY_PAYEEID, it.id)
                }
            }
        }
        if (change.methodLabel() == NULL_CHANGE_INDICATOR) {
            values.putNull(KEY_METHODID)
        } else {
            change.methodLabel()?.let { label ->
                values.put(KEY_METHODID, extractMethodId(label))
            }
        }
        change.crStatus()?.let { values.put(KEY_CR_STATUS, it) }
        change.status()?.let { values.put(KEY_STATUS, it) }
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
        } ?:  categoryInfo()?.let { repository.ensureCategoryPath(it) }
    }

    private fun extractParty(party: String): DisplayParty? =
        (payeeToId[party] ?: repository.requireParty(party)?.also {
            payeeToId[party] = it
        })?.let {
            DisplayParty(it, party)
        }

    private fun extractMethodId(methodLabel: String): Long =
        methodToId[methodLabel] ?: (repository.findPaymentMethod(methodLabel).takeIf { it != -1L }
            ?: repository.writePaymentMethod(methodLabel, account.type.id)).also {
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
        val t: Transaction = when {
            change.status() == STATUS_ARCHIVE -> Transaction(account.id, money)
            change.splitParts() != null -> SplitTransaction(account.id, money)
            else -> (change.transferAccount()?.let { transferAccount ->
                //if the account exists locally and the peer has already been synced
                //we create a Transfer, the Transfer class will take care in buildSaveOperations
                //of linking them together
                findTransferAccount(transferAccount)?.takeIf { accountId ->
                    resolver(
                        accountId,
                        change.uuid()
                    ) != -1L
                }?.let { Transfer(account.id, money, it) }
            } ?: Transaction(account.id, money)).apply {
                if (change.categoryInfo()?.firstOrNull()?.uuid != NULL_CHANGE_INDICATOR) {
                    catId = change.extractCatId()
                }
            }
        }
        t.uuid = change.uuid()
        change.comment()?.let { t.comment = it }
        change.date()?.let { t.date = it }
        t.valueDate = change.valueDate() ?: t.date
        change.payeeName()?.takeIf { it != NULL_CHANGE_INDICATOR }?.let { name ->
            t.party = extractParty(name)
        }
        change.methodLabel()?.takeIf { it != NULL_CHANGE_INDICATOR }?.let { label ->
            t.methodId = extractMethodId(label)
        }
        change.crStatus()?.let { t.crStatus = CrStatus.valueOf(it) }
        change.status()?.let { t.status = it }
        t.referenceNumber = change.referenceNumber()
        if (parentOffset == -1) {
            change.parentUuid()?.let {
                val parentId = resolver(account.id, it)
                if (parentId == -1L) {
                    CrashHandler.report(Exception("Could not find parent for split"))
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