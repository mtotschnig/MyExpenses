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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Activity for switching accounts
 * also allows to manage accounts
 * @author Michael Totschnig
 *
 */
public class SelectAccount extends ListActivity {
  private static final int ACTIVITY_CREATE=0;
  private static final int ACTIVITY_EDIT=1;
  private static final int EDIT_ID = Menu.FIRST;
  private static final int DELETE_ID = Menu.FIRST +1;
  private static final int INSERT_ACCOUNT_ID = Menu.FIRST + 2;
  private ExpensesDbAdapter mDbHelper;
  Cursor mAccountsCursor;
  long mCurrentAccount;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.select_account);
    setTitle(R.string.select_account);
    // Set up our adapter
    mDbHelper = new ExpensesDbAdapter(SelectAccount.this);
    mDbHelper.open();
    Bundle extras = getIntent().getExtras();
    mCurrentAccount = extras.getLong("current_account");
    fillData();
    registerForContextMenu(getListView());
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, INSERT_ACCOUNT_ID, 0, R.string.menu_insert_account);
    return true;
  }
  
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch(item.getItemId()) {
    case INSERT_ACCOUNT_ID:
      Intent i = new Intent(this, AccountEdit.class);
      startActivityForResult(i, ACTIVITY_CREATE);
      return true;
    }
    return super.onMenuItemSelected(featureId, item);
  }
  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    Intent intent=new Intent();         
    intent.putExtra("account_id", (int) id);
    setResult(RESULT_OK,intent);
    finish();
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (resultCode == RESULT_OK)
      fillData();
  }
  private void fillData () {
    if (mAccountsCursor == null) {
      mAccountsCursor = mDbHelper.fetchAccountAll();
      startManagingCursor(mAccountsCursor);
    } else {
      mAccountsCursor.requery();
    }
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"description","label","opening_balance","currency"};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.description,R.id.label,R.id.opening_balance,R.id.currency};

    // Now create a simple cursor adapter and set it to display
    SimpleCursorAdapter account = new SimpleCursorAdapter(this, R.layout.account_row, mAccountsCursor, from, to);
    setListAdapter(account);
  }
  public void onDestroy() {
    super.onDestroy();
    mDbHelper.close();
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
    menu.add(0, EDIT_ID, 0, R.string.menu_edit_account);
    //currentAccount should not be deleted
    if (info.id != mCurrentAccount)
      menu.add(0, DELETE_ID, 0, R.string.menu_delete_account);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case EDIT_ID:
      Intent i = new Intent(this, AccountEdit.class);
      i.putExtra(ExpensesDbAdapter.KEY_ROWID, info.id);
      startActivityForResult(i, ACTIVITY_EDIT);
      return true;
    case DELETE_ID:
      mDbHelper.deleteAccount(info.id);
      fillData();
      return true;
    }
    return super.onContextItemSelected(item);
  }  
}
