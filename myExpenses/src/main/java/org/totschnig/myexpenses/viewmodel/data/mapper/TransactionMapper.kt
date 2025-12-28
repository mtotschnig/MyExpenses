package org.totschnig.myexpenses.viewmodel.data.mapper

import org.totschnig.myexpenses.db2.RepositoryTemplate
import org.totschnig.myexpenses.db2.RepositoryTransaction
import org.totschnig.myexpenses.db2.entities.Plan
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.epoch2LocalDateTime
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.viewmodel.data.PlanEditData
import org.totschnig.myexpenses.viewmodel.data.PlanLoadedData
import org.totschnig.myexpenses.viewmodel.data.TemplateEditData
import org.totschnig.myexpenses.viewmodel.data.TransactionEditData
import org.totschnig.myexpenses.viewmodel.data.TransferEditData
import java.time.ZoneId

object TransactionMapper {
    fun map(
        repositoryTransaction: RepositoryTransaction,
        currencyContext: CurrencyContext,
    ): TransactionEditData {
        val transaction = repositoryTransaction.data
        val currencyUnit = currencyContext[transaction.currency!!]
        val money = Money(currencyUnit, transaction.amount)
        return TransactionEditData(
            id = transaction.id,
            amount = money,
            date = epoch2LocalDateTime(transaction.date),
            valueDate = epoch2LocalDate(transaction.valueDate),
            party = transaction.payeeId?.let { DisplayParty(it, transaction.payeeName!!) },
            categoryId = transaction.categoryId,
            categoryPath = transaction.categoryPath,
            categoryIcon = transaction.categoryIcon,
            accountId = transaction.accountId,
            tags = repositoryTransaction.tags ?: emptyList(),
            methodId = transaction.methodId,
            methodLabel = transaction.methodLabel,
            originalAmount = transaction.originalAmount?.let {
                Money(
                    currencyContext[transaction.originalCurrency!!],
                    it
                )
            },
            equivalentAmount = transaction.equivalentAmount?.let {
                Money(
                    currencyContext.homeCurrencyUnit,
                    it
                )
            },
            parentId = transaction.parentId,
            crStatus = transaction.crStatus,
            uuid = transaction.uuid,
            debtId = transaction.debtId,
            templateEditData = null,
            comment = transaction.comment,
            referenceNumber = transaction.referenceNumber,
            transferEditData = repositoryTransaction.transferPeer?.let {
                TransferEditData(
                    transferAccountId = it.accountId,
                    transferPeer = it.id,
                    transferAmount = Money(currencyUnit, it.amount)
                )
            },
            isSealed = transaction.sealed,
            splitParts = repositoryTransaction.splitParts?.map {
                map(it, currencyContext).copy(
                    isSplitPart = true
                )
            }
        )
    }

    fun map(template: Template, currencyContext: CurrencyContext): TransactionEditData {
        val currencyUnit = currencyContext[template.currency!!]
        val money = Money(currencyUnit, template.amount)
        return TransactionEditData(
            id = template.id,
            amount = money,
            party = template.payeeId?.let { DisplayParty(it, template.payeeName!!) },
            categoryId = template.categoryId,
            categoryPath = template.categoryPath,
            categoryIcon = template.categoryIcon,
            accountId = template.accountId,
            methodId = template.methodId,
            methodLabel = template.methodLabel,
            originalAmount = template.originalAmount?.let {
                Money(
                    currencyContext[template.originalCurrency!!],
                    it
                )
            },
            parentId = template.parentId,
            planId = template.planId,
            uuid = template.uuid,
            debtId = template.debtId,
            comment = template.comment,
            transferEditData = template.transferAccountId?.let {
                TransferEditData(//for templates peer information is created when instantiated
                    transferAccountId = it,
                    transferPeer = null,
                    transferAmount = null
                )
            },
            isSealed = template.sealed,
        )
    }

    fun mapPlan(plan: Plan) = PlanLoadedData(
        id = plan.id,
        rRule = plan.rRule,
        dtStart = plan.dtStart,
    )

    fun mapTemplateEditData(repositoryTemplate: RepositoryTemplate): TemplateEditData {
        val template = repositoryTemplate.data
        return TemplateEditData(
            templateId = template.id,
            title = template.title,
            defaultAction = template.defaultAction,
            plan = repositoryTemplate.plan?.let {
                mapPlan(it)
            },
            planEditData = if (repositoryTemplate.plan != null) {
                PlanEditData(
                    isPlanExecutionAutomatic = repositoryTemplate.data.planExecutionAutomatic,
                    planExecutionAdvance = repositoryTemplate.data.planExecutionAdvance
                )
            } else null
        )
    }

    fun map(
        repositoryTemplate: RepositoryTemplate,
        currencyContext: CurrencyContext,
    ): TransactionEditData {
        val template = repositoryTemplate.data
        return map(template, currencyContext).copy(
            templateEditData = mapTemplateEditData(repositoryTemplate),
            splitParts = repositoryTemplate.splitParts?.map {
                map(it, currencyContext).copy(isSplitPart = true)
            },
            tags = repositoryTemplate.tags ?: emptyList()
        )
    }

    fun mapTransaction(
        transactionEditData: TransactionEditData,
        date: Long = transactionEditData.date.toEpoch(),
    ): RepositoryTransaction {
        val transaction = Transaction(
            id = transactionEditData.id,
            amount = transactionEditData.amount.amountMinor,
            date = date,
            valueDate = transactionEditData.valueDate.toEpoch(),
            accountId = transactionEditData.accountId,
            categoryId = transactionEditData.categoryId,
            methodId = transactionEditData.methodId,
            originalAmount = transactionEditData.originalAmount?.amountMinor,
            originalCurrency = transactionEditData.originalAmount?.currencyUnit?.code,
            equivalentAmount = transactionEditData.equivalentAmount?.amountMinor,
            parentId = transactionEditData.parentId,
            crStatus = transactionEditData.crStatus,
            uuid = transactionEditData.uuid,
            comment = transactionEditData.comment,
            referenceNumber = transactionEditData.referenceNumber,
            transferAccountId = transactionEditData.transferEditData?.transferAccountId,
            payeeId = transactionEditData.party?.id,
            transferPeerId = transactionEditData.transferEditData?.transferPeer,
            debtId = transactionEditData.debtId,
            tagList = transactionEditData.tags.map { it.id }
        )
        val transferPeer = transactionEditData.transferEditData?.let { transferEditData ->
            Transaction(
                id = transferEditData.transferPeer ?: 0,
                amount = transferEditData.transferAmount?.amountMinor
                    ?: -transactionEditData.amount.amountMinor,
                date = date,
                accountId = transferEditData.transferAccountId,
                transferAccountId = transactionEditData.accountId,
                categoryId = transactionEditData.categoryId,
                comment = transactionEditData.comment,
                transferPeerId = transactionEditData.id,
                uuid = transactionEditData.uuid,
                tagList = transactionEditData.tags.map { it.id }
            )
        }
        return RepositoryTransaction(
            data = transaction,
            transferPeer = transferPeer,
            splitParts = transactionEditData.splitParts?.map {
                mapTransaction(it.copy(crStatus = transactionEditData.crStatus), date)
            }
        )
    }

    fun mapTemplate(transactionEditData: TransactionEditData): RepositoryTemplate {
        val templateEditData = transactionEditData.templateEditData!!
        val template = Template(
            id = transactionEditData.id,
            title = templateEditData.title,
            defaultAction = templateEditData.defaultAction,
            amount = transactionEditData.amount.amountMinor,
            accountId = transactionEditData.accountId,
            categoryId = transactionEditData.categoryId,
            methodId = transactionEditData.methodId,
            originalAmount = transactionEditData.originalAmount?.amountMinor,
            originalCurrency = transactionEditData.originalAmount?.currencyUnit?.code,
            parentId = transactionEditData.parentId,
            comment = transactionEditData.comment,
            planId = transactionEditData.planId,
            transferAccountId = transactionEditData.transferEditData?.transferAccountId,
            payeeId = transactionEditData.party?.id,
            uuid = transactionEditData.uuid,
            tagList = transactionEditData.tags.map { it.id },
            planExecutionAutomatic = templateEditData.planEditData?.isPlanExecutionAutomatic
                ?: false,
            planExecutionAdvance = templateEditData.planEditData?.planExecutionAdvance ?: 0,
            debtId = transactionEditData.debtId
        )
        return RepositoryTemplate(
            data = template,
            splitParts = transactionEditData.splitParts?.map { mapTemplate(it) }
        )
    }
}