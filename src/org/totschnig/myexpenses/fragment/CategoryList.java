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
import java.util.Arrays;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.activity.ManageCategories.HelpVariant;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import org.totschnig.myexpenses.ui.SimpleCursorTreeAdapter;
import org.totschnig.myexpenses.util.Utils;

public class CategoryList extends ContextualActionBarFragment implements
    OnChildClickListener, OnGroupClickListener,LoaderManager.LoaderCallbacks<Cursor> {

  private static final String KEY_CHILD_COUNT = "child_count";
  private static final String TABS = "\u0009\u0009\u0009\u0009";

  protected int getMenuResource() {
    return R.menu.categorylist_context;
  }

  private static final int CATEGORY_CURSOR = -1;
  private static final int SUM_CURSOR = -2;
  private static final int DATEINFO_CURSOR = -3;

  private MyExpandableListAdapter mAdapter;
  private ExpandableListView mListView;
  private LoaderManager mManager;
  private TextView incomeSumTv,expenseSumTv;
  private View bottomLine;
  public Grouping mGrouping;
  int mGroupingYear;
  int mGroupingSecond;
  int thisYear,thisMonth,thisWeek,thisDay,maxValue;

  private Account mAccount;
  private Cursor mGroupCursor;

  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final ManageCategories ctx = (ManageCategories) getActivity();
    int viewResource;
    Bundle extras = ctx.getIntent().getExtras();
    mManager = getLoaderManager();
    if (ctx.helpVariant.equals(ManageCategories.HelpVariant.distribution)) {
      viewResource = R.layout.distribution_list;
      mAccount = Account.getInstanceFromDb(extras.getLong(KEY_ACCOUNTID));
      if (mAccount == null) {
        TextView tv = new TextView(ctx);
        tv.setText("Error loading distribution for account "+extras.getLong(KEY_ACCOUNTID));
        return  tv;
      }
      Bundle b = savedInstanceState != null ? savedInstanceState : extras;
      mGrouping = (Grouping) b.getSerializable("grouping");
      mGroupingYear = b.getInt("groupingYear");
      mGroupingSecond = b.getInt("groupingSecond");
      //emptyView.findViewById(R.id.importButton).setVisibility(View.GONE);
      //((TextView) emptyView.findViewById(R.id.noCategories)).setText(R.string.no_mapped_transactions);
      getActivity().supportInvalidateOptionsMenu();
      mManager.initLoader(SUM_CURSOR, null, this);
      mManager.initLoader(DATEINFO_CURSOR, null, this);
    } else {
      viewResource = R.layout.categories_list;
    }
    View v = inflater.inflate(viewResource, null, false);
    incomeSumTv = (TextView) v.findViewById(R.id.sum_income);
    expenseSumTv = (TextView) v.findViewById(R.id.sum_expense);
    bottomLine = v.findViewById(R.id.BottomLine);
    updateColor();
    mListView = (ExpandableListView) v.findViewById(R.id.list);
    mListView.setEmptyView(v.findViewById(R.id.empty));
    mManager.initLoader(CATEGORY_CURSOR, null, this);
    String[] from;
    int[] to;
    if (mAccount != null) {
      from = new String[] {KEY_LABEL,KEY_SUM};
      to = new int[] {R.id.label,R.id.amount};
    } else {
      from = new String[] {KEY_LABEL};
      to = new int[] {R.id.label};
    }
    mAdapter = new MyExpandableListAdapter(ctx,
        null,
        R.layout.category_row,R.layout.category_row,
        from,to,from,to);
    mListView.setAdapter(mAdapter);
    registerForContextualActionBar(mListView);
    return v;
  }

  @Override
  public boolean dispatchCommandMultiple(int command,
      SparseBooleanArray positions,Long[]itemIds) {
    ManageCategories ctx = (ManageCategories) getActivity();
    switch(command) {
    case R.id.DELETE_COMMAND:
      int mappedTransactionsCount = 0, mappedTemplatesCount = 0, hasChildrenCount = 0;
      ArrayList<Long> idList = new ArrayList<Long>();
      for (int i=0; i<positions.size(); i++) {
        Cursor c;
        if (positions.valueAt(i)) {
          boolean deletable = true;
          int position = positions.keyAt(i);
          long pos = mListView.getExpandableListPosition(position);
          int type = ExpandableListView.getPackedPositionType(pos);
          int group = ExpandableListView.getPackedPositionGroup(pos),
              child = ExpandableListView.getPackedPositionChild(pos);
          if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            c = (Cursor) mAdapter.getChild(group,child);
            c.moveToPosition(child);
          } else  {
            c = mGroupCursor;
            c.moveToPosition(group);
          }
          long itemId = c.getLong(c.getColumnIndex(KEY_ROWID));
          Bundle extras = ctx.getIntent().getExtras();
          if ((extras != null && extras.getLong(KEY_ROWID) == itemId) || c.getInt(c.getColumnIndex(DatabaseConstants.KEY_MAPPED_TRANSACTIONS)) > 0) {
            mappedTransactionsCount++;
            deletable = false;
          } else if (c.getInt(c.getColumnIndex(DatabaseConstants.KEY_MAPPED_TEMPLATES)) > 0) {
            mappedTemplatesCount++;
            deletable = false;
          }
          if (deletable) {
            if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP && c.getInt(c.getColumnIndex(KEY_CHILD_COUNT)) > 0) {
              hasChildrenCount++;
            }
            idList.add(itemId);
          }
        }
      }
      if (idList.size()>0) {
        Long[] objectIds = idList.toArray(new Long[idList.size()]);
        if (hasChildrenCount>0) {
          MessageDialogFragment.newInstance(
              R.string.dialog_title_warning_delete_main_category,
              getResources().getQuantityString(R.plurals.warning_delete_main_category,hasChildrenCount,hasChildrenCount),
              new MessageDialogFragment.Button(android.R.string.yes, R.id.DELETE_COMMAND_DO, objectIds),
              null,
              new MessageDialogFragment.Button(android.R.string.no,R.id.CANCEL_CALLBACK_COMMAND,null))
            .show(ctx.getSupportFragmentManager(),"DELETE_CATEGORY");
        } else {
          ctx.dispatchCommand(R.id.DELETE_COMMAND_DO, objectIds);
        }
      }
      if (mappedTransactionsCount > 0 || mappedTemplatesCount > 0 ) {
        String message = "";
        if (mappedTransactionsCount > 0)
          message += getResources().getQuantityString(
              R.plurals.not_deletable_mapped_transactions,
              mappedTransactionsCount,
              mappedTransactionsCount);
        if (mappedTemplatesCount > 0)
          message += getResources().getQuantityString(
              R.plurals.not_deletable_mapped_templates,
              mappedTemplatesCount,
              mappedTemplatesCount);
        Toast.makeText(getActivity(),message, Toast.LENGTH_LONG).show();
      }
      return true;
    }
    return false;
  }
  @Override
  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    ManageCategories ctx = (ManageCategories) getActivity();
    ExpandableListContextMenuInfo elcmi = (ExpandableListContextMenuInfo) info;
    int type = ExpandableListView.getPackedPositionType(elcmi.packedPosition);
    Cursor c;
    boolean isMain;
    int group = ExpandableListView.getPackedPositionGroup(elcmi.packedPosition),
        child = ExpandableListView.getPackedPositionChild(elcmi.packedPosition);
    if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
      c = (Cursor) mAdapter.getChild(group,child);
      isMain = false;
    } else  {
      c = mGroupCursor;
      isMain = true;
    }
    if (c==null||c.getCount()==0) {
      //observed on Blackberry Z10
      return false;
    }
    String label = c.getString(c.getColumnIndex(KEY_LABEL));
    switch(command) {
    case R.id.EDIT_COMMAND:
      ctx.editCat(label,elcmi.id);
      return true;
    case R.id.SELECT_COMMAND:
      if (ctx.helpVariant.equals(ManageCategories.HelpVariant.distribution)) {
        label += TABS + Utils.convAmount(c.getString(c.getColumnIndex(KEY_SUM)),mAccount.currency);
      }
      doSelection(elcmi.id,label,isMain);
      finishActionMode();
      return true;
    case R.id.CREATE_COMMAND:
      ctx.createCat(elcmi.id);
      return true;
    }
    return super.dispatchCommandSingle(command, info);
  }
  /**
   * Mapping the categories table into the ExpandableList
   * @author Michael Totschnig
   *
   */
  public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {
    private int colorExpense;
    private int colorIncome;
    public MyExpandableListAdapter(Context context, Cursor cursor, int groupLayout,
            int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom,
            int[] childrenTo) {
        super(context, cursor, groupLayout, groupFrom, groupTo, childLayout, childrenFrom,
                childrenTo);
        colorIncome = ((ProtectedFragmentActivity) context).getColorIncome();
        colorExpense = ((ProtectedFragmentActivity) context).getColorExpense();
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
    @Override
    public void setViewText(TextView v, String text) {
      switch (v.getId()) {
      case R.id.amount:
        v.setTextColor(Long.valueOf(text)<0?colorExpense:colorIncome);
        text = Utils.convAmount(text,mAccount.currency);
      }
      super.setViewText(v, text);
    }
  }
  private String buildGroupingClause() {
    String year = YEAR + " = " + mGroupingYear;
    switch(mGrouping) {
    case YEAR:
      return year;
    case DAY:
      return year + " AND " + DAY + " = " + mGroupingSecond;
    case WEEK:
      return YEAR_OF_WEEK_START + " = " + mGroupingYear + " AND " + WEEK + " = " + mGroupingSecond;
    case MONTH:
      return year + " AND " + MONTH + " = " + mGroupingSecond;
    default:
      return null;
    }
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    if (id == SUM_CURSOR) {
      Builder builder = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon();
      if (mAccount.getId() < 0) {
        builder.appendQueryParameter(KEY_CURRENCY, mAccount.currency.getCurrencyCode());
      } else {
        builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(mAccount.getId()));
      }
      return new CursorLoader(
          getActivity(),
          builder.build(),
          null,
          buildGroupingClause(),
          null,
          null);
    }
    if (id == DATEINFO_CURSOR) {
      ArrayList<String> projectionList = new ArrayList<String>(Arrays.asList(
          new String[] {
              THIS_YEAR_OF_WEEK_START + " AS " + KEY_THIS_YEAR_OF_WEEK_START,
              THIS_YEAR + " AS " + KEY_THIS_YEAR,
              THIS_MONTH + " AS " + KEY_THIS_MONTH,
              THIS_WEEK + " AS " + KEY_THIS_WEEK,
              THIS_DAY + " AS " + KEY_THIS_DAY
          }
      ));
      //if we are at the beginning of the year we are interested in the max of the previous year
      int yearToLookUp = mGroupingSecond ==1 ? mGroupingYear -1 : mGroupingYear;
      switch (mGrouping) {
      case DAY:
        projectionList.add(String.format(Locale.US,"strftime('%%j','%d-12-31') AS " + KEY_MAX_VALUE,yearToLookUp));
        break;
      case WEEK:
        projectionList.add(String.format(Locale.US,"strftime('%%W','%d-12-31') AS " + KEY_MAX_VALUE,yearToLookUp));
        break;
      case MONTH:
        projectionList.add("12 as " + KEY_MAX_VALUE);
        break;
      default:
        projectionList.add("0 as " + KEY_MAX_VALUE);
      }
      if (mGrouping.equals(Grouping.WEEK)) {
        //we want to find out the week range when we are given a week number
        //we find out the first Monday in the year, which is the beginning of week 1 and than
        //add (weekNumber-1)*7 days to get at the beginning of the week
        projectionList.add(DbUtils.weekStartFromGroupSqlExpression(mGroupingYear, mGroupingSecond));
        projectionList.add(DbUtils.weekEndFromGroupSqlExpression(mGroupingYear, mGroupingSecond));
      }
      return new CursorLoader(getActivity(),
          TransactionProvider.DUAL_URI,
          projectionList.toArray(new String[projectionList.size()]),
          null,null, null);
    }
    //CATEGORY_CURSOR
    long parentId;
    String selection = "",accountSelector="",sortOrder=null;
    String[] selectionArgs,projection = null;
    String CATTREE_WHERE_CLAUSE = KEY_CATID + " IN (SELECT " + KEY_ROWID + " FROM "
        + TABLE_CATEGORIES + " subtree WHERE " + KEY_PARENTID + " = " + TABLE_CATEGORIES
        + "." + KEY_ROWID + " OR " + KEY_ROWID + " = " + TABLE_CATEGORIES + "." + KEY_ROWID + ")";
    String CHILD_COUNT_SELECT = "(select count(*) FROM categories subtree where parent_id = categories._id) as "
        + KEY_CHILD_COUNT;
    if (mAccount != null) {
      if (mAccount.getId() < 0) {
        selection = " IN " +
            "(SELECT _id from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
        accountSelector = mAccount.currency.getCurrencyCode();
      } else {
        selection = " = ?";
        accountSelector = String.valueOf(mAccount.getId());
      }
      String catFilter = "FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_ACCOUNTID + selection;
      if (!mGrouping.equals(Grouping.NONE)) {
        catFilter += " AND " +buildGroupingClause();
      }
      //we need to include transactions mapped to children for main categories
      if (bundle == null) {
        catFilter += " AND " + CATTREE_WHERE_CLAUSE;
      }
      else {
        catFilter += " AND " + KEY_CATID + "  = " + TABLE_CATEGORIES + "." + KEY_ROWID;
      }
      selection = " AND exists (SELECT 1 " + catFilter +")";
      projection = new String[] {
          KEY_ROWID,
          KEY_LABEL,
          KEY_PARENTID,
          CHILD_COUNT_SELECT,
          "(SELECT sum(amount) " + catFilter + ") AS " + KEY_SUM
      };
      sortOrder="abs(" + KEY_SUM + ") DESC";
    } else {
      projection = new String[] {
          KEY_ROWID,
          KEY_LABEL,
          KEY_PARENTID,
          CHILD_COUNT_SELECT,
          "(select count(*) FROM " + TABLE_TRANSACTIONS + " WHERE " + CATTREE_WHERE_CLAUSE + ") AS " + DatabaseConstants.KEY_MAPPED_TRANSACTIONS,
          "(select count(*) FROM " + TABLE_TEMPLATES    + " WHERE " + CATTREE_WHERE_CLAUSE + ") AS " + DatabaseConstants.KEY_MAPPED_TEMPLATES
      };
    }
    if (bundle == null) {
      //group cursor
      selection = KEY_PARENTID + " is null" + selection;
      selectionArgs = mAccount != null ? new String[]{accountSelector,accountSelector} : null;
    } else {
      //child cursor
      parentId = bundle.getLong(KEY_PARENTID);
      selection = KEY_PARENTID + " = ?"  + selection;
      selectionArgs = mAccount != null ?
          new String[]{accountSelector,String.valueOf(parentId),accountSelector} :
          new String[]{String.valueOf(parentId)};
    }
    return new CursorLoader(getActivity(),TransactionProvider.CATEGORIES_URI, projection,
        selection,selectionArgs, sortOrder);
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
    if (getActivity()==null)
      return;
    int id = loader.getId();
    ActionBarActivity ctx = (ActionBarActivity) getActivity();
    ActionBar actionBar =  ctx.getSupportActionBar();
    switch(id) {
    case SUM_CURSOR:
      boolean[] seen = new boolean[2];
      c.moveToFirst();
      while (c.isAfterLast() == false) {
        int type = c.getInt(c.getColumnIndex(KEY_TYPE));
        updateSum(type > 0 ? "+ " : "- ",
            type > 0 ? incomeSumTv : expenseSumTv,
            c.getLong(c.getColumnIndex(KEY_SUM)));
        c.moveToNext();
        seen[type] = true;
      }
      //if we have no income or expense, there is no row in the cursor
      if (!seen[1]) updateSum("+ ",incomeSumTv,0);
      if (!seen[0]) updateSum("- ",expenseSumTv,0);
      break;
    case DATEINFO_CURSOR:
      c.moveToFirst();
      actionBar.setSubtitle(mGrouping.getDisplayTitle(ctx,
          mGroupingYear, mGroupingSecond,c));
      thisYear = c.getInt(c.getColumnIndex(KEY_THIS_YEAR));
      thisMonth = c.getInt(c.getColumnIndex(KEY_THIS_MONTH));
      thisWeek = c.getInt(c.getColumnIndex(KEY_THIS_WEEK));
      thisDay = c.getInt(c.getColumnIndex(KEY_THIS_DAY));
      maxValue = c.getInt(c.getColumnIndex(KEY_MAX_VALUE));
      break;
    case CATEGORY_CURSOR:
      mGroupCursor=c;
      mAdapter.setGroupCursor(c);
      if (mAccount != null) {
        actionBar.setTitle(mAccount.label);
      }
      break;
    default:
      //check if group still exists
      if (mAdapter.getGroupId(id) != 0)
          mAdapter.setChildrenCursor(id, c);
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    int id = loader.getId();
    if (id == CATEGORY_CURSOR) {
      mGroupCursor = null;
      mAdapter.setGroupCursor(null);
    } else if (id>0){
      // child cursor
      try {
          mAdapter.setChildrenCursor(id, null);
      } catch (NullPointerException e) {
          Log.w("TAG", "Adapter expired, try again on the next query: "
                  + e.getMessage());
      }
    }
  }
  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    if (mGrouping != null) {
      boolean grouped = !mGrouping.equals(Grouping.NONE);
      Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.FORWARD_COMMAND), grouped);
      Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.BACK_COMMAND), grouped);    }
  }
  public void back() {
    if (mGrouping.equals(Grouping.YEAR))
      mGroupingYear--;
    else {
      mGroupingSecond--;
      if (mGroupingSecond < 1) {
        mGroupingYear--;
        mGroupingSecond = maxValue;
      }
    }
    reset();
  }
  public void forward() {
    if (mGrouping.equals(Grouping.YEAR))
      mGroupingYear++;
    else{
      mGroupingSecond++;
      if (mGroupingSecond > maxValue) {
        mGroupingYear++;
        mGroupingSecond = 1;
      }
    }
    reset();
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.BACK_COMMAND:
      back();
      return true;
    case R.id.FORWARD_COMMAND:
      forward();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
  /*     (non-Javadoc)
   * return the sub cat to the calling activity
   * @see android.app.ExpandableListActivity#onChildClick(android.widget.ExpandableListView, android.view.View, int, int, long)
*/
  @Override
  public boolean onChildClick (ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
    if (super.onChildClick(parent, v, groupPosition,childPosition, id))
      return true;
    ManageCategories ctx = (ManageCategories) getActivity();
    if (ctx==null || ctx.helpVariant.equals(ManageCategories.HelpVariant.manage)) {
      return false;
    }
    String label =  ((TextView) v.findViewById(R.id.label)).getText().toString();
    if (ctx.helpVariant.equals(ManageCategories.HelpVariant.distribution)) {
      label += TABS + ((TextView) v.findViewById(R.id.amount)).getText().toString();
    }
    doSelection(id,label,false);
    return true;
  }
  @Override
  public boolean onGroupClick(ExpandableListView parent, View v,
      int groupPosition, long id) {
    if (super.onGroupClick(parent, v, groupPosition, id))
      return true;
    ManageCategories ctx = (ManageCategories) getActivity();
    if (ctx==null || ctx.helpVariant.equals(ManageCategories.HelpVariant.manage)) {
      return false;
    }
    long cat_id = id;
    mGroupCursor.moveToPosition(groupPosition);
    if (mGroupCursor.getInt(mGroupCursor.getColumnIndex(KEY_CHILD_COUNT)) > 0)
      return false;
    String label =   ((TextView) v.findViewById(R.id.label)).getText().toString();
    if (ctx.helpVariant.equals(ManageCategories.HelpVariant.distribution)) {
      label += TABS + ((TextView) v.findViewById(R.id.amount)).getText().toString();
    }
    doSelection(cat_id,label,true);
    return true;
  }
  private void doSelection(long cat_id,String label,boolean isMain) {
    ManageCategories ctx = (ManageCategories) getActivity();
    if (ctx.helpVariant.equals(ManageCategories.HelpVariant.distribution)) {
      TransactionListDialogFragment.newInstance(
          mAccount.getId(), cat_id, isMain, mGrouping,buildGroupingClause(),label)
          .show(getFragmentManager(), TransactionListDialogFragment.class.getName());
      return;
    }
    Intent intent=new Intent();
    intent.putExtra(KEY_CATID,cat_id);
    intent.putExtra(KEY_LABEL, label);
    ctx.setResult(ManageCategories.RESULT_OK,intent);
    ctx.finish();
  }
  public void setGrouping(Grouping grouping) {
    mGrouping = grouping;
    mGroupingYear = thisYear;
    switch(grouping) {
    case NONE:
      mGroupingYear = 0;
      break;
    case DAY:
      mGroupingSecond = thisDay;
      break;
    case WEEK:
      mGroupingSecond = thisWeek;
      break;
    case MONTH:
      mGroupingSecond = thisMonth;
      break;
    case YEAR:
      mGroupingSecond = 0;
      break;
    }
    getActivity().supportInvalidateOptionsMenu();
    reset();
  }

  private void reset() {
    int count =  mAdapter.getGroupCount();
    for (int i = 0; i <count ; i++) {
//TODO: would be nice to retrieve the same open groups on the next or previous group
//the following does not work since the groups will not necessarily stay the same
//      if (mListView.isGroupExpanded(i)) {
//        mGroupCursor.moveToPosition(i);
//        long parentId = mGroupCursor.getLong(mGroupCursor.getColumnIndexOrThrow(KEY_ROWID));
//        Bundle bundle = new Bundle();
//        bundle.putLong("parent_id", parentId);
//        mManager.restartLoader(i, bundle, CategoryList.this);
//      }
      mListView.collapseGroup(i);
    }
    mManager.restartLoader(CATEGORY_CURSOR, null, this);
    mManager.restartLoader(SUM_CURSOR, null, this);
    mManager.restartLoader(DATEINFO_CURSOR, null, this);
  }
  private void updateSum(String prefix, TextView tv,long amount) {
    if (tv != null)
      tv.setText(prefix + Utils.formatCurrency(
          new Money(mAccount.currency,amount)));
  }
  private void updateColor() {
    if (bottomLine != null)
      bottomLine.setBackgroundColor(mAccount.color);
  }
  @Override
  public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putSerializable("grouping", mGrouping);
      outState.putInt("groupingYear",mGroupingYear);
      outState.putInt("groupingSecond",mGroupingSecond);
  }
  @Override
  protected void configureMenu(Menu menu, int count) {
    ManageCategories ctx = (ManageCategories) getActivity();
    if (ctx == null) {
      return;
    }
    boolean inGroup = expandableListSelectionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP;
    boolean inFilterOrDistribution = ctx.helpVariant.equals(HelpVariant.select_filter) ||
        ctx.helpVariant.equals(HelpVariant.distribution);
    menu.findItem(R.id.EDIT_COMMAND).setVisible(count==1 && !inFilterOrDistribution);
    menu.findItem(R.id.DELETE_COMMAND).setVisible(!inFilterOrDistribution);
    menu.findItem(R.id.SELECT_COMMAND).setVisible(count==1 && !ctx.helpVariant.equals(HelpVariant.manage));
    menu.findItem(R.id.CREATE_COMMAND).setVisible(inGroup && count==1 && !inFilterOrDistribution);
  }
}
