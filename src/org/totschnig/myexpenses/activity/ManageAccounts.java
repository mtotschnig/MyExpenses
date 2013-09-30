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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.io.Serializable;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.AggregatesDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.ProgressDialogFragment;
import org.totschnig.myexpenses.fragment.TaskExecutionFragment;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.TransactionProvider;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import android.support.v4.app.FragmentManager;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * allows to manage accounts
 * @author Michael Totschnig
 *
 */
public class ManageAccounts extends ProtectedFragmentActivity implements
    OnItemClickListener,ContribIFace, TaskExecutionFragment.TaskCallbacks {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_accounts);
    setTitle(R.string.pref_manage_accounts_title);
    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
  }
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    Cursor c = getContentResolver().query(TransactionProvider.AGGREGATES_URI.buildUpon().appendPath("count").build(),
        null, null, null, null);
    menu.findItem(R.id.AGGREGATES_COMMAND).setVisible(c.getCount()>0);
    c.close();
    menu.findItem(R.id.RESET_ACCOUNT_ALL_COMMAND).setVisible
      (Transaction.countAll() > 0);
    return true;
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.accounts, menu);
    super.onCreateOptionsMenu(menu);
    return true;
  }
  
  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    Intent i = new Intent(this, MyExpenses.class);
    i.putExtra(KEY_ROWID, id);
    //i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivityForResult(i,0);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    configButtons();
  }
  public boolean dispatchCommand(int command, Object tag) {
    Intent i;
    switch(command) {
    //in super home executes finish(), which is not what we want here
    case android.R.id.home:
      return true;
    case R.id.AGGREGATES_COMMAND:
      if (MyApplication.getInstance().isContribEnabled) {
        contribFeatureCalled(Feature.AGGREGATE, null);
      } else {
        CommonCommands.showContribDialog(this,Feature.AGGREGATE, null);
      }
      return true;
    case R.id.DELETE_COMMAND_DO:
      FragmentManager fm = getSupportFragmentManager();
      fm.beginTransaction()
        .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_DELETE_ACCOUNT,(Long)tag), "DELETE_TASK")
        .add(ProgressDialogFragment.newInstance(R.string.progress_dialog_deleting),"PROGRESS")
        .commit();
      return true;
    case R.id.RESET_ACCOUNT_ALL_COMMAND:
      if (MyApplication.getInstance().isContribEnabled) {
        contribFeatureCalled(Feature.RESET_ALL, null);
      } else {
        CommonCommands.showContribDialog(this,Feature.RESET_ALL, null);
      }
      return true;
    }
    configButtons();
    return super.dispatchCommand(command, tag);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    if (Account.count(null, null) > 1)
      menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
    if (Transaction.countPerAccount(info.id) > 0)
       menu.add(0,R.id.RESET_ACCOUNT_COMMAND,0,R.string.menu_reset);
    menu.add(0,R.id.EDIT_ACCOUNT_COMMAND,0,R.string.menu_edit);
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case R.id.DELETE_COMMAND:
      MessageDialogFragment.newInstance(R.string.dialog_title_warning_delete_account,
          R.string.warning_delete_account,R.id.DELETE_COMMAND_DO,info.id)
        .show(getSupportFragmentManager(),"DELETE_ACCOUNT");
      return true;
    case R.id.RESET_ACCOUNT_COMMAND:
      DialogUtils.showWarningResetDialog(this, info.id);
      return true;
    case R.id.EDIT_ACCOUNT_COMMAND:
      Intent i = new Intent(this, AccountEdit.class);
      i.putExtra(KEY_ROWID, info.id);
      startActivityForResult(i, 0);
      return true;
    }
    return super.onContextItemSelected(item);
  }
  private void configButtons() {
    supportInvalidateOptionsMenu();
  }
  @SuppressWarnings("incomplete-switch")
  @Override
  public void contribFeatureCalled(Feature feature, Serializable tag) {
    switch (feature) {
    case AGGREGATE:
      feature.recordUsage();
      showAggregatesDialog();
      break;
    case RESET_ALL:
      DialogUtils.showWarningResetDialog(this, null);
      break;
    }
  }
  private void showAggregatesDialog() {
    new AggregatesDialogFragment().show(getSupportFragmentManager(),"AGGREGATES");
  }
  @Override
  public void contribFeatureNotCalled() {
  }
}
