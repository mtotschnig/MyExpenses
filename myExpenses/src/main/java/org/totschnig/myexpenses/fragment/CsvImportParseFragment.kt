package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.CsvImportActivity
import org.totschnig.myexpenses.activity.IMPORT_FILENAME_REQUEST_CODE
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.IdAdapter
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.databinding.FilenameBinding
import org.totschnig.myexpenses.databinding.ImportCsvParseBinding
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.configureDateFormat
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.ImportFileResultHandler
import org.totschnig.myexpenses.util.ImportFileResultHandler.FileNameHostFragment
import org.totschnig.myexpenses.util.checkNewAccountLimitation
import org.totschnig.myexpenses.viewmodel.Account
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.ImportViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import javax.inject.Inject

class CsvImportParseFragment : Fragment(), View.OnClickListener, AdapterView.OnItemSelectedListener, FileNameHostFragment {

    private var _binding: ImportCsvParseBinding? = null
    private var _fileNameBinding: FilenameBinding? = null
    private val binding
        get() = _binding!!
    private val fileNameBinding
        get() = _fileNameBinding!!
    private var mUri: Uri? = null
    private val currencyViewModel: CurrencyViewModel by viewModels()
    private val viewModel: ImportViewModel by viewModels()

    @Inject
    lateinit var prefHandler: PrefHandler
    override fun getPrefKey(): String {
        return "import_csv_file_uri"
    }

    override fun getUri(): Uri? {
        return mUri
    }

    override fun setUri(uri: Uri?) {
        this.mUri = uri
        requireActivity().invalidateOptionsMenu()
    }

    override fun getFilenameEditText(): EditText {
        return fileNameBinding.Filename
    }

    @Suppress("UNCHECKED_CAST")
    private val accountsAdapter: IdAdapter<Account>
        get() = binding.AccountTable.Account.adapter as IdAdapter<Account>
    private val currencyAdapter: CurrencyAdapter
        get() = binding.AccountTable.Currency.adapter as CurrencyAdapter

    private var currency: String? = null
    private var type: AccountType? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (savedInstanceState != null) {
            currency = savedInstanceState.getString(DatabaseConstants.KEY_CURRENCY)
            type = savedInstanceState.getSerializable(DatabaseConstants.KEY_TYPE) as AccountType?
        }
        _binding = ImportCsvParseBinding.inflate(inflater, container, false)
        _fileNameBinding = FilenameBinding.bind(binding.root)
        binding.DateFormatTable.DateFormat.configureDateFormat(requireContext(), prefHandler, PREF_KEY_IMPORT_CSV_DATE_FORMAT)
        DialogUtils.configureEncoding(binding.EncodingTable.Encoding, activity, prefHandler, PREF_KEY_IMPORT_CSV_ENCODING)
        DialogUtils.configureDelimiter(binding.Delimiter, activity, prefHandler, PREF_KEY_IMPORT_CSV_DELIMITER)
        with(binding.AccountTable.Account) {
            adapter = IdAdapter<Account>(requireContext())
            onItemSelectedListener = this@CsvImportParseFragment
        }
        DialogUtils.configureCurrencySpinner(binding.AccountTable.Currency, this)
        lifecycleScope.launchWhenStarted {
            currencyViewModel.currencies.collect { currencies: List<Currency?> ->
                currencyAdapter.addAll(currencies)
                binding.AccountTable.Currency.setSelection(currencyAdapter.getPosition(currencyViewModel.default))
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.accounts.collect {
                accountsAdapter.addAll(it)
                binding.AccountTable.Account.setSelection(accountsAdapter.getPosition(viewModel.accountId))
            }
        }
        with(binding.AccountTable.AccountType) {
            DialogUtils.configureTypeSpinner(this)
            onItemSelectedListener = this@CsvImportParseFragment
        }
        fileNameBinding.btnBrowse.setOnClickListener(this)
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _fileNameBinding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        with((requireActivity().application as MyApplication).appComponent) {
            inject(this@CsvImportParseFragment)
            inject(currencyViewModel)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == IMPORT_FILENAME_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                try {
                    uri = intent.data
                    ImportFileResultHandler.handleFilenameRequestResult(this, uri)
                } catch (throwable: Throwable) {
                    uri = null
                    (requireActivity() as ProtectedFragmentActivity).showSnackBar(throwable.message!!)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ImportFileResultHandler.handleFileNameHostOnResume(this, prefHandler)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mUri != null) {
            outState.putString(prefKey, mUri.toString())
        }
        outState.putString(DatabaseConstants.KEY_CURRENCY, currency)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            val restoredUriString = savedInstanceState.getString(prefKey)
            if (restoredUriString != null) {
                val restoredUri = Uri.parse(restoredUriString)
                val displayName = DialogUtils.getDisplayName(restoredUri)
                if (displayName != null) {
                    uri = restoredUri
                    fileNameBinding.Filename.setText(displayName)
                }
            }
        }
    }

    override fun onClick(v: View) {
        DialogUtils.openBrowse(mUri, this)
    }

    override fun checkTypeParts(typeParts: Array<String>, extension: String): Boolean {
        return ImportFileResultHandler.checkTypePartsDefault(typeParts)
    }

    override fun getTypeName(): String {
        return "CSV"
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.csv_parse, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == R.id.PARSE_COMMAND) {
        val format = binding.DateFormatTable.DateFormat.selectedItem as QifDateFormat
        val encoding = binding.EncodingTable.Encoding.selectedItem as String
        val delimiter = resources.getStringArray(R.array.pref_csv_import_delimiter_values)[binding.Delimiter.selectedItemPosition]
        with(prefHandler) {
            putString(PREF_KEY_IMPORT_CSV_DELIMITER, delimiter)
            putString(PREF_KEY_IMPORT_CSV_ENCODING, encoding)
            putString(PREF_KEY_IMPORT_CSV_DATE_FORMAT, format.name)
        }
        ImportFileResultHandler.maybePersistUri(this, prefHandler)
        (activity as? CsvImportActivity)?.parseFile(mUri!!, delimiter[0], encoding)
        true
    } else super.onOptionsItemSelected(item)

    val isReady: Boolean
        get() {
            return mUri != null && (prefHandler.getBoolean(PrefKey.NEW_ACCOUNT_ENABLED, true) ||
                    binding.AccountTable.Account.selectedItemId != 0L)
        }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int,
                                id: Long) {
        when (parent.id) {
            R.id.Currency -> {
                if (viewModel.accountId == 0L) {
                    currency = (parent.selectedItem as Currency).code
                }
                return
            }
            R.id.AccountType -> {
                if (viewModel.accountId == 0L) {
                    type = parent.selectedItem as AccountType
                }
                return
            }
            else -> {
                requireActivity().invalidateOptionsMenu()
                val selected = accountsAdapter.getItem(position)!!
                viewModel.accountId = selected.id

                binding.AccountTable.Account.checkNewAccountLimitation(
                    prefHandler,
                    requireContext()
                )
                with(binding.AccountTable.Currency) {
                    setSelection(
                        currencyAdapter.getPosition(
                            create(
                                if (selected.id == 0L && currency != null) currency!! else selected.currency,
                                requireActivity()
                            )
                        )
                    )
                    isEnabled = position == 0
                }
                with(binding.AccountTable.AccountType) {
                    setSelection(
                        (if (selected.id == 0L && type != null) type!! else selected.type).ordinal
                    )
                    isEnabled = position == 0
                }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun getSelectedAccountId(): Long {
        return binding.AccountTable.Account.selectedItemId
    }

    fun getSelectedCurrency(): String {
        return (binding.AccountTable.Currency.selectedItem as Currency).code
    }

    val dateFormat: QifDateFormat
        get() = binding.DateFormatTable.DateFormat.selectedItem as QifDateFormat

    val accountType: AccountType
        get() = binding.AccountTable.AccountType.selectedItem as AccountType

    val autoFillCategories: Boolean
        get() = binding.autofillCategories.isChecked

    companion object {
        const val PREF_KEY_IMPORT_CSV_DATE_FORMAT = "import_csv_date_format"
        const val PREF_KEY_IMPORT_CSV_ENCODING = "import_csv_encoding"
        const val PREF_KEY_IMPORT_CSV_DELIMITER = "import_csv_delimiter"
        fun newInstance(): CsvImportParseFragment {
            return CsvImportParseFragment()
        }
    }
}