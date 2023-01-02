package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SimpleCursorAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import kotlinx.coroutines.flow.collect
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.QifImport
import org.totschnig.myexpenses.adapter.CurrencyAdapter
import org.totschnig.myexpenses.export.qif.QifDateFormat
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.Currency.Companion.create

class QifImportDialogFragment : TextSourceDialogFragment(), LoaderManager.LoaderCallbacks<Cursor>,
    AdapterView.OnItemSelectedListener {
    private lateinit var mAccountSpinner: Spinner
    private lateinit var mDateFormatSpinner: Spinner
    private lateinit var mCurrencySpinner: Spinner
    private lateinit var mEncodingSpinner: Spinner
    private var mAccountsAdapter: SimpleCursorAdapter? = null
    private var mAccountsCursor: MergeCursor? = null
    private var accountId: Long = 0
    private var currency: String? = null
    private lateinit var currencyViewModel: CurrencyViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currencyViewModel = ViewModelProvider(this)[CurrencyViewModel::class.java]
        (requireActivity().application as MyApplication).appComponent.inject(currencyViewModel)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null) {
            accountId = savedInstanceState.getLong(DatabaseConstants.KEY_ACCOUNTID)
            currency = savedInstanceState.getString(DatabaseConstants.KEY_CURRENCY)
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun getLayoutId(): Int {
        return R.layout.import_dialog
    }

    override fun getLayoutTitle(): String {
        return getString(
            R.string.pref_import_title,
            format.name
        )
    }

    private val format: ExportFormat
        get() = ExportFormat.QIF

    override fun getTypeName(): String {
        return format.name
    }

    override fun getPrefKey(): String {
        return "import_" + format.extension + "_file_uri"
    }

    override fun onClick(dialog: DialogInterface, id: Int) {
        if (activity == null) {
            return
        }
        if (id == AlertDialog.BUTTON_POSITIVE) {
            val format = mDateFormatSpinner.selectedItem as QifDateFormat
            val encoding = mEncodingSpinner.selectedItem as String
            maybePersistUri()
            prefHandler.putString(PREF_KEY_IMPORT_ENCODING, encoding)
            prefHandler.putString(PREF_KEY_IMPORT_DATE_FORMAT, format.name)
            (activity as QifImport?)!!.onSourceSelected(
                mUri,
                format,
                mAccountSpinner.selectedItemId,
                (mCurrencySpinner.selectedItem as Currency).code,
                mImportTransactions.isChecked,
                mImportCategories.isChecked,
                mImportParties.isChecked,
                encoding
            )
        } else {
            super.onClick(dialog, id)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(
            requireActivity(),
            TransactionProvider.ACCOUNTS_BASE_URI, arrayOf(
                DatabaseConstants.KEY_ROWID,
                DatabaseConstants.KEY_LABEL,
                DatabaseConstants.KEY_CURRENCY
            ),
            DatabaseConstants.KEY_SEALED + " = 0 ", null, null
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        val extras = MatrixCursor(
            arrayOf(
                DatabaseConstants.KEY_ROWID,
                DatabaseConstants.KEY_LABEL,
                DatabaseConstants.KEY_CURRENCY
            )
        )
        extras.addRow(
            arrayOf(
                "0",
                getString(R.string.menu_create_account),
                Utils.getHomeCurrency().code
            )
        )
        mAccountsCursor = MergeCursor(arrayOf(extras, data))
        mAccountsAdapter!!.swapCursor(mAccountsCursor)
        UiUtils.selectSpinnerItemByValue(mAccountSpinner, accountId)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mAccountsCursor = null
        mAccountsAdapter!!.swapCursor(null)
    }

    override fun setupDialogView(view: View) {
        super.setupDialogView(view)
        mAccountSpinner = view.findViewById(R.id.Account)
        val wrappedCtx = view.context
        mAccountsAdapter = SimpleCursorAdapter(
            wrappedCtx,
            android.R.layout.simple_spinner_item,
            null,
            arrayOf(DatabaseConstants.KEY_LABEL),
            intArrayOf(android.R.id.text1),
            0
        )
        mAccountsAdapter!!.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        mAccountSpinner.adapter = mAccountsAdapter
        mAccountSpinner.onItemSelectedListener = this
        LoaderManager.getInstance(this).initLoader(0, null, this)
        mDateFormatSpinner = view.findViewById(R.id.DateFormat)
        mDateFormatSpinner.configureDateFormat(wrappedCtx, prefHandler, PREF_KEY_IMPORT_DATE_FORMAT)
        mEncodingSpinner = view.findViewById(R.id.Encoding)
        DialogUtils.configureEncoding(
            mEncodingSpinner,
            wrappedCtx,
            prefHandler,
            PREF_KEY_IMPORT_ENCODING
        )
        mCurrencySpinner = view.findViewById(R.id.Currency)
        DialogUtils.configureCurrencySpinner(mCurrencySpinner, this)
        lifecycleScope.launchWhenStarted {
            currencyViewModel.currencies.collect {
                val adapter = mCurrencySpinner.adapter as CurrencyAdapter
                adapter.addAll(it)
                mCurrencySpinner.setSelection(adapter.getPosition(currencyViewModel.default))
            }
        }
        view.findViewById<View>(R.id.AccountType).visibility =
            View.GONE //QIF data should specify type
    }

    override fun onItemSelected(
        parent: AdapterView<*>, view: View, position: Int,
        id: Long
    ) {
        if (parent.id == R.id.Currency) {
            if (accountId == 0L) {
                currency = (parent.selectedItem as Currency).code
            }
            return
        }
        if (mAccountsCursor != null) {
            accountId = id
            mAccountsCursor!!.moveToPosition(position)
            val currency =
                if (accountId == 0L && currency != null) currency else mAccountsCursor!!.getString(2) //2=KEY_CURRENCY
            mCurrencySpinner.setSelection(
                (mCurrencySpinner.adapter as ArrayAdapter<Currency?>)
                    .getPosition(create(currency!!, requireActivity()))
            )
            mCurrencySpinner.isEnabled = position == 0
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(DatabaseConstants.KEY_ACCOUNTID, accountId)
        outState.putSerializable(DatabaseConstants.KEY_CURRENCY, currency)
    }

    companion object {
        const val PREF_KEY_IMPORT_DATE_FORMAT = "import_qif_date_format"
        const val PREF_KEY_IMPORT_ENCODING = "import_qif_encoding"

        @JvmStatic
        fun newInstance(): QifImportDialogFragment {
            return QifImportDialogFragment()
        }
    }
}