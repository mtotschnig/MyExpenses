package org.totschnig.myexpenses.activity

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import icepick.State
import org.apache.commons.csv.CSVRecord
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.fragment.CsvImportDataFragment
import org.totschnig.myexpenses.fragment.CsvImportParseFragment
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable
import org.totschnig.myexpenses.viewmodel.CsvImportViewModel
import java.io.FileNotFoundException
import java.util.*


class CsvImportActivity : TabbedActivity(), ConfirmationDialogListener {
    @JvmField
    @State
    var dataReady = false

    @JvmField
    @State
    var mUsageRecorded = false
    private fun setDataReady() {
        dataReady = true
        mSectionsPagerAdapter.notifyDataSetChanged()
    }

    private lateinit var csvImportViewModel: CsvImportViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = getString(R.string.pref_import_title, "CSV")
        csvImportViewModel = ViewModelProvider(this)[CsvImportViewModel::class.java]
    }

    override fun setupTabs() {
        //we only add the first tab, the second one once data has been parsed
        addTab(0)
        if (dataReady) {
            addTab(1)
        }
    }

    private fun addTab(index: Int) {
        when (index) {
            0 -> mSectionsPagerAdapter.addFragment(CsvImportParseFragment.newInstance(), getString(
                    R.string.menu_parse))
            1 -> mSectionsPagerAdapter.addFragment(CsvImportDataFragment.newInstance(), getString(
                    R.string.csv_import_preview))
        }
    }

    override fun onPositive(args: Bundle) {
        if (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE) == R.id.SET_HEADER_COMMAND) {
            (supportFragmentManager.findFragmentByTag(
                    mSectionsPagerAdapter.getFragmentName(1)) as? CsvImportDataFragment)?.setHeader()
        }
    }

    private fun showProgress(total: Int = 0, progress: Int = 0) {
        showProgressSnackBar(getString(R.string.pref_import_title, "CSV"), total, progress)
    }

    fun parseFile(uri: Uri, delimiter: Char, encoding: String) {
        showProgress()
        csvImportViewModel.parseFile(uri, delimiter, encoding).observe(this) { result ->
            dismissSnackbar()
            result.onSuccess { data ->
                if (data.isNotEmpty()) {
                    if (!dataReady) {
                        addTab(1)
                        setDataReady()
                    }
                    (supportFragmentManager.findFragmentByTag(
                            mSectionsPagerAdapter.getFragmentName(1)) as? CsvImportDataFragment)?.let {
                        it.setData(data)
                        binding.viewPager.currentItem = 1
                    }
                } else {
                    showSnackbar(R.string.parse_error_no_data_found)
                }
            }.onFailure {
                showSnackbar(when (it) {
                    is FileNotFoundException -> getString(R.string.parse_error_file_not_found, uri)
                    else -> getString(R.string.parse_error_other_exception, it.message)
                })
            }
        }
    }

    fun importData(dataSet: ArrayList<CSVRecord>, columnToFieldMap: IntArray, discardedRows: SparseBooleanArrayParcelable) {
        val totalToImport =  dataSet.size - discardedRows.size()
        showProgress(total = totalToImport)
        csvImportViewModel.progress.observe(this) {
            showProgress(total = totalToImport, progress = it)
        }
        csvImportViewModel.importData(dataSet, columnToFieldMap, discardedRows, dateFormat) {
            if (accountId == 0L) {
                Account(getString(R.string.pref_import_title, "CSV"), currency, 0, accountType).apply {
                    save()
                }
            } else {
                @Suppress("DEPRECATION") //runs on background thread
                Account.getInstanceFromDb(accountId)
            }
        }.observe(this) { result ->
            result.onSuccess {
                if (!mUsageRecorded) {
                    recordUsage(ContribFeature.CSV_IMPORT)
                    mUsageRecorded = true
                }
                var msg = "${getString(R.string.import_transactions_success, it.first.first, it.first.second)}."
                if (it.second > 0) {
                    msg += " ${getString(R.string.csv_import_records_failed, it.second)}"
                }
                if (it.third > 0) {
                    msg += " ${getString(R.string.csv_import_records_discarded, it.third)}"
                }

                showSnackbar(msg)
            }.onFailure {
                showSnackbar(it.message!!)
            }
        }
    }

    private val parseFragment: CsvImportParseFragment
        get() = supportFragmentManager.findFragmentByTag(
                mSectionsPagerAdapter.getFragmentName(0)) as CsvImportParseFragment

    val accountId: Long
        get() {
            return parseFragment.getSelectedAccountId()
        }

    val currency: CurrencyUnit
        get() {
            return currencyContext[parseFragment.getSelectedCurrency()]
        }

    val dateFormat: QifDateFormat
        get() {
            return parseFragment.dateFormat
        }

    val accountType: AccountType
        get() {
            return parseFragment.accountType
        }
}