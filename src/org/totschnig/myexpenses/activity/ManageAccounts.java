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

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.AggregatesDialogFragment;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.util.Utils;

import android.content.Intent;
import android.os.Bundle;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * allows to manage accounts
 * TODO: either prevent deletion of last account or gracefully recreate a new one
 * @author Michael Totschnig
 *
 */
public class ManageAccounts extends ProtectedFragmentActivity implements
    OnItemClickListener {
  private static final int DELETE_ID = Menu.FIRST;
  private static final int RESET_ID = Menu.FIRST + 1;
  private static final int DELETE_COMMAND_ID = 1;
  private Button mAddButton, mAggregateButton, mResetAllButton;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_accounts);
    MyApplication.updateUIWithAppColor(this);
    setTitle(R.string.pref_manage_accounts_title);

    mAddButton = (Button) findViewById(R.id.addOperation);
    mAggregateButton = (Button) findViewById(R.id.aggregate);
    mAggregateButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (MyApplication.getInstance().isContribEnabled) {
          showAggregatesDialog();
        } else {
          showContribDialog(Feature.AGGREGATE);
        }
      }
    });
    mResetAllButton = (Button) findViewById(R.id.resetAll);
    mResetAllButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (MyApplication.getInstance().isContribEnabled) {
          DialogUtils.showWarningResetDialog(ManageAccounts.this, null);
        } else {
          showContribDialog(Feature.RESET_ALL);
        }
      }
    });
    mAddButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(ManageAccounts.this, AccountEdit.class);
        startActivityForResult(i, 0);
      }
    });
    configButtons();
  }
  
  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    Intent i = new Intent(this, AccountEdit.class);
    i.putExtra(KEY_ROWID, id);
    startActivityForResult(i, 0);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (resultCode == RESULT_OK) {
/*      getSupportLoaderManager().getLoader(0).forceLoad();*/
      configButtons();
    }
  }
  public boolean dispatchCommand(int command, Object tag) {
    switch(command) {
    case DELETE_COMMAND_ID:
      Account.delete((Long) tag);
      break;
    case R.id.RESET_ACCOUNT_COMMAND_DO:
      if (Utils.isExternalStorageAvailable()) {
        Intent i = new Intent(this, Export.class);
        i.putExtra(KEY_ROWID, (Integer) tag);
        startActivityForResult(i,0);
      } else {
        Toast.makeText(getBaseContext(),
            getString(R.string.external_storage_unavailable),
            Toast.LENGTH_LONG)
            .show();
      }
      break;
    case R.id.RESET_ACCOUNT_ALL_COMMAND:
      if (Utils.isExternalStorageAvailable()) {
        Intent i = new Intent(this, Export.class);
        Feature.RESET_ALL.recordUsage();
        startActivityForResult(i,0);
      } else {
        Toast.makeText(getBaseContext(),
            getString(R.string.external_storage_unavailable),
            Toast.LENGTH_LONG)
            .show();
      }
      break;
    }
    configButtons();
    return true;
  }
  private void configButtons () {
    mAggregateButton.setVisibility(View.VISIBLE);
    //mAggregateButton.setVisibility(mCurrencyCursor.getCount() > 0 ? View.VISIBLE : View.GONE);
    mResetAllButton.setVisibility(Transaction.countAll() > 0 ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    if (Account.count(null, null) > 1)
      menu.add(0, DELETE_ID, 0, R.string.menu_delete);
    if (Transaction.countPerAccount(info.id) > 0)
       menu.add(0,RESET_ID,0,R.string.menu_reset);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case DELETE_ID:
      MessageDialogFragment.newInstance(R.string.dialog_tite_warning_delete_account,R.string.warning_delete_account,DELETE_COMMAND_ID,info.id)
        .show(getSupportFragmentManager(),"DELETE_ACCOUNT");
      return true;
    case RESET_ID:
      DialogUtils.showWarningResetDialog(this, info.id);
      return true;
    }
    return super.onContextItemSelected(item);
  }
  @SuppressWarnings("incomplete-switch")
  @Override
  public void contribFeatureCalled(Feature feature) {
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
