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

package org.totschnig.myexpenses.activity;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageTemplates extends ProtectedFragmentActivity {

  private static final int DELETE_TEMPLATE = Menu.FIRST;
  private static final int CREATE_INSTANCE_EDIT = Menu.FIRST +1;
  private static final int EDIT = Menu.FIRST +3;

  public long mAccountId;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      setTheme(MyApplication.getThemeId());
      super.onCreate(savedInstanceState);
      mAccountId = getIntent().getExtras().getLong(DatabaseConstants.KEY_ACCOUNTID);
      getSupportActionBar().setSubtitle(Account.getInstanceFromDb(mAccountId).label);
      setContentView(R.layout.manage_templates);
      setTitle(R.string.menu_manage_plans);
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.templates, menu);
    super.onCreateOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean dispatchCommand(int command, Object tag) {
    if (command == R.id.INSERT_TA_COMMAND || command == R.id.INSERT_TRANSFER_COMMAND) {
      Intent intent = new Intent(this, ExpenseEdit.class);
      intent.putExtra("operationType",
          command == R.id.INSERT_TA_COMMAND ? MyExpenses.TYPE_TRANSACTION : MyExpenses.TYPE_TRANSFER);
      intent.putExtra("newTemplate", true);
      intent.putExtra(DatabaseConstants.KEY_ACCOUNTID,mAccountId);
      startActivity(intent);
      return true;
    }
    return super.dispatchCommand(command, tag);
   }

  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0,DELETE_TEMPLATE,0,R.string.menu_delete);
    menu.add(0,CREATE_INSTANCE_EDIT,0,R.string.menu_create_transaction_from_template_and_edit);
    menu.add(0,EDIT,0,R.string.menu_edit);
  }
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    Intent intent;
    switch(item.getItemId()) {
    case DELETE_TEMPLATE:   
      FragmentManager fm = getSupportFragmentManager();
      fm.beginTransaction()
        .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_DELETE_TEMPLATE,info.id, null), "ASYNC_TASK")
        .commit();
      return true;
    case CREATE_INSTANCE_EDIT:
      intent = new Intent(this, ExpenseEdit.class);
      intent.putExtra("template_id", info.id);
      intent.putExtra("instantiate", true);
      startActivity(intent);
      return true;
    case EDIT:
      intent = new Intent(this, ExpenseEdit.class);
      intent.putExtra("template_id", info.id);
      intent.putExtra("instantiate", false);
      startActivity(intent);
      return true;
    }
    return super.onContextItemSelected(item);
  }
  public void createInstanceAndSave (View v) {
    //TODO Strict mode
    if (Transaction.getInstanceFromTemplate((Long) v.getTag()).save() == null)
      Toast.makeText(getBaseContext(),getString(R.string.save_transaction_error), Toast.LENGTH_LONG).show();
    else
      Toast.makeText(getBaseContext(),getString(R.string.save_transaction_from_template_success), Toast.LENGTH_LONG).show();
    finish();
  }
}
