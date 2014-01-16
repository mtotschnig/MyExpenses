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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PartiesList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter mAdapter;
  private Cursor mPartiesCursor;
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0, R.id.EDIT_COMMAND, 0, R.string.menu_edit_party);
    menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    FragmentActivity ctx = getActivity();
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    switch(item.getItemId()) {
    case R.id.DELETE_COMMAND:
      mPartiesCursor.moveToPosition(info.position);
      int message = 0;
      if (mPartiesCursor.getInt(mPartiesCursor.getColumnIndex("mapped_transactions")) > 0) {
        message = R.string.not_deletable_mapped_transactions;
      } else if (mPartiesCursor.getInt(mPartiesCursor.getColumnIndex("mapped_templates")) > 0) {
        message = R.string.not_deletable_mapped_templates;
      }
      if (message == 0)
        ctx.getSupportFragmentManager().beginTransaction()
          .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_DELETE_PAYEE,info.id, null), "ASYNC_TASK")
          .commit();
      else
        Toast.makeText(ctx,getString(message), Toast.LENGTH_LONG).show();
      return true;
    case R.id.EDIT_COMMAND:
      Bundle args = new Bundle();
      args.putLong("partyId", info.id);
      args.putString("dialogTitle", getString(R.string.menu_edit_party));
      args.putString("value",((TextView) info.targetView.findViewById(android.R.id.text1)).getText().toString());
      EditTextDialog.newInstance(args).show(ctx.getSupportFragmentManager(), "EDIT_PARTY");
      return true;
    }
    return super.onContextItemSelected(item);
  }
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.parties_list, null, false);
    
    final ListView lv = (ListView) v.findViewById(R.id.list);
    lv.setItemsCanFocus(false);
    lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    //((TextView) findViewById(android.R.id.empty)).setText(R.string.no_parties);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{"name"};

    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{android.R.id.text1};

    // Now create a simple cursor adapter and set it to display
    mAdapter = new SimpleCursorAdapter(getActivity(), 
        android.R.layout.simple_list_item_1, null, from, to,0);

    getLoaderManager().initLoader(0, null, this);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    registerForContextMenu(lv);
    return v;
  }
  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    CursorLoader cursorLoader = new CursorLoader(getActivity(),
        TransactionProvider.PAYEES_URI, null, null,null, null);
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
