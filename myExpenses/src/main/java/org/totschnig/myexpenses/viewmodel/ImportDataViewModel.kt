package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.text.TextUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.AutoFillInfo
import org.totschnig.myexpenses.db2.CategoryHelper
import org.totschnig.myexpenses.db2.countAccounts
import org.totschnig.myexpenses.db2.createAccount
import org.totschnig.myexpenses.db2.findAnyOpenByLabel
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.saveTagsForTransaction
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.export.CategoryInfo
import org.totschnig.myexpenses.export.qif.QifUtils.reduceTransfers
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.io.ImportTransaction
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.io.FileUtils
import org.totschnig.myexpenses.viewmodel.data.Tag

data class ImportResult(val label: String, val successCount: Int)

val accountTitleToAccount: MutableMap<String, Account> = mutableMapOf()

private val payeeToId: MutableMap<String, Long> = mutableMapOf()
private val autoFillCache: MutableMap<Long, AutoFillInfo> = mutableMapOf()
private val categoryToId: MutableMap<String, Long> = mutableMapOf()
val tagToId: MutableMap<String, Long> = mutableMapOf()

abstract class ImportDataViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    private val publishProgressInternal: MutableSharedFlow<String?> = MutableSharedFlow()
    val publishProgress: SharedFlow<String?> = publishProgressInternal

    suspend fun publishProgress(string: String) {
        publishProgressInternal.emit(string)
    }

    private fun getDefaultAccountName(uri: Uri): String {
        var displayName = DialogUtils.getDisplayName(uri)
        if (FileUtils.getExtension(displayName).equals("qif", ignoreCase = true)) {
            displayName = displayName.substring(0, displayName.lastIndexOf('.'))
        }
        return displayName.replace('-', ' ').replace('_', ' ')
    }

    fun insertAccounts(accounts: List<ImportAccount>, currencyUnit: CurrencyUnit, uri: Uri): Int {
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
                        if (it.label.isEmpty()) it.copy(label = getDefaultAccountName(uri)) else it
                    )
                }
            }
            accountTitleToAccount[account.memo] = dbAccount
        }
        return importCount
    }

    suspend fun insertTransactions(
        accounts: List<ImportAccount>,
        currencyUnit: CurrencyUnit,
        autofill: Boolean
    ) = reduceTransfers(accounts).sumOf { (_, memo, _, _, transactions) ->
        accountTitleToAccount[memo]?.let {
            insertTransactions(it, currencyUnit, transactions, autofill)
            publishProgress(
                if (transactions.isEmpty()) getString(
                    R.string.import_transactions_none,
                    it.label
                ) else getString(
                    R.string.import_transactions_success,
                    transactions.size,
                    it.label
                )
            )
            transactions.size
        } ?: 0
    }

    fun insertPayees(payees: Set<String>): Int {
        var count = 0
        for (payee in payees) {
            (repository.findPayee(payee) ?: repository.createPayee(payee).also {
                if (it != null) count++
            })?.let {
                payeeToId[payee] = it
            }
        }
        return count
    }

    fun insertCategories(categories: Set<CategoryInfo>) = categories.sumOf {
        CategoryHelper.insert(repository, it.name, categoryToId, true)
    }

    private fun insertTransactions(
        account: Account,
        currencyUnit: CurrencyUnit,
        transactions: List<ImportTransaction>,
        autofill: Boolean
    ) {
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
                    findCategory(split, s, autofill)
                    s.save()
                }
            } else {
                findCategory(transaction, t, autofill)
            }
            t.save(true)?.let {
                transaction.tags?.let { list ->
                    repository.saveTagsForTransaction(
                        list.mapNotNull { tag ->
                            tagToId[tag]?.let { id -> Tag(id, tag) }
                        },
                        ContentUris.parseId(it)
                    )
                }
            }
        }
    }

    private fun findToAccount(transaction: ImportTransaction, t: Transaction) {
        if (transaction.isTransfer) {
            accountTitleToAccount[transaction.toAccount]?.let {
                t.transferAccountId = it.id
            }
        }
    }

    private fun findCategory(transaction: ImportTransaction, t: Transaction, autofill: Boolean) {
        t.catId = categoryToId[transaction.category] ?: if (autofill) {
            t.payeeId?.let {
                (autoFillCache[it] ?: repository.autoFill(it)
                    ?.apply { autoFillCache[it] = this })?.categoryId
            }
        } else null
    }
}