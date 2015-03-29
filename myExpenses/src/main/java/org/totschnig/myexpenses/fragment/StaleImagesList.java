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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.ui.SimpleCursorAdapter;

import java.util.ArrayList;

public class StaleImagesList extends ContextualActionBarFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter mAdapter;
  private Cursor mImagesCursor;
  @Override
  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) info;
    switch(command) {
      case R.id.VIEW_COMMAND:
      return true;
    }
    return super.dispatchCommandSingle(command, info);
  }
  @Override
  public boolean dispatchCommandMultiple(int command,
      SparseBooleanArray positions,Long[]itemIds) {
    switch(command) {
      case R.id.DELETE_COMMAND:
        return true;
    }
    return super.dispatchCommandMultiple(command, positions,itemIds);
  }
  @Override
  @SuppressLint("InlinedApi")
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.images_list, null, false);
    
    final ListView lv = (ListView) v.findViewById(R.id.list);

    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{DatabaseConstants.KEY_PICTURE_URI};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{android.R.id.text1};

    // Now create a simple cursor adapter and set it to display
    mAdapter = new SimpleCursorAdapter(
        getActivity(), 
        Build.VERSION.SDK_INT >= 11 ?
            android.R.layout.simple_list_item_activated_1 :
            android.R.layout.simple_list_item_1,
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
        TransactionProvider.STALE_IMAGES_URI, null, null,null, null);
    return cursorLoader;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
    mImagesCursor = c;
    mAdapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mImagesCursor = null;
    mAdapter.swapCursor(null);
  }
}
