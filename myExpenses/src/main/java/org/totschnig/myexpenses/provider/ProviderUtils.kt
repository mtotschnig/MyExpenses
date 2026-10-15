package org.totschnig.myexpenses.provider

import android.os.Bundle
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER
import org.totschnig.myexpenses.db2.CategoryHelper
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.findAnyOpen
import org.totschnig.myexpenses.db2.findAnyOpenByCurrency
import org.totschnig.myexpenses.db2.findAnyOpenByLabel
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.model.Money.Companion.buildWithMicros
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.util.epoch2LocalDateTime
import org.totschnig.myexpenses.viewmodel.data.TransactionEditData
import org.totschnig.myexpenses.viewmodel.data.TransferEditData

object ProviderUtils {
    //TODO add tags to contract
    fun buildFromExtras(repository: Repository, extras: Bundle): TransactionEditData? {
        return (extras.getString(Transactions.ACCOUNT_LABEL)
            ?.takeIf { it.isNotEmpty() }
            ?.let { repository.findAnyOpenByLabel(it) }
            ?: extras.getString(Transactions.CURRENCY)
                ?.let { repository.findAnyOpenByCurrency(it) }
            ?: repository.findAnyOpen())
            ?.let { repository.loadAccount(it) }
            ?.let { account ->
                val currencyUnit = repository.currencyContext[account.currency]
                val operationType = extras.getInt(Transactions.OPERATION_TYPE)
                if (operationType == TYPE_SPLIT) throw NotImplementedError("Split transactions not yet implemented")
                val categoryLabel = extras.getString(Transactions.CATEGORY_LABEL)
                val catId = categoryLabel?.let {
                    val categoryToId: MutableMap<String, Long> = mutableMapOf()
                    CategoryHelper.insert(repository, categoryLabel, categoryToId, false)
                    categoryToId[categoryLabel]
                }
                TransactionEditData(
                    accountId = account.id,
                    transferEditData = if (operationType == TYPE_TRANSFER)
                        TransferEditData(
                            transferAccountId = extras.getString(Transactions.TRANSFER_ACCOUNT_LABEL)
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { repository.findAnyOpenByLabel(it) }
                                ?.takeIf { it != -1L }
                                ?: 0L
                        ) else null,
                    amount = buildWithMicros(currencyUnit, extras.getLong(Transactions.AMOUNT_MICROS)),
                    date = epoch2LocalDateTime(extras.getLong(Transactions.DATE)),
                    party = extras.getString(Transactions.PAYEE_NAME)
                        ?.takeIf { it.isNotEmpty() }
                        ?.let {
                            DisplayParty(null, it)
                        },
                    categoryId = catId,
                    categoryPath = if (catId != null) categoryLabel else null,
                    comment = extras.getString(Transactions.COMMENT)
                        ?.takeIf { it.isNotEmpty() },
                    methodId = extras.getString(Transactions.METHOD_LABEL)
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { repository.findPaymentMethod(it) },
                    referenceNumber = extras.getString(Transactions.REFERENCE_NUMBER)
                        ?.takeIf { it.isNotEmpty() },
                    uuid = generateUuid()
                )
            }
    }
}