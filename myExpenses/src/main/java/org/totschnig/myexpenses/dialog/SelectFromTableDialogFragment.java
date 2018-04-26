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

package org.totschnig.myexpenses.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;

public abstract class SelectFromTableDialogFragment extends CommitSafeDialogFragment implements OnClickListener,
    LoaderManager.LoaderCallbacks<Cursor>
{
  private SimpleCursorAdapter mAdapter;
  private Cursor mCursor;
  
  abstract int getDialogTitle();
  abstract Uri getUri();
  abstract String getColumn();
  abstract void onResult(ArrayList<String> labelList, long[] itemIds);
  abstract String[] getSelectionArgs();
  abstract String getSelection();

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    mAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_multiple_choice, null,
        new String[] {getColumn()}, new int[] {android.R.id.text1}, 0);
    getLoaderManager().initLoader(0, null, this);
    final AlertDialog dialog = new AlertDialog.Builder(getActivity())
        .setTitle(getDialogTitle())
        .setAdapter(mAdapter,null)
        .setPositiveButton(android.R.string.ok,this)
        .setNegativeButton(android.R.string.cancel,null)
        .create();
    dialog.getListView().setItemsCanFocus(false);
    dialog.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    return dialog;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity()==null || mCursor ==null) {
      return;
    }
    ListView listView = ((AlertDialog) dialog).getListView();
    SparseBooleanArray positions = listView.getCheckedItemPositions();

    long[] itemIds = listView.getCheckedItemIds();

    if (itemIds.length>0) {
      ArrayList<String> labelList = new ArrayList<>();
      for (int i = 0; i < positions.size(); i++) {
        if (positions.valueAt(i)) {
          mCursor.moveToPosition(positions.keyAt(i));
          labelList.add(mCursor.getString(mCursor.getColumnIndex(getColumn())));
        }
      }
      onResult(labelList, itemIds);
    }
    dismiss();
  }

  @Override
  public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
    if (getActivity()==null) {
      return null;
    }
    return new CursorLoader(
        getActivity(),
        getUri(),
        null,
        getSelection(),
        getSelectionArgs(),
        null);

  }

  @Override
  public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
    mCursor = data;
    mAdapter.swapCursor(data);
  }
  @Override
  public void onLoaderReset(Loader<Cursor> arg0) {
    mCursor = null;
    mAdapter.swapCursor(null);
  }
}