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

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * allows to manage accounts
 * TODO: either prevent deletion of last account or gracefully recreate a new one
 * @author Michael Totschnig
 *
 */
public class ManageAccounts extends ProtectedFragmentActivity implements
    OnItemClickListener,ContribIFace,LoaderManager.LoaderCallbacks<Cursor> {
  private static final int DELETE_ID = Menu.FIRST;
  private static final int RESET_ID = Menu.FIRST + 1;
  private static final int DELETE_COMMAND_ID = 1;
  Cursor mCurrencyCursor;
  private Button mAddButton, mAggregateButton, mResetAllButton;
  static final int AGGREGATE_DIALOG_ID = 2;
  private int mCurrentDialog = 0;
  SimpleCursorAdapter currencyAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    setContentView(R.layout.manage_accounts);
    MyApplication.updateUIWithAppColor(this);
    setTitle(R.string.pref_manage_accounts_title);
    getSupportLoaderManager().initLoader(0, null, this);

    mAddButton = (Button) findViewById(R.id.addOperation);
    mAggregateButton = (Button) findViewById(R.id.aggregate);
    mAggregateButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (MyApplication.getInstance().isContribEnabled) {
          showDialogWrapper(AGGREGATE_DIALOG_ID);
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
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
    case AGGREGATE_DIALOG_ID:
      LayoutInflater li = LayoutInflater.from(this);
      View view = li.inflate(R.layout.aggregate_dialog, null);
      DialogUtils.setDialogOneButton(view,
          android.R.string.ok,0,null);
      ListView listView = (ListView) view.findViewById(R.id.list);
      // Create an array to specify the fields we want to display in the list
      String[] from = new String[]{"currency","opening_balance","sum_income","sum_expenses","current_balance"};

      // and an array of the fields we want to bind those fields to 
      int[] to = new int[]{R.id.currency,R.id.opening_balance,R.id.sum_income,R.id.sum_expenses,R.id.current_balance};

      // Now create a simple cursor adapter and set it to display
      currencyAdapter = new SimpleCursorAdapter(this, R.layout.aggregate_row, mCurrencyCursor, from, to,0) {
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
      listView.setAdapter(currencyAdapter);
      return new AlertDialog.Builder(this)
      .setTitle(R.string.menu_aggregate)
      .setView(view)
      .create();
    }
    return null;
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
  /**
   * @param id we store the dialog id, so that we can dismiss it in our generic button handler
   */
  public void showDialogWrapper(int id) {
    mCurrentDialog = id;
    showDialog(id);
  }
  private void configButtons () {
    mAggregateButton.setVisibility(mCurrencyCursor.getCount() > 0 ? View.VISIBLE : View.GONE);
    mResetAllButton.setVisibility(Transaction.countAll() > 0 ? View.VISIBLE : View.GONE);
  }
  private void setConvertedAmount(TextView tv,Currency currency) {
    tv.setText(Utils.convAmount(tv.getText().toString(),currency));
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
      showDialogWrapper(AGGREGATE_DIALOG_ID);
      break;
    case RESET_ALL:
      DialogUtils.showWarningResetDialog(this, null);
      break;
    }
  }

  @Override
  public void contribFeatureNotCalled() {
  }

  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    CursorLoader cursorLoader = new CursorLoader(this,
        TransactionProvider.AGGREGATES_URI, null, null, null, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
    mCurrencyCursor = cursor;
    if (currencyAdapter != null)
      currencyAdapter.swapCursor(cursor);
    configButtons();
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    if (currencyAdapter != null)
      currencyAdapter.swapCursor(null);
  }
}
