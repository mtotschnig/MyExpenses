package org.totschnig.myexpenses.provider

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
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
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.UNSPLIT_URI
import org.totschnig.myexpenses.sync.SyncAdapter
import org.totschnig.myexpenses.sync.json.TransactionChange
import java.io.IOException

class SyncHandler(
    val accountId: Long,
    val currency: CurrencyUnit,
    val type: Long,
    val repository: Repository,
    val currencyContext: CurrencyContext,
    val resolver: (accountId: Long, transactionUUid: String) -> Long = repository.contentResolver::findByAccountAndUuid,
) {

    val homeCurrency = currencyContext.homeCurrencyUnit

    private val categoryToId: MutableMap<String, Long> = HashMap()
    private val payeeToId: MutableMap<String, Long> = HashMap()
    private val methodToId: MutableMap<String, Long> = HashMap()
    private val tagToId: MutableMap<String, Long> = HashMap()
    private val accountUuidToId: MutableMap<String, Long> = HashMap()

    fun collectOperations(
        change: TransactionChange,
        parentOffset: Int = -1,
    ): ArrayList<ContentProviderOperation> {
        val ops = ArrayList<ContentProviderOperation>()
        val uri = TRANSACTIONS_URI.fromSyncAdapter()
        var skipped = false
        val tagIds: List<Long>? = buildList {
            change.tags?.let { addAll(repository.extractTagIds(it, tagToId)) }
            change.tagsV2?.let { addAll(repository.extractTagIdsV2(it, tagToId)) }
        }.takeIf { it.isNotEmpty() }
        when (change.type) {
            TransactionChange.Type.created -> {
                val transactionId = resolver(accountId, change.uuid)
                if (transactionId > -1) {
                    if (parentOffset > -1) {
                        //if we find a split part that already exists, we need to assume that it has already been synced
                        //by a previous sync of its transfer account, so all we do here is reparent it as child
                        //of the split transaction we currently ingest
                        ops.add(
                            ContentProviderOperation.newUpdate(uri)
                                .withValues(change.toContentValues(false))
                                .withSelection(
                                    "$KEY_ROWID = ?",
                                    arrayOf(transactionId.toString())
                                )
                                .withValueBackReference(KEY_PARENTID, parentOffset)
                                .build()
                        )
                        tagIds?.let {
                            ops.addAll(saveTagLinks(it, null, 0))
                        }

                        change.attachments?.let {
                            ops.addAll(saveAttachmentLinks(it, null, 0))
                        }
                    } else {
                        skipped = true
                        SyncAdapter.log()
                            .i("Uuid found in changes already exists locally, likely a transfer implicitly created from its peer")
                    }
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(uri)
                            .withValues(change.toContentValues(true).apply {
                                put(KEY_ACCOUNTID, accountId)
                                if (parentOffset == -1) {
                                    change.parentUuid?.let {
                                        val parentId = resolver(accountId, it)
                                        if (parentId != -1L) {
                                            put(KEY_PARENTID, parentId)
                                        }
                                    }
                                }
                            }).apply {
                                if (parentOffset != -1) {
                                    withValueBackReference(KEY_PARENTID, parentOffset)
                                }
                            }
                            .build()
                    )
                    tagIds?.let {
                        ops.addAll(saveTagLinks(it, null, 0))
                    }

                    change.attachments?.let {
                        ops.addAll(saveAttachmentLinks(it, null, 0))
                    }
                }
            }

            TransactionChange.Type.updated -> {
                val values: ContentValues = change.toContentValues(false)
                val transactionId = resolver(accountId, change.uuid)
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
                                arrayOf(change.uuid)
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
                    tagIds?.let { list ->
                        ops.addAll(
                            saveTagLinks(
                                list,
                                transactionId.takeIf { it != -1L },
                                parentOffset.takeIf { it != -1 })
                        )
                    }
                    change.attachments?.let { set ->
                        ops.addAll(
                            saveAttachmentLinks(
                                set,
                                transactionId.takeIf { it != -1L },
                                parentOffset.takeIf { it != -1 }
                            )
                        )
                    }
                }
            }

            TransactionChange.Type.deleted -> {
                val transactionId = resolver(accountId, change.uuid)
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
                                arrayOf(change.uuid, accountId.toString())
                            )
                            .build()
                    )
                }
            }

            TransactionChange.Type.unsplit -> {
                ops.add(
                    ContentProviderOperation.newUpdate(UNSPLIT_URI)
                        .withValue(KEY_UUID, change.uuid)
                        .build()
                )
            }

            TransactionChange.Type.unarchive -> {
                ops.add(
                    ContentProviderOperation.newAssertQuery(TRANSACTIONS_URI)
                        .withSelection(
                            "$KEY_UUID = ? AND $KEY_STATUS == $STATUS_ARCHIVE",
                            arrayOf(change.uuid)
                        )
                        .withExpectedCount(1).build()
                )
                ops.add(
                    ContentProviderOperation.newUpdate(
                        uri.buildUpon()
                            .appendPath(TransactionProvider.URI_SEGMENT_UNARCHIVE).build()
                    )
                        .withValue(KEY_UUID, change.uuid)
                        .build()
                )
            }

            TransactionChange.Type.link -> {
                ops.add(
                    ContentProviderOperation.newUpdate(
                        uri.buildUpon()
                            .appendPath(TransactionProvider.URI_SEGMENT_LINK_TRANSFER)
                            .appendPath(change.uuid).build()
                    )
                        .withValue(KEY_UUID, change.referenceNumber)
                        .build()
                )
            }

            else -> {}
        }
        if (change.isCreateOrUpdate && !skipped) {
            change.splitParts?.let { splitParts ->
                splitParts.forEach { splitChange: TransactionChange ->
                    require(change.uuid == splitChange.parentUuid)
                    //back reference is only used when we insert a new split,
                    //for updating an existing split we search for its _id via its uuid
                    ops.addAll(
                        collectOperations(
                            splitChange,
                            if (change.isCreate) 0 else -1
                        )
                    )
                }
            }
        }
        return ops
    }

    private fun TransactionChange.toContentValues(forInsert: Boolean): ContentValues {
        val values = ContentValues()
        if (forInsert) {
            values.put(KEY_UUID, uuid)
        }
        amount?.let {
            values.put(KEY_AMOUNT, Money(currency, it).amountMinor)
        }
        transferAccount
            ?.let { findTransferAccount(it) }
            ?.let { transferAccountId ->
                values.put(KEY_TRANSFER_ACCOUNT, transferAccountId)
                if (forInsert) {
                    resolver(transferAccountId, uuid).takeIf { peer -> peer != -1L }?.let {
                        values.put(KEY_TRANSFER_PEER, it)
                    }
                }
            }

        comment?.let {
            if (it.isEmpty()) {
                values.putNull(KEY_COMMENT)
            } else {
                values.put(KEY_COMMENT, it)
            }
        }
        if (date != null || forInsert) {
            values.put(KEY_DATE, date ?: System.currentTimeMillis())
        }

        if (valueDate != null || forInsert) {
            values.put(KEY_VALUE_DATE, valueDate ?: date ?: System.currentTimeMillis())
        }
        amount?.let { values.put(KEY_AMOUNT, it) }
        if (splitParts?.isNotEmpty() == true) {
            values.put(KEY_CATID, SPLIT_CATID)
        } else if (categoryInfo?.firstOrNull()?.uuid == NULL_CHANGE_INDICATOR) {
            values.putNull(KEY_CATID)
        } else {
            this.extractCatId()?.let { values.put(KEY_CATID, it) }
        }
        if (payeeName == NULL_CHANGE_INDICATOR) {
            values.putNull(KEY_PAYEEID)
        } else {
            payeeName?.let { name ->
                extractParty(name)?.let {
                    values.put(KEY_PAYEEID, it)
                }
            }
        }
        if (methodLabel == NULL_CHANGE_INDICATOR) {
            values.putNull(KEY_METHODID)
        } else {
            methodLabel?.let { label ->
                values.put(KEY_METHODID, extractMethodId(label))
            }
        }
        crStatus?.let { values.put(KEY_CR_STATUS, it) }
        status?.let { values.put(KEY_STATUS, it) }
        referenceNumber?.let { values.put(KEY_REFERENCE_NUMBER, it) }
        if (originalAmount != null && originalCurrency != null) {
            if (originalAmount == Long.MIN_VALUE) {
                values.putNull(KEY_ORIGINAL_AMOUNT)
                values.putNull(KEY_ORIGINAL_CURRENCY)
            } else {
                values.put(KEY_ORIGINAL_AMOUNT, originalAmount)
                values.put(KEY_ORIGINAL_CURRENCY, originalCurrency)
            }
        }
        if (equivalentAmount!= null && equivalentCurrency != null) {
            if (equivalentAmount == Long.MIN_VALUE || equivalentCurrency != homeCurrency.code) {
                values.putNull(KEY_EQUIVALENT_AMOUNT)
            } else {
                values.put(KEY_EQUIVALENT_AMOUNT, equivalentAmount)
            }
        }
        return values
    }

    private fun saveTagLinks(
        tagIds: List<Long>,
        transactionId: Long?,
        backReference: Int? = null,
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
                    .withValue(KEY_TAGID, tagId)
            transactionId?.let {
                insert.withValue(KEY_TRANSACTIONID, it)
            } ?: backReference?.let {
                insert.withValueBackReference(KEY_TRANSACTIONID, it)
            } ?: throw IllegalArgumentException("neither id nor backReference provided")
            add(insert.build())
        }
    }

    private fun saveAttachmentLinks(
        attachments: Set<String>,
        transactionId: Long?,
        backReference: Int?,
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

    private fun TransactionChange.extractCatId(): Long? {
        return label?.let {
            CategoryHelper.insert(repository, it, categoryToId, false)
            categoryToId[it] ?: throw IOException("Saving category $it failed")
        } ?: categoryInfo?.let { repository.ensureCategoryPath(it) }
    }

    private fun extractParty(party: String): Long? =
        payeeToId[party] ?: repository.requireParty(party)?.also {
            payeeToId[party] = it
        }

    private fun extractMethodId(methodLabel: String): Long =
        methodToId[methodLabel] ?: (repository.findPaymentMethod(methodLabel).takeIf { it != -1L }
            ?: repository.writePaymentMethod(methodLabel, type)).also {
            methodToId[methodLabel] = it
        }

    private fun findTransferAccount(uuid: String): Long? =
        accountUuidToId[uuid] ?: repository.findAccountByUuid(uuid)?.also {
            accountUuidToId[uuid] = it
        }
}