package org.totschnig.myexpenses.viewmodel.data

import android.net.Uri
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TransactionType
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Plan
import org.totschnig.myexpenses.model.Plan.Recurrence
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.DisplayParty
import java.time.LocalDate
import java.time.LocalDateTime

data class PlanEditData(
    val isPlanExecutionAutomatic: Boolean,
    val planExecutionAdvance: Int,
    val plan: Plan
)

data class TemplateEditData(
    val templateId: Long = 0,
    val title: String = "",
    val defaultAction: Template.Action = Template.Action.EDIT,
    val planEditData: PlanEditData? = null,
    val isPlanExecutionAutomatic: Boolean = false,
    val planExecutionAdvance: Int = 0
)

data class TransferEditData(
    val transferAccountId: Long = 0L,
    val transferPeer: Long? = null,
    val transferAmount: Money? = null,
)

data class TransactionEditData(
    val id: Long = 0,
    val amount: Money,
    val date: LocalDateTime = LocalDateTime.now(),
    val valueDate: LocalDate = date.toLocalDate(),
    val party: DisplayParty? = null,
    val categoryId: Long? = null,
    val categoryPath: String? = null,
    val categoryIcon: String? = null,
    val accountId: Long,
    val tags: List<String> = emptyList(),
    val attachments: List<Uri> = emptyList(),
    val methodId: Long? = null,
    val methodLabel: String? = null,
    val originalAmount: Money? = null,
    val equivalentAmount: Money? = null,
    val exchangeRate: Long? = null,
    val parentId: Long? = null,
    val crStatus: CrStatus = CrStatus.UNRECONCILED,
    val originTemplateId: Long? = null,
    val planId: Long? = null,
    val uuid: String? = null,
    val debtId: Long? = null,
    val templateEditData: TemplateEditData? = null,
    val comment: String? = null,
    val referenceNumber: String? = null,
    val initialPlan: Triple<String, Recurrence, LocalDate>? = null,
    val transferEditData: TransferEditData? = null,
    val isSealed: Boolean = false
) {
    val isSplit = categoryId == DatabaseConstants.SPLIT_CATID
    val isTransfer = transferEditData != null
    val isTemplate = templateEditData != null
    @TransactionType val operationType: Int = when {
        isSplit -> TYPE_SPLIT
        isTransfer -> TYPE_TRANSFER
        else -> TYPE_TRANSACTION
    }
}