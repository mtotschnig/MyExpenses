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
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.util.SparseBooleanArray;

public class ManageMethods extends ListActivity {
  private static final int ACTIVITY_CREATE=0;
  private static final int ACTIVITY_EDIT=1;
  private ExpensesDbAdapter mDbHelper;
  Cursor mMethodsCursor;
  private Button mAddButton;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.manage_accounts);
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
