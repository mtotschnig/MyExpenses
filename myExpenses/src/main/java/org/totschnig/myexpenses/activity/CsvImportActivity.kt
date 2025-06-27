package org.totschnig.myexpenses.activity

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.AdapterView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.evernote.android.state.State
import org.apache.commons.csv.CSVRecord
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.fragment.CsvImportDataFragment
import org.totschnig.myexpenses.fragment.CsvImportParseFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.ContribFeatureNotAvailableException
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.AccountConfiguration
import org.totschnig.myexpenses.viewmodel.CsvImportViewModel
import org.totschnig.myexpenses.viewmodel.CsvImportViewModel.Companion.KEY_HEADER_LINE_POSITION
import java.io.FileNotFoundException
import javax.inject.Inject


class CsvImportActivity : TabbedActivity(), ConfirmationDialogListener {

    @State
    var mUsageRecorded = false

    @State
    var idle = true

    @Inject
    lateinit var repository: Repository

    private val csvImportViewModel: CsvImportViewModel by viewModels()

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = getString(R.string.pref_import_title, "CSV")
        with(injector) {
            inject(csvImportViewModel)
            inject(this@CsvImportActivity)
        }
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                val currentItem = binding.viewPager.currentItem
                if (currentItem > 0) {
                    binding.viewPager.currentItem = currentItem - 1
                } else {
                    onBackPressedDispatcher.onBackPressed()

                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                onBackPressedCallback.isEnabled = position > 0
            }
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val allowed = parseFragment?.isReady == true && idle
        menu.findItem(R.id.PARSE_COMMAND)?.isEnabled = allowed
        menu.findItem(R.id.IMPORT_COMMAND)?.isEnabled = allowed
        super.onPrepareOptionsMenu(menu)
        return true
    }

    override fun dispatchCommand(command: Int, tag: Any?) = when {
        super.dispatchCommand(command, tag) -> true
        command == R.id.CLOSE_COMMAND -> {
            finish()
            true
        }
        else -> false
    }

    override fun doHome() {
        if (binding.viewPager.currentItem == 1) {
            binding.viewPager.currentItem = 0
        } else super.doHome()
    }

    override fun createFragment(position: Int) = when(position) {
        0 -> CsvImportParseFragment.newInstance()
        1 -> CsvImportDataFragment.newInstance()
        else -> throw IllegalArgumentException()
    }

    override fun getItemCount() = 2

    override fun getTitle(position: Int) = when(position) {
        0 -> getString(R.string.menu_parse)
        1 -> getString(R.string.preview)
        else -> throw IllegalArgumentException()
    }


    override fun onPositive(args: Bundle, checked: Boolean) {
        super.onPositive(args, checked)
        if (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE) == R.id.SET_HEADER_COMMAND) {
            dataFragment!!.setHeader(args.getInt(KEY_HEADER_LINE_POSITION))
        }
    }

    private fun showProgress() {
        showProgressSnackBar(getString(R.string.pref_import_title, "CSV"))
        idle = false
        invalidateOptionsMenu()
    }

    private fun hideProgress() {
        dismissSnackBar()
        idle = true
        invalidateOptionsMenu()
    }

    fun parseFile(uri: Uri, delimiter: Char, encoding: String) {
        showProgress()
        csvImportViewModel.withAccountColumn = accountId == 0L
        csvImportViewModel.parseFile(uri, delimiter, encoding).observe(this) { result ->
            hideProgress()
            result.onSuccess {
                binding.viewPager.currentItem = 1
            }.onFailure {
                showSnackBar(
                    when (it) {
                        is FileNotFoundException -> getString(
                            R.string.parse_error_file_not_found,
                            uri
                        )
                        else -> getString(R.string.parse_error_other_exception, it.message)
                    }
                )
            }
        }
    }

    fun importData(dataSet: List<CSVRecord>, columnToFieldMap: IntArray, discardedRows: Int) {
        accountId.takeIf { it != AdapterView.INVALID_ROW_ID }?.also { accountId ->
            showProgress()
            csvImportViewModel.importData(
                dataSet,
                columnToFieldMap,
                dateFormat,
                parseFragment!!.autoFillCategories,
                AccountConfiguration(accountId, currency, accountType),
                parseFragment!!.uri!!
            ).observe(this) { result ->
                hideProgress()
                result.onSuccess { resultList ->
                    if (!mUsageRecorded) {
                        recordUsage(ContribFeature.CSV_IMPORT)
                        mUsageRecorded = true
                    }
                    val msg = StringBuilder()
                    if (discardedRows > 0) {
                        msg.append(" ${getString(R.string.csv_import_records_discarded, discardedRows)}")
                    }
                    msg.append(resultList.joinToString(" ") {
                        "${getString(R.string.import_transactions_success, it.successCount, it.label)}."
                    })

                    showMessage(
                        msg,
                        neutral = MessageDialogFragment.nullButton(R.string.button_label_continue),
                        positive = MessageDialogFragment.Button(
                            R.string.button_label_close,
                            R.id.CLOSE_COMMAND,
                            null
                        )
                    )
                }.onFailure {
                    if (it !is ContribFeatureNotAvailableException) {
                        CrashHandler.report(it)
                    }
                    showSnackBar(it.message ?: it.javaClass.simpleName)
                }
            }
        } ?: kotlin.run {
            val exception = Exception("No account selected")
            CrashHandler.report(exception)
            showSnackBar(exception.message!!)
        }
    }

    private val dataFragment: CsvImportDataFragment?
        get() = supportFragmentManager.findFragmentByTag(
            mSectionsPagerAdapter.getFragmentName(1)
        ) as CsvImportDataFragment?

    private val parseFragment: CsvImportParseFragment?
        get() = supportFragmentManager.findFragmentByTag(
            mSectionsPagerAdapter.getFragmentName(0)
        ) as? CsvImportParseFragment

    val accountId: Long
        get() {
            return parseFragment!!.getSelectedAccountId()
        }

    val currency: String
        get() = parseFragment!!.getSelectedCurrency()

    val dateFormat: QifDateFormat
        get() {
            return parseFragment!!.dateFormat
        }

    val accountType: AccountType
        get() {
            return parseFragment!!.accountType
        }
}