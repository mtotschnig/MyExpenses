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

package org.totschnig.myexpenses.dialog;


import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.TransactionAdapter;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.Utils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_SPLIT_PART;

public class TransactionListDialogFragment extends CommitSafeDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String KEY_IS_MAIN = "is_main";
  private static final String KEY_GROUPING_CLAUSE = "grouping_clause";
  private static final String KEY_GROUPING_ARGS = "grouping_args";
  private static final String KEY_WITH_TRANSFERS = "with_transfers";
  public static final int TRANSACTION_CURSOR = 1;
  public static final int SUM_CURSOR = 2;
  private static final String TABS = "\u0009\u0009\u0009\u0009";
  private Account mAccount;
  private TransactionAdapter mAdapter;
  private ListView mListView;
  private boolean isMain;

  @Inject
  CurrencyFormatter currencyFormatter;
  @Inject
  PrefHandler prefHandler;
  @Inject
  CurrencyContext currencyContext;
  private long catId;

  public static TransactionListDialogFragment newInstance(
      Long account_id, long cat_id, boolean isMain, Grouping grouping, String groupingClause,
      String[] groupingArgs, String label, int type, boolean withTransfers) {
    TransactionListDialogFragment dialogFragment = new TransactionListDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_ACCOUNTID, account_id);
    bundle.putLong(KEY_CATID, cat_id);
    bundle.putString(KEY_GROUPING_CLAUSE, groupingClause);
    bundle.putSerializable(KEY_GROUPING, grouping);
    bundle.putStringArray(KEY_GROUPING_ARGS, groupingArgs);
    bundle.putString(KEY_LABEL, label);
    bundle.putBoolean(KEY_IS_MAIN, isMain);
    bundle.putInt(KEY_TYPE, type);
    bundle.putBoolean(KEY_WITH_TRANSFERS, withTransfers);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mAccount = Account.getInstanceFromDb(getArguments().getLong(KEY_ACCOUNTID));
    isMain = getArguments().getBoolean(KEY_IS_MAIN);
    catId = getArguments().getLong(KEY_CATID);
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    //Context wrappedCtx = DialogUtils.wrapContext2(getActivity());

    mListView = new ListView(getActivity());
    mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_INSET);

    mAdapter = new TransactionAdapter(
        (Grouping) getArguments().getSerializable(KEY_GROUPING),
        getActivity(),
        R.layout.expense_row,
        null,
        0, currencyFormatter, prefHandler, currencyContext) {
      @Override
      protected CharSequence getCatText(CharSequence catText,
                                        String label_sub) {
        return catId == 0L ? super.getCatText(catText, label_sub) :
            ((isMain && label_sub != null) ? label_sub : "");
      }
    };
    mAdapter.setAccount(mAccount);
    mListView.setAdapter(mAdapter);
    final LoaderManager loaderManager = LoaderManager.getInstance(this);
    loaderManager.initLoader(TRANSACTION_CURSOR, null, this);
    loaderManager.initLoader(SUM_CURSOR, null, this);
    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> a, View v, int position, long id) {
        FragmentManager fm = getFragmentManager();
        DialogFragment f = (DialogFragment) fm.findFragmentByTag(TransactionDetailFragment.class.getName());
        if (f == null) {
          FragmentTransaction ft = fm.beginTransaction();
          TransactionDetailFragment.newInstance(id).show(ft, TransactionDetailFragment.class.getName());
        }
      }
    });
    //TODO pretify layout
//    View titleView = LayoutInflater.from(getActivity()).inflate(R.layout.transaction_list_dialog_title, null);
//    ((TextView) titleView.findViewById(R.id.label)).setText(getArguments().getString(KEY_LABEL));
//    ((TextView) titleView.findViewById(R.id.amount)).setText("TBF");

    return new AlertDialog.Builder(getActivity())
        .setTitle(getArguments().getString(KEY_LABEL))
        .setView(mListView)
        .setPositiveButton(android.R.string.ok, null)
        .create();
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
    String selection, accountSelect, amountCalculation = KEY_AMOUNT;
    String[] selectionArgs;
    if (mAccount.isHomeAggregate()) {
      selection = "";
      accountSelect = null;
      amountCalculation = DatabaseConstants.getAmountHomeEquivalent();
    } else if (mAccount.isAggregate()) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ? AND " +
          KEY_EXCLUDE_FROM_TOTALS + "=0)";
      accountSelect = mAccount.getCurrencyUnit().code();
    } else {
      selection = KEY_ACCOUNTID + " = ?";
      accountSelect = String.valueOf(mAccount.getId());
    }
    if (catId == 0L) {
      if (!TextUtils.isEmpty(selection)) {
        selection += " AND ";
      }
      selection += WHERE_NOT_SPLIT_PART;
      selectionArgs = accountSelect == null ? null : new String[]{accountSelect};
    } else {
      if (!TextUtils.isEmpty(selection)) {
        selection += " AND ";
      }
      selection += KEY_CATID + " IN (SELECT " + DatabaseConstants.KEY_ROWID + " FROM "
          + TABLE_CATEGORIES + " WHERE " + KEY_PARENTID + " = ? OR "
          + KEY_ROWID + " = ?)";

      String catSelect = String.valueOf(catId);
      selectionArgs = accountSelect == null ?
          new String[]{catSelect, catSelect} :
          new String[]{accountSelect, catSelect, catSelect};
    }
    String groupingClause = getArguments().getString(KEY_GROUPING_CLAUSE);
    if (groupingClause != null) {
      if (!TextUtils.isEmpty(selection)) {
        selection += " AND ";
      }
      selection += groupingClause;
      selectionArgs = Utils.joinArrays(selectionArgs, getArguments().getStringArray(KEY_GROUPING_ARGS));
    }
    int type = getArguments().getInt(KEY_TYPE);
    if (type != 0) {
      if (!TextUtils.isEmpty(selection)) {
        selection += " AND ";
      }
      selection +=  KEY_AMOUNT + (type == -1 ? "<" : ">") + "0";
    }
    if (!getArguments().getBoolean(KEY_WITH_TRANSFERS)) {
      if (!TextUtils.isEmpty(selection)) {
        selection += " AND ";
      }
      selection += KEY_TRANSFER_PEER + " is null";
    }
    switch (id) {
      case TRANSACTION_CURSOR:
        return new CursorLoader(getActivity(),
            mAccount.getExtendedUriForTransactionList(type != 0), mAccount.getExtendedProjectionForTransactionList(),
            selection, selectionArgs, null);
      case SUM_CURSOR:
        return new CursorLoader(getActivity(),
            Transaction.EXTENDED_URI, new String[]{"sum(" + amountCalculation + ")"}, selection,
            selectionArgs, null);
    }
    throw new IllegalArgumentException();
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
    switch (loader.getId()) {
      case TRANSACTION_CURSOR:
        mAdapter.swapCursor(cursor);
        break;
      case SUM_CURSOR:
        cursor.moveToFirst();
        String title = getArguments().getString(KEY_LABEL) + TABS +
            currencyFormatter.convAmount(cursor.getLong(0), mAccount.getCurrencyUnit());
        getDialog().setTitle(title);
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    switch (loader.getId()) {
      case TRANSACTION_CURSOR:
        mAdapter.swapCursor(null);
        break;
    }
  }
}
