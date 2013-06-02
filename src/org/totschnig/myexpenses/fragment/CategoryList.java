package org.totschnig.myexpenses.fragment;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.TransactionProvider;

//TODO: check if we still need this workaround class:
//http://code.google.com/p/android/issues/detail?id=9170
import com.ozdroid.adapter.SimpleCursorTreeAdapter2;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;


public class CategoryList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private MyExpandableListAdapter mAdapter;
  int mGroupIdColumnIndex;
  private LoaderManager mManager;
  
  /**
   * how should categories be sorted, configurable through setting
   */
  String mOrderBy;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      mOrderBy = (MyApplication.getInstance().getSettings()
          .getBoolean(MyApplication.PREFKEY_CATEGORIES_SORT_BY_USAGES, true) ? "usages DESC, " : "") + "label";
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.categories_list, null, false);
    ExpandableListView lv = (ExpandableListView) v.findViewById(R.id.list);
    mManager = getLoaderManager();
    Bundle bundle = new Bundle();
    bundle.putLong("parent_id", 0);
    mManager.initLoader(-1, bundle, this);
    mAdapter = new MyExpandableListAdapter(getActivity(),
        null,
        android.R.layout.simple_expandable_list_item_1,
        android.R.layout.simple_expandable_list_item_1,
        new String[]{"label"},
        new int[] {android.R.id.text1},
        new String[] {"label"},
        new int[] {android.R.id.text1});
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
  public class MyExpandableListAdapter extends SimpleCursorTreeAdapter2 {
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
          mManager.restartLoader(groupPos, bundle, CategoryList.this);
      }
      else {
          mManager.initLoader(groupPos, bundle, CategoryList.this);
      }
      return null;
    }
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    long parentId = bundle.getLong("parent_id");
    return new CursorLoader(getActivity(),TransactionProvider.CATEGORIES_URI, null,
        "parent_id = ?", new String[]{String.valueOf(parentId)}, mOrderBy);
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    int id = loader.getId();
    if (id == -1)
      mAdapter.setGroupCursor(data);
    else
      mAdapter.setChildrenCursor(id, data);
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    if (loader.getId() == -1)
      mAdapter.setGroupCursor(null);
  }
}
