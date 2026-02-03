package org.totschnig.myexpenses.sync

import android.content.ContentProviderClient
import android.content.Context
import android.content.OperationApplicationException
import android.os.Bundle
import android.os.RemoteException
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.json.Json
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.findByAccountAndUuid
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_TYPE
import org.totschnig.myexpenses.provider.SyncContract
import org.totschnig.myexpenses.provider.SyncContract.KEY_EXCEPTION
import org.totschnig.myexpenses.provider.SyncContract.KEY_RESULT
import org.totschnig.myexpenses.provider.SyncContract.METHOD_APPLY_CHANGES
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

    private val json = Json { ignoreUnknownKeys = true }

    @Throws(RemoteException::class, OperationApplicationException::class)
    fun writeRemoteChangesToDb(
        context: Context,
        provider: ContentProviderClient,
        remoteChanges: List<TransactionChange>,
    ) {
        if (remoteChanges.isNotEmpty()) {
            SyncContract.getSyncFile(context).writer().use { writer ->
                writer.write(json.encodeToString(remoteChanges).also {
                    Timber.d("Remote changes :\n%s", it)
                })
            }
            val result = provider.call(
                METHOD_APPLY_CHANGES,
                null, Bundle(3).apply {
                    putLong(KEY_ACCOUNTID, account.id)
                    putString(KEY_CURRENCY, account.currency)
                    putLong(KEY_TYPE, account.type.id)
                }
            )
            if (result?.getBoolean(KEY_RESULT) != true) {
                throw OperationApplicationException(result?.getString(KEY_EXCEPTION))
            }
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
            change.parentUuid?.let {
                ensureList(splitsPerUuid, it).add(change)
                i.remove()
            }
        }

        //When a split transaction is changed, we do not necessarily have an entry for the parent, so we
        //create one here
        splitsPerUuid.keys.forEach { uuid: String ->
            if (!changeList.any { change: TransactionChange -> change.uuid == uuid }) {
                splitsPerUuid[uuid]?.let { list ->
                    changeList.add(
                        TransactionChange(
                            type = TransactionChange.Type.updated,
                            timeStamp = list[0].timeStamp,
                            uuid = uuid
                        )
                    )
                }
            }
        }
        return changeList.map { change: TransactionChange ->
            if (change.type == TransactionChange.Type.unsplit) change else
                splitsPerUuid[change.uuid]?.let { parts ->
                    if (parts.all { it.parentUuid == change.uuid }) {
                        change.copy(splitParts = parts)
                    } else {
                        throw IllegalStateException("parts parentUuid does not match parents uuid")
                    }
                } ?: change
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
                ensureList(updatesPerUuid, change.uuid).add(
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
            change.takeIf { it.isCreateOrUpdate }?.nullifyIfNeeded(mergedMap[change.uuid])
                ?: change
        }.distinct()
    }

    /**
     * For all fields in a give change log entry, we compare if the final merged change has a different
     * non-null value, in which case we set the field to null. This prevents a value that has already
     * been overwritten from being written again either remotely or locally
     */
    private fun TransactionChange.nullifyIfNeeded(merged: TransactionChange?): TransactionChange {
        if (merged == null) return this
        return this.copy(
            comment = if (comment != null && merged.comment != null && merged.comment != comment) null else comment,
            date = if (date != null && merged.date != null && merged.date != date) null else date,
            valueDate = if (valueDate != null && merged.valueDate != null && merged.valueDate != valueDate) null else valueDate,
            amount = if (amount != null && merged.amount != null && merged.amount != amount) null else amount,
            label = if ((label != null || categoryInfo != null) && (merged.label != null || merged.categoryInfo != null) && (merged.label != label || merged.categoryInfo != categoryInfo)) null else label,
            categoryInfo = if ((label != null || categoryInfo != null) && (merged.label != null || merged.categoryInfo != null) && (merged.label != label || merged.categoryInfo != categoryInfo)) null else categoryInfo,
            payeeName = if (payeeName != null && merged.payeeName != null && merged.payeeName != payeeName) null else payeeName,
            transferAccount = if (transferAccount != null && merged.transferAccount != null && merged.transferAccount != transferAccount) null else transferAccount,
            methodLabel = if (methodLabel != null && merged.methodLabel != null && merged.methodLabel != methodLabel) null else methodLabel,
            crStatus = if (crStatus != null && merged.crStatus != null && merged.crStatus != crStatus) null else crStatus,
            status = if (status != null && merged.status != null && merged.status != status) null else status,
            referenceNumber = if (referenceNumber != null && merged.referenceNumber != null && merged.referenceNumber != referenceNumber) null else referenceNumber,
            pictureUri = if ((pictureUri != null || attachments != null) && (merged.pictureUri != null || merged.attachments != null) && (merged.pictureUri != pictureUri || merged.attachments != attachments)) null else pictureUri,
            attachments = if ((pictureUri != null || attachments != null) && (merged.pictureUri != null || merged.attachments != null) && (merged.pictureUri != pictureUri || merged.attachments != attachments)) null else attachments,
            splitParts = if (splitParts != null && merged.splitParts != null) {
                splitParts.map { part ->
                    merged.splitParts.find { it.uuid == part.uuid }?.let { mergedPart ->
                        part.nullifyIfNeeded(mergedPart)
                    } ?: part
                }
            } else splitParts,
            tags = if ((tags != null || tagsV2 != null) && (merged.tags != null || merged.tagsV2 != null) && (merged.tags != tags || merged.tagsV2 != tagsV2)) null else tags,
            tagsV2 = if ((tags != null || tagsV2 != null) && (merged.tags != null || merged.tagsV2 != null) && (merged.tags != tags || merged.tagsV2 != tagsV2)) null else tagsV2
        )
    }

    private fun mergeChanges(input: List<TransactionChange>): List<TransactionChange> =
        input.groupBy(TransactionChange::uuid).flatMap { entry ->
            val (cudItems, specials) = entry.value.partition { it.isCUD }
            if (cudItems.isEmpty()) specials else
                listOf(mergeUpdates(cudItems)) + specials
        }


    @VisibleForTesting
    fun mergeUpdates(changeList: List<TransactionChange>): TransactionChange {
        check(changeList.isNotEmpty()) { "nothing to merge" }
        return changeList
            .sortedBy { if (it.isCreate) 0L else it.timeStamp }
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
        check(initial.uuid == change.uuid) { "Can only merge changes with same uuid" }
        if (initial.isDelete) return initial
        if (change.isDelete) return change

        var result = initial
        if (change.parentUuid != null) result = result.copy(parentUuid = change.parentUuid)
        if (change.comment != null) result = result.copy(comment = change.comment)
        if (change.date != null) result = result.copy(date = change.date)
        if (change.valueDate != null) result = result.copy(valueDate = change.valueDate)
        if (change.amount != null) result = result.copy(amount = change.amount)
        if (change.label != null) result = result.copy(label = change.label)
        if (change.payeeName != null) result = result.copy(payeeName = change.payeeName)
        if (change.transferAccount != null) result =
            result.copy(transferAccount = change.transferAccount)
        if (change.methodLabel != null) result = result.copy(methodLabel = change.methodLabel)
        if (change.crStatus != null) result = result.copy(crStatus = change.crStatus)
        if (change.status != null) result = result.copy(status = change.status)
        if (change.referenceNumber != null) result =
            result.copy(referenceNumber = change.referenceNumber)
        if (change.pictureUri != null) result = result.copy(pictureUri = change.pictureUri)
        if (change.splitParts != null) {
            val merged = mergeChanges((initial.splitParts ?: emptyList()) + change.splitParts)
            result = result.copy(splitParts = merged)
        }
        if (change.tags != null) result = result.copy(tags = change.tags)
        if (change.tagsV2 != null) result = result.copy(tagsV2 = change.tagsV2)
        if (change.attachments != null) result = result.copy(attachments = change.attachments)
        if (change.categoryInfo != null) result = result.copy(categoryInfo = change.categoryInfo)

        return result.withCurrentTimeStamp()
    }

    private fun findDeletedUuids(list: List<TransactionChange>): List<String> =
        list.filter { it.isDelete }.map { it.uuid }

    private fun filterDeleted(
        input: List<TransactionChange>,
        deletedUuids: List<String>,
    ): List<TransactionChange> {
        return input.filter { change: TransactionChange ->
            change.isDelete || !deletedUuids.contains(change.uuid)
        }
    }

    fun findMetadataChange(input: List<TransactionChange>) =
        input.findLast { it.type == TransactionChange.Type.metadata }

    fun removeMetadataChange(input: List<TransactionChange>) =
        input.filter { it.type != TransactionChange.Type.metadata }

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
