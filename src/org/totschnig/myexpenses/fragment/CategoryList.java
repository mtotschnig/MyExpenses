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

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import org.totschnig.myexpenses.ui.SimpleCursorTreeAdapter;


public class CategoryList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private MyExpandableListAdapter mAdapter;
  int mGroupIdColumnIndex;
  private LoaderManager mManager;
  long accountId;
  String groupingClause;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Bundle extras = getActivity().getIntent().getExtras();
    if (extras != null) {
      accountId = extras.getLong(KEY_ACCOUNTID);
      groupingClause = extras.getString("groupingClause");
    } else 
      accountId = 0L;
    View v = inflater.inflate(R.layout.categories_list, null, false);
    ExpandableListView lv = (ExpandableListView) v.findViewById(R.id.list);
    mManager = getLoaderManager();
    mManager.initLoader(-1, null, this);
    String[] from;
    int[] to;
    if (accountId != 0) {
      from = new String[] {"label","sum"};
      to = new int[] {R.id.label,R.id.amount};
    } else {
      from = new String[] {"label"};
      to = new int[] {R.id.label};
    }
    mAdapter = new MyExpandableListAdapter(getActivity(),
        null,
        R.layout.category_row,R.layout.category_row,
        from,to,from,to);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    //requires using activity (SelectCategory) to implement OnChildClickListener
    lv.setOnChildClickListener((OnChildClickListener) getActivity());
    lv.setOnGroupClickListener((OnGroupClickListener) getActivity());
    registerForContextMenu(lv);
    return v;
  }
  /**
   * Mapping the categories table into the ExpandableList
   * @author Michael Totschnig
   *
   */
  public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {
    public MyExpandableListAdapter(Context context, Cursor cursor, int groupLayout,
            int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
            int[] childrenTo) {
        super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom,
                childrenTo);
    }
    /* (non-Javadoc)
     * returns a cursor with the subcategories for the group
     * @see android.widget.CursorTreeAdapter#getChildrenCursor(android.database.Cursor)
     */
    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        // Given the group, we return a cursor for all the children within that group
      long parentId = groupCursor.getLong(groupCursor.getColumnIndexOrThrow(KEY_ROWID));
      Bundle bundle = new Bundle();
      bundle.putLong("parent_id", parentId);
      int groupPos = groupCursor.getPosition();
      if (mManager.getLoader(groupPos) != null && !mManager.getLoader(groupPos).isReset()) {
          try {
            mManager.restartLoader(groupPos, bundle, CategoryList.this);
          } catch (NullPointerException e) {
            // a NPE is thrown in the following scenario:
            //1)open a group
            //2)orientation change
            //3)open the same group again
            //in this scenario getChildrenCursor is called twice, second time leads to error
            //maybe it is trying to close the group that had been kept open before the orientation change
            e.printStackTrace();
          }
      } else {
        mManager.initLoader(groupPos, bundle, CategoryList.this);
      }
      return null;
    }
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    long parentId;
    String selection = "",strAccountId="";
    String[] selectionArgs,projection = null;
    if (accountId != 0) {
      strAccountId = String.valueOf(accountId);
      String catFilter = "FROM transactions WHERE account_id = ?";
      if (groupingClause != null)
        catFilter += " AND " +groupingClause;
      //we need to include transactions mapped to children for main categories
      if (bundle == null)
        catFilter += " AND cat_id IN (select _id FROM categories subtree where parent_id = categories._id OR _id = categories._id)";
      else
        catFilter += " AND cat_id  = categories._id";
      selection = " AND exists (select 1 " + catFilter +")";
      projection = new String[] {KEY_ROWID, KEY_LABEL, KEY_PARENTID,
          "(SELECT sum(amount) " + catFilter + ") AS sum"};
    }
    if (bundle == null) {
      selection = "parent_id is null" + selection;
      selectionArgs = accountId != 0 ? new String[]{strAccountId,strAccountId} : null;
    } else {
      parentId = bundle.getLong("parent_id");
      selection = "parent_id = ?"  + selection;
      selectionArgs = accountId != 0 ?
          new String[]{strAccountId,String.valueOf(parentId),strAccountId} :
          new String[]{String.valueOf(parentId)};
    }
    return new CursorLoader(getActivity(),TransactionProvider.CATEGORIES_URI, projection,
        selection,selectionArgs, null);
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    int id = loader.getId();
    if (id == -1)
      mAdapter.setGroupCursor(data);
    else {
      //check if group still exists
      if (mAdapter.getGroupId(id) != 0)
          mAdapter.setChildrenCursor(id, data);
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    int id = loader.getId();
    if (id != -1) {
        // child cursor
        try {
            mAdapter.setChildrenCursor(id, null);
        } catch (NullPointerException e) {
            Log.w("TAG", "Adapter expired, try again on the next query: "
                    + e.getMessage());
        }
    } else {
      mAdapter.setGroupCursor(null);
    }
  }
}
