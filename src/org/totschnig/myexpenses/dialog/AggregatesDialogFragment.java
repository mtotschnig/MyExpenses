package org.totschnig.myexpenses.dialog;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AggregatesDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor>, OnClickListener {
  SimpleCursorAdapter currencyAdapter;
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FragmentActivity ctx = (FragmentActivity) getActivity();
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"currency","opening_balance","sum_income","sum_expenses","current_balance"};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.currency,R.id.opening_balance,R.id.sum_income,R.id.sum_expenses,R.id.current_balance};

    // Now create a simple cursor adapter and set it to display
    currencyAdapter = new SimpleCursorAdapter(ctx, R.layout.aggregate_row, null, from, to,0) {
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
          setConvertedAmount((TextView)row.findViewById(R.id.opening_balance), currency);
          setConvertedAmount((TextView)row.findViewById(R.id.sum_income), currency);
          setConvertedAmount((TextView)row.findViewById(R.id.sum_expenses), currency);
          setConvertedAmount((TextView)row.findViewById(R.id.current_balance), currency);
          return row;
        }
    };
    ctx.getSupportLoaderManager().initLoader(0, null, this);
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getActivity())
      .setTitle(R.string.menu_aggregates)
      .setAdapter(currencyAdapter, null)
      .setNegativeButton(android.R.string.ok,this)
      .create();
  }
  private void setConvertedAmount(TextView tv,Currency currency) {
    tv.setText(Utils.convAmount(tv.getText().toString(),currency));
  }
  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    CursorLoader cursorLoader = new CursorLoader(getActivity(),
        TransactionProvider.AGGREGATES_URI, null, null, null, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
    if (currencyAdapter != null)
      currencyAdapter.swapCursor(cursor);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    currencyAdapter.swapCursor(null);
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    this.dismiss();
  }
}
