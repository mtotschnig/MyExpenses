/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.database.Cursor
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.TransactionAdapter
import org.totschnig.myexpenses.dialog.TransactionDetailFragment.Companion.newInstance
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.TransactionListViewModel
import javax.inject.Inject

class TransactionListDialogFragment : BaseDialogFragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private lateinit var mAccount: Account
    private lateinit var mAdapter: TransactionAdapter
    private var isMain = false
    private lateinit var viewModel: TransactionListViewModel

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var currencyContext: CurrencyContext

    private var catId: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[TransactionListViewModel::class.java]
        with(requireArguments()) {
            viewModel.account(getLong(DatabaseConstants.KEY_ACCOUNTID)).observe(this@TransactionListDialogFragment, {
                mAccount = it
                fillData()
            })
            isMain = getBoolean(KEY_IS_MAIN)
            catId = getLong(DatabaseConstants.KEY_CATID)
        }
        (requireActivity().applicationContext as MyApplication).appComponent.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilder()
        val listView = ListView(builder.context)
        val padding = resources.getDimensionPixelSize(R.dimen.general_padding)
        listView.setPadding(padding, 0, padding, 0)
        listView.scrollBarStyle = ListView.SCROLLBARS_OUTSIDE_INSET
        mAdapter = object : TransactionAdapter(
                requireArguments().getSerializable(DatabaseConstants.KEY_GROUPING) as Grouping?,
                activity,
                R.layout.expense_row,
                null,
                0, currencyFormatter, prefHandler, currencyContext) {
            override fun getCatText(catText: CharSequence,
                                    label_sub: String?): CharSequence {
                return if (catId == 0L) super.getCatText(catText, label_sub) else if (isMain && label_sub != null) label_sub else ""
            }
        }
        listView.adapter = mAdapter
        listView.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, id: Long ->
            val c = mAdapter.getItem(position) as Cursor
            val index = c.getColumnIndexOrThrow(DatabaseConstants.KEY_PARENTID)
            val idToDisplay = if (c.isNull(index)) id else c.getLong(index)
            lifecycleScope.launchWhenResumed {
                with(parentFragmentManager)  {
                    if (findFragmentByTag(TransactionDetailFragment::class.java.name) == null) {
                        newInstance(idToDisplay).show(this, TransactionDetailFragment::class.java.name)
                    }
                }
            }
        }
        //TODO prettify layout
//    View titleView = LayoutInflater.from(getActivity()).inflate(R.layout.transaction_list_dialog_title, null);
//    ((TextView) titleView.findViewById(R.id.label)).setText(getArguments().getString(KEY_LABEL));
//    ((TextView) titleView.findViewById(R.id.amount)).setText("TBF");
        return builder.setTitle(R.string.progress_dialog_loading)
                .setView(listView)
                .setPositiveButton(android.R.string.ok, null)
                .create()
    }

    private fun fillData() {
        (dialog as? AlertDialog)?.setTitle(requireArguments().getString(DatabaseConstants.KEY_LABEL))
        mAdapter.setAccount(mAccount)
        val loaderManager = LoaderManager.getInstance(this)
        loaderManager.initLoader(TRANSACTION_CURSOR, null, this)
        loaderManager.initLoader(SUM_CURSOR, null, this)
    }

    override fun onCreateLoader(id: Int, arg1: Bundle?): Loader<Cursor> {
        var selection: String?
        val accountSelect: String?
        var amountCalculation = DatabaseConstants.KEY_AMOUNT
        var selectionArgs: Array<String>?
        when {
            mAccount.isHomeAggregate -> {
                selection = ""
                accountSelect = null
                amountCalculation = DatabaseConstants.getAmountHomeEquivalent(DatabaseConstants.VIEW_EXTENDED)
            }
            mAccount.isAggregate -> {
                selection = DatabaseConstants.KEY_ACCOUNTID + " IN " +
                        "(SELECT " + DatabaseConstants.KEY_ROWID + " from " + DatabaseConstants.TABLE_ACCOUNTS + " WHERE " + DatabaseConstants.KEY_CURRENCY + " = ? AND " +
                        DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS + "=0)"
                accountSelect = mAccount.currencyUnit.code
            }
            else -> {
                selection = DatabaseConstants.KEY_ACCOUNTID + " = ?"
                accountSelect = mAccount.id.toString()
            }
        }
        if (catId == 0L) {
            if (!TextUtils.isEmpty(selection)) {
                selection += " AND "
            }
            selection += DatabaseConstants.WHERE_NOT_SPLIT_PART
            selectionArgs = accountSelect?.let { arrayOf(it) }
        } else {
            if (!TextUtils.isEmpty(selection)) {
                selection += " AND "
            }
            selection += (DatabaseConstants.KEY_CATID + " IN (SELECT " + DatabaseConstants.KEY_ROWID + " FROM "
                    + DatabaseConstants.TABLE_CATEGORIES + " WHERE " + DatabaseConstants.KEY_PARENTID + " = ? OR "
                    + DatabaseConstants.KEY_ROWID + " = ?)")
            val catSelect = catId.toString()
            selectionArgs = accountSelect?.let { arrayOf(it, catSelect, catSelect) }
                    ?: arrayOf(catSelect, catSelect)
        }
        val groupingClause = requireArguments().getString(KEY_GROUPING_CLAUSE)
        if (groupingClause != null) {
            if (!TextUtils.isEmpty(selection)) {
                selection += " AND "
            }
            selection += groupingClause
            selectionArgs = Utils.joinArrays(selectionArgs, requireArguments().getStringArray(KEY_GROUPING_ARGS))
        }
        val type = requireArguments().getInt(DatabaseConstants.KEY_TYPE)
        if (type != 0) {
            if (!TextUtils.isEmpty(selection)) {
                selection += " AND "
            }
            selection += DatabaseConstants.KEY_AMOUNT + (if (type == -1) "<" else ">") + "0"
        }
        if (!requireArguments().getBoolean(KEY_WITH_TRANSFERS)) {
            if (!TextUtils.isEmpty(selection)) {
                selection += " AND "
            }
            selection += DatabaseConstants.KEY_TRANSFER_PEER + " is null"
        }
        when (id) {
            TRANSACTION_CURSOR -> return CursorLoader(requireActivity(),
                    mAccount.getExtendedUriForTransactionList(type != 0), mAccount.extendedProjectionForTransactionList,
                    selection, selectionArgs, null)
            SUM_CURSOR -> return CursorLoader(requireActivity(),
                    Transaction.EXTENDED_URI, arrayOf("sum($amountCalculation)"), selection,
                    selectionArgs, null)
        }
        throw IllegalArgumentException()
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
        when (loader.id) {
            TRANSACTION_CURSOR -> mAdapter.swapCursor(cursor)
            SUM_CURSOR -> {
                cursor.moveToFirst()
                val title = requireArguments().getString(DatabaseConstants.KEY_LABEL) + TABS +
                        currencyFormatter.convAmount(cursor.getLong(0), mAccount.currencyUnit)
                dialog!!.setTitle(title)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        when (loader.id) {
            TRANSACTION_CURSOR -> mAdapter.swapCursor(null)
        }
    }

    companion object {
        private const val KEY_IS_MAIN = "is_main"
        private const val KEY_GROUPING_CLAUSE = "grouping_clause"
        private const val KEY_GROUPING_ARGS = "grouping_args"
        private const val KEY_WITH_TRANSFERS = "with_transfers"
        const val TRANSACTION_CURSOR = 1
        const val SUM_CURSOR = 2
        private const val TABS = "\u0009\u0009\u0009\u0009"
        @JvmStatic
        fun newInstance(
                account_id: Long, cat_id: Long, isMain: Boolean, grouping: Grouping?, groupingClause: String?,
                groupingArgs: Array<String?>?, label: String?, type: Int, withTransfers: Boolean): TransactionListDialogFragment {
            return TransactionListDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(DatabaseConstants.KEY_ACCOUNTID, account_id)
                    putLong(DatabaseConstants.KEY_CATID, cat_id)
                    putString(KEY_GROUPING_CLAUSE, groupingClause)
                    putSerializable(DatabaseConstants.KEY_GROUPING, grouping)
                    putStringArray(KEY_GROUPING_ARGS, groupingArgs)
                    putString(DatabaseConstants.KEY_LABEL, label)
                    putBoolean(KEY_IS_MAIN, isMain)
                    putInt(DatabaseConstants.KEY_TYPE, type)
                    putBoolean(KEY_WITH_TRANSFERS, withTransfers)
                }
            }
        }
    }
}