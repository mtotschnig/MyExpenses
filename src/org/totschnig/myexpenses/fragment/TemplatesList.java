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
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragment;

public class TemplatesList extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private SimpleCursorAdapter mAdapter;
  int mGroupIdColumnIndex;
  private LoaderManager mManager;
  long mAccountId;
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mAccountId = ((ManageTemplates) getActivity()).mAccountId;
    View v = inflater.inflate(R.layout.templates_list, null, false);
    ListView lv = (ListView) v.findViewById(R.id.list);
    mManager = getLoaderManager();
    mManager.initLoader(0, null, this);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{DatabaseConstants.KEY_TITLE};
    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{android.R.id.text1};
    mAdapter = new SimpleCursorAdapter(getActivity(), 
        android.R.layout.simple_list_item_1, null, from, to,0);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    //requires using activity (ManageTemplates) to implement OnChildClickListener
    //lv.setOnChildClickListener((OnChildClickListener) getActivity());
    registerForContextMenu(lv);
    return v;
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    return new CursorLoader(getActivity(),TransactionProvider.TEMPLATES_URI, null,
        "account_id = ?",new String[] { String.valueOf(mAccountId) }, "usages DESC");
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      mAdapter.swapCursor(data);
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
      mAdapter.swapCursor(null);
  }
}
