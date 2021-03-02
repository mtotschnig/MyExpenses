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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MethodEdit;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

public class MethodList extends ContextualActionBarFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter mAdapter;
  private Cursor mMethodsCursor;

  @SuppressLint("InlinedApi")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.methods_list, container, false);
    final ListView lv = v.findViewById(R.id.list);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{DatabaseConstants.KEY_LABEL};
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
    LoaderManager.getInstance(this).initLoader(0, null, this);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    registerForContextualActionBar(lv);
    return v;
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    return new CursorLoader(getActivity(),
        TransactionProvider.METHODS_URI, null, null, null, null);
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> arg0, Cursor c) {
    mMethodsCursor = c;
    mAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> arg0) {
    mMethodsCursor = null;
    mAdapter.swapCursor(null);
  }

  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    if (super.dispatchCommandSingle(command, info)) {
      return true;
    }
    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) info;
    if (command == R.id.EDIT_COMMAND) {
      Intent i = new Intent(getActivity(), MethodEdit.class);
      i.putExtra(DatabaseConstants.KEY_ROWID, menuInfo.id);
      startActivity(i);
      finishActionMode();
      return true;
    }
    return false;
  }

  @Override
  public boolean dispatchCommandMultiple(int command,
                                         SparseBooleanArray positions, Long[] itemIds) {
    if (super.dispatchCommandMultiple(command, positions, itemIds)) {
      return true;
    }
    if (command == R.id.DELETE_COMMAND) {
      int columnIndexMappedTransactions = mMethodsCursor.getColumnIndex(DatabaseConstants.KEY_MAPPED_TRANSACTIONS);
      int columnIndexMappedTemplates = mMethodsCursor.getColumnIndex(DatabaseConstants.KEY_MAPPED_TEMPLATES);
      int columnIndexRowId = mMethodsCursor.getColumnIndex(DatabaseConstants.KEY_ROWID);
      int mappedTransactionsCount = 0, mappedTemplatesCount = 0;
      ArrayList<Long> idList = new ArrayList<>();
      for (int i = 0; i < positions.size(); i++) {
        if (positions.valueAt(i)) {
          boolean deletable = true;
          mMethodsCursor.moveToPosition(positions.keyAt(i));
          if (mMethodsCursor.getInt(columnIndexMappedTransactions) > 0) {
            mappedTransactionsCount++;
            deletable = false;
          }
          if (mMethodsCursor.getInt(columnIndexMappedTemplates) > 0) {
            mappedTemplatesCount++;
            deletable = false;
          }
          if (deletable) {
            idList.add(mMethodsCursor.getLong(columnIndexRowId));
          }
        }
      }
      ProtectedFragmentActivity activity = (ProtectedFragmentActivity) requireActivity();
      if (!idList.isEmpty()) {
        activity.startTaskExecution(
            TaskExecutionFragment.TASK_DELETE_PAYMENT_METHODS,
            idList.toArray(new Long[0]),
            null,
            R.string.progress_dialog_deleting);
      }
      if (mappedTransactionsCount > 0 || mappedTemplatesCount > 0) {
        String message = "";
        if (mappedTransactionsCount > 0)
          message += getResources().getQuantityString(
              R.plurals.not_deletable_mapped_transactions,
              mappedTransactionsCount,
              mappedTransactionsCount);
        if (mappedTemplatesCount > 0)
          message += getResources().getQuantityString(
              R.plurals.not_deletable_mapped_templates,
              mappedTemplatesCount,
              mappedTemplatesCount);
        activity.showSnackbar(message);
      }
      return true;
    }
    return false;
  }
}
