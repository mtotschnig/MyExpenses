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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.ui.SimpleCursorTreeAdapter;
import org.totschnig.myexpenses.util.Utils;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import com.android.calendar.CalendarContractCompat.Events;
import com.android.calendar.CalendarContractCompat.Instances;

public class PlanList extends BudgetListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  public static final int TEMPLATES_CURSOR = -1;
  public static final int PLANS_CURSOR  =-2;
  Cursor mTemplatesCursor;
  private HashMap<Long,String> mPlanTimeInfo;
  private HashMap<Long,Long> mInstance2TransactionMap = new HashMap<Long,Long>();
  private MyExpandableListAdapter mAdapter;
  //private SimpleCursorAdapter mAdapter;
  //private StickyListHeadersListView mListView;
  int mGroupIdColumnIndex;
  private LoaderManager mManager;
  
  private int columnIndexPlanId, columnIndexAmount, columnIndexLabelSub, columnIndexComment,
    columnIndexPayee, columnIndexTitle, columnIndexColor,columnIndexTransferPeer,
    columnIndexCurrency, columnIndexRowId;
  boolean indexesCalculated = false;
  
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setColors();
    View v = inflater.inflate(R.layout.plans_list, null, false);
    ExpandableListView lv = (ExpandableListView) v.findViewById(R.id.list);

    mManager = getLoaderManager();
    mManager.initLoader(TEMPLATES_CURSOR, null, this);
    mAdapter = new MyExpandableListAdapter(
        getActivity(),
        null,
        R.layout.plan_row,
        R.layout.plan_instance_row,
        new String[]{KEY_TITLE,KEY_LABEL_MAIN,KEY_AMOUNT},
        new int[]{R.id.title,R.id.category,R.id.amount},
        new String[]{Instances.BEGIN},
        new int[]{R.id.date}
        );
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    //requires using activity (ManageTemplates) to implement OnChildClickListener
    //lv.setOnChildClickListener((OnChildClickListener) getActivity());
//    lv.setOnItemClickListener(new OnItemClickListener()
//    {
//         @Override
//         public void onItemClick(AdapterView<?> a, View v,int position, long id)
//         {
//           TemplateDetailFragment.newInstance(id)
//           .show(getActivity().getSupportFragmentManager(), "TEMPLATE_DETAIL");
//         }
//    });
    registerForContextMenu(lv);
    return v;
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
    int type = ExpandableListView.getPackedPositionType(info.packedPosition);

    // Menu entries relevant only for the group
    if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
      super.onCreateContextMenu(menu, v, menuInfo);
    } else {
      Long transactionId = mInstance2TransactionMap.get(info.id);
      if (transactionId == null) {
        //state open
        menu.add(0,R.id.CREATE_INSTANCE_SAVE_COMMAND,0,R.string.menu_apply_template_and_save);
        menu.add(0,R.id.CREATE_INSTANCE_EDIT_COMMAND,0,R.string.menu_apply_template_and_edit);
        menu.add(0,R.id.CANCEL_PLAN_INSTANCE_COMMAND,0,R.string.menu_cancel_plan_instance);
      }
      else if (transactionId == 0L) {
        //state cancelled
        menu.add(0,R.id.RESET_PLAN_INSTANCE_COMMAND,0,R.string.menu_reset_plan_instance);
      }
      else {
        //state applied
        menu.add(0,R.id.EDIT_COMMAND,0,R.string.menu_edit);
        menu.add(0,R.id.CANCEL_PLAN_INSTANCE_COMMAND,0,R.string.menu_cancel_plan_instance);
        menu.add(0,R.id.RESET_PLAN_INSTANCE_COMMAND,0,R.string.menu_reset_plan_instance);
      }
    }
  }
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (!getUserVisibleHint())
      return false;
    ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
    if (ExpandableListView.getPackedPositionType(info.packedPosition) ==
        ExpandableListView.PACKED_POSITION_TYPE_GROUP)
      return ((ManageTemplates) getActivity()).dispatchCommand(item.getItemId(),info.id);
    int group = ExpandableListView.getPackedPositionGroup(info.packedPosition),
        child = ExpandableListView.getPackedPositionChild(info.packedPosition);
    Cursor c = mAdapter.getChild(group,child);
    long date = c.getLong(c.getColumnIndex(Instances.BEGIN));
    long templateId = mTemplatesCursor.getLong(columnIndexRowId);
    switch(item.getItemId()) {
    case R.id.CREATE_INSTANCE_EDIT_COMMAND:
      Intent intent = new Intent(getActivity(), ExpenseEdit.class);
      intent.putExtra("template_id", templateId);
      intent.putExtra("instance_id", info.id);
      intent.putExtra("instance_date", date);
      startActivity(intent);
      return true;
    case R.id.CREATE_INSTANCE_SAVE_COMMAND:
      if (Template.getInstanceFromDb(templateId).applyInstance(info.id,date)) {
        Toast.makeText(getActivity(),getString(R.string.save_transaction_from_template_success), Toast.LENGTH_LONG).show();
      } else {
        Toast.makeText(getActivity(),getString(R.string.save_transaction_error), Toast.LENGTH_LONG).show();
      }
      return true;
      }
    return false;
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    switch(id) {
    case TEMPLATES_CURSOR:
      return new CursorLoader(getActivity(),
        TransactionProvider.TEMPLATES_URI,
        null,
        KEY_PLANID + " is not null",
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
    default:
      if (id % 2 == 0) {
        // The ID of the recurring event whose instances you are searching
        // for in the Instances table
        String selection = Instances.EVENT_ID + " = " + bundle.getLong("plan_id");
        // Construct the query with the desired date range.
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        long now = System.currentTimeMillis();
        ContentUris.appendId(builder, now);
        ContentUris.appendId(builder, now + 7776000000L); //90 days
        return new CursorLoader(
            getActivity(),
            builder.build(),
            new String[]{
              Instances._ID,
              Instances.BEGIN
            },
            selection,
            null,
            null);
      } else {
        return new CursorLoader(
            getActivity(),
            TransactionProvider.PLAN_INSTANCE_STATUS_URI,
            new String[]{
              KEY_TEMPLATEID,
              KEY_INSTANCEID,
              KEY_TRANSACTIONID
            },
            KEY_TEMPLATEID + " = ?",
            new String[]{bundle.getString("template_id")},
            null);
      }
    }
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
    int id = loader.getId();
    switch (id) {
    case TEMPLATES_CURSOR:
      mTemplatesCursor = c;
      if (!indexesCalculated) {
        columnIndexRowId = c.getColumnIndex(KEY_ROWID);
        columnIndexPlanId = c.getColumnIndex(KEY_PLANID);
        columnIndexAmount = c.getColumnIndex(KEY_AMOUNT);
        columnIndexLabelSub = c.getColumnIndex(KEY_LABEL_SUB);
        columnIndexComment = c.getColumnIndex(KEY_COMMENT);
        columnIndexPayee = c.getColumnIndex(KEY_PAYEE_NAME);
        columnIndexTitle = c.getColumnIndex(KEY_TITLE);
        columnIndexColor = c.getColumnIndex(KEY_COLOR);
        columnIndexTransferPeer = c.getColumnIndex(KEY_TRANSFER_PEER);
        columnIndexCurrency = c.getColumnIndex(KEY_CURRENCY);
        indexesCalculated = true;
      }
      if (mTemplatesCursor.getCount()>0) {
        mTemplatesCursor.moveToFirst();
        ArrayList<Long> plans = new ArrayList<Long>();
        long planId;
        Bundle planBundle = new Bundle();
        while (mTemplatesCursor.isAfterLast() == false) {
          Bundle instanceBundle = new Bundle();
          instanceBundle.putString("template_id", mTemplatesCursor.getString(columnIndexRowId));
          mManager.initLoader(mTemplatesCursor.getPosition()*2+1, instanceBundle, this);
          if ((planId = mTemplatesCursor.getLong(columnIndexPlanId)) != 0L) {
            plans.add(planId);
          }
          mTemplatesCursor.moveToNext();
        }
        planBundle.putSerializable("plans", plans);
        mManager.initLoader(PLANS_CURSOR, planBundle, this);
      } else {
        mPlanTimeInfo = new HashMap<Long, String>();
        mAdapter.setGroupCursor(mTemplatesCursor);
      }
      break;
    case PLANS_CURSOR:
      mPlanTimeInfo = new HashMap<Long, String>();
      c.moveToFirst();
      while (c.isAfterLast() == false) {
        mPlanTimeInfo.put(
            c.getLong(c.getColumnIndex(Events._ID)),
            Plan.prettyTimeInfo(
                getActivity(),
                c.getString(c.getColumnIndex(Events.RRULE)),
                c.getLong(c.getColumnIndex(Events.DTSTART))));
        c.moveToNext();
      }
      mAdapter.setGroupCursor(mTemplatesCursor);
      break;
    default:
      int groupPosition = id / 2;
      if (id % 2 == 0) {
        //check if group still exists
        if (mAdapter.getGroupId(groupPosition) != 0)
            mAdapter.setChildrenCursor(groupPosition, c);
      } else {
        c.moveToFirst();
        while (c.isAfterLast() == false) {
          mInstance2TransactionMap.put(
              c.getLong(c.getColumnIndex(KEY_INSTANCEID)),
              c.getLong(c.getColumnIndex(KEY_TRANSACTIONID)));
          c.moveToNext();
        }
      }
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    int id = loader.getId();
    switch (id) {
    case TEMPLATES_CURSOR:
      mTemplatesCursor = null;
      mAdapter.setGroupCursor(null);
    case PLANS_CURSOR:
      mPlanTimeInfo = null;
    default:
      int groupPosition = id / 2;
      if (id % 2 == 0) {
        try {
          mAdapter.setChildrenCursor(groupPosition, null);
        } catch (NullPointerException e) {
          Log.w("TAG", "Adapter expired, try again on the next query: "
                  + e.getMessage());
        }
      }
    }
  }

  public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {
    String categorySeparator = " : ",
        commentSeparator = " / ";
    Calendar calendar = Calendar.getInstance();
    java.text.DateFormat dateFormat = java.text.DateFormat.
        getDateInstance(java.text.DateFormat.FULL);
    public MyExpandableListAdapter(Context context, Cursor cursor, int groupLayout,
            int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
            int[] childrenTo) {
        super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom,
                childrenTo);
    }
    @Override
    public void setViewText(TextView v, String text) {
      switch (v.getId()) {
      case R.id.date:
        calendar.setTimeInMillis(Long.valueOf(text));
        text = dateFormat.format(calendar.getTime());
      }
      super.setViewText(v, text);
    }
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
        View convertView, ViewGroup parent) {
      convertView= super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
      Cursor c = getChild(groupPosition, childPosition);
      ImageView iv = (ImageView)convertView.findViewById(R.id.planInstanceStatus);
      Long instanceId = c.getLong(c.getColumnIndex(Instances._ID));
      Log.i(MyApplication.TAG, "looking up instance2transactionamp for instance "+instanceId);
      Long transactionId = mInstance2TransactionMap.get(instanceId);
      if (transactionId == null)
        iv.setImageResource(R.drawable.ic_stat_open);
      else if (transactionId == 0L)
        iv.setImageResource(R.drawable.ic_stat_cancelled);
      else
        iv.setImageResource(R.drawable.ic_stat_applied);
      return convertView;
    }
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {
      convertView= super.getGroupView(groupPosition, isExpanded, convertView, parent);
      Cursor c = getCursor();
      c.moveToPosition(groupPosition);
      TextView tv1 = (TextView)convertView.findViewById(R.id.amount);
      long amount = c.getLong(columnIndexAmount);
      if (amount < 0) {
        tv1.setTextColor(colorExpense);
        // Set the background color of the text.
      }
      else {
        tv1.setTextColor(colorIncome);
      }
      tv1.setText(Utils.convAmount(amount,Utils.getSaveInstance(c.getString(columnIndexCurrency))));
      Long planId = DbUtils.getLongOrNull(c, KEY_PLANID);
      String planInfo = mPlanTimeInfo.get(planId);
      if (planInfo == null) {
        planInfo = "Event deleted from Calendar";
      }
      ((TextView) convertView.findViewById(R.id.title)).setText(
          c.getString(columnIndexTitle)
          +" (" + planInfo + ")");
      int color = c.getInt(columnIndexColor);
      convertView.findViewById(R.id.colorAccount).setBackgroundColor(color);
      TextView tv2 = (TextView)convertView.findViewById(R.id.category);
      CharSequence catText = tv2.getText();
      if (c.getInt(columnIndexTransferPeer) > 0) {
        catText = ((amount < 0) ? "=> " : "<= ") + catText;
      } else {
        Long catId = DbUtils.getLongOrNull(c,KEY_CATID);
        if (catId == null) {
          catText = getString(R.string.no_category_assigned);
        } else {
          String label_sub = c.getString(columnIndexLabelSub);
          if (label_sub != null && label_sub.length() > 0) {
            catText = catText + categorySeparator + label_sub;
          }
        }
      }
      SpannableStringBuilder ssb;
      String comment = c.getString(columnIndexComment);
      if (comment != null && comment.length() > 0) {
        ssb = new SpannableStringBuilder(comment);
        ssb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, comment.length(), 0);
        catText = TextUtils.concat(catText,commentSeparator,ssb);
      }
      String payee = c.getString(columnIndexPayee);
      if (payee != null && payee.length() > 0) {
        ssb = new SpannableStringBuilder(payee);
        ssb.setSpan(new UnderlineSpan(), 0, payee.length(), 0);
        catText = TextUtils.concat(catText,commentSeparator,ssb);
      }
      tv2.setText(catText);
      return convertView;
    }
    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
      long planId = groupCursor.getLong(groupCursor.getColumnIndexOrThrow(KEY_PLANID));
      
      Bundle bundle = new Bundle();
      bundle.putLong("plan_id", planId);
      int groupLoaderId = groupCursor.getPosition()*2;
      //we use groupPos*2 as id of the calendar instances query
      //and groupPos*2+1 as id of the plan instance status query
      if (mManager.getLoader(groupLoaderId) != null && !mManager.getLoader(groupLoaderId).isReset()) {
          try {
            mManager.restartLoader(groupLoaderId, bundle, PlanList.this);
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
        mManager.initLoader(groupLoaderId, bundle, PlanList.this);
      }
      return null;
    }
  }
}
