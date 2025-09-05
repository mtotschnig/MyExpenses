package org.totschnig.myexpenses.provider

import android.os.Bundle
import android.text.TextUtils
import org.apache.commons.lang3.NotImplementedException
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.*
import org.totschnig.myexpenses.model.Money.Companion.buildWithMicros
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.ui.DisplayParty

object ProviderUtils {
    //TODO add tags to contract
    @Throws(NotImplementedException::class)
    fun buildFromExtras(repository: Repository, extras: Bundle) =
        (extras.getString(Transactions.ACCOUNT_LABEL)
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                repository.findAnyOpenByLabel(it)
            } ?: extras.getString(Transactions.CURRENCY)?.let {
            repository.findAnyOpenByCurrency(it)
        } ?: repository.findAnyOpen())?.let {
            repository.loadAccount(it)
        }?.let { account ->
            val currencyUnit = repository.currencyContext[account.currency]
            when (extras.getInt(Transactions.OPERATION_TYPE)) {
                Transactions.TYPE_TRANSFER -> {
                    Transfer.getNewInstance(account.id, currencyUnit, null).apply {
                        val transferAccountLabel =
                            extras.getString(Transactions.TRANSFER_ACCOUNT_LABEL)
                        transferAccountLabel?.takeIf { it.isNotEmpty() }?.let {
                            repository.findAnyOpenByLabel(it)
                        }?.takeIf { it != -1L }?.let {
                            setTransferAccountId(it)
                        }
                    }
                }
                Transactions.TYPE_SPLIT -> throw NotImplementedException("Building split transaction not yet implemented")
                else -> {
                    Transaction.getNewInstance(account.id, currencyUnit)
                }
            }.apply {
                val amountMicros = extras.getLong(Transactions.AMOUNT_MICROS)
                if (amountMicros != 0L) {
                    amount = buildWithMicros(currencyUnit, amountMicros)
                }
                val date = extras.getLong(Transactions.DATE)
                if (date != 0L) {
                    this.date = date
                }
                extras.getString(Transactions.PAYEE_NAME)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
                        party = DisplayParty(null, it)
                    }
                if (this !is Transfer) {
                    val categoryLabel = extras.getString(Transactions.CATEGORY_LABEL)
                    if (!TextUtils.isEmpty(categoryLabel)) {
                        val categoryToId: MutableMap<String, Long> = mutableMapOf()
                        CategoryHelper.insert(repository, categoryLabel!!, categoryToId, false)
                        catId = categoryToId[categoryLabel]
                        if (catId != null) {
                            categoryPath = categoryLabel
                        }
                    }
                }

                val comment = extras.getString(Transactions.COMMENT)
                if (!TextUtils.isEmpty(comment)) {
                    this.comment = comment
                }

                extras.getString(Transactions.METHOD_LABEL)?.takeIf { it.isNotEmpty() }?.let {
                    repository.findPaymentMethod(it)
                }?.let {
                    this.methodId = it
                }

                val referenceNumber = extras.getString(Transactions.REFERENCE_NUMBER)
                if (!TextUtils.isEmpty(referenceNumber)) {
                    this.referenceNumber = referenceNumber
                }
            }
        }
}