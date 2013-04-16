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

package org.totschnig.myexpenses;

import java.util.Currency;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Activity for switching accounts
 * also allows to manage accounts
 * @author Michael Totschnig
 *
 */
public class ManageAccounts extends ListActivity {
  private static final int ACTIVITY_CREATE=0;
  private static final int ACTIVITY_EDIT=1;
  private static final int DELETE_ID = Menu.FIRST;
  private static final int DELETE_COMMAND_ID = 1;
  private ExpensesDbAdapter mDbHelper;
  Cursor mAccountsCursor;
  Cursor mCurrencyCursor;
  long mCurrentAccount;
  private Button mAddButton, mAggregateButton;
  private long mContextAccountId;
  static final int DELETE_DIALOG_ID = 1;
  static final int AGGREGATE_DIALOG_ID = 2;
  private int mCurrentDialog = 0;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_accounts);
    MyApplication.updateUIWithAppColor(this);
    setTitle(R.string.pref_manage_accounts_title);
    // Set up our adapter
    mDbHelper = MyApplication.db();
    mCurrentAccount = MyApplication.getInstance()
        .getSettings()
        .getLong(MyApplication.PREFKEY_CURRENT_ACCOUNT, 0);
    fillData();
    mAddButton = (Button) findViewById(R.id.addOperation);
    mAggregateButton = (Button) findViewById(R.id.aggregate);
    mAggregateButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        showDialogWrapper(AGGREGATE_DIALOG_ID);
      }
    });
    mCurrencyCursor = mDbHelper.fetchAggregatesForCurrenciesHavingMultipleAccounts();
    if (mCurrencyCursor.getCount() > 0) {
      mAggregateButton.setVisibility(View.VISIBLE);
    }
    mAddButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(ManageAccounts.this, AccountEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
      }
    });
    
    registerForContextMenu(getListView());
  }
  
  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    Intent i = new Intent(this, AccountEdit.class);
    i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
    startActivityForResult(i, ACTIVITY_EDIT);
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (resultCode == RESULT_OK)
      fillData();
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
    case DELETE_DIALOG_ID:
      return Utils.createMessageDialog(this,R.string.warning_delete_account,DELETE_COMMAND_ID,null).create();
    case AGGREGATE_DIALOG_ID:
      LayoutInflater li = LayoutInflater.from(this);
      View view = li.inflate(R.layout.aggregate_dialog, null);
      Utils.setDialogOneButton(view,
          android.R.string.ok,0,null);
      ListView listView = (ListView) view.findViewById(R.id.list);
      // Create an array to specify the fields we want to display in the list
      String[] from = new String[]{"currency","opening_balance","sum_income","sum_expenses","current_balance"};

      // and an array of the fields we want to bind those fields to 
      int[] to = new int[]{R.id.currency,R.id.opening_balance,R.id.sum_income,R.id.sum_expenses,R.id.current_balance};

      // Now create a simple cursor adapter and set it to display
      SimpleCursorAdapter currency = new SimpleCursorAdapter(this, R.layout.aggregate_row, mCurrencyCursor, from, to) {
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
      listView.setAdapter(currency);
      return new AlertDialog.Builder(this)
      .setTitle("Total")
      .setView(view)
      .create();
    }
    return null;
  }
  public void onDialogButtonClicked(View v) {
    if (mCurrentDialog != 0)
      dismissDialog(mCurrentDialog);
    if (v.getId() == DELETE_COMMAND_ID) {
      Account.delete(mContextAccountId);
      fillData();
    }
  }
  /**
   * @param id we store the dialog id, so that we can dismiss it in our generic button handler
   */
  public void showDialogWrapper(int id) {
    mCurrentDialog = id;
    showDialog(id);
  }
  private void fillData () {
    if (mAccountsCursor == null) {
      mAccountsCursor = mDbHelper.fetchAccountAll();
      startManagingCursor(mAccountsCursor);
    } else {
      mAccountsCursor.requery();
    }
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"description","label","opening_balance","sum_income","sum_expenses","sum_transfer","current_balance"};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.description,R.id.label,R.id.opening_balance,R.id.sum_income,R.id.sum_expenses,R.id.sum_transfer,R.id.current_balance};

    // Now create a simple cursor adapter and set it to display
    SimpleCursorAdapter account = new SimpleCursorAdapter(this, R.layout.account_row, mAccountsCursor, from, to) {
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
          TextView v = (TextView) row.findViewById(R.id.label);
          int bg = c.getInt(c.getColumnIndex("color"));
          v.setBackgroundColor(bg);
          v.setTextColor(Utils.getTextColorForBackground(bg));
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
    setListAdapter(account);
  }
  private void setConvertedAmount(TextView tv,Currency currency) {
    tv.setText(Utils.convAmount(tv.getText().toString(),currency));
  }
  /* (non-Javadoc)
   * makes sure that current account is not deleted
   * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
   */
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    //currentAccount should not be deleted
    if (info.id != mCurrentAccount)
      menu.add(0, DELETE_ID, 0, R.string.menu_delete_account);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case DELETE_ID:
      //passing a bundle to showDialog is available only with API level 8
      mContextAccountId = info.id;
      showDialogWrapper(DELETE_DIALOG_ID);
      //mDbHelper.deleteAccount(info.id);
      //fillData();
      return true;
    }
    return super.onContextItemSelected(item);
  }
  //safeguard for orientation change during dialog
  @Override
  protected void onSaveInstanceState(Bundle outState) {
   super.onSaveInstanceState(outState);
   outState.putLong("contextAccountId", mContextAccountId);
   outState.putInt("currentDialog",mCurrentDialog);
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
   super.onRestoreInstanceState(savedInstanceState);
   mContextAccountId = savedInstanceState.getLong("contextAccountId");
   mCurrentDialog = savedInstanceState.getInt("currentDialog");
  }
}
