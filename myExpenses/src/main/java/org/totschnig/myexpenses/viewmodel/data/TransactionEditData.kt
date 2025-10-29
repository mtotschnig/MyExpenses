package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
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
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.formatMoney
import java.time.LocalDate
import java.time.LocalDateTime

@Parcelize
data class PlanEditData(
    val isPlanExecutionAutomatic: Boolean,
    val planExecutionAdvance: Int,
    val plan: Plan?
) : Parcelable

@Parcelize
data class TemplateEditData(
    val templateId: Long = 0,
    val title: String = "",
    val defaultAction: Template.Action = Template.Action.EDIT,
    val planEditData: PlanEditData? = null
) : Parcelable

@Parcelize
data class TransferEditData(
    val transferAccountId: Long = 0L,
    val transferPeer: Long? = null,
    val transferAmount: Money? = null,
) : Parcelable

@Parcelize
data class TransactionEditData(
    val id: Long = 0,
    val uuid: String,
    val amount: Money,
    val date: LocalDateTime = LocalDateTime.now(),
    val valueDate: LocalDate = date.toLocalDate(),
    val party: DisplayParty? = null,
    val categoryId: Long? = null,
    val categoryPath: String? = null,
    val categoryIcon: String? = null,
    val accountId: Long,
    val tags: List<Tag> = emptyList(),
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
    val debtId: Long? = null,
    val templateEditData: TemplateEditData? = null,
    val comment: String? = null,
    val referenceNumber: String? = null,
    val initialPlan: Triple<String?, Recurrence, LocalDate>? = null,
    val transferEditData: TransferEditData? = null,
    val isSealed: Boolean = false,
    val isSplitPart: Boolean = false,
    val splitParts: List<TransactionEditData>? = null,
    val planInstanceId: Long? = null
) : Parcelable {
    @IgnoredOnParcel
    val isSplit = categoryId == DatabaseConstants.SPLIT_CATID

    @IgnoredOnParcel
    val isTransfer = transferEditData != null

    @IgnoredOnParcel
    val isTemplate = templateEditData != null

    @IgnoredOnParcel
    @TransactionType
    val operationType: Int = when {
        isSplit -> TYPE_SPLIT
        isTransfer -> TYPE_TRANSFER
        else -> TYPE_TRANSACTION
    }

    fun compileDescription(
        ctx: Context,
        currencyFormatter: ICurrencyFormatter
    ) = buildString {
        append(ctx.getString(R.string.amount))
        append(" : ")
        append(currencyFormatter.formatMoney(amount))
        append("\n")
        if (categoryId != null && categoryId > 0) {
            append(ctx.getString(R.string.category))
            append(" : ")
            append(categoryPath)
            append("\n")
        }
        if (isTransfer) {
            append(ctx.getString(R.string.account))
            append(" : ")
            append(categoryPath)
            append("\n")
        }

        //comment
        if (comment != "") {
            append(ctx.getString(R.string.notes))
            append(" : ")
            append(comment)
            append("\n")
        }

        //payee
        if (party != null) {
            append(
                ctx.getString(
                    if (amount.amountMajor.signum() == 1) R.string.payer else R.string.payee
                )
            )
            append(" : ")
            append(party.name)
            append("\n")
        }

        //Method
        if (methodId != null) {
            append(ctx.getString(R.string.method))
            append(" : ")
            append(methodLabel)
            append("\n")
        }
        append("UUID : ")
        append(uuid)
    }
}

data class TransactionEditResult(
    val id: Long,
    val amount: Long,
    val transferAmount: Long?,
    val planId: Long?
)