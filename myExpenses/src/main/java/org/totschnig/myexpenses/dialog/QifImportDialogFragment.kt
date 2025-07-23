package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TableRow
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.QifImport
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.adapter.IdAdapter
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.ui.checkNewAccountLimitation
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.ImportConfigurationViewModel
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create

class QifImportDialogFragment : TextSourceDialogFragment(), AdapterView.OnItemSelectedListener {
    private lateinit var accountSpinner: Spinner
    private lateinit var dateFormatSpinner: Spinner
    private lateinit var currencySpinner: Spinner
    private lateinit var encodingSpinner: Spinner
    private lateinit var autoFillRow: TableRow
    private lateinit var autoFillCategories: CheckBox
    @Suppress("UNCHECKED_CAST")
    private val accountsAdapter: IdAdapter<AccountMinimal>
        get() = accountSpinner.adapter as IdAdapter<AccountMinimal>
    private val currencyAdapter: CurrencyAdapter
        get() = currencySpinner.adapter as CurrencyAdapter
    private var currency: String? = null
    private val currencyViewModel: CurrencyViewModel by viewModels()
    private val viewModel: ImportConfigurationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with((requireActivity().application as MyApplication).appComponent) {
            inject(currencyViewModel)
            inject(viewModel)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null) {
            currency = savedInstanceState.getString(DatabaseConstants.KEY_CURRENCY)
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override val layoutId = R.layout.import_dialog

    override val layoutTitle: String
        get() = getString(
            R.string.pref_import_title,
            format.name
        )

    private val format: ExportFormat
        get() = ExportFormat.QIF

    override val typeName = format.name

    override val prefKey = "import_" + format.extension + "_file_uri"

    override fun onClick(dialog: DialogInterface, id: Int) {
        if (activity == null) {
            return
        }
        if (id == AlertDialog.BUTTON_POSITIVE) {
            val format = dateFormatSpinner.selectedItem as QifDateFormat
            val encoding = encodingSpinner.selectedItem as String
            maybePersistUri()
            prefHandler.putString(PREF_KEY_IMPORT_ENCODING, encoding)
            prefHandler.putString(PREF_KEY_IMPORT_DATE_FORMAT, format.name)
            val currency = currencySpinner.selectedItem as? Currency
            if (currency != null) {
                (activity as QifImport).onSourceSelected(
                    uri!!,
                    format,
                    accountSpinner.selectedItemId,
                    currency.code,
                    mImportTransactions.isChecked,
                    mImportCategories.isChecked,
                    mImportParties.isChecked,
                    encoding,
                    autoFillCategories.isChecked
                )
            } else {
                showSnackBar("Currency is null")
            }
        } else {
            super.onClick(dialog, id)
        }
    }

    override fun setupDialogView(view: View) {
        super.setupDialogView(view)
        autoFillRow = view.findViewById(R.id.AutoFillRow)
        autoFillCategories = view.findViewById(R.id.autofill_categories)
        accountSpinner = view.findViewById(R.id.Account)
        accountSpinner.adapter = IdAdapter<AccountMinimal>(requireContext())
        accountSpinner.onItemSelectedListener = this
        dateFormatSpinner = view.findViewById(R.id.DateFormat)
        dateFormatSpinner.configureDateFormat(requireContext(), prefHandler, PREF_KEY_IMPORT_DATE_FORMAT)
        encodingSpinner = view.findViewById(R.id.Encoding)
        DialogUtils.configureEncoding(
            encodingSpinner,
            requireContext(),
            prefHandler,
            PREF_KEY_IMPORT_ENCODING
        )
        currencySpinner = view.findViewById(R.id.Currency)
        currencySpinner.configureCurrencySpinner(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                currencyViewModel.currencies.collect {
                    val adapter = currencySpinner.adapter as CurrencyAdapter
                    adapter.addAll(it)
                    currencySpinner.setSelection(adapter.getPosition(currencyViewModel.default))
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.accounts.collect {
                    accountsAdapter.clear()
                    accountsAdapter.addAll(it)
                    accountSpinner.setSelection(accountsAdapter.getPosition(viewModel.accountId))
                }
            }
        }

        view.findViewById<View>(R.id.AccountType).isVisible = false
        mImportTransactions.setOnCheckedChangeListener { _, isChecked ->
            autoFillRow.isVisible = isChecked
        }
    }

    override fun onItemSelected(
        parent: AdapterView<*>, view: View, position: Int,
        id: Long
    ) {
        if (parent.id == R.id.Currency) {
            if (viewModel.accountId == 0L) {
                currency = (parent.selectedItem as Currency).code
            }
            return
        }
        accountSpinner.checkNewAccountLimitation(prefHandler, requireContext())
        val selected = accountsAdapter.getItem(position)!!
        viewModel.accountId = selected.id
        val currency =
            if (selected.id == 0L && currency != null) currency else selected.currency
        currencySpinner.setSelection(
            currencyAdapter.getPosition(create(currency!!, requireActivity()))
        )
        currencySpinner.isEnabled = position == 0
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(DatabaseConstants.KEY_CURRENCY, currency)
    }

    companion object {
        const val PREF_KEY_IMPORT_DATE_FORMAT = "import_qif_date_format"
        const val PREF_KEY_IMPORT_ENCODING = "import_qif_encoding"

        @JvmStatic
        fun newInstance() = QifImportDialogFragment()
    }
}