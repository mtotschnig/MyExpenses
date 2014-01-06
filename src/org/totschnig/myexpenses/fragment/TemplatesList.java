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
import java.util.HashMap;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.TemplateDetailFragment;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.Transaction;
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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.calendar.CalendarContractCompat.Events;
//TODO: cache column indexes
public class TemplatesList extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  public static final int TEMPLATES_CURSOR=1;
  public static final int PLANS_CURSOR=2;
  Cursor mTemplatesCursor;
  private HashMap<Long,String> mPlanTimeInfo;
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
    mManager.initLoader(TEMPLATES_CURSOR, null, this);
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
    switch(id) {
    case TEMPLATES_CURSOR:
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
    case PLANS_CURSOR:
      return new CursorLoader(getActivity(),
        Events.CONTENT_URI,
        new String[]{
          Events._ID,
          Events.DTSTART,
          Events.RRULE,
        },
        Events._ID + " IN (" + TextUtils.join(",",(ArrayList<Long>) bundle.getSerializable("plans"))  + ")",
        null,
        null);
    }
    return null;
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    switch (loader.getId()) {
    case TEMPLATES_CURSOR:
      mTemplatesCursor = data;
      mTemplatesCursor.moveToFirst();
      ArrayList<Long> plans = new ArrayList<Long>();
      long planId;
      Bundle bundle = new Bundle();
      while (mTemplatesCursor.isAfterLast() == false) {
        if ((planId = mTemplatesCursor.getLong(data.getColumnIndexOrThrow(DatabaseConstants.KEY_PLANID))) != 0L) {
          plans.add(planId);
        }
        mTemplatesCursor.moveToNext();
      }
      if (plans.size()>0) {
        bundle.putSerializable("plans", plans);
        mManager.initLoader(PLANS_CURSOR, bundle, this);
      } else {
        ((SimpleCursorAdapter) mAdapter).swapCursor(mTemplatesCursor);
      }
      break;
    case PLANS_CURSOR:
      mPlanTimeInfo = new HashMap<Long, String>();
      data.moveToFirst();
      while (data.isAfterLast() == false) {
        mPlanTimeInfo.put(
            data.getLong(data.getColumnIndex(Events._ID)),
            Plan.prettyTimeInfo(
                getActivity(),
                data.getString(data.getColumnIndex(Events.RRULE)),
                data.getLong(data.getColumnIndex(Events.DTSTART))));
        data.moveToNext();
      }
      ((SimpleCursorAdapter) mAdapter).swapCursor(mTemplatesCursor);
      break;
    }
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
      ImageView button = (ImageView) convertView.findViewById(R.id.handleTemplateOrPlan);
      button.setTag(position);
      Cursor c = getCursor();
      c.moveToPosition(position);
      Long planId = DbUtils.getLongOrNull(c, DatabaseConstants.KEY_PLANID);
      if (planId != null) {
        String planInfo = mPlanTimeInfo.get(planId);
        if (planInfo == null) {
          planInfo = "Event deleted from Calendar";
          button.setVisibility(View.GONE);
        } else {
          button.setImageResource(android.R.drawable.ic_menu_my_calendar);
          button.setVisibility(View.VISIBLE);
        }
        ((TextView) convertView.findViewById(R.id.title)).setText(
            c.getString(c.getColumnIndex(DatabaseConstants.KEY_TITLE))
            +" (" + planInfo + ")");
      } else {
        button.setImageResource(R.drawable.manage_plans_icon);
        button.setVisibility(View.VISIBLE);
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
  public void handleTemplateOrPlan(View v) {
    mTemplatesCursor.moveToPosition((Integer) v.getTag());
    if (DbUtils.getLongOrNull(mTemplatesCursor, DatabaseConstants.KEY_PLANID) == null) {
    //TODO Strict mode
      if (Transaction.getInstanceFromTemplate(mTemplatesCursor.getLong(mTemplatesCursor.getColumnIndex(DatabaseConstants.KEY_ROWID))).save() == null)
        Toast.makeText(getActivity(),getString(R.string.save_transaction_error), Toast.LENGTH_LONG).show();
      else
        Toast.makeText(getActivity(),getString(R.string.save_transaction_from_template_success), Toast.LENGTH_LONG).show();
      getActivity().finish();
    } else {
        Toast.makeText(getActivity(),"TODO: show instance list", Toast.LENGTH_LONG).show();
    }
  }
}
