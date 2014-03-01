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
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.ui.SimpleCursorTreeAdapter;
import org.totschnig.myexpenses.util.Utils;

import android.annotation.SuppressLint;
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
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.calendar.CalendarContractCompat.Events;
import com.android.calendar.CalendarContractCompat.Instances;

public class PlanList extends BudgetListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

  protected int getMenuResource() {
    return R.menu.planlist_context;
  }

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
  private ExpandableListView mListView;
  private int mExpandedPosition = -1;
  public boolean newPlanEnabled;
  private enum TransactionState {
    OPEN,APPLIED,CANCELLED
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setColors();
    View v = inflater.inflate(R.layout.plans_list, null, false);
    mListView = (ExpandableListView) v.findViewById(R.id.list);

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
    mListView.setAdapter(mAdapter);
    mListView.setEmptyView(v.findViewById(R.id.empty));
    registerForContextualActionBar(mListView);
    return v;
  }
  @Override
  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    ExpandableListContextMenuInfo menuInfo = (ExpandableListContextMenuInfo) info;
    if (ExpandableListView.getPackedPositionType(menuInfo.packedPosition) ==
        ExpandableListView.PACKED_POSITION_TYPE_GROUP)
      return ((ManageTemplates) getActivity()).dispatchCommand(command, menuInfo.id);
    Intent i;
    Long transactionId = mInstance2TransactionMap.get(menuInfo.id);
    switch(command) {
    case R.id.EDIT_PLAN_INSTANCE_COMMAND:
      i = new Intent(getActivity(), ExpenseEdit.class);
      i.putExtra(KEY_ROWID, transactionId);
      startActivity(i);
      break;
    case R.id.CREATE_INSTANCE_EDIT_COMMAND:
      int group = ExpandableListView.getPackedPositionGroup(menuInfo.packedPosition),
        child = ExpandableListView.getPackedPositionChild(menuInfo.packedPosition);
      Cursor c = mAdapter.getChild(group,child);
      long date = c.getLong(c.getColumnIndex(Instances.BEGIN));
      i = new Intent(getActivity(), ExpenseEdit.class);
      i.putExtra("template_id", mAdapter.getGroupId(group));
      i.putExtra("instance_id", menuInfo.id);
      i.putExtra("instance_date", date);
      startActivityForResult(i,0);
      break;
    }
    //super is handling deactivation of mActionMode
    return super.dispatchCommandSingle(command, info);
  }
  @Override
  public boolean dispatchCommandMultiple(int command,
      SparseBooleanArray positions,Long[]itemIds) {
    int checkedItemCount = positions.size();
    ArrayList<Long[]> extra2dAL = new ArrayList<Long[]>();
    ArrayList<Long> objectIdsAL = new ArrayList<Long>();
    switch(command) {
    case R.id.DELETE_COMMAND:
      MessageDialogFragment.newInstance(
          R.string.dialog_title_warning_delete_plan,
          getResources().getQuantityString(R.plurals.warning_delete_plan,itemIds.length,itemIds.length),
          new MessageDialogFragment.Button(
              R.string.menu_delete,
              R.id.DELETE_COMMAND_DO,
              itemIds),
          null,
          new MessageDialogFragment.Button(android.R.string.no,R.id.CANCEL_CALLBACK_COMMAND,null))
        .show(getActivity().getSupportFragmentManager(),"DELETE_TEMPLATE");
      return true;
    case R.id.CREATE_INSTANCE_SAVE_COMMAND:
      for (int i=0; i<positions.size(); i++) {
        if (positions.valueAt(i)) {
          int position = positions.keyAt(i);
          long pos = mListView.getExpandableListPosition(position);
          int group = ExpandableListView.getPackedPositionGroup(pos),
              child = ExpandableListView.getPackedPositionChild(pos);
          Cursor c = mAdapter.getChild(group,child);
          long itemId = c.getLong(c.getColumnIndex(KEY_ROWID));
          //ignore instances that are not open
          if (mInstance2TransactionMap.get(itemId)!=null)
            continue;
          long date = c.getLong(c.getColumnIndex(Instances.BEGIN));
          //pass event instance id and date as extra
          extra2dAL.add(new Long[]{itemId,date});
          objectIdsAL.add(mAdapter.getGroupId(group));
        }
      }
      getActivity().getSupportFragmentManager().beginTransaction()
      .add(TaskExecutionFragment.newInstance(
          TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE,
          objectIdsAL.toArray(new Long[objectIdsAL.size()]),
          extra2dAL.toArray(new Long[extra2dAL.size()][2])),
        "ASYNC_TASK")
      .commit();
    break;
    case R.id.CANCEL_PLAN_INSTANCE_COMMAND:
      for (int i=0; i<positions.size(); i++) {
        if (positions.valueAt(i)) {
          int position = positions.keyAt(i);
          long pos = mListView.getExpandableListPosition(position);
          int group = ExpandableListView.getPackedPositionGroup(pos),
              child = ExpandableListView.getPackedPositionChild(pos);
          //pass templateId and transactionId in extra
          long itemId = mAdapter.getChildId(group, child);
          objectIdsAL.add(itemId);
          extra2dAL.add(new Long[]{mAdapter.getGroupId(group),mInstance2TransactionMap.get(itemId)});
        }
      }
      getActivity().getSupportFragmentManager().beginTransaction()
        .add(TaskExecutionFragment.newInstance(
            TaskExecutionFragment.TASK_CANCEL_PLAN_INSTANCE,
            objectIdsAL.toArray(new Long[objectIdsAL.size()]),
            extra2dAL.toArray(new Long[extra2dAL.size()][2])),
          "ASYNC_TASK")
        .commit();
      break;
    case R.id.RESET_PLAN_INSTANCE_COMMAND:
      Long[] extra = new Long[checkedItemCount];
      for (int i=0; i<itemIds.length; i++) {
        //pass transactionId in extra
        extra[i] = mInstance2TransactionMap.get(itemIds[i]);
        mInstance2TransactionMap.remove(itemIds[i]);
      }
      getActivity().getSupportFragmentManager().beginTransaction()
      .add(TaskExecutionFragment.newInstance(
          TaskExecutionFragment.TASK_RESET_PLAN_INSTANCE,
          itemIds,
          extra),
        "ASYNC_TASK")
      .commit();
      break;
    }
    return super.dispatchCommandMultiple(command, positions, itemIds);
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
            new String[]{String.valueOf(bundle.getLong("template_id"))},
            null);
      }
    }
  }
  @SuppressLint("NewApi")
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
    if (getActivity()==null)
      return;
    int id = loader.getId();
    switch (id) {
    case TEMPLATES_CURSOR:
      long expandedId = ((ManageTemplates) getActivity()).calledFromCalendarWithId;
      mExpandedPosition = -1;
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
      int planCount = mTemplatesCursor.getCount();
      newPlanEnabled = planCount < 3;
      if (planCount>0) {
        mTemplatesCursor.moveToFirst();
        ArrayList<Long> plans = new ArrayList<Long>();
        long templateId,planId;
        Bundle planBundle = new Bundle();
        while (mTemplatesCursor.isAfterLast() == false) {
          templateId = mTemplatesCursor.getLong(columnIndexRowId);
          if (expandedId == templateId) {
            mExpandedPosition = mTemplatesCursor.getPosition();
          }
          Bundle instanceBundle = new Bundle();
          instanceBundle.putLong("template_id", templateId);
          //loader for instance2transactionmap
          int loaderId = mTemplatesCursor.getPosition()*2+1;
          if (mManager.getLoader(loaderId) != null && !mManager.getLoader(loaderId).isReset()) {
            mManager.restartLoader(loaderId, instanceBundle, this);
          } else {
            mManager.initLoader(loaderId, instanceBundle, this);
          }
          if ((planId = mTemplatesCursor.getLong(columnIndexPlanId)) != 0L) {
            plans.add(planId);
          }
          mTemplatesCursor.moveToNext();
        }
        if (expandedId != 0 && mExpandedPosition == -1) {
          Toast.makeText(getActivity(), R.string.save_transaction_template_deleted, Toast.LENGTH_LONG).show();
        }
        planBundle.putSerializable("plans", plans);
        if (mManager.getLoader(PLANS_CURSOR) != null && !mManager.getLoader(PLANS_CURSOR).isReset()) {
          mManager.restartLoader(PLANS_CURSOR, planBundle, this);
        } else {
          mManager.initLoader(PLANS_CURSOR, planBundle, this);
        }
      } else {
        mPlanTimeInfo = new HashMap<Long, String>();
        mAdapter.setGroupCursor(mTemplatesCursor);
      }
      invalidateCAB();
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
      if (mExpandedPosition != -1) {
        mListView.expandGroup(mExpandedPosition);
      }
      break;
    default:
      int groupPosition = id / 2;
      if (id % 2 == 0) {
        //check if group still exists
        if (mAdapter.getGroupId(groupPosition) != 0) {
            mAdapter.setChildrenCursor(groupPosition, c);
            if (mExpandedPosition != -1) {
              mListView.setSelectionFromTop(mExpandedPosition,0);
            }
        }
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
      Long planId = c.getLong(columnIndexPlanId);
      String planInfo = mPlanTimeInfo.get(planId);
      if (planInfo == null) {
        planInfo = getString(R.string.plan_event_deleted);
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
  public void refresh() {
   mAdapter.notifyDataSetChanged();
  }
  public void listFocus() {
    mListView.requestFocus();
  }
  @Override
  public void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    refresh();
  }
  private void configureMenuInternal(Menu menu, int count,boolean withOpen,boolean withApplied,boolean withCancelled) {
    //Long transactionId = mInstance2TransactionMap.get(id);
    //state open
    menu.findItem(R.id.CREATE_INSTANCE_SAVE_COMMAND).setVisible(withOpen);
    menu.findItem(R.id.CREATE_INSTANCE_EDIT_COMMAND).setVisible(count==1 && withOpen);
    //state open or applied
    menu.findItem(R.id.CANCEL_PLAN_INSTANCE_COMMAND).setVisible(withOpen || withApplied);
    //state cancelled or applied
    menu.findItem(R.id.RESET_PLAN_INSTANCE_COMMAND).setVisible(withApplied || withCancelled);
    //state applied
    menu.findItem(R.id.EDIT_PLAN_INSTANCE_COMMAND).setVisible(count==1 && withApplied);
  }
  private TransactionState getState(Long id) {
    Long transactionId = mInstance2TransactionMap.get(id);
    if (transactionId==null) {
      return TransactionState.OPEN;
    } else if (transactionId != 0L) {
      return TransactionState.APPLIED;
    } else {
      return TransactionState.CANCELLED;
    }
  }
  @Override
  protected void configureMenuLegacy(Menu menu, ContextMenuInfo menuInfo) {
    super.configureMenuLegacy(menu, menuInfo);
    ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
    int type = ExpandableListView.getPackedPositionType(info.packedPosition);
    if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
      boolean withOpen=false,withApplied=false,withCancelled=false;
      switch(getState(info.id)) {
      case APPLIED:
        withApplied=true;
        break;
      case CANCELLED:
        withCancelled=true;
        break;
      case OPEN:
        withOpen=true;
        break;
      }
      configureMenuInternal(menu,1,withOpen,withApplied,withCancelled);
    }
  }
  @Override
  protected void configureMenu11(Menu menu, int count) {
    super.configureMenu11(menu, count);
    if (expandableListSelectionType == ExpandableListView.PACKED_POSITION_TYPE_CHILD && mAdapter != null) {
      //find out the checked ids and check their states
      boolean withOpen=false,withApplied=false,withCancelled=false;
      SparseBooleanArray checkedItemPositions = mListView.getCheckedItemPositions();
      for (int i=0; i<checkedItemPositions.size(); i++) {
        if (checkedItemPositions.valueAt(i)) {
          int position = checkedItemPositions.keyAt(i);
          long id;
          long pos = mListView.getExpandableListPosition(position);
          int groupPos = ExpandableListView.getPackedPositionGroup(pos);
          if (ExpandableListView.getPackedPositionType(pos) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            id = mAdapter.getGroupId(groupPos);
          } else {
            int childPos = ExpandableListView.getPackedPositionChild(pos);
            id = mAdapter.getChildId(groupPos,childPos);
          }
          switch(getState(id)) {
          case APPLIED:
            withApplied=true;
            break;
          case CANCELLED:
            withCancelled=true;
            break;
          case OPEN:
            withOpen=true;
            break;
          }
          configureMenuInternal(menu,count,withOpen,withApplied,withCancelled);
        }
      }
    }
  }
}
