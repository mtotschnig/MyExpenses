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


import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.adapter.TransactionAdapter;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.SingleCategoryCriteria;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;
import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.ListView;

public class TransactionListDialogFragment extends CommitSafeDialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String KEY_IS_MAIN = "is_main";
  private static final String KEY_GROUPING_CLAUSE = "grouping_clause";
  Account mAccount;
  SimpleCursorAdapter mAdapter;
  ListView mLayout;
  boolean isMain;
  
  public static final TransactionListDialogFragment newInstance(
      Long account_id,long cat_id, boolean isMain, Grouping grouping,String groupingClause,String label) {
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
    
    mLayout = new ListView(getActivity());
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
            return isMain ? label_sub : "";
          }
      };
    mLayout.setAdapter(mAdapter);
    getLoaderManager().initLoader(0, null, this);
    
    return new AlertDialog.Builder(getActivity())
      .setTitle(getArguments().getString(KEY_LABEL))
      .setView(mLayout)
      .setPositiveButton(android.R.string.ok,null)
      .create();
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
    String selection;
    String[] selectionArgs;
    WhereFilter wf = new WhereFilter();
    wf.put(0, new SingleCategoryCriteria(
        getArguments().getLong(KEY_CATID),
        getArguments().getString(KEY_LABEL)));
    if (mAccount.getId() < 0) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ? AND " +
          KEY_EXCLUDE_FROM_TOTALS + "=0)";
      selectionArgs = new String[] {mAccount.currency.getCurrencyCode()};
    } else {
      selection = KEY_ACCOUNTID + " = ?";
      selectionArgs = new String[] { String.valueOf(mAccount.getId()) };
    }
    selection += " AND " + wf.getSelection();
    selectionArgs = Utils.joinArrays(selectionArgs, wf.getSelectionArgs());
    String groupingClause = getArguments().getString(KEY_GROUPING_CLAUSE);
    if (groupingClause!= null) {
      selection += " AND " + groupingClause;
    }
    Uri uri = TransactionProvider.TRANSACTIONS_URI.buildUpon().appendQueryParameter("extended", "1").build();
    return new CursorLoader(getActivity(),
        uri, null, selection + " AND " + KEY_PARENTID + " is null",
        selectionArgs, null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    mAdapter.swapCursor(cursor);
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mAdapter.swapCursor(null);
  }
}
