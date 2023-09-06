package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.text.TextUtils
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.CategoryHelper
import org.totschnig.myexpenses.db2.countAccounts
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.findAnyOpenByLabel
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.export.CategoryInfo
import org.totschnig.myexpenses.export.qif.QifUtils.convertUnknownTransfersLegacy
import org.totschnig.myexpenses.export.qif.QifUtils.reduceTransfers
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.io.ImportTransaction
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants

data class ImportResult(val label: String, val success: Int, val failure: Int)

val accountTitleToAccount: MutableMap<String, Account> = mutableMapOf()

private val payeeToId: MutableMap<String, Long> = mutableMapOf()
private val categoryToId: MutableMap<String, Long> = mutableMapOf()

abstract class ImportDataViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {

    abstract val defaultAccountName: String

    fun insertAccounts(accounts: List<ImportAccount>, currencyUnit: CurrencyUnit): Int {
        val nrOfAccounts = repository.countAccounts(null, null)
        var importCount = 0
        for (account: ImportAccount in accounts) {
            val licenceHandler = getApplication<MyApplication>().appComponent.licenceHandler()
            if (!licenceHandler.hasAccessTo(ContribFeature.ACCOUNTS_UNLIMITED)
                && nrOfAccounts + importCount > ContribFeature.FREE_ACCOUNTS
            ) {
                throw Exception(
                    localizedContext.getString(R.string.qif_parse_failure_found_multiple_accounts) + " " +
                            ContribFeature.ACCOUNTS_UNLIMITED.buildUsageLimitString(localizedContext) +
                            ContribFeature.ACCOUNTS_UNLIMITED.buildRemoveLimitation(
                                localizedContext,
                                false
                            )
                )
            }
            val dbAccountId =
                if (TextUtils.isEmpty(account.memo)) null else repository.findAnyOpenByLabel(account.memo)
            val dbAccount = if (dbAccountId != null) {
                repository.loadAccount(dbAccountId) ?: throw Exception(
                    "Exception during QIF import. Did not get instance from DB for id " +
                            dbAccountId
                )
            } else {
                account.toAccount(currencyUnit).let {
                    importCount++
                    repository.createAccount(
                        if (it.label.isEmpty()) it.copy(label = defaultAccountName) else it
                    )
                }
            }
            accountTitleToAccount[account.memo] = dbAccount
        }
        return importCount
    }

    fun insertTransactions(
        accounts: List<ImportAccount>,
        currencyUnit: CurrencyUnit
    ) {
        val reducedList = reduceTransfers(accounts)
        val finalList = convertUnknownTransfersLegacy(reducedList)
        val count = finalList.size
        for (i in 0 until count) {
            val (_, memo, _, _, transactions) = finalList[i]
            val a = accountTitleToAccount[memo]
            var countTransactions = 0
            if (a != null) {
                countTransactions = insertTransactions(a, currencyUnit, transactions)
                /*                publishProgress(
                                    if (countTransactions == 0) context.getString(
                                        R.string.import_transactions_none,
                                        a.label
                                    ) else context.getString(
                                        R.string.import_transactions_success,
                                        countTransactions,
                                        a.label
                                    )
                                )*/
            }
        }
    }

    fun insertPayees(payees: Set<String>): Int {
        var count = 0
        for (payee in payees) {
            repository.findPayee(payee) ?: repository.createPayee(payee).also {
                if (it != null) count++
            }?.let {
                payeeToId[payee] = it
            }
        }
        return count
    }

    fun insertCategories(categories: Set<CategoryInfo>) = categories.sumOf {
        CategoryHelper.insert(repository, it.name, categoryToId, true)
    }

    fun insertTransactions(
        account: Account,
        currencyUnit: CurrencyUnit,
        transactions: List<ImportTransaction>
    ): Int {
        var count = 0
        for (transaction in transactions) {
            val t = transaction.toTransaction(account, currencyUnit)
            t.payeeId = payeeToId[transaction.payee]
            findToAccount(transaction, t)
            if (transaction.splits != null) {
                t.save()
                for (split in transaction.splits) {
                    val s = split.toTransaction(account, currencyUnit)
                    s.parentId = t.id
                    s.status = DatabaseConstants.STATUS_UNCOMMITTED
                    findToAccount(split, s)
                    findCategory(split, s)
                    s.save()
                }
            } else {
                findCategory(transaction, t)
            }
            t.save(true)
            count++
        }
        return count
    }

    private fun findToAccount(transaction: ImportTransaction, t: Transaction) {
        if (transaction.isTransfer) {
            accountTitleToAccount[transaction.toAccount]?.let {
                t.transferAccountId = it.id
            }
        }
    }

    private fun findCategory(transaction: ImportTransaction, t: Transaction) {
        t.catId = categoryToId[transaction.category]
    }
}