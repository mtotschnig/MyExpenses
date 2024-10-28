package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
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

private fun BooleanArray.saveSelection() = bundleOf("selection" to this)

private fun Bundle.restoreSelection() = getBooleanArray("selection")

class CsvImportViewModel(application: Application, val savedStateHandle: SavedStateHandle) :
    ImportDataViewModel(application) {

    companion object {
        const val KEY_DATA = "DATA"
        const val KEY_SELECTED_ROWS = "SELECTED_ROWS"
        const val KEY_WITH_ACCOUNT_COLUMN = "WITH_ACCOUNT_COLUMN"
        const val KEY_HEADER_LINE_POSITION = "HEADER_LINE_POSITION"
        const val KEY_MAPPING = "MAPPING"
    }

    override val format = "CSV"

    val dataFlow: StateFlow<List<CSVRecord>> = savedStateHandle.getStateFlow(KEY_DATA, emptyList())

    private var data: List<CSVRecord>
        get() = savedStateHandle[KEY_DATA] ?: emptyList()
        set(value) {
            selectedRows = BooleanArray(value.size) { true }
            headerLine = -1
            mapping = null
            savedStateHandle[KEY_DATA] = value
        }

    var selectedRows: BooleanArray

    fun selectRow(position: Int) {
        selectedRows[position] = true
    }

    fun unselectRow(position: Int) {
        selectedRows[position] = false
    }

    fun isSelected(position: Int) = selectedRows[position]

    init {
        selectedRows = savedStateHandle.get<Bundle>(KEY_SELECTED_ROWS)?.restoreSelection() ?: BooleanArray(0)
        savedStateHandle.setSavedStateProvider(KEY_SELECTED_ROWS) {
            selectedRows.saveSelection()
        }
    }

    var withAccountColumn: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_WITH_ACCOUNT_COLUMN) == true
        set(value) {
            savedStateHandle.set(KEY_WITH_ACCOUNT_COLUMN, value)
        }

    var headerLine: Int
        get() = savedStateHandle.get<Int>(KEY_HEADER_LINE_POSITION) ?: -1
        set(value) {
            savedStateHandle.set(KEY_HEADER_LINE_POSITION, value)
        }

    var mapping: IntArray?
        get() = savedStateHandle.get<IntArray>(KEY_MAPPING)
        set(value) {
            savedStateHandle.set(KEY_MAPPING, value)
        }

    fun parseFile(uri: Uri, delimiter: Char, encoding: String): LiveData<Result<Unit>> =
        liveData(context = coroutineContext()) {
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

    fun importData(
        data: List<CSVRecord>,
        columnToFieldMap: IntArray,
        dateFormat: QifDateFormat,
        autoFill: Boolean,
        accountConfiguration: AccountConfiguration,
        uri: Uri,
    ): LiveData<Result<List<ImportResult>>> = liveData(context = coroutineContext()) {

        emit(runCatching {
            val currencyUnit = currencyContext[accountConfiguration.currency]
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
            insertCategories(parser.categories, false)

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