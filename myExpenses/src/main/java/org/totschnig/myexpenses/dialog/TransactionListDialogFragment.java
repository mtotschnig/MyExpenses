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


import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.TransactionAdapter;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.Utils;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;

public class TransactionListDialogFragment extends CommitSafeDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String KEY_IS_MAIN = "is_main";
  private static final String KEY_GROUPING_CLAUSE = "grouping_clause";
  public static final int TRANSACTION_CURSOR = 1;
  public static final int SUM_CURSOR = 2;
  private static final String TABS = "\u0009\u0009\u0009\u0009";
  Account mAccount;
  SimpleCursorAdapter mAdapter;
  ListView mListView;
  boolean isMain;
  
  public static final TransactionListDialogFragment newInstance(
      Long account_id, long cat_id, boolean isMain, Grouping grouping, String groupingClause, String label) {
    TransactionListDialogFragment dialogFragment = new TransactionListDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_ACCOUNTID, account_id);
    bundle.putLong(KEY_CATID, cat_id);
    bundle.putString(KEY_GROUPING_CLAUSE, groupingClause);
    bundle.putSerializable(KEY_GROUPING, grouping);
    bundle.putString(KEY_LABEL,label);
    bundle.putBoolean(KEY_IS_MAIN, isMain);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mAccount = Account.getInstanceFromDb(getArguments().getLong(KEY_ACCOUNTID));
    isMain = getArguments().getBoolean(KEY_IS_MAIN);
    
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    //Context wrappedCtx = DialogUtils.wrapContext2(getActivity());
    
    mListView = new ListView(getActivity());
    mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_INSET);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{KEY_LABEL_MAIN,KEY_DATE,KEY_AMOUNT};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.category,R.id.date,R.id.amount};
    mAdapter = new TransactionAdapter(
        mAccount,
        (Grouping) getArguments().getSerializable(KEY_GROUPING),
        getActivity(),
        R.layout.expense_row,
        null,
        from,
        to,
        0) {
          @Override
          protected CharSequence getCatText(CharSequence catText,
              String label_sub) {
            return (isMain && label_sub != null) ? label_sub : "";
          }
      };
    mListView.setAdapter(mAdapter);
    getLoaderManager().initLoader(TRANSACTION_CURSOR, null, this);
    getLoaderManager().initLoader(SUM_CURSOR, null, this);
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
      .setPositiveButton(android.R.string.ok,null)
      .create();
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
    String selection,accountSelect;
    String[] selectionArgs;
    String catSelect = String.valueOf(getArguments().getLong(KEY_CATID));
    if (mAccount.getId() < 0) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ? AND " +
          KEY_EXCLUDE_FROM_TOTALS + "=0)";
      accountSelect = mAccount.currency.getCurrencyCode();
    } else {
      selection = KEY_ACCOUNTID + " = ?";
      accountSelect = String.valueOf(mAccount.getId());
    }
    selection += " AND " + KEY_CATID + " IN (SELECT " + DatabaseConstants.KEY_ROWID + " FROM "
        + TABLE_CATEGORIES + " WHERE " + KEY_PARENTID + " = ? OR "
        + KEY_ROWID + " = ?)";
    selectionArgs = new String[]{accountSelect,catSelect,catSelect};
    String groupingClause = getArguments().getString(KEY_GROUPING_CLAUSE);
    if (groupingClause!= null) {
      selection += " AND " + groupingClause;
    }
    switch (id) {
      case TRANSACTION_CURSOR:
        return new CursorLoader(getActivity(),
            Transaction.EXTENDED_URI, null, selection,
            selectionArgs, null);
      case SUM_CURSOR:
        return new CursorLoader(getActivity(),
            Transaction.EXTENDED_URI, new String[] {"sum(" + KEY_AMOUNT + ")"}, selection,
            selectionArgs, null);
    }
    return null;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    switch (loader.getId()) {
      case TRANSACTION_CURSOR:
        mAdapter.swapCursor(cursor);
        break;
      case SUM_CURSOR:
        cursor.moveToFirst();
        String title = getArguments().getString(KEY_LABEL) + TABS + Utils.convAmount(
            cursor.getString(0),
            mAccount.currency);
        getDialog().setTitle(title);
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    switch (loader.getId()) {
      case TRANSACTION_CURSOR:
        mAdapter.swapCursor(null);
        break;
    }
  }
}
