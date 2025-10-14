package org.totschnig.myexpenses.viewmodel.data.mapper

import org.totschnig.myexpenses.db2.RepositoryTemplate
import org.totschnig.myexpenses.db2.RepositoryTransaction
import org.totschnig.myexpenses.db2.entities.Template
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.util.epoch2LocalDate
import org.totschnig.myexpenses.util.epoch2LocalDateTime
import org.totschnig.myexpenses.viewmodel.data.PlanEditData
import org.totschnig.myexpenses.viewmodel.data.TemplateEditData
import org.totschnig.myexpenses.viewmodel.data.TransactionEditData
import org.totschnig.myexpenses.viewmodel.data.TransferEditData
import java.time.ZoneId

object TransactionMapper {
    fun map(repositoryTransaction: RepositoryTransaction, currencyContext: CurrencyContext): TransactionEditData {
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
            categoryPath =transaction.categoryPath,
            categoryIcon = null, //TODO
            accountId = transaction.accountId,
            tags = emptyList(), //TODO
            attachments = emptyList(), //TODO
            methodId = transaction.methodId,
            methodLabel = null, //TODO
            originalAmount = transaction.originalAmount?.let { Money(currencyContext[transaction.originalCurrency!!], it) },
            equivalentAmount = transaction.equivalentAmount?.let { Money(currencyContext.homeCurrencyUnit, it) },
            exchangeRate = null, //TODO where does this come from?
            parentId = transaction.parentId,
            crStatus = transaction.crStatus,
            //TODO originTemplateId = transaction.originTemplateId,
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
            isSealed = transaction.sealed
        )
    }

    fun map(repositoryTemplate: RepositoryTemplate, currencyContext: CurrencyContext): TransactionEditData {
        val template = repositoryTemplate.data
        val currencyUnit = currencyContext[template.currency!!]
        val money = Money(currencyUnit, template.amount)
        return TransactionEditData(
            id = template.id,
            amount = money,
            party = template.payeeId?.let { DisplayParty(it, template.payeeName!!) },
            categoryId = template.categoryId,
            categoryPath = template.categoryPath,
            categoryIcon = null, //TODO
            accountId = template.accountId,
            tags = emptyList(), //TODO
            attachments = emptyList(), //TODO
            methodId = template.methodId,
            methodLabel = null, //TODO
            originalAmount = template.originalAmount?.let { Money(currencyContext[template.originalCurrency!!], it) },
            equivalentAmount = null,
            exchangeRate = null,
            parentId = template.parentId,
            originTemplateId = null,
            planId = template.planId,
            uuid = template.uuid,
            debtId = null,
            templateEditData = TemplateEditData(
                templateId = template.id,
                title = template.title,
                defaultAction = template.defaultAction,
                planEditData = repositoryTemplate.plan?.let {
                    PlanEditData(
                        plan = it,
                        isPlanExecutionAutomatic = template.planExecutionAutomatic,
                        planExecutionAdvance = template.planExecutionAdvance,
                    )
                }
            ),
            comment = template.comment,
            transferEditData = template.transferAccountId?.let {
                TransferEditData(
                    transferAccountId = it,
                    transferPeer = null,
                    transferAmount = null //TODO is this correct?
                )
            },
            isSealed = template.sealed
        )
    }

    fun mapTransaction(transactionEditData: TransactionEditData): RepositoryTransaction {
        val transaction = Transaction(
            id = transactionEditData.id,
            amount = transactionEditData.amount.amountMinor,
            date = transactionEditData.date.atZone(ZoneId.systemDefault()).toInstant().epochSecond,
            accountId = transactionEditData.accountId,
            categoryId = transactionEditData.categoryId,
            methodId = transactionEditData.methodId,
            originalAmount = transactionEditData.originalAmount?.amountMinor,
            originalCurrency = transactionEditData.originalAmount?.currencyUnit?.code,
            equivalentAmount = transactionEditData.equivalentAmount?.amountMinor,
            parentId = transactionEditData.parentId,
            crStatus = transactionEditData.crStatus,
            //originTemplateId = transactionEditData.originTemplateId,
            uuid = transactionEditData.uuid,
            comment = transactionEditData.comment,
            referenceNumber = transactionEditData.referenceNumber,
            transferAccountId = transactionEditData.transferEditData?.transferAccountId,
            payeeId = transactionEditData.party?.id
        )
        val transferPeer = if (transactionEditData.isTransfer) {
            val transferEditData = transactionEditData.transferEditData!!
            Transaction(
                id = transferEditData.transferPeer ?: 0,
                amount = transactionEditData.transferEditData.transferAmount?.amountMinor
                    ?: -transactionEditData.amount.amountMinor,
                date = transactionEditData.date.atZone(ZoneId.systemDefault()).toInstant().epochSecond,
                accountId = transferEditData.transferAccountId,
                transferAccountId = transactionEditData.accountId,
                categoryId = transactionEditData.categoryId,
                comment = transactionEditData.comment
            )
        } else null
        return RepositoryTransaction(
            data = transaction,
            transferPeer = transferPeer,
            splitParts = emptyList() // TODO
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
            payeeId = transactionEditData.party?.id
        )
        return RepositoryTemplate(data = template)
    }
}