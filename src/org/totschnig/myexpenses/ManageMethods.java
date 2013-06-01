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

import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;

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
import android.widget.AdapterView.OnItemClickListener;

public class ManageMethods extends ProtectedFragmentActivity implements OnItemClickListener {
  private static final int ACTIVITY_CREATE=0;
  private static final int ACTIVITY_EDIT=1;
  private static final int DELETE_ID = Menu.FIRST;
  Cursor mMethodsCursor;
  private Button mAddButton;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      setTheme(MyApplication.getThemeId());
      super.onCreate(savedInstanceState);
      setContentView(R.layout.manage_methods);
      MyApplication.updateUIWithAppColor(this);
      setTitle(R.string.pref_manage_methods_title);
      mAddButton = (Button) findViewById(R.id.addOperation);
      mAddButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent i = new Intent(ManageMethods.this, MethodEdit.class);
          startActivityForResult(i, ACTIVITY_CREATE);
        }
      });
  }
  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    Intent i = new Intent(this, MethodEdit.class);
    i.putExtra(ExpensesDbAdapter.KEY_ROWID, id);
    startActivityForResult(i, ACTIVITY_EDIT);
  }
  /* (non-Javadoc)
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
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
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
      if (Transaction.countPerMethod(info.id) > 0 ) {
        Toast.makeText(this,getString(R.string.not_deletable_mapped_transactions), Toast.LENGTH_LONG).show();
      } else if (Template.countPerMethod(info.id) > 0 ) {
        Toast.makeText(this,getString(R.string.not_deletable_mapped_templates), Toast.LENGTH_LONG).show();
      }  else {
        PaymentMethod.delete(info.id);
      }
      return true;
    }
    return super.onContextItemSelected(item);
  }
}
