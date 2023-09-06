package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.text.TextUtils
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.countAccounts
import org.totschnig.myexpenses.db2.findAnyOpenByLabel
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.export.qif.QifUtils.convertUnknownTransfersLegacy
import org.totschnig.myexpenses.export.qif.QifUtils.reduceTransfers
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.io.ImportTransaction
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants

data class ImportResult(val label: String, val success: Int, val failure: Int)

val accountTitleToAccount: MutableMap<String, Account> = mutableMapOf()

abstract class ImportDataViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

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
                            ContribFeature.ACCOUNTS_UNLIMITED.buildRemoveLimitation(localizedContext, false)
                )
            }
            val dbAccountId =
                if (TextUtils.isEmpty(account.memo)) null else repository.findAnyOpenByLabel(account.memo)
            var dbAccount: Account
            if (dbAccountId != null) {
                dbAccount = repository.loadAccount(dbAccountId) ?: throw
                Exception(
                    "Exception during QIF import. Did not get instance from DB for id " +
                            dbAccountId
                )
            } else {
                dbAccount = account.toAccount(currencyUnit)
                if (TextUtils.isEmpty(dbAccount.label)) {
                    dbAccount = dbAccount.withLabel(defaultAccountName)
                }
                importCount++
            }
            accountTitleToAccount[account.memo] = dbAccount
        }
        return importCount
    }

    fun insertTransactions(accounts: List<ImportAccount>) {
        val reducedList = reduceTransfers(accounts)
        val finalList = convertUnknownTransfersLegacy(reducedList)
        val t2 = System.currentTimeMillis()
        val count = finalList.size
        for (i in 0 until count) {
            val (_, memo, _, _, transactions) = finalList[i]
            val a = accountTitleToAccount[memo]
            var countTransactions = 0
            if (a != null) {
                countTransactions = insertTransactions(a, transactions)
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

    fun insertTransactions(
        account: Account,
        transactions: List<ImportTransaction>
    ): Int {
        var count = 0
        for (transaction in transactions) {
            val t = transaction.toTransaction(account, currencyUnit)
            t.payeeId = findPayee(transaction.payee)
            // t.projectId = findProject(transaction.categoryClass);
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
}