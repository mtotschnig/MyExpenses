package org.totschnig.myexpenses.next.transactions

import org.totschnig.myexpenses.db2.FinTsAttribute
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.getCurrencyUnitForAccount
import org.totschnig.myexpenses.db2.getPartyName
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.loadAttributes
import org.totschnig.myexpenses.db2.loadTransaction
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.db2.updateTransaction
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod.Companion.translateIfPredefined
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.STATUS_ARCHIVE
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.viewmodel.data.Transaction2

/** Loads and saves what the details of a transaction can change */
interface TransactionStore {
    /**
     * Null if the details cannot change it: a split, a transfer, a part of a split, cancelled, an archive,
     * or in a closed account. These are left to the classic form.
     */
    suspend fun load(id: Long): EditableTransaction?

    /** The payment methods of accounts of [accountType], for income or for expenses */
    suspend fun methods(accountType: Long, isIncome: Boolean): List<MethodOption>

    suspend fun save(id: Long, values: EditValues)
}

/** [TransactionStore] on the database of the app */
class RepositoryTransactionStore(private val repository: Repository) : TransactionStore {

    override suspend fun load(id: Long): EditableTransaction? {
        val data = runCatching { repository.loadTransaction(id, withTransfer = false).data }.getOrNull() ?: return null
        if (data.isSplit || data.transferPeerId != null || data.parentId != null ||
            data.crStatus == CrStatus.VOID || data.status == STATUS_ARCHIVE || data.sealed
        ) return null
        return EditableTransaction(
            id = id,
            values = EditValues(
                amount = data.amount,
                date = data.date,
                valueDate = data.valueDate,
                payee = data.payeeName ?: data.payeeId?.let { repository.getPartyName(it) },
                methodId = data.methodId,
                comment = data.comment,
            ),
            fractionDigits = repository.getCurrencyUnitForAccount(data.accountId)?.fractionDigits ?: 2,
            accountType = repository.loadAccount(data.accountId)?.type?.id,
            isAmountEditable = data.originalAmount == null && data.equivalentAmount == null,
            isImported = repository.loadAttributes(id).any { it.first is FinTsAttribute },
        )
    }

    override suspend fun methods(accountType: Long, isIncome: Boolean): List<MethodOption> =
        repository.contentResolver.query(
            TransactionProvider.METHODS_URI.buildUpon()
                .appendPath(TransactionProvider.URI_SEGMENT_TYPE_FILTER)
                .appendPath(if (isIncome) "1" else "-1")
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_ACCOUNT_TYPE_LIST, accountType.toString())
                .build(), null, null, null, null
        )?.useAndMapToList { MethodOption(it.getLong(KEY_ROWID), it.getString(KEY_LABEL).translateIfPredefined(repository.context)) }.orEmpty()

    override suspend fun save(id: Long, values: EditValues) {
        val data = repository.loadTransaction(id, withTransfer = false).data
        val payeeId = when (values.payee) {
            data.payeeName ?: data.payeeId?.let { repository.getPartyName(it) } -> data.payeeId
            null -> null
            else -> repository.requireParty(values.payee)
        }
        repository.updateTransaction(
            data.copy(
                amount = values.amount,
                date = values.date,
                valueDate = values.valueDate,
                payeeId = payeeId,
                methodId = values.methodId,
                comment = values.comment,
            )
        )
    }
}

/**
 * Whether the details may be able to change this transaction, before [TransactionStore.load] tells
 * for sure; the others go to the classic form at once
 */
fun Transaction2.isEditableInDetails() =
    !isSplit && !isTransfer && !isSplitPart && !isArchive && crStatus != CrStatus.VOID
