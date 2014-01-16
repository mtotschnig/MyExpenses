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

package org.totschnig.myexpenses.fragment;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.CommonCommands;
import org.totschnig.myexpenses.activity.ManageAccounts;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class AccountList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter mAdapter;
  int accountCount;

  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.accounts_list, null, false);
    ListView lv = (ListView) v.findViewById(R.id.list);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"description","label","opening_balance","sum_income","sum_expenses","sum_transfer","current_balance"};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.description,R.id.label,R.id.opening_balance,R.id.sum_income,R.id.sum_expenses,R.id.sum_transfer,R.id.current_balance};

    // Now create a simple cursor adapter and set it to display
    mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.account_row, null, from, to,0) {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
          View row=super.getView(position, convertView, parent);
          Cursor c = getCursor();
          c.moveToPosition(position);
          int col = c.getColumnIndex("currency");
          Currency currency = Utils.getSaveInstance(c.getString(col));
          View v = row.findViewById(R.id.color1);
          v.setBackgroundColor(c.getInt(c.getColumnIndex("color")));
          setConvertedAmount((TextView)row.findViewById(R.id.opening_balance), currency);
          setConvertedAmount((TextView)row.findViewById(R.id.sum_income), currency);
          setConvertedAmount((TextView)row.findViewById(R.id.sum_expenses), currency);
          setConvertedAmount((TextView)row.findViewById(R.id.sum_transfer), currency);
          setConvertedAmount((TextView)row.findViewById(R.id.current_balance), currency);
          col = c.getColumnIndex("description");
          String description = c.getString(col);
          if (description.equals(""))
            row.findViewById(R.id.description).setVisibility(View.GONE);
          return row;
        }
    };
    getLoaderManager().initLoader(0, null, this);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    //requires using activity (ManageAccounts) to implement OnItemClickListener
    lv.setOnItemClickListener((OnItemClickListener) getActivity());
    registerForContextMenu(lv);
    return v;
  }
  private void setConvertedAmount(TextView tv,Currency currency) {
    tv.setText(Utils.convAmount(tv.getText().toString(),currency));
  }
  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    CursorLoader cursorLoader = new CursorLoader(getActivity(),
        TransactionProvider.ACCOUNTS_URI, Account.PROJECTION_FULL, null,null, null);
    return cursorLoader;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    ManageAccounts ctx = (ManageAccounts) getActivity();
    switch (item.getItemId()) {
    case R.id.CREATE_COMMAND:
    if (MyApplication.getInstance().isContribEnabled || accountCount < 5) {
      Intent i = new Intent(ctx, AccountEdit.class);
      startActivityForResult(i, 0);
    }
    else {
      CommonCommands.showContribDialog(ctx,Feature.ACCOUNTS_UNLIMITED, null);
    }
    return true;
    }
    return super.onOptionsItemSelected(item);
  }
  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    mAdapter.swapCursor(c);
    accountCount = c.getCount();
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mAdapter.swapCursor(null);
    accountCount = 0;
  }
}
