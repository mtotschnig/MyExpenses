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

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.util.SparseBooleanArray;

public class ManageParties extends ProtectedListActivity {
  private ExpensesDbAdapter mDbHelper;
  Cursor mPartiesCursor;
  Button mDeleteButton;

  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      setTheme(MyApplication.getThemeId());
      super.onCreate(savedInstanceState);
      setContentView(R.layout.manage_list);
      setTitle(R.string.pref_manage_parties_title);
      MyApplication.updateUIWithAppColor(this);
      mDbHelper = MyApplication.db();
      final ListView listView = getListView();

      listView.setItemsCanFocus(false);
      listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
      fillData();
      ((TextView) findViewById(android.R.id.empty)).setText(R.string.no_parties);
      mDeleteButton = (Button) findViewById(R.id.deleteItems);
      mDeleteButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          int cntChoice = listView.getCount();
          SparseBooleanArray sparseBooleanArray = listView.getCheckedItemPositions();

          for(int i = 0; i < cntChoice; i++){
            if(sparseBooleanArray.get(i)) {
              mDbHelper.deletePayee(listView.getAdapter().getItemId(i));
            }
          }
          fillData();
        }
      });
  }
  public void fillData() {
    if (mPartiesCursor == null) {
      mPartiesCursor = mDbHelper.fetchPayeeAll();
      startManagingCursor(mPartiesCursor);
    } else {
      mPartiesCursor.requery();
    }
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"name"};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{android.R.id.text1};

    // Now create a simple cursor adapter and set it to display
    SimpleCursorAdapter parties = new SimpleCursorAdapter(this, 
        android.R.layout.simple_list_item_multiple_choice, mPartiesCursor, from, to);
    setListAdapter(parties);
  }
}
