package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.core.os.BundleCompat
import org.totschnig.myexpenses.db2.Repository.Companion.RECORD_SEPARATOR
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.dialog.ArchiveInfo
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.uriBuilderForTransactionList
import org.totschnig.myexpenses.provider.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.KEY_AMOUNT
import org.totschnig.myexpenses.provider.KEY_CATID
import org.totschnig.myexpenses.provider.KEY_COMMENT
import org.totschnig.myexpenses.provider.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.KEY_CURRENCY
import org.totschnig.myexpenses.provider.KEY_DATE
import org.totschnig.myexpenses.provider.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.KEY_END
import org.totschnig.myexpenses.provider.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.KEY_HAS_SEALED_ACCOUNT_WITH_TRANSFER
import org.totschnig.myexpenses.provider.KEY_HAS_SEALED_DEBT
import org.totschnig.myexpenses.provider.KEY_ICON
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_METHODID
import org.totschnig.myexpenses.provider.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.KEY_PARENTID
import org.totschnig.myexpenses.provider.KEY_PAYEEID
import org.totschnig.myexpenses.provider.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.KEY_START
import org.totschnig.myexpenses.provider.KEY_STATUS
import org.totschnig.myexpenses.provider.KEY_TAGLIST
import org.totschnig.myexpenses.provider.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.KEY_UUID
import org.totschnig.myexpenses.provider.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.VIEW_EXTENDED
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT_PART
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_VOID
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.SPLIT_CATID
import org.totschnig.myexpenses.provider.TABLE_TRANSACTIONS
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.AUTHORITY
import org.totschnig.myexpenses.provider.TransactionProvider.EXTENDED_URI
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_ARCHIVE
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_CAN_BE_ARCHIVED
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_DISTINCT
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_GROUP_BY
import org.totschnig.myexpenses.provider.TransactionProvider.QUERY_PARAMETER_TRANSACTION_ID_LIST
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_UNARCHIVE
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.filter.Operation
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getLongOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.provider.withLimit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.joinArrays
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

fun Transaction.asContentValues(forInsert: Boolean) = ContentValues().apply {
    put(KEY_COMMENT, comment)
    put(KEY_DATE, date)
    put(KEY_VALUE_DATE, valueDate)
    put(KEY_AMOUNT, amount)
    put(KEY_CATID, categoryId)
    put(KEY_ACCOUNTID, accountId)
    put(KEY_PAYEEID, payeeId?.takeIf { it > 0L })
    put(KEY_TRANSFER_ACCOUNT, transferAccountId?.takeIf { it > 0L })
    put(KEY_METHODID, methodId?.takeIf { it > 0L })
    put(KEY_PARENTID, parentId?.takeIf { it > 0L })
    put(KEY_CR_STATUS, crStatus.name)
    put(KEY_REFERENCE_NUMBER, referenceNumber)
    put(KEY_ORIGINAL_AMOUNT, originalAmount)
    put(KEY_ORIGINAL_CURRENCY, originalCurrency)
    put(KEY_EQUIVALENT_AMOUNT, equivalentAmount)
    put(KEY_DEBT_ID, debtId?.takeIf { it > 0L })
    if (forInsert) {
        require(uuid.isNotBlank())
        put(KEY_UUID, uuid)
    }
    put(KEY_TAGLIST, tagList.joinToString("$RECORD_SEPARATOR"))
}

data class RepositoryTransaction(
    val data: Transaction,
    val transferPeer: Transaction? = null,
    val splitParts: List<RepositoryTransaction>? = null,
    val tags: List<Tag>? = null,
) {
    val id = data.id
    val isTransfer = transferPeer != null
    val isSplit = splitParts != null
}

fun Repository.createTransaction(repositoryTransaction: RepositoryTransaction) = when {

    repositoryTransaction.isTransfer -> createTransfer(
        repositoryTransaction.data,
        repositoryTransaction.transferPeer!!
    )

    repositoryTransaction.isSplit -> createSplitTransaction(
        repositoryTransaction.data,
        repositoryTransaction.splitParts!!.map { it.data to it.transferPeer }
    )

    else -> createTransaction(repositoryTransaction.data)
}

fun Repository.updateTransaction(repositoryTransaction: RepositoryTransaction): Array<ContentProviderResult> =
    when {

        repositoryTransaction.isTransfer -> updateTransfer(
            repositoryTransaction.data,
            repositoryTransaction.transferPeer!!
        )

        repositoryTransaction.isSplit -> updateSplitTransaction(repositoryTransaction)

        else -> updateTransaction(repositoryTransaction.data)
    }

fun Repository.createTransaction(transaction: Transaction): RepositoryTransaction {
    require(transaction.id == 0L) { "Use updateTransaction for existing transactions" }
    require(transaction.transferAccountId == null) { "Use createTransfer instead" }
    require(transaction.categoryId != SPLIT_CATID) { "Use createSplitTransaction instead" }
    require((transaction.originalAmount != null) == (transaction.originalCurrency != null)) {
        "originalAmount and originalCurrency must be set together"
    }
    requireNotNull(transaction.uuid)
    val id = ContentUris.parseId(
        contentResolver.insert(
            TRANSACTIONS_URI,
            transaction.asContentValues(true)
        )!!
    )
    return RepositoryTransaction(
        transaction.copy(id = id)
    )
}

fun Repository.updateTransaction(
    transaction: Transaction,
) = contentResolver.applyBatch(
    AUTHORITY,
    ArrayList<ContentProviderOperation>().apply {
        add(
            ContentProviderOperation.newUpdate(
                ContentUris.withAppendedId(TRANSACTIONS_URI, transaction.id)
            )
                .withValues(transaction.asContentValues(false))
                .build()
        )
    }
)

fun Repository.createTransfer(
    sourceTransaction: Transaction,
    destinationTransaction: Transaction,
): RepositoryTransaction {
    require(sourceTransaction.date == destinationTransaction.date)
    requireNotNull(sourceTransaction.uuid)
    requireNotNull(sourceTransaction.uuid == destinationTransaction.uuid)
    val operations = ArrayList<ContentProviderOperation>()

    operations.addAll(
        getTransferOperations(
            sourceTransaction,
            destinationTransaction,
            0
        )
    )

    val results = contentResolver.applyBatch(AUTHORITY, operations)

    val first = ContentUris.parseId(results[0].uri!!)
    val second = ContentUris.parseId(results[1].uri!!)

    return RepositoryTransaction(
        sourceTransaction.copy(id = first, transferPeerId = second),
        destinationTransaction.copy(id = second, transferPeerId = first)
    )
}

fun Repository.updateTransfer(
    sourceTransaction: Transaction,
    destinationTransaction: Transaction,
): Array<ContentProviderResult> {
    require(
        sourceTransaction.transferAccountId == destinationTransaction.accountId &&
                sourceTransaction.accountId == destinationTransaction.transferAccountId
    )
    require(
        sourceTransaction.transferPeerId == destinationTransaction.id &&
                sourceTransaction.id == destinationTransaction.transferPeerId
    )

    val operations = ArrayList<ContentProviderOperation>()
    val destinationUri = ContentUris.withAppendedId(TRANSACTIONS_URI, destinationTransaction.id)

    val destinationOriginalStatus = destinationTransaction.status
    val tempStatusValues = ContentValues(1).apply {
       put(KEY_STATUS, -1)
    }
    operations.add(
        ContentProviderOperation
            .newUpdate(destinationUri)
            .withValues(tempStatusValues).build()
    )

    operations.add(
        ContentProviderOperation.newUpdate(
            ContentUris.withAppendedId(
                TRANSACTIONS_URI,
                sourceTransaction.id
            )
        )
            .withValues(sourceTransaction.asContentValues(false))
            .build()
    )

    operations.add(
        ContentProviderOperation.newUpdate(destinationUri)
            .withValues(destinationTransaction.asContentValues(false))
            .withValue(KEY_STATUS, destinationOriginalStatus)
            .build()
    )
    return contentResolver.applyBatch(AUTHORITY, operations)
}

@VisibleForTesting
fun Repository.createSplitTransaction(
    parentTransaction: Transaction,
    splitTransactions: List<Transaction>,
): RepositoryTransaction = createSplitTransaction(
    parentTransaction,
    splitTransactions.map { it to null }
)

@VisibleForTesting
@JvmName("createSplitTransactionWithTransfers")
fun Repository.createSplitTransaction(
    parentTransaction: Transaction,
    splitParts: List<Pair<Transaction, Transaction?>>,
): RepositoryTransaction {
    // --- Validation ---
    require(parentTransaction.isSplit) { "Parent transaction must be a split." }
    require(splitParts.sumOf { it.first.amount } == parentTransaction.amount) { "Sum of splits must equal parent amount." }
    require(splitParts.all { it.first.date == parentTransaction.date && (it.second == null || it.second!!.date == parentTransaction.date) }) {
        "Split transactions date must match parent date."
    }
    require(splitParts.all { it.first.accountId == parentTransaction.accountId }) { "All splits must be in the same account." }

    requireNotNull(parentTransaction.uuid)
    require(splitParts.all { it.second == null || it.second?.uuid == it.first.uuid })

    val operations = ArrayList<ContentProviderOperation>()

    // --- Operation 0: Insert the Parent Transaction ---
    operations.add(
        ContentProviderOperation.newInsert(TRANSACTIONS_URI)
            .withValues(parentTransaction.asContentValues(true))
            .build()
    )
    val parentBackRefIndex = 0

    // Prepare to build the complete return objects
    val finalSplitParts = mutableListOf<RepositoryTransaction>()

    // --- Process each split part ---
    splitParts.forEach { (splitPart, peer) ->
        if (peer == null) {
            // --- This is a REGULAR split part ---
            require(splitPart.transferAccountId == null)
            operations.add(
                ContentProviderOperation.newInsert(TRANSACTIONS_URI)
                    .withValues(splitPart.asContentValues(true))
                    .withValueBackReference(KEY_PARENTID, parentBackRefIndex)
                    .build()
            )
            // Prepare the final object, ID will be filled in later
            finalSplitParts.add(RepositoryTransaction(splitPart))
        } else {
            // --- This is a TRANSFER split part ---
            operations.addAll(
                getTransferOperations(
                    splitPart,
                    peer,
                    offset = operations.size, // Pass the current absolute offset
                    parentBackRefIndex = parentBackRefIndex // Pass the parent's index
                )
            )
            // Prepare the final objects, IDs will be filled in later
            finalSplitParts.add(
                RepositoryTransaction(
                    splitPart,
                    peer
                )
            )
        }
    }

    // --- Atomically execute all operations ---
    val results = contentResolver.applyBatch(AUTHORITY, operations)

    // --- Construct the final return object ---
    val parentId = ContentUris.parseId(results[0].uri!!)
    val finalParent = parentTransaction.copy(id = parentId)

    var resultIndex = 1 // Start processing results after the parent
    val enrichedSplitParts = finalSplitParts.map { (splitPart, peer) ->
        if (peer == null) {
            // Regular split part
            val newId = ContentUris.parseId(results[resultIndex].uri!!)
            resultIndex++
            RepositoryTransaction(splitPart.copy(id = newId, parentId = parentId))
        } else {
            // Transfer split part
            val sourceId = ContentUris.parseId(results[resultIndex].uri!!)
            val peerId = ContentUris.parseId(results[resultIndex + 1].uri!!)
            resultIndex += 2
            RepositoryTransaction(
                splitPart.copy(
                    id = sourceId,
                    parentId = parentId,
                    transferPeerId = peerId
                ), peer.copy(
                    id = peerId,
                    transferPeerId = sourceId
                )
            )
        }
    }
    return RepositoryTransaction(finalParent, splitParts = enrichedSplitParts)
}


fun Repository.updateSplitTransaction(repositoryTransaction: RepositoryTransaction): Array<ContentProviderResult> {
    // --- Validation ---
    val parentTransaction = repositoryTransaction.data
    require(parentTransaction.isSplit) { "Parent transaction must be a split." }
    val splitParts = repositoryTransaction.splitParts!!
    require(splitParts.sumOf { it.data.amount } == parentTransaction.amount) { "Sum of splits must equal parent amount." }
    require(splitParts.all { it.data.date == parentTransaction.date }) {
        "Split transactions date must match parent date."
    }
    require(splitParts.all { it.data.accountId == parentTransaction.accountId }) { "All splits must be in the same account." }

    val operations = ArrayList<ContentProviderOperation>()

    // --- 1. Handle Deletions ---
    // Get IDs of parts that have a non-zero ID (i.e., they already exist in the DB).
    val keepIds = splitParts.mapNotNull { if (it.id != 0L) it.id else null }
    val placeholders = List(keepIds.size) { "?" }.joinToString(",")

    val deleteSubquery =
        "SELECT $KEY_ROWID FROM $TABLE_TRANSACTIONS WHERE $KEY_PARENTID = ? AND $KEY_ROWID NOT IN ($placeholders)"

    val selection = "$KEY_ROWID IN ($deleteSubquery) OR $KEY_TRANSFER_PEER IN ($deleteSubquery)"

    // The selection arguments need to be duplicated because the subquery appears twice.
    val baseArgs = arrayOf(parentTransaction.id.toString())
    val keepArgs = keepIds.map { it.toString() }.toTypedArray()
    val selectionArgs = baseArgs + keepArgs + baseArgs + keepArgs

    operations.add(
        ContentProviderOperation.newDelete(TRANSACTIONS_URI)
            .withSelection(selection, selectionArgs)
            .build()
    )

    // --- 2. Update Parent Transaction ---
    operations.add(
        ContentProviderOperation.newUpdate(
            ContentUris.withAppendedId(TRANSACTIONS_URI, parentTransaction.id)
        )
            .withValues(parentTransaction.asContentValues(false))
            .build()
    )

    for (transaction in splitParts) {
        if (transaction.id == 0L) {
            if (transaction.isTransfer) {
                operations.addAll(
                    getTransferOperations(
                        source = transaction.data,
                        destination = transaction.transferPeer!!,
                        offset = operations.size,
                        parentId = parentTransaction.id
                    )
                )
            } else {
                operations.add(
                    ContentProviderOperation.newInsert(TRANSACTIONS_URI)
                        .withValues(
                            transaction.data.asContentValues(true)
                                .apply {
                                    put(KEY_PARENTID, parentTransaction.id)
                                })
                        .build()
                )
            }
        } else {
            operations.add(
                ContentProviderOperation.newUpdate(
                    ContentUris.withAppendedId(TRANSACTIONS_URI, transaction.id)
                )
                    .withValues(transaction.data.asContentValues(false)).build()
            )
            transaction.transferPeer?.let {
                operations.add(
                    ContentProviderOperation.newUpdate(
                        ContentUris.withAppendedId(TRANSACTIONS_URI, it.id)
                    )
                        .withValues(it.asContentValues(false)).build()
                )
            }
        }
    }

    return contentResolver.applyBatch(AUTHORITY, operations)
}

suspend fun Repository.loadTransactions(
    accountId: Long,
    limit: Int? = 200,
    withTags: Boolean = false,
): List<Transaction> {
    val filter = FilterPersistence(
        dataStore = dataStore,
        prefKey = MyExpensesViewModel.prefNameForCriteria(accountId),
    ).getValue()?.let {
        it.getSelectionForParents() to it.getSelectionArgs(false)
    }
    //noinspection Recycle
    return contentResolver.query(
        uriBuilderForTransactionList(accountId = accountId, currency = null)
            .build()
            .let { if (limit != null) it.withLimit(limit) else it },
        Transaction.projection.let { if (withTags) it + KEY_TAGLIST else it },
        "$KEY_PARENTID IS NULL" + (filter?.first?.takeIf { it != "" }?.let { " AND $it" } ?: ""),
        filter?.second,
        null
    )!!.useAndMapToList { cursor -> Transaction.fromCursor(cursor) }
}

fun Repository.loadTransaction(
    transactionId: Long,
    withTransfer: Boolean = true,
    withTags: Boolean = false,
): RepositoryTransaction = contentResolver.query(
    ContentUris.withAppendedId(TRANSACTIONS_URI, transactionId),
    Transaction.projection,
    null,
    null,
    null
)!!.use { cursor ->
    if (cursor.moveToFirst()) Transaction.fromCursor(cursor).let {
        RepositoryTransaction(
            data = it,
            transferPeer = if (withTransfer && it.transferPeerId != null) loadTransaction(
                it.transferPeerId,
                false
            ).data else null,
            splitParts = if (it.isSplit) loadSplitParts(it.id).map { split ->
                RepositoryTransaction(
                    split,
                    transferPeer = if (withTransfer && split.transferPeerId != null) loadTransaction(
                        split.transferPeerId,
                        false
                    ).data else null,
                    tags = if (withTags) loadTagsForTransaction(split.id) else null
                )
            } else null,
            tags = if (withTags) loadTagsForTransaction(transactionId) else null
        )
    } else
        throw IllegalArgumentException("Transaction not found")
}

private fun Repository.loadSplitParts(transactionId: Long): List<Transaction> =
    contentResolver.query(
        TRANSACTIONS_URI.buildUpon().appendQueryParameter(
            KEY_PARENTID, transactionId.toString()
        ).build(), Transaction.projection, null, null, null
    )!!.useAndMapToList {
        Transaction.fromCursor(it)
    }

@VisibleForTesting
fun Repository.transactionExists(transactionId: Long) = contentResolver.query(
    ContentUris.withAppendedId(TRANSACTIONS_URI, transactionId), null, null, null, null
)!!.use {
    it.count == 1
}

fun Repository.getTransactionSum(account: DataBaseAccount, filter: Criterion? = null) =
    getTransactionSum(account.id, account.currency, filter)

fun Repository.getTransactionSum(
    id: Long,
    currency: String? = null,
    filter: Criterion? = null,
): Long {
    var selection =
        "$KEY_ACCOUNTID = ? AND $WHERE_NOT_SPLIT_PART AND $WHERE_NOT_VOID"
    var selectionArgs: Array<String>? = arrayOf(id.toString())
    if (filter != null) {
        selection += " AND " + filter.getSelectionForParents()
        selectionArgs = joinArrays(selectionArgs, filter.getSelectionArgs(false))
    }
    return contentResolver.query(
        uriBuilderForTransactionList(id, currency, extended = false).build(),
        arrayOf("${DbUtils.aggregateFunction(prefHandler)}($KEY_AMOUNT)"),
        selection,
        selectionArgs,
        null
    )!!.use {
        it.moveToFirst()
        it.getLong(0)
    }
}


fun Repository.archive(
    accountId: Long,
    range: Pair<LocalDate, LocalDate>,
) = contentResolver.call(TransactionProvider.DUAL_URI, METHOD_ARCHIVE, null, Bundle().apply {
    putLong(KEY_ACCOUNTID, accountId)
    putSerializable(KEY_START, range.first)
    putSerializable(KEY_END, range.second)
})!!.getLong(KEY_TRANSACTIONID)

fun Repository.unarchive(id: Long) {
    val ops = ArrayList<ContentProviderOperation>().apply {
        add(
            ContentProviderOperation.newAssertQuery(
                ContentUris.withAppendedId(TRANSACTIONS_URI, id)
            )
                .withSelection("$KEY_STATUS = $STATUS_ARCHIVE", null)
                .withExpectedCount(1).build()
        )
        add(
            ContentProviderOperation.newUpdate(
                TRANSACTIONS_URI.buildUpon().appendPath(URI_SEGMENT_UNARCHIVE).build()
            )
                .withValue(KEY_ROWID, id)
                .build()
        )
    }
    val result = contentResolver.applyBatch(AUTHORITY, ops)
    val affectedRows = result[1].count
    if (affectedRows != 1) {
        CrashHandler.report(Exception("Unarchive returned $affectedRows affected rows"))
    }
}

fun Repository.canBeArchived(
    accountId: Long,
    range: Pair<LocalDate, LocalDate>,
) = BundleCompat.getParcelable(
    contentResolver.call(
        TransactionProvider.DUAL_URI,
        METHOD_CAN_BE_ARCHIVED,
        null,
        Bundle().apply {
            putLong(KEY_ACCOUNTID, accountId)
            putSerializable(KEY_START, range.first)
            putSerializable(KEY_END, range.second)
        })!!, KEY_RESULT, ArchiveInfo::class.java
)!!

fun Repository.countTransactionsPerAccount(
    accountId: Long,
) = count(
    TRANSACTIONS_URI,
    "$KEY_ACCOUNTID = ? AND $KEY_PARENTID is null",
    arrayOf(accountId.toString())
)

fun ContentResolver.findByAccountAndUuid(accountId: Long, uuid: String) = findBySelection(
    "$KEY_UUID = ? AND $KEY_ACCOUNTID = ?",
    arrayOf(uuid, accountId.toString()),
    KEY_ROWID
)

fun Repository.hasSealed(accountId: Long) = contentResolver.query(
    TRANSACTIONS_URI.buildUpon().appendQueryParameter(
        TransactionProvider.QUERY_PARAMETER_INCLUDE_ALL, "1"
    ).build(),
    arrayOf(
        KEY_HAS_SEALED_ACCOUNT_WITH_TRANSFER,
        KEY_HAS_SEALED_DEBT
    ),
    "$KEY_ACCOUNTID = ?",
    arrayOf(accountId.toString()),
    null
)!!.use {
    it.moveToFirst()
    it.getBoolean(0) to it.getBoolean(1)
}

fun Repository.getPayeeForTransaction(id: Long) = contentResolver.findBySelection(
    "$KEY_ROWID = ?",
    arrayOf(id.toString()),
    KEY_PAYEEID
)

private fun ContentResolver.findBySelection(
    selection: String,
    selectionArgs: Array<String>,
    column: String,
) =
    query(
        TRANSACTIONS_URI
            .buildUpon()
            .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_INCLUDE_ALL, "1")
            .build(),
        arrayOf(column),
        selection,
        selectionArgs,
        null
    )?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    } ?: -1

fun Repository.calculateSplitSummary(id: Long): List<Pair<String, String?>>? {
    return contentResolver.query(
        TransactionProvider.CATEGORIES_URI.buildUpon()
            .appendQueryParameter(KEY_TRANSACTIONID, id.toString()).build(),
        arrayOf(KEY_LABEL, KEY_ICON), null, null, null
    )
        ?.useAndMapToList {
            it.getString(KEY_LABEL) to it.getStringOrNull(KEY_ICON)
        }?.takeIf { it.isNotEmpty() }
}

fun Repository.insertTransaction(
    accountId: Long,
    amount: Long,
    parentId: Long? = null,
    categoryId: Long? = null,
    crStatus: CrStatus = CrStatus.UNRECONCILED,
    date: LocalDateTime = LocalDateTime.now(),
    equivalentAmount: Long? = null,
    originalAmount: Long? = null,
    originalCurrency: String? = null,
    payeeId: Long? = null,
    comment: String? = null,
    methodId: Long? = null,
    referenceNumber: String? = null,
    debtId: Long? = null,
): RepositoryTransaction = createTransaction(
    Transaction(
        accountId = accountId,
        amount = amount,
        categoryId = categoryId,
        crStatus = crStatus,
        parentId = parentId,
        date = date.toEpoch(),
        equivalentAmount = equivalentAmount,
        payeeId = payeeId,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        comment = comment,
        methodId = methodId,
        referenceNumber = referenceNumber,
        debtId = debtId,
        uuid = generateUuid()
    )
)

fun Repository.insertTransfer(
    accountId: Long,
    transferAccountId: Long,
    amount: Long,
    transferAmount: Long = -amount,
    parentId: Long? = null,
    categoryId: Long? = prefHandler.defaultTransferCategory,
    crStatus: CrStatus = CrStatus.UNRECONCILED,
    date: LocalDateTime = LocalDateTime.now(),
    payeeId: Long? = null,
    comment: String? = null,
    uuid: String = generateUuid(),
): RepositoryTransaction = createTransfer(
    Transaction(
        accountId = accountId,
        transferAccountId = transferAccountId,
        amount = amount,
        categoryId = categoryId,
        crStatus = crStatus,
        parentId = parentId,
        date = date.toEpoch(),
        payeeId = payeeId,
        comment = comment,
        uuid = uuid
    ), Transaction(
        accountId = transferAccountId,
        transferAccountId = accountId,
        amount = transferAmount,
        categoryId = categoryId,
        crStatus = crStatus,
        parentId = parentId,
        date = date.toEpoch(),
        payeeId = payeeId,
        comment = comment,
        uuid = uuid
    )
)

/**
 * Generates the three ContentProviderOperations needed to atomically create a linked transfer.
 *
 * @param source The outgoing part of the transfer.
 * @param destination The incoming part of the transfer.
 * @param offset The starting index where these operations will be placed in the final batch list.
 * @param parentBackRefIndex If the transfer is part of a split, this is the back-reference index to the parent transaction.
 * @return A list of three operations for creating the transfer.
 */
private fun getTransferOperations(
    source: Transaction,
    destination: Transaction,
    offset: Int,
    parentBackRefIndex: Int? = null,
    parentId: Long? = null,
): List<ContentProviderOperation> {
    // The builder for the source transaction, which may or may not be linked to a parent.
    require(
        source.transferAccountId == destination.accountId &&
                source.accountId == destination.transferAccountId
    ) { "Account IDs must match." }
    require(source.date == destination.date)

    return listOf(
        // Operation at index (offset + 0): Insert the source transaction.
        ContentProviderOperation.newInsert(TRANSACTIONS_URI)
            .withValues(source.asContentValues(true))
            .apply {
                if (parentBackRefIndex != null) {
                    withValueBackReference(KEY_PARENTID, parentBackRefIndex)
                }
                if (parentId != null) {
                    withValue(KEY_PARENTID, parentId)
                }
            }.build(),

        // Operation at index (offset + 1): Insert the destination, linking to the source.
        ContentProviderOperation.newInsert(TRANSACTIONS_URI)
            .withValues(destination.asContentValues(true))
            .withValueBackReference(KEY_TRANSFER_PEER, offset)
            .build(),

        //the source is updated by trigger TRANSFER_PEER_TRIGGER

    )
}

fun Repository.undeleteTransaction(id: Long): Int {
    val uri = ContentUris.appendId(TRANSACTIONS_URI.buildUpon(), id)
        .appendPath(TransactionProvider.URI_SEGMENT_UNDELETE)
        .build()
    return contentResolver.update(uri, null, null, null)
}

fun Repository.groupToSplitTransaction(ids: LongArray): Result<Boolean> {
    val count = ids.size
    val projection = arrayOf(
        KEY_ACCOUNTID,
        KEY_CURRENCY,
        KEY_PAYEEID,
        KEY_CR_STATUS,
        "avg($KEY_DATE) AS $KEY_DATE",
        "sum($KEY_AMOUNT) AS $KEY_AMOUNT",
        "sum($KEY_EQUIVALENT_AMOUNT) AS $KEY_EQUIVALENT_AMOUNT"
    )

    val groupBy = String.format(
        Locale.ROOT,
        "%s, %s, %s, %s",
        "$VIEW_EXTENDED.$KEY_ACCOUNTID",
        "$VIEW_EXTENDED.$KEY_CURRENCY",
        KEY_PAYEEID,
        KEY_CR_STATUS
    )
    return contentResolver.query(
        EXTENDED_URI.buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_TRANSACTION_ID_LIST, ids.joinToString())
            .appendQueryParameter(QUERY_PARAMETER_GROUP_BY, groupBy)
            .appendQueryParameter(QUERY_PARAMETER_DISTINCT, "1")
            .build(),
        projection, null, null, null
    )!!.use { cursor ->

        when (cursor.count) {
            1 -> {
                cursor.moveToFirst()
                val accountId = cursor.getLong(KEY_ACCOUNTID)
                val currencyUnit = currencyContext[cursor.getString(KEY_CURRENCY)]
                val amount = Money(
                    currencyUnit,
                    cursor.getLong(KEY_AMOUNT)
                )
                val equivalentAmount = Money(
                    currencyContext.homeCurrencyUnit,
                    cursor.getLong(KEY_EQUIVALENT_AMOUNT)
                )
                val payeeId = cursor.getLongOrNull(KEY_PAYEEID)
                val date = cursor.getLong(KEY_DATE)
                val crStatus =
                    enumValueOrDefault(
                        cursor.getString(KEY_CR_STATUS),
                        CrStatus.UNRECONCILED
                    )
                val parent = Transaction(
                    accountId = accountId,
                    amount = amount.amountMinor,
                    categoryId = SPLIT_CATID,
                    date = date,
                    uuid = generateUuid(),
                    payeeId = payeeId,
                    crStatus = crStatus,
                    equivalentAmount = equivalentAmount.amountMinor
                )
                val operations = ArrayList<ContentProviderOperation>()
                operations.add(
                    ContentProviderOperation.newInsert(TRANSACTIONS_URI)
                        .withValues(parent.asContentValues(true))
                        .build()
                )
                val where = KEY_ROWID + " " + Operation.IN.getOp(count)
                val selectionArgs = ids.map { it.toString() }.toTypedArray()

                operations.add(
                    ContentProviderOperation.newUpdate(TRANSACTIONS_URI)
                        .withValues(ContentValues().apply {
                            put(KEY_CR_STATUS, CrStatus.UNRECONCILED.name)
                            put(KEY_DATE, parent.date)
                            putNull(KEY_PAYEEID)
                        })
                        .withValueBackReference(KEY_PARENTID, 0)
                        .withSelection(where, selectionArgs)
                        .withExpectedCount(count)
                        .build()
                )
                contentResolver.applyBatch(AUTHORITY, operations)
                Result.success(true)
            }

            0 -> Result.failure(IllegalStateException().also {
                CrashHandler.report(it)
            })

            else -> Result.success(false)
        }
    }
}
