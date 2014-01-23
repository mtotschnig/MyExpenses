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

import java.util.ArrayList;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MethodEdit;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class MethodList extends ContextualActionBarFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter mAdapter;
  private Cursor mMethodsCursor;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.methods_list, null, false);
    final ListView lv = (ListView) v.findViewById(R.id.list);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{DatabaseConstants.KEY_ROWID};
    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{android.R.id.text1};
    // Now create a simple cursor adapter and set it to display
    mAdapter = new SimpleCursorAdapter(getActivity(), 
        android.R.layout.simple_list_item_1, null, from, to,0) {
      @Override
      public void setViewText(TextView v, String text) {
        super.setViewText(v, PaymentMethod.getInstanceFromDb(Long.valueOf(text)).getDisplayLabel());
      }
    };
    getLoaderManager().initLoader(0, null, this);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    registerForContextualActionBar(lv);
    return v;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    CursorLoader cursorLoader = new CursorLoader(getActivity(),
        TransactionProvider.METHODS_URI, null, null,null, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    mMethodsCursor = c;
    mAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mMethodsCursor = null;
    mAdapter.swapCursor(null);
  }
  public boolean dispatchCommandSingle(int command, AdapterContextMenuInfo info) {
    switch(command) {
    case R.id.EDIT_COMMAND:
      Intent i = new Intent(getActivity(), MethodEdit.class);
      i.putExtra(DatabaseConstants.KEY_ROWID, info.id);
      startActivity(i);
      return true;
    }
    return super.dispatchCommandSingle(command, info);
  }
  @Override
  public boolean dispatchCommandMultiple(int command,
      SparseBooleanArray positions,Long[]itemIds) {
    switch(command) {
    case R.id.DELETE_COMMAND:
      int columnIndexMappedTransactions = mMethodsCursor.getColumnIndex("mapped_transactions");
      int columnIndexMappedTemplates = mMethodsCursor.getColumnIndex("mapped_templates");
      int columnIndexRowId = mMethodsCursor.getColumnIndex(DatabaseConstants.KEY_ROWID);
      int mappedTransactionsCount = 0, mappedTemplatesCount = 0;
      ArrayList<Long> idList = new ArrayList<Long>();
      for (int i=0; i<positions.size(); i++) {
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
      if (idList.size()>0) {
        getActivity().getSupportFragmentManager().beginTransaction()
          .add(TaskExecutionFragment.newInstance(
              TaskExecutionFragment.TASK_DELETE_PAYMENT_METHODS,
              idList.toArray(new Long[idList.size()]),
              null),
            "ASYNC_TASK")
          .commit();
      }
      if (mappedTransactionsCount > 0 || mappedTemplatesCount > 0 ) {
        String message = "";
        if (mappedTransactionsCount > 0)
          message += getString(R.string.not_deletable_mapped_transactions,mappedTransactionsCount);
        if (mappedTemplatesCount > 0)
          message += getString(R.string.not_deletable_mapped_templates,mappedTemplatesCount);
        Toast.makeText(getActivity(),message, Toast.LENGTH_LONG).show();
      }
      return true;
    }
    return super.dispatchCommandMultiple(command, positions,itemIds);
  }
}
