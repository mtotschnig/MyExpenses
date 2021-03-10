package org.totschnig.myexpenses.fragment

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
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
import android.widget.SimpleCursorAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.CsvImportActivity
import org.totschnig.myexpenses.activity.IMPORT_FILENAME_REQUEST_CODE
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.databinding.FilenameBinding
import org.totschnig.myexpenses.databinding.ImportCsvParseBinding
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.ImportFileResultHandler
import org.totschnig.myexpenses.util.ImportFileResultHandler.FileNameHostFragment
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create
import javax.inject.Inject

class CsvImportParseFragment : Fragment(), View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener, FileNameHostFragment {

    private var _binding: ImportCsvParseBinding? = null
    private var _fileNameBinding: FilenameBinding? = null
    private val binding
        get() = _binding!!
    private val fileNameBinding
        get() = _fileNameBinding!!
    private var mUri: Uri? = null
    private lateinit var currencyViewModel: CurrencyViewModel

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

    private var mAccountsCursor: MergeCursor? = null
    private val accountsAdapter: SimpleCursorAdapter? //can be called after onDestroyView
        get() = _binding?.AccountTable?.Account?.adapter as? SimpleCursorAdapter
    private val currencyAdapter: CurrencyAdapter
        get() = binding.AccountTable.Currency.adapter as CurrencyAdapter

    private var accountId: Long = 0
    private var currency: String? = null
    private var type: AccountType? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (savedInstanceState != null) {
            accountId = savedInstanceState.getLong(DatabaseConstants.KEY_ACCOUNTID)
            currency = savedInstanceState.getString(DatabaseConstants.KEY_CURRENCY)
            type = savedInstanceState.getSerializable(DatabaseConstants.KEY_TYPE) as AccountType?
        }
        _binding = ImportCsvParseBinding.inflate(inflater, container, false)
        _fileNameBinding = FilenameBinding.bind(binding.root)
        DialogUtils.configureDateFormat(binding.DateFormatTable.DateFormat, activity, prefHandler, PREF_KEY_IMPORT_CSV_DATE_FORMAT)
        DialogUtils.configureEncoding(binding.EncodingTable.Encoding, activity, prefHandler, PREF_KEY_IMPORT_CSV_ENCODING)
        DialogUtils.configureDelimiter(binding.Delimiter, activity, prefHandler, PREF_KEY_IMPORT_CSV_DELIMITER)
        with(binding.AccountTable.Account) {
            adapter = SimpleCursorAdapter(binding.root.context, android.R.layout.simple_spinner_item, null,
                    arrayOf(DatabaseConstants.KEY_LABEL), intArrayOf(android.R.id.text1), 0).also {
                it.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
            }
            onItemSelectedListener = this@CsvImportParseFragment
        }
        DialogUtils.configureCurrencySpinner(binding.AccountTable.Currency, this)
        currencyViewModel.getCurrencies().observe(viewLifecycleOwner, { currencies: List<Currency?> ->
            currencyAdapter.addAll(currencies)
            binding.AccountTable.Currency.setSelection(currencyAdapter.getPosition(currencyViewModel.default))
        })
        with(binding.AccountTable.AccountType) {
            DialogUtils.configureTypeSpinner(this)
            onItemSelectedListener = this@CsvImportParseFragment
        }
        LoaderManager.getInstance(this).initLoader(0, null, this)
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
        (requireActivity().application as MyApplication).appComponent.inject(this)
        currencyViewModel = ViewModelProvider(this).get(CurrencyViewModel::class.java)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IMPORT_FILENAME_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    uri = ImportFileResultHandler.handleFilenameRequestResult(this, data)
                } catch (throwable: Throwable) {
                    uri = null
                    (requireActivity() as ProtectedFragmentActivity).showSnackbar(throwable.message!!)
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
        outState.putLong(DatabaseConstants.KEY_ACCOUNTID, accountId)
        outState.putString(DatabaseConstants.KEY_CURRENCY, currency)
    }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.csv_parse, menu)
    }

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

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(requireActivity(),
                TransactionProvider.ACCOUNTS_BASE_URI, arrayOf(
                DatabaseConstants.KEY_ROWID,
                DatabaseConstants.KEY_LABEL,
                DatabaseConstants.KEY_CURRENCY,
                DatabaseConstants.KEY_TYPE),
                DatabaseConstants.KEY_SEALED + " = 0 ", null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        val extras = MatrixCursor(arrayOf(
                DatabaseConstants.KEY_ROWID,
                DatabaseConstants.KEY_LABEL,
                DatabaseConstants.KEY_CURRENCY,
                DatabaseConstants.KEY_TYPE
        ))
        extras.addRow(arrayOf(
                "0",
                getString(R.string.menu_create_account),
                Utils.getHomeCurrency().code,
                AccountType.CASH.name
        ))
        mAccountsCursor = MergeCursor(arrayOf(extras, data))
        accountsAdapter?.swapCursor(mAccountsCursor)
        UiUtils.selectSpinnerItemByValue(binding.AccountTable.Account, accountId)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        accountsAdapter?.swapCursor(null)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int,
                                id: Long) {
        if (parent.id == R.id.Currency) {
            if (accountId == 0L) {
                currency = (parent.selectedItem as Currency).code
            }
            return
        }
        if (parent.id == R.id.AccountType) {
            if (accountId == 0L) {
                type = parent.selectedItem as AccountType
            }
            return
        }
        //account selection
        mAccountsCursor?.let { cursor ->
            accountId = id
            cursor.moveToPosition(position)
            with(binding.AccountTable.Currency) {
                setSelection(currencyAdapter.getPosition(create(
                        if (accountId == 0L && currency != null) currency!! else cursor.getString(2),
                        requireActivity())))
                isEnabled = position == 0
            }
            with(binding.AccountTable.AccountType) {
                setSelection((if (accountId == 0L && type != null) type!! else AccountType.valueOf(cursor.getString(3))).ordinal)
                isEnabled = position == 0
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