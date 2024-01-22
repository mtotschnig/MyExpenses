package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.export.qif.QifBufferedReader
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.export.qif.QifParser
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.TransactionProvider
import java.io.BufferedReader
import java.io.InputStreamReader

class QifImportViewModel(application: Application) : ImportDataViewModel(application) {

    override val format= "QIF"

    fun importData(
        uri: Uri,
        qifDateFormat: QifDateFormat,
        accountId: Long,
        currencyUnit: CurrencyUnit,
        withTransactions: Boolean,
        withCategories: Boolean,
        withParties: Boolean,
        encoding: String?,
        autoFillCategories: Boolean
    ): LiveData<Result<Unit>> = liveData(context = coroutineContext()) {

        emit(runCatching {
            QifBufferedReader(
                BufferedReader(
                    InputStreamReader(
                        contentResolver.openInputStream(uri),
                        encoding
                    )
                )
            ).use {
                val parser = QifParser(it, qifDateFormat, currencyUnit)
                parser.parse()
                publishProgress(getString(
                            R.string.qif_parse_result,
                            parser.accounts.size.toString(),
                            parser.categories.size.toString(),
                            parser.payees.size.toString()
                        )
                )
                contentResolver.call(
                    TransactionProvider.DUAL_URI,
                    TransactionProvider.METHOD_BULK_START,
                    null,
                    null
                )
                doImport(parser, withParties, withCategories, withTransactions, accountId, currencyUnit, uri, autoFillCategories)
                contentResolver.call(
                    TransactionProvider.DUAL_URI,
                    TransactionProvider.METHOD_BULK_END,
                    null,
                    null
                )
                Unit
            }
        })
    }

    private suspend fun doImport(
        parser: QifParser,
        withParties: Boolean,
        withCategories: Boolean,
        withTransactions: Boolean,
        accountId: Long,
        currencyUnit: CurrencyUnit,
        uri: Uri,
        autoFillCategories: Boolean
    ) {
        if (withParties) {
            val totalParties = insertPayees(parser.payees)
            publishProgress(
                if (totalParties == 0) getString(R.string.import_parties_none) else getString(
                    R.string.import_parties_success,
                    totalParties
                )
            )
        }
        if (withCategories) {
            val totalCategories = insertCategories(parser.categories, true)
            publishProgress(
                if (totalCategories == 0) getString(R.string.import_categories_none) else getString(
                    R.string.import_categories_success,
                    totalCategories
                )
            )
        }
        if (withTransactions) {
            if (accountId == 0L) {
                val importedAccounts = insertAccounts(parser.accounts, currencyUnit, uri)
                publishProgress(
                    if (importedAccounts == 0) getString(R.string.import_accounts_none) else getString(
                        R.string.import_accounts_success,
                        importedAccounts
                    )
                )
            } else {
                if (parser.accounts.size > 1) {
                    publishProgress(
                        getString(R.string.qif_parse_failure_found_multiple_accounts, format)
                                + " "
                                + getString(R.string.qif_parse_failure_found_multiple_accounts_cannot_merge)
                    )
                    return
                }
                if (parser.accounts.isEmpty()) {
                    return
                }
                accountTitleToAccount[parser.accounts[0].memo] = repository.loadAccount(accountId)
                    ?: throw Exception("Exception during QIF import. Did not get instance from DB for id $accountId")
            }
            insertTransactions(parser.accounts, currencyUnit, autoFillCategories)
        }
    }
}