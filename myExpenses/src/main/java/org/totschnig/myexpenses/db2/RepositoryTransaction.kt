package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.core.os.BundleCompat
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.dialog.ArchiveInfo
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Model.generateUuid
import org.totschnig.myexpenses.provider.DataBaseAccount
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.uriBuilderForTransactionList
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DEBT_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_END
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EQUIVALENT_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_SEALED_ACCOUNT_WITH_TRANSFER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HAS_SEALED_DEBT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_AMOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ORIGINAL_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_START
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_ACCOUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_VALUE_DATE
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT_PART
import org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_VOID
import org.totschnig.myexpenses.provider.DbUtils
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_ARCHIVE
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_CAN_BE_ARCHIVED
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.URI_SEGMENT_UNARCHIVE
import org.totschnig.myexpenses.provider.filter.Criterion
import org.totschnig.myexpenses.provider.filter.FilterPersistence
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.getStringOrNull
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.provider.withLimit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.joinArrays
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.viewmodel.MyExpensesViewModel
import java.time.LocalDate
import java.time.LocalDateTime


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
}

data class RepositoryTransaction(
    val data: Transaction,
    val transferPeer: Transaction? = null,
    val splitParts: List<RepositoryTransaction>? = null
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
        repositoryTransaction.splitParts!!.map { it.data }
    )

    else -> createTransaction(repositoryTransaction.data)
}

fun Repository.updateTransaction(repositoryTransaction: RepositoryTransaction) = when {

    repositoryTransaction.isTransfer -> updateTransfer(
        repositoryTransaction.data,
        repositoryTransaction.transferPeer!!
    )

    repositoryTransaction.isSplit -> updateSplitTransaction(
        repositoryTransaction.data,
        repositoryTransaction.splitParts!!.map { it.data }
    )

    else -> updateTransaction(repositoryTransaction.data)
}

fun Repository.createTransaction(transaction: Transaction): RepositoryTransaction {
    require(transaction.id == 0L) { "Use updateTemplate for existing templates" }
    require(transaction.transferAccountId == null) { "Use createTransfer instead" }
    require(transaction.categoryId != DatabaseConstants.SPLIT_CATID) { "Use createSplitTransaction instead" }
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
    transaction: Transaction
) = contentResolver.update(
    ContentUris.withAppendedId(TRANSACTIONS_URI, transaction.id),
    transaction.asContentValues(false),
    null, null
) == 1

fun Repository.createTransfer(
    sourceTransaction: Transaction,
    destinationTransaction: Transaction
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

    val results = contentResolver.applyBatch(TransactionProvider.AUTHORITY, operations)

    val first = ContentUris.parseId(results[0].uri!!)
    val second = ContentUris.parseId(results[1].uri!!)

    return RepositoryTransaction(
        sourceTransaction.copy(id = first, transferPeerId = second),
        destinationTransaction.copy(id = second, transferPeerId = first)
    )
}

fun Repository.updateTransfer(
    sourceTransaction: Transaction,
    destinationTransaction: Transaction
): Boolean {
    require(
        sourceTransaction.transferAccountId == destinationTransaction.accountId &&
                sourceTransaction.accountId == destinationTransaction.transferAccountId
    )
    require(
        sourceTransaction.transferPeerId == destinationTransaction.id &&
                sourceTransaction.id == destinationTransaction.transferPeerId
    )
    val operations = ArrayList<ContentProviderOperation>()

    //we set the transfer peers uuid to null initially to prevent violation of unique index which
    //happens if the account after update is identical to transferAccountId before update
    val destinationUri = ContentUris.withAppendedId(TRANSACTIONS_URI, destinationTransaction.id)
    val uuidNullValues = ContentValues(1).apply {
        putNull(KEY_UUID)
    }
    operations.add(
        ContentProviderOperation
            .newUpdate(destinationUri)
            .withValues(uuidNullValues).build()
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
            .withValues(destinationTransaction.asContentValues(true)) // we need to set the uuid again
            .build()
    )
    val results = contentResolver.applyBatch(TransactionProvider.AUTHORITY, operations)
    return results[0].count == 1 && results[1].count == 1
}

@VisibleForTesting
fun Repository.createSplitTransaction(
    parentTransaction: Transaction,
    splitTransactions: List<Transaction>
): RepositoryTransaction = createSplitTransaction(
    parentTransaction,
    splitTransactions.map { it to null }
)

@VisibleForTesting
@JvmName("createSplitTransactionWithTransfers")
fun Repository.createSplitTransaction(
    parentTransaction: Transaction,
    splitParts: List<Pair<Transaction, Transaction?>>
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
    var opIndex = 1 // Start counting operations after the parent

    // --- Process each split part ---
    splitParts.forEach { (splitPart, peer) ->
        if (peer == null) {
            // --- This is a REGULAR split part ---
            operations.add(
                ContentProviderOperation.newInsert(TRANSACTIONS_URI)
                    .withValues(splitPart.asContentValues(true))
                    .withValueBackReference(KEY_PARENTID, parentBackRefIndex)
                    .build()
            )
            // Prepare the final object, ID will be filled in later
            finalSplitParts.add(RepositoryTransaction(splitPart))
            opIndex++
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
            opIndex += 3 // A transfer adds 3 operations
        }
    }

    // --- Atomically execute all operations ---
    val results = contentResolver.applyBatch(TransactionProvider.AUTHORITY, operations)

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
            resultIndex += 3 // Move past all 3 transfer operations
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


fun Repository.updateSplitTransaction(
    parentTransaction: Transaction,
    splitParts: List<Transaction>
): Boolean {
    // --- Validation ---
    require(parentTransaction.isSplit) { "Parent transaction must be a split." }
    require(splitParts.sumOf { it.amount } == parentTransaction.amount) { "Sum of splits must equal parent amount." }
    require(splitParts.all { it.date == parentTransaction.date }) {
        "Split transactions date must match parent date."
    }
    require(splitParts.all { it.accountId == parentTransaction.accountId }) { "All splits must be in the same account." }

    val operations = ArrayList<ContentProviderOperation>()

    // --- 1. Handle Deletions ---
    // Get IDs of parts that have a non-zero ID (i.e., they already exist in the DB).
    val keepIds = splitParts.mapNotNull { if (it.id != 0L) it.id else null }

    if (keepIds.isEmpty()) {
        // If no existing parts are being kept, delete all children of the parent.
        operations.add(
            ContentProviderOperation.newDelete(TRANSACTIONS_URI)
                .withSelection("$KEY_PARENTID = ?", arrayOf(parentTransaction.id.toString()))
                .build()
        )
    } else {
        // Otherwise, delete any children that are NOT in the list of IDs to keep.
        val placeholders = List(keepIds.size) { "?" }.joinToString(",")
        val selection = "$KEY_PARENTID = ? AND $KEY_ROWID NOT IN ($placeholders)"
        val selectionArgs = arrayOf(parentTransaction.id.toString()) + keepIds.map { it.toString() }

        operations.add(
            ContentProviderOperation.newDelete(TRANSACTIONS_URI)
                .withSelection(selection, selectionArgs)
                .build()
        )
    }

    // --- 2. Update Parent Transaction ---
    operations.add(
        ContentProviderOperation.newUpdate(
            ContentUris.withAppendedId(TRANSACTIONS_URI, parentTransaction.id)
        )
            .withValues(parentTransaction.asContentValues(false))
            .build()
    )

    // --- 3. Insert New Parts and Update Existing Parts ---
    for (transaction in splitParts) {
        val operation = if (transaction.id == 0L) {
            // NEW: This is a new split part, so insert it.
            ContentProviderOperation.newInsert(TRANSACTIONS_URI)
                .withValues(
                    transaction.asContentValues(true)
                        .apply {
                            put(KEY_PARENTID, parentTransaction.id)
                        })
        } else {
            // EXISTING: This part already exists, so update it.
            ContentProviderOperation.newUpdate(
                ContentUris.withAppendedId(TRANSACTIONS_URI, transaction.id)
            )
                .withValues(transaction.asContentValues(false))
        }
        operations.add(operation.build())
    }

    val results = contentResolver.applyBatch(TransactionProvider.AUTHORITY, operations)
    return results.mapIndexed { index, result ->
        //first result is delete, others either insert or update
        if (index == 0) true else result.uri != null || result.count == 1
    }.all { it }
}

suspend fun Repository.loadTransactions(accountId: Long, limit: Int? = 200): List<Transaction> {
    val filter = FilterPersistence(
        dataStore = dataStore,
        prefKey = MyExpensesViewModel.prefNameForCriteria(accountId),
    ).getValue()?.let {
        it.getSelectionForParents() to it.getSelectionArgs(false)
    }
    //noinspection Recycle
    return contentResolver.query(
        DataBaseAccount.uriForTransactionList(true).let {
            if (limit != null) it.withLimit(limit) else it
        },
        Transaction.projection,
        "$KEY_ACCOUNTID = ? AND $KEY_PARENTID IS NULL ${
            filter?.first?.takeIf { it != "" }?.let { "AND $it" } ?: ""
        }",
        filter?.let { arrayOf(accountId.toString(), *it.second) }
            ?: arrayOf(accountId.toString()),
        null
    )!!.useAndMapToList { cursor -> Transaction.fromCursor(cursor) }
}

fun Repository.loadTransaction(
    transactionId: Long,
    withTransfer: Boolean = true
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
                    ).data else null
                )
            } else null
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
    filter: Criterion? = null
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
    range: Pair<LocalDate, LocalDate>
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
    val result = contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops)
    val affectedRows = result[1].count
    if (affectedRows != 1) {
        CrashHandler.report(Exception("Unarchive returned $affectedRows affected rows"))
    }
}

fun Repository.canBeArchived(
    accountId: Long,
    range: Pair<LocalDate, LocalDate>
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
    accountId: Long
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
    column: String
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
    referenceNumber: String? = null
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
    uuid: String = generateUuid()
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
    parentBackRefIndex: Int? = null
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
            }.build(),

        // Operation at index (offset + 1): Insert the destination, linking to the source.
        ContentProviderOperation.newInsert(TRANSACTIONS_URI)
            .withValues(destination.asContentValues(true))
            .withValueBackReference(KEY_TRANSFER_PEER, offset)
            .build(),

        // Operation at index (offset + 2): Update the source to link to the destination.
        ContentProviderOperation.newUpdate(TRANSACTIONS_URI)
            .withSelection("$KEY_ROWID = ?", emptyArray())//replaced by back reference
            .withSelectionBackReference(0, offset)
            .withValueBackReference(KEY_TRANSFER_PEER, offset + 1)
            .build()
    )
}

fun Repository.undeleteTransaction(id: Long): Int {
    val uri = ContentUris.appendId(TRANSACTIONS_URI.buildUpon(), id)
        .appendPath(TransactionProvider.URI_SEGMENT_UNDELETE)
        .build()
    return contentResolver.update(uri, null, null, null)
}
