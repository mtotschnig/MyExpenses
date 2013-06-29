package org.totschnig.myexpenses.fragment;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class AccountList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter mAdapter;
  
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
          String currencyStr = c.getString(col);
          Currency currency;
          try {
            currency = Currency.getInstance(currencyStr);
          } catch (IllegalArgumentException e) {
            currency = Currency.getInstance(Locale.getDefault());
          }
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
        TransactionProvider.ACCOUNTS_URI, null, null,null, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    mAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mAdapter.swapCursor(null);
  }
}
