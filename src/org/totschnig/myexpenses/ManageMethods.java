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
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageMethods extends ProtectedListActivity {
  private static final int ACTIVITY_CREATE=0;
  private static final int ACTIVITY_EDIT=1;
  private static final int DELETE_ID = Menu.FIRST;
  private ExpensesDbAdapter mDbHelper;
  Cursor mMethodsCursor;
  private Button mAddButton;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      setTheme(MyApplication.getThemeId());
      super.onCreate(savedInstanceState);
      setContentView(R.layout.manage_accounts);
      MyApplication.updateUIWithAppColor(this);
      setTitle(R.string.pref_manage_methods_title);
      mDbHelper = MyApplication.db();
      fillData();
      mAddButton = (Button) findViewById(R.id.addOperation);
      mAddButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent i = new Intent(ManageMethods.this, MethodEdit.class);
          startActivityForResult(i, ACTIVITY_CREATE);
        }
      });
      registerForContextMenu(getListView());
  }
  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    Intent i = new Intent(this, MethodEdit.class);
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
  /* (non-Javadoc)
   * makes sure that current account is not deleted
   * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
   */
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    //predefined methods can not be deleted
    PaymentMethod method;
    try {
      method = PaymentMethod.getInstanceFromDb(info.id);
      if (method.predef == null) {
        menu.add(0, DELETE_ID, 0, R.string.menu_delete_method);
      }
    } catch (DataObjectNotFoundException e) {
      //should not happen
      e.printStackTrace();
    }
  }


  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case DELETE_ID:
      if (mDbHelper.getTransactionCountPerMethod(info.id) > 0 ) {
        Toast.makeText(this,getString(R.string.not_deletable_mapped_transactions), Toast.LENGTH_LONG).show();
      } else if (mDbHelper.getTemplateCountPerMethod(info.id) > 0 ) {
        Toast.makeText(this,getString(R.string.not_deletable_mapped_templates), Toast.LENGTH_LONG).show();
      }  else {
        mDbHelper.deletePaymentMethod(info.id);
        fillData();
      }
      return true;
    }
    return super.onContextItemSelected(item);
  }
  public void fillData() {
    if (mMethodsCursor == null) {
      mMethodsCursor = mDbHelper.fetchPaymentMethodsAll();
      startManagingCursor(mMethodsCursor);
    } else {
      mMethodsCursor.requery();
    }
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{ExpensesDbAdapter.KEY_ROWID};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{android.R.id.text1};

    // Now create a simple cursor adapter and set it to display
    SimpleCursorAdapter parties = new SimpleCursorAdapter(this, 
        android.R.layout.simple_list_item_1, mMethodsCursor, from, to) {
      @Override
      public void setViewText(TextView v, String text) {
        try {
          super.setViewText(v, PaymentMethod.getInstanceFromDb(Long.valueOf(text)).getDisplayLabel(ManageMethods.this));
        } catch (DataObjectNotFoundException e) {
          e.printStackTrace();
          setResult(RESULT_CANCELED);
          finish();
        }
        
      }
    };
    setListAdapter(parties);
  }
}
