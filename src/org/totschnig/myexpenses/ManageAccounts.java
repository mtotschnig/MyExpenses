/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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
import android.view.KeyEvent;
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
import android.widget.Toast;
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
  private ExpensesDbAdapter mDbHelper;
  Cursor mAccountsCursor;
  long mCurrentAccount;
  private Button mAddButton;
  private long mContextAccountId;
  static final int DELETE_DIALOG_ID = 1;
  
/*  private int monkey_state = 0;

  @Override
  public boolean onKeyUp (int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_ENVELOPE) {
      switch (monkey_state) {
      case 0:
        getListView().requestFocus();
        return true;
      }
    }
    return super.onKeyUp(keyCode, event);
  }*/

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.select_account);
    setTitle(R.string.select_account);
    // Set up our adapter
    mDbHelper = MyApplication.db();
    mCurrentAccount = ((MyApplication) getApplicationContext())
        .getSettings()
        .getLong("current_account", 0);
    fillData();
    mAddButton = (Button) findViewById(R.id.addOperation);
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
      return new AlertDialog.Builder(this)
      .setMessage(R.string.warning_delete_account)
      .setCancelable(false)
      .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            mDbHelper.deleteAccount(mContextAccountId);
            fillData();
          }
      })
      .setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.cancel();
        }
      }).create();
    }
    return null;
  }
  private void fillData () {
    if (mAccountsCursor == null) {
      mAccountsCursor = mDbHelper.fetchAccountAll();
      startManagingCursor(mAccountsCursor);
    } else {
      mAccountsCursor.requery();
    }
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"description","label","opening_balance"};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.description,R.id.label,R.id.opening_balance};

    // Now create a simple cursor adapter and set it to display
    SimpleCursorAdapter account = new SimpleCursorAdapter(this, R.layout.account_row, mAccountsCursor, from, to) {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
          View row=super.getView(position, convertView, parent);
          TextView tv1 = (TextView)row.findViewById(R.id.opening_balance);
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
          tv1.setText(Utils.convAmount(tv1.getText().toString(),currency));
          return row;
        }
    };
    setListAdapter(account);
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
      showDialog(DELETE_DIALOG_ID);
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
  }
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
   super.onRestoreInstanceState(savedInstanceState);
   mContextAccountId = savedInstanceState.getLong("contextAccountId");
  }
}
