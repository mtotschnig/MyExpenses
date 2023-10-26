package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.extractTagIds
import org.totschnig.myexpenses.db2.loadAccount
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.io.CSVParser
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.ResultUnit
import java.io.InputStreamReader

data class AccountConfiguration(val id: Long, val currency: String, val type: AccountType)

class CsvImportViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
    ImportDataViewModel(application) {

    override val format = "CSV"

    val dataFlow: StateFlow<List<CSVRecord>> = savedStateHandle.getStateFlow("data", emptyList())

    private var data: List<CSVRecord>
        get() = savedStateHandle["data"] ?: emptyList()
        set(value) {
            savedStateHandle["data"] = value
        }


    fun parseFile(uri: Uri, delimiter: Char, encoding: String): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
            contentResolver.openInputStream(uri)?.use {
                try {
                    contentResolver.openInputStream(uri)?.use {
                        data = CSVFormat.DEFAULT.withDelimiter(delimiter)
                            .parse(InputStreamReader(it, encoding)).records
                        emit(ResultUnit)
                    } ?: throw java.lang.Exception("OpenInputStream returned null")
                } catch (e: Exception) {
                    emit(Result.failure(e))
                }
            }
        }

    fun importData(
        data: List<CSVRecord>,
        columnToFieldMap: IntArray,
        dateFormat: QifDateFormat,
        autoFill: Boolean,
        accountConfiguration: AccountConfiguration,
        uri: Uri
    ): LiveData<Result<List<ImportResult>>> = liveData(context = coroutineContext()) {

        emit(runCatching {
            val currencyUnit = currencyContext.get(accountConfiguration.currency)
            val parser = CSVParser(
                localizedContext,
                data,
                columnToFieldMap,
                dateFormat,
                currencyUnit,
                accountConfiguration.type
            )
            parser.parse()
            val accounts = parser.accounts

            contentResolver.call(
                TransactionProvider.DUAL_URI,
                TransactionProvider.METHOD_BULK_START,
                null,
                null
            )

            if (columnToFieldMap.indexOf(R.string.account) > -1) {
                insertAccounts(accounts, currencyUnit, uri)
            } else {
                accountTitleToAccount[accounts[0].memo] = if (accountConfiguration.id == 0L)
                    Account(
                        label = getString(R.string.pref_import_title, "CSV"),
                        currency = accountConfiguration.currency,
                        openingBalance = 0,
                        type = accountConfiguration.type
                    ).createIn(repository)
                else repository.loadAccount(accountConfiguration.id)!!
            }

            insertPayees(parser.payees)
            repository.extractTagIds(parser.tags, tagToId)
            insertCategories(parser.categories)

            val count = insertTransactions(accounts, currencyUnit, autoFill)

            contentResolver.call(
                TransactionProvider.DUAL_URI,
                TransactionProvider.METHOD_BULK_END,
                null,
                null
            )
            count.filterNotNull()
        })
    }
}