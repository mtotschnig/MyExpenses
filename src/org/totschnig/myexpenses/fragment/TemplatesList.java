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
import org.totschnig.myexpenses.dialog.TemplateDetailFragment;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockFragment;

public class TemplatesList extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String EXTRA_COLUMN = "RecurrenceInfo";
  private StickyListHeadersAdapter mAdapter;
  //private SimpleCursorAdapter mAdapter;
  //private StickyListHeadersListView mListView;
  int mGroupIdColumnIndex;
  private LoaderManager mManager;
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.templates_list, null, false);
    StickyListHeadersListView lv = (StickyListHeadersListView) v.findViewById(R.id.list);
    mManager = getLoaderManager();
    mManager.initLoader(0, null, this);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{DatabaseConstants.KEY_TITLE};
    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.title};
    mAdapter = new MyGroupedAdapter(
        getActivity(), 
        R.layout.template_row,
        null,
        from,
        to,
        0);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    //requires using activity (ManageTemplates) to implement OnChildClickListener
    //lv.setOnChildClickListener((OnChildClickListener) getActivity());
    lv.setOnItemClickListener(new OnItemClickListener()
    {
         @Override
         public void onItemClick(AdapterView<?> a, View v,int position, long id)
         {
           TemplateDetailFragment.newInstance(id)
           .show(getActivity().getSupportFragmentManager(), "TEMPLATE_DETAIL");
         }
    });
    registerForContextMenu(lv);
    return v;
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    return new CursorLoader(getActivity(),
        TransactionProvider.TEMPLATES_URI,
        new String[] {
          DatabaseConstants.KEY_ROWID,
          DatabaseConstants.KEY_TITLE,
          DatabaseConstants.KEY_PLANID,
          DatabaseConstants.KEY_COLOR
        },
        null,
        null,
        null);
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    ((SimpleCursorAdapter) mAdapter).swapCursor(new CursorExtendedWithPlanInfo(data,EXTRA_COLUMN));
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
      ((SimpleCursorAdapter) mAdapter).swapCursor(null);
  }
  public class MyAdapter extends SimpleCursorAdapter {
    public MyAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(context, layout, c, from, to, flags);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      convertView=super.getView(position, convertView, parent);
      ImageView button = (ImageView) convertView.findViewById(R.id.apply);
      button.setTag(getItemId(position));
      Cursor c = getCursor();
      c.moveToPosition(position);
      if (DbUtils.getLongOrNull(c, DatabaseConstants.KEY_PLANID) != null) {
        button.setImageResource(android.R.drawable.ic_menu_my_calendar);
        ((TextView) convertView.findViewById(R.id.title)).setText(
            c.getString(c.getColumnIndex(DatabaseConstants.KEY_TITLE))
            +" (" + c.getString(c.getColumnIndex(EXTRA_COLUMN))+ ")");
      }
      int color = c.getInt(c.getColumnIndex("color"));
      convertView.findViewById(R.id.colorAccount).setBackgroundColor(color);
      return convertView;
    }
  }
  public class MyGroupedAdapter extends MyAdapter implements StickyListHeadersAdapter {
    LayoutInflater inflater;
    public MyGroupedAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(context, layout, c, from, to, flags);
      inflater = LayoutInflater.from(getSherlockActivity());
    }
    @Override
    public long getHeaderId(int position) {
      Cursor c = getCursor();
      c.moveToPosition(position);
      return DbUtils.getLongOrNull(c, DatabaseConstants.KEY_PLANID) == null ?
          0 : 1;
    }
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = inflater.inflate(R.layout.template_plan_header, parent, false);
      }
      Cursor c = getCursor();
      c.moveToPosition(position);
      ((TextView) convertView.findViewById(R.id.text)).setText(
          DbUtils.getLongOrNull(c, DatabaseConstants.KEY_PLANID) == null ?
              "Templates" : "Plans");
      return convertView;
    }
  }
  private class CursorExtendedWithPlanInfo extends CursorWrapper {
    int additionalColumnIndex;
    String additionalColumnName;
    public CursorExtendedWithPlanInfo(Cursor cursor,String additionalColumnName) {
      super(cursor);
      this.additionalColumnName = additionalColumnName;
      additionalColumnIndex =cursor.getColumnCount();
    }
    public int getColumnCount() {
      return super.getColumnCount()+1;
    }
    public int getColumnIndex(String columnName) {
      if (columnName == additionalColumnName) {
        return additionalColumnIndex;
      }
      return super.getColumnIndex(columnName);
  }
    public String getString(int columnIndex) {
      if (columnIndex == additionalColumnIndex) {
        return "EXTRA INFO";
      }
      return super.getString(columnIndex);
    }
  }
}
