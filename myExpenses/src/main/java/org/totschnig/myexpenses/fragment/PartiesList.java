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

package org.totschnig.myexpenses.fragment;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import java.util.ArrayList;

import eltos.simpledialogfragment.input.SimpleInputDialog;

public class PartiesList extends ContextualActionBarFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  public static final String DIALOG_EDIT_PARTY = "dialogEditParty";
  SimpleCursorAdapter mAdapter;
  private Cursor mPartiesCursor;

  @Override
  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) info;
    switch (command) {
      case R.id.EDIT_COMMAND:
        Bundle args = new Bundle();
        args.putLong(DatabaseConstants.KEY_ROWID, menuInfo.id);
        String name = mPartiesCursor.getString(mPartiesCursor.getColumnIndex(DatabaseConstants.KEY_PAYEE_NAME));
        SimpleInputDialog.build()
            .title(R.string.menu_edit_party)
            .cancelable(false)
            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
            .hint(R.string.label)
            .text(name)
            .pos(R.string.menu_save)
            .neut()
            .extra(args)
            .show(this, DIALOG_EDIT_PARTY);
        return true;
    }
    return super.dispatchCommandSingle(command, info);
  }

  @Override
  public boolean dispatchCommandMultiple(int command,
                                         SparseBooleanArray positions, Long[] itemIds) {
    switch (command) {
      case R.id.DELETE_COMMAND:
        int columnIndexMappedTransactions = mPartiesCursor.getColumnIndex(DatabaseConstants.KEY_MAPPED_TRANSACTIONS);
        int columnIndexMappedTemplates = mPartiesCursor.getColumnIndex(DatabaseConstants.KEY_MAPPED_TEMPLATES);
        int columnIndexRowId = mPartiesCursor.getColumnIndex(DatabaseConstants.KEY_ROWID);
        int mappedTransactionsCount = 0, mappedTemplatesCount = 0;
        ArrayList<Long> idList = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
          if (positions.valueAt(i)) {
            boolean deletable = true;
            mPartiesCursor.moveToPosition(positions.keyAt(i));
            if (mPartiesCursor.getInt(columnIndexMappedTransactions) > 0) {
              mappedTransactionsCount++;
              deletable = false;
            }
            if (mPartiesCursor.getInt(columnIndexMappedTemplates) > 0) {
              mappedTemplatesCount++;
              deletable = false;
            }
            if (deletable) {
              idList.add(mPartiesCursor.getLong(columnIndexRowId));
            }
          }
        }
        ProtectedFragmentActivity activity = (ProtectedFragmentActivity) getActivity();
        if (!idList.isEmpty()) {
          activity.startTaskExecution(
              TaskExecutionFragment.TASK_DELETE_PAYEES,
              idList.toArray(new Long[idList.size()]),
              null,
              R.string.progress_dialog_deleting);
          return true;
        }
        if (mappedTransactionsCount > 0 || mappedTemplatesCount > 0) {
          String message = "";
          if (mappedTransactionsCount > 0) {
            message += getResources().getQuantityString(
                R.plurals.not_deletable_mapped_transactions,
                mappedTransactionsCount,
                mappedTransactionsCount);
          }
          if (mappedTemplatesCount > 0) {
            message += getResources().getQuantityString(
                R.plurals.not_deletable_mapped_templates,
                mappedTemplatesCount,
                mappedTemplatesCount);
          }
          activity.showSnackbar(message, Snackbar.LENGTH_LONG);
        }
        break;
    }
    return super.dispatchCommandMultiple(command, positions, itemIds);
  }

  @Override
  @SuppressLint("InlinedApi")
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.parties_list, container, false);

    final ListView lv = (ListView) v.findViewById(R.id.list);
    lv.setItemsCanFocus(false);
    //((TextView) findViewById(android.R.id.empty)).setText(R.string.no_parties);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{DatabaseConstants.KEY_PAYEE_NAME};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{android.R.id.text1};

    // Now create a simple cursor adapter and set it to display
    mAdapter = new SimpleCursorAdapter(
        getActivity(),
        android.R.layout.simple_list_item_activated_1,
        null,
        from,
        to,
        0);

    getLoaderManager().initLoader(0, null, this);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    registerForContextualActionBar(lv);
    return v;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    CursorLoader cursorLoader = new CursorLoader(getActivity(),
        TransactionProvider.PAYEES_URI, null, null, null, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    mPartiesCursor = c;
    mAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mPartiesCursor = null;
    mAdapter.swapCursor(null);
  }
}
