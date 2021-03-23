package org.totschnig.myexpenses.sync

import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.OperationApplicationException
import android.net.Uri
import android.os.RemoteException
import androidx.annotation.VisibleForTesting
import androidx.core.util.Pair
import org.apache.commons.collections4.ListUtils
import org.totschnig.myexpenses.export.CategoryInfo
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Payee
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.model.extractTagIds
import org.totschnig.myexpenses.model.saveTagLinks
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.util.*

class SyncDelegate @JvmOverloads constructor(val currencyContext: CurrencyContext, val resolver: (accountId: Long, transactionUUid: String) -> Long = Transaction::findByAccountAndUuid) {

    private val categoryToId: MutableMap<String, Long> = HashMap()
    private val payeeToId: MutableMap<String, Long> = HashMap()
    private val methodToId: MutableMap<String, Long> = HashMap()
    private val tagToId: MutableMap<String, Long> = HashMap()
    private val accountUuidToId: MutableMap<String, Long> = HashMap()

    lateinit var account: Account

    @Throws(RemoteException::class, OperationApplicationException::class)
    fun writeRemoteChangesToDb(provider: ContentProviderClient, remoteChanges: List<TransactionChange>) {
        if (remoteChanges.isEmpty()) {
            return
        }
        if (remoteChanges.size > SyncAdapter.BATCH_SIZE) {
            for (part in ListUtils.partition(remoteChanges, SyncAdapter.BATCH_SIZE)) {
                writeRemoteChangesToDbPart(provider, part)
            }
        } else {
            writeRemoteChangesToDbPart(provider, remoteChanges)
        }
    }

    @Throws(RemoteException::class, OperationApplicationException::class)
    private fun writeRemoteChangesToDbPart(provider: ContentProviderClient, remoteChanges: List<TransactionChange>) {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(TransactionProvider.pauseChangeTrigger())
        remoteChanges.forEach { change: TransactionChange -> collectOperations(change, ops, -1) }
        ops.add(TransactionProvider.resumeChangeTrigger())
        val contentProviderResults = provider.applyBatch(ops)
        val opsSize = ops.size
        val resultsSize = contentProviderResults.size
        if (opsSize != resultsSize) {
            CrashHandler.reportWithTag(String.format(Locale.ROOT, "applied %d operations, received %d results",
                    opsSize, resultsSize), SyncAdapter.TAG)
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
                    changeList.add(TransactionChange.builder().setType(TransactionChange.Type.updated).setTimeStamp(list[0].timeStamp()).setUuid(uuid).build())
                    splitsPerUuidFiltered[uuid] = filterDeleted(list, findDeletedUuids(list))
                }
            }
        }
        return changeList.map { change: TransactionChange -> if (splitsPerUuid.containsKey(change.uuid())) change.toBuilder().setSplitPartsAndValidate(splitsPerUuid[change.uuid()]).build() else change }
    }

    fun mergeChangeSets(
            first: List<TransactionChange>, second: List<TransactionChange>): Pair<List<TransactionChange>, List<TransactionChange>> {

        //filter out changes made obsolete by later delete
        val deletedUuids = findDeletedUuids(first + second)
        var firstResult: List<TransactionChange> = filterDeleted(first, deletedUuids)
        var secondResult: List<TransactionChange> = filterDeleted(second, deletedUuids)

        //merge update changes
        val updatesPerUuid = HashMap<String, MutableList<TransactionChange>>()
        val mergesPerUuid = HashMap<String, TransactionChange>()
        (firstResult + secondResult)
                .filter { obj: TransactionChange -> obj.isCreateOrUpdate }
                .forEach { change: TransactionChange -> ensureList(updatesPerUuid, change.uuid()).add(change) }
        val uuidsRequiringMerge = updatesPerUuid.keys
                .filter { uuid: String -> updatesPerUuid[uuid]!!.size > 1 }
        uuidsRequiringMerge.forEach { uuid: String -> mergesPerUuid[uuid] = mergeUpdates(updatesPerUuid[uuid]!!) }
        firstResult = replaceByMerged(firstResult, mergesPerUuid)
        secondResult = replaceByMerged(secondResult, mergesPerUuid)
        return Pair.create(firstResult, secondResult)
    }

    private fun ensureList(map: HashMap<String, MutableList<TransactionChange>>, uuid: String): MutableList<TransactionChange> {
        var changesForUuid = map[uuid]
        if (changesForUuid == null) {
            changesForUuid = ArrayList()
            map[uuid] = changesForUuid
        }
        return changesForUuid
    }

    private fun replaceByMerged(input: List<TransactionChange>, mergedMap: HashMap<String, TransactionChange>): List<TransactionChange> {
        return input.map { change ->
            change.takeIf { it.isCreateOrUpdate }?.let { mergedMap[change.uuid()] } ?: change
        }.distinct()
    }

    private fun mergeChanges(input: List<TransactionChange>): List<TransactionChange> =
            input.groupBy(TransactionChange::uuid).map { entry -> mergeUpdates(entry.value) }

    @VisibleForTesting
    fun mergeUpdates(changeList: List<TransactionChange>): TransactionChange {
        check(changeList.isNotEmpty()) { "nothing to merge" }
        return changeList
                .sortedBy { obj: TransactionChange -> obj.timeStamp() }
                .reduce { initial: TransactionChange, change: TransactionChange -> mergeUpdate(initial, change) }
    }

    private fun mergeUpdate(initial: TransactionChange, change: TransactionChange): TransactionChange {
        check(change.isCreateOrUpdate && initial.isCreateOrUpdate) { "Can only merge creates and updates" }
        check(initial.uuid() == change.uuid()) { "Can only merge changes with same uuid" }
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
        return builder.setCurrentTimeStamp().build()
    }

    @VisibleForTesting
    fun collectOperations(change: TransactionChange,
                          ops: ArrayList<ContentProviderOperation>,
                          parentOffset: Int) {
        val uri = Transaction.CALLER_IS_SYNC_ADAPTER_URI
        var skipped = false
        val offset = ops.size
        var tagOpsCount = 0
        val tagIds = change.tags()?.let { extractTagIds(it, tagToId) }
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (change.type()) {
            TransactionChange.Type.created -> {
                val transactionId = resolver(account.id, change.uuid())
                if (transactionId > -1) {
                    if (parentOffset > -1) {
                        //if we find a split part that already exists, we need to assume that it has already been synced
                        //by a previous sync of its transfer account, so all we do here is reparent it as child
                        //of the split transaction we currently ingest
                        ops.add(ContentProviderOperation.newUpdate(uri)
                                .withValues(toContentValues(change))
                                .withSelection(DatabaseConstants.KEY_ROWID + " = ?", arrayOf(transactionId.toString()))
                                .withValueBackReference(DatabaseConstants.KEY_PARENTID, parentOffset)
                                .build())
                        val tagOps: ArrayList<ContentProviderOperation> = saveTagLinks(tagIds, transactionId, null, true)
                        ops.addAll(tagOps)
                        tagOpsCount = tagOps.size
                    } else {
                        skipped = true
                        SyncAdapter.log().i("Uuid found in changes already exists locally, likely a transfer implicitly created from its peer")
                    }
                } else {
                    ops.addAll(getContentProviderOperationsForCreate(change, offset, parentOffset))
                    val tagOps: ArrayList<ContentProviderOperation> = saveTagLinks(tagIds, null, offset, true)
                    ops.addAll(tagOps)
                    tagOpsCount = tagOps.size
                }
            }
            TransactionChange.Type.updated -> {
                val values: ContentValues = toContentValues(change)
                if (values.size() > 0) {
                    val transactionId = resolver(account.id, change.uuid())
                    if (transactionId != -1L) {
                        val builder = ContentProviderOperation.newUpdate(uri)
                                .withSelection(DatabaseConstants.KEY_ROWID + " = ?", arrayOf(transactionId.toString()))
                        if (values.size() > 0) {
                            builder.withValues(values)
                        }
                        ops.add(builder.build())
                        val tagOps: ArrayList<ContentProviderOperation> = saveTagLinks(tagIds, transactionId, null, true)
                        ops.addAll(tagOps)
                        tagOpsCount = tagOps.size
                    }
                }
            }
            TransactionChange.Type.deleted -> {
                val transactionId = resolver(account.id, change.uuid())
                if (transactionId != -1L) {
                    ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(uri, transactionId))
                            .withSelection(DatabaseConstants.KEY_UUID + " = ? AND " + DatabaseConstants.KEY_ACCOUNTID + " = ?", arrayOf(change.uuid(), account.id.toString()))
                            .build())
                }
            }
            TransactionChange.Type.unsplit -> {
                ops.add(ContentProviderOperation.newUpdate(uri.buildUpon()
                        .appendPath(TransactionProvider.URI_SEGMENT_UNSPLIT).build())
                        .withValue(DatabaseConstants.KEY_UUID, change.uuid())
                        .build())
            }
            TransactionChange.Type.link -> {
                ops.add(ContentProviderOperation.newUpdate(uri.buildUpon()
                        .appendPath(TransactionProvider.URI_SEGMENT_LINK_TRANSFER)
                        .appendPath(change.uuid()).build())
                        .withValue(DatabaseConstants.KEY_UUID, change.referenceNumber())
                        .build())
            }
        }
        if (change.isCreateOrUpdate && !skipped) {
            change.splitParts()?.let { splitParts ->
                val newParentOffset = ops.size - 1 - tagOpsCount
                mergeChanges(filterDeleted(splitParts,
                        findDeletedUuids(splitParts))).forEach { splitChange: TransactionChange ->
                    if (change.uuid() != splitChange.parentUuid()) throw AssertionError()
                    //back reference is only used when we insert a new split,
                    //for updating an existing split we search for its _id via its uuid
                    collectOperations(splitChange, ops, if (change.isCreate) newParentOffset else -1)
                }
            }
        }
    }

    private fun findDeletedUuids(list: List<TransactionChange>): List<String> =
            list.filter { obj: TransactionChange -> obj.isDelete }
                    .map { obj: TransactionChange -> obj.uuid() }

    private fun toContentValues(change: TransactionChange): ContentValues {
        val values = ContentValues()
        change.comment()?.let { values.put(DatabaseConstants.KEY_COMMENT, it) }
        change.date()?.let { values.put(DatabaseConstants.KEY_DATE, it) }
        change.valueDate()?.let { values.put(DatabaseConstants.KEY_VALUE_DATE, it) }
        change.amount()?.let { values.put(DatabaseConstants.KEY_AMOUNT, it) }
        change.label()?.let { label ->
            extractCatId(label).takeIf { it != -1L }?.let { values.put(DatabaseConstants.KEY_CATID, it) }
        }
        change.payeeName()?.let { name ->
            Payee.extractPayeeId(name, payeeToId).takeIf { it != -1L }?.let { values.put(DatabaseConstants.KEY_PAYEEID, it) }
        }
        change.methodLabel()?.let { label ->
            extractMethodId(label).takeIf { it != -1L }?.let { values.put(DatabaseConstants.KEY_METHODID, it) }
        }
        change.crStatus()?.let { values.put(DatabaseConstants.KEY_CR_STATUS, it) }
        change.referenceNumber()?.let { values.put(DatabaseConstants.KEY_REFERENCE_NUMBER, it) }
        change.pictureUri()?.let { values.put(DatabaseConstants.KEY_PICTURE_URI, it) }
        if (change.originalAmount() != null && change.originalCurrency() != null) {
            values.put(DatabaseConstants.KEY_ORIGINAL_AMOUNT, change.originalAmount())
            values.put(DatabaseConstants.KEY_ORIGINAL_CURRENCY, change.originalCurrency())
        }
        if (change.equivalentAmount() != null && change.equivalentCurrency() != null) {
            val homeCurrency = Utils.getHomeCurrency()
            if (change.equivalentCurrency() == homeCurrency.code) {
                values.put(DatabaseConstants.KEY_EQUIVALENT_AMOUNT, change.equivalentAmount())
            }
        }
        return values
    }

    private fun extractCatId(label: String): Long {
        CategoryInfo(label).insert(categoryToId, false)
        return categoryToId[label] ?: -1
    }

    private fun extractMethodId(methodLabel: String): Long =
            methodToId[methodLabel] ?: (PaymentMethod.find(methodLabel).takeIf { it != -1L }
                    ?: PaymentMethod.maybeWrite(methodLabel, account.type)).also {
                methodToId[methodLabel] = it
            }

    private fun findTransferAccount(uuid: String): Long =
            accountUuidToId[uuid] ?: Account.findByUuid(uuid).also {
                if (it != -1L) {
                    accountUuidToId[uuid] = it
                }
            }

    private fun getContentProviderOperationsForCreate(
            change: TransactionChange, offset: Int, parentOffset: Int): ArrayList<ContentProviderOperation> {
        if (!change.isCreate) throw java.lang.AssertionError()
        val amount = change.amount() ?: 0L
        val money = Money(account.currencyUnit, amount)
        val t: Transaction = if (change.splitParts() != null) {
            SplitTransaction(account.id, money)
        } else {
            change.transferAccount()?.let { transferAccount ->
                //if the account exists locally and the peer has already been synced
                //we create a Transfer, the Transfer class will take care in buildSaveOperations
                //of linking them together
                findTransferAccount(transferAccount).takeIf { accountId -> resolver(accountId, change.uuid()) != -1L }?.let { Transfer(account.id, money, it) }
            } ?: Transaction(account.id, money).apply {
                if (change.transferAccount() == null) {
                    change.label()?.let { label ->
                        extractCatId(label).takeIf { it != -1L }?.let { catId = it }
                    }
                }
            }
        }
        t.uuid = change.uuid()
        change.comment()?.let { t.comment = it }
        change.date()?.let { t.date = it }
        t.valueDate = change.valueDate() ?: t.date
        change.payeeName()?.let { name ->
            Payee.extractPayeeId(name, payeeToId).takeIf { it != -1L }?.let { t.payeeId = it }
        }
        change.methodLabel()?.let { label ->
            extractMethodId(label).takeIf { it != -1L }?.let { t.methodId = it }
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
        change.pictureUri()?.let { t.pictureUri = Uri.parse(it) }
        change.originalAmount()?.let { originalAmount ->
            change.originalCurrency()?.let { originalCurrency ->
                t.originalAmount = Money(currencyContext[originalCurrency], originalAmount)
            }
        }
        change.equivalentAmount()?.let { equivalentAmount ->
            change.equivalentCurrency()?.let { equivalentCurrency ->
                with(Utils.getHomeCurrency()) {
                    if (equivalentCurrency == code) {
                        t.equivalentAmount = Money(this, equivalentAmount)
                    }
                }
            }
        }
        return t.buildSaveOperations(offset, parentOffset, true, false)
    }

    private fun filterDeleted(input: List<TransactionChange>, deletedUuids: List<String>): List<TransactionChange> {
        return input.filter { change: TransactionChange -> change.isDelete || !deletedUuids.contains(change.uuid()) }
    }

    fun findMetadataChange(input: List<TransactionChange>) =
            input.findLast { value: TransactionChange -> value.type() == TransactionChange.Type.metadata }

    fun removeMetadataChange(input: List<TransactionChange>) =
            input.filter { value: TransactionChange -> value.type() != TransactionChange.Type.metadata }

    fun concat(contentBuilders: List<CharSequence>) =
            contentBuilders.foldIndexed(StringBuilder(), { index, sum, element ->
                if (index > 0) {
                    sum.append("\n")
                }
                sum.append(element)
            })
}