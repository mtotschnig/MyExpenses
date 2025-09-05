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
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.findAccountType
import org.totschnig.myexpenses.db2.findAnyOpenByLabel
import org.totschnig.myexpenses.db2.findParty
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.db2.saveTagsForTransaction
import org.totschnig.myexpenses.dialog.getDisplayName
import org.totschnig.myexpenses.export.CategoryInfo
import org.totschnig.myexpenses.export.qif.QifUtils.reduceTransfers
import org.totschnig.myexpenses.io.ImportAccount
import org.totschnig.myexpenses.io.ImportTransaction
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.ContribFeatureNotAvailableException
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.util.io.FileUtils

data class ImportResult(val label: String, val successCount: Int)

val accountTitleToAccount: MutableMap<String, Account> = mutableMapOf()

private val payeeToId: MutableMap<String, Long> = mutableMapOf()
private val autoFillCache: MutableMap<Long, AutoFillInfo> = mutableMapOf()
private val categoryToId: MutableMap<String, Long> = mutableMapOf()
val tagToId: MutableMap<String, Long> = mutableMapOf()

abstract class ImportDataViewModel(application: Application) :
    ContentResolvingAndroidViewModel(application) {
    abstract val format: String
    private val publishProgressInternal: MutableSharedFlow<String?> = MutableSharedFlow()
    val publishProgress: SharedFlow<String?> = publishProgressInternal

    suspend fun publishProgress(string: String) {
        publishProgressInternal.emit(string)
    }

    private fun getDefaultAccountName(uri: Uri): String {
        var displayName = contentResolver.getDisplayName(uri)
        if (FileUtils.getExtension(displayName).equals("qif", ignoreCase = true)) {
            displayName = displayName.substring(0, displayName.lastIndexOf('.'))
        }
        return displayName.replace('-', ' ').replace('_', ' ')
    }

    fun insertAccounts(accounts: List<ImportAccount>, currencyUnit: CurrencyUnit, uri: Uri): Int {
        val nrOfAccounts = repository.countAccounts()
        val hasUnlimitedAccounts = getApplication<MyApplication>().appComponent.licenceHandler()
            .hasAccessTo(ContribFeature.ACCOUNTS_UNLIMITED)
        var importCount = 0
        for (account: ImportAccount in accounts) {
            val dbAccountId =
                if (TextUtils.isEmpty(account.memo)) null else repository.findAnyOpenByLabel(account.memo)
            val dbAccount = if (dbAccountId != null) {
                repository.loadAccount(dbAccountId) ?: throw Exception(
                    "Exception during QIF import. Did not get instance from DB for id " +
                            dbAccountId
                )
            } else {
                val accountType = account.type?.let {
                    repository.findAccountType(it)
                } ?: throw IllegalArgumentException("Account type is null")
                account.toAccount(currencyUnit, accountType).let {
                    importCount++
                    if (!hasUnlimitedAccounts &&
                        nrOfAccounts + importCount > ContribFeature.FREE_ACCOUNTS
                    ) {
                        throw ContribFeatureNotAvailableException(
                            localizedContext.getString(
                                R.string.qif_parse_failure_found_multiple_accounts,
                                format
                            ) + " " +
                                    ContribFeature.ACCOUNTS_UNLIMITED.buildTrialString(
                                        localizedContext
                                    ) +
                                    ContribFeature.ACCOUNTS_UNLIMITED.buildRemoveLimitation(
                                        localizedContext,
                                        false
                                    )
                        )
                    }
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
        autofill: Boolean,
    ) = reduceTransfers(accounts).map { (_, memo, _, _, transactions) ->
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
            ImportResult(it.label, transactions.size)
        }
    }

    fun insertPayees(payees: Set<String>): Int {
        var count = 0
        for (payee in payees) {
                (repository.findParty(payee) ?: repository.createParty(payee)?.also { count++ }?.id)?.let {
                    payeeToId[payee] = it
                }
        }
        return count
    }

    fun insertCategories(categories: Set<CategoryInfo>, stripQifCategoryClass: Boolean) =
        categories.sumOf {
            CategoryHelper.insert(repository, it.name, categoryToId, stripQifCategoryClass, it.type)
        }

    private fun insertTransactions(
        account: Account,
        currencyUnit: CurrencyUnit,
        transactions: List<ImportTransaction>,
        autofill: Boolean,
    ) {
        for (transaction in transactions) {
            val t = transaction.toTransaction(account, currencyUnit)
            t.party = transaction.payee?.let {
                DisplayParty(payeeToId[it], it)
            }
            findToAccount(transaction, t)
            if (transaction.splits != null) {
                t.save(contentResolver)
                for (split in transaction.splits) {
                    val s = split.toTransaction(account, currencyUnit)
                    s.parentId = t.id
                    s.status = DatabaseConstants.STATUS_UNCOMMITTED
                    findToAccount(split, s)
                    findCategory(split, s, autofill)
                    s.save(contentResolver)
                }
            } else {
                findCategory(transaction, t, autofill)
            }
            findMethod(transaction, t)
            t.save(contentResolver, true)?.let {
                transaction.tags?.let { list ->
                    contentResolver.saveTagsForTransaction(
                        list.mapNotNull { tag -> tagToId[tag] }.toLongArray(),
                        ContentUris.parseId(it)
                    )
                }
            }
        }
    }

    private fun findToAccount(transaction: ImportTransaction, t: Transaction) {
        if (transaction.isTransfer) {
            accountTitleToAccount[transaction.toAccount]?.let { transferAccount ->
                t.transferAccountId = transferAccount.id
                transaction.toAmount?.let {
                    t.transferAmount =
                        Money(currencyContext[transferAccount.currency], transaction.toAmount)
                }
            }
        }
    }

    private fun findCategory(transaction: ImportTransaction, t: Transaction, autofill: Boolean) {
        t.catId = categoryToId[transaction.category] ?: when {
            autofill -> t.party?.id?.let {
                (autoFillCache[it] ?: repository.autoFill(it)
                    ?.apply { autoFillCache[it] = this })?.categoryId
            }

            transaction.isTransfer -> prefHandler.defaultTransferCategory
            else -> null
        }
    }

    private fun findMethod(transaction: ImportTransaction, t: Transaction) {
        t.methodId = transaction.method?.let { label ->
            PreDefinedPaymentMethod.entries.find { localizedContext.getString(it.resId) == label }?.name
                ?: label
        }?.let {
            repository.findPaymentMethod(it)
        }
    }
}