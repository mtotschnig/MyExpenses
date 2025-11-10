package org.totschnig.myexpenses.sync

import android.content.ContentProviderClient
import android.content.Context
import android.content.OperationApplicationException
import android.os.Bundle
import android.os.RemoteException
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.findByAccountAndUuid
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.SyncContract
import org.totschnig.myexpenses.sync.json.AdapterFactory
import org.totschnig.myexpenses.sync.json.TransactionChange
import timber.log.Timber

class SyncDelegate(
    val currencyContext: CurrencyContext,
    val featureManager: FeatureManager,
    val repository: Repository,
    val homeCurrency: CurrencyUnit,
    val resolver: (accountId: Long, transactionUUid: String) -> Long = repository.contentResolver::findByAccountAndUuid,
) {

    lateinit var account: Account

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create()

    @Throws(RemoteException::class, OperationApplicationException::class)
    fun writeRemoteChangesToDb(
        context: Context,
        provider: ContentProviderClient,
        remoteChanges: List<TransactionChange>,
    ) {
        if (remoteChanges.isNotEmpty()) {
            SyncContract.getSyncFile(context).writer().use { writer ->
                writer.write(gson.toJson(remoteChanges).also {
                    Timber.d("Remote changes :\n%s", it)
                })
            }

            provider.call(
                SyncContract.METHOD_APPLY_CHANGES,
                null, Bundle(3).apply {
                    putLong(KEY_ACCOUNTID, account.id)
                    putString(KEY_CURRENCY, account.currency)
                    putLong(KEY_TYPE, account.type.id)
                }
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
        first: List<TransactionChange>, second: List<TransactionChange>,
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
        uuid: String,
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
        mergedMap: HashMap<String, TransactionChange>,
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
        change: TransactionChange,
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
            val merged = mergeChanges(
                (initial.splitParts() ?: emptyList()) + (change.splitParts() ?: emptyList())
            )
            builder.setSplitParts(merged)
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

    private fun findDeletedUuids(list: List<TransactionChange>): List<String> =
        list.filter { obj: TransactionChange -> obj.isDelete }
            .map { obj: TransactionChange -> obj.uuid() }

    private fun filterDeleted(
        input: List<TransactionChange>,
        deletedUuids: List<String>,
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