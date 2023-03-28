package org.totschnig.myexpenses.provider

import android.os.Bundle
import android.text.TextUtils
import org.apache.commons.lang3.NotImplementedException
import org.totschnig.myexpenses.contract.TransactionsContract.Transactions
import org.totschnig.myexpenses.db2.*
import org.totschnig.myexpenses.model.Money.Companion.buildWithMicros
import org.totschnig.myexpenses.model.PaymentMethod
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model.Transfer

object ProviderUtils {
    //TODO add tags to contract
    @Throws(NotImplementedException::class)
    fun buildFromExtras(repository: Repository, extras: Bundle): Transaction {
        val accountId = extras.getString(Transactions.ACCOUNT_LABEL)
            ?.takeIf { it.isNotEmpty() }?.let {
                repository.findAnyOpenByLabel(it)
            } ?: extras.getString(Transactions.CURRENCY)?.let {
            repository.findAnyOpenByCurrency(it)
        } ?: repository.findAnyOpen()
        val account = repository.loadAccount(accountId)!!
        val currencyUnit = repository.currencyContext[account.currency]
        val transaction: Transaction
        when (extras.getInt(Transactions.OPERATION_TYPE)) {
            Transactions.TYPE_TRANSFER -> {
                transaction = Transfer.getNewInstance(account.id, currencyUnit, null)
                val transferAccountLabel = extras.getString(Transactions.TRANSFER_ACCOUNT_LABEL)
                transferAccountLabel?.takeIf { it.isNotEmpty() }?.let {
                    repository.findAnyOpenByLabel(it)
                }?.takeIf { it != -1L }?.let {
                    transaction.setTransferAccountId(it)
                    transaction.label = transferAccountLabel
                }
            }
            Transactions.TYPE_SPLIT -> throw NotImplementedException("Building split transaction not yet implemented")
            else -> {
                transaction = Transaction.getNewInstance(account.id, currencyUnit)
            }
        }

        val amountMicros = extras.getLong(Transactions.AMOUNT_MICROS)
        if (amountMicros != 0L) {
            transaction.amount = buildWithMicros(currencyUnit, amountMicros)
        }
        val date = extras.getLong(Transactions.DATE)
        if (date != 0L) {
            transaction.date = date
        }
        val payeeName = extras.getString(Transactions.PAYEE_NAME)
        if (!TextUtils.isEmpty(payeeName)) {
            transaction.payee = payeeName
        }
        if (transaction !is Transfer) {
            val categoryLabel = extras.getString(Transactions.CATEGORY_LABEL)
            if (!TextUtils.isEmpty(categoryLabel)) {
                val categoryToId: MutableMap<String, Long> = mutableMapOf()
                CategoryHelper.insert(repository, categoryLabel!!, categoryToId, false)
                val catId = categoryToId[categoryLabel]
                if (catId != null) {
                    transaction.catId = catId
                    transaction.label = categoryLabel
                }
            }
        }
        val comment = extras.getString(Transactions.COMMENT)
        if (!TextUtils.isEmpty(comment)) {
            transaction.comment = comment
        }
        val methodLabel = extras.getString(Transactions.METHOD_LABEL)
        if (!TextUtils.isEmpty(methodLabel)) {
            val methodId = PaymentMethod.find(methodLabel)
            if (methodId > -1) {
                transaction.methodId = methodId
            }
        }
        val referenceNumber = extras.getString(Transactions.REFERENCE_NUMBER)
        if (!TextUtils.isEmpty(referenceNumber)) {
            transaction.referenceNumber = referenceNumber
        }
        return transaction
    }
}