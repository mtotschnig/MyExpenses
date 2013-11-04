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
import org.totschnig.myexpenses.activity.ManageCategories.HelpVariant;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;

import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import org.totschnig.myexpenses.ui.SimpleCursorTreeAdapter;
import org.totschnig.myexpenses.util.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class CategoryList extends BudgetListFragment implements
    OnChildClickListener, OnGroupClickListener,LoaderManager.LoaderCallbacks<Cursor> {
  private static final int CATEGORY_CURSOR = -1;
  private static final int SUM_CURSOR = -2;
  private static final int DATEINFO_CURSOR = -3;
  /**
   * create a new sub category
   */
  private static final int CREATE_SUB_CAT = Menu.FIRST+2;
  /**
   * return the main cat to the calling activity
   */
  private static final int SELECT_MAIN_CAT = Menu.FIRST+1;
  /**
   * edit the category label
   */
  private static final int EDIT_CAT = Menu.FIRST+3;
  /**
   * delete the category after checking if
   * there are mapped transactions or subcategories
   */
  private static final int DELETE_CAT = Menu.FIRST+4;

  private MyExpandableListAdapter mAdapter;
  private LoaderManager mManager;
  long mAccountId;
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
    setColors();
    final ManageCategories ctx = (ManageCategories) getActivity();
    int viewResource;
    Bundle extras = ctx.getIntent().getExtras();
    mManager = getLoaderManager();
    if (extras != null) {
      viewResource = R.layout.distribution_list;
      mAccountId = extras.getLong(KEY_ACCOUNTID);
      mAccount = Account.getInstanceFromDb(mAccountId);
      Bundle b = savedInstanceState != null ? savedInstanceState : extras;
      mGrouping = (Grouping) b.getSerializable("grouping");
      mGroupingYear = b.getInt("groupingYear");
      mGroupingSecond = b.getInt("groupingSecond");
      //emptyView.findViewById(R.id.importButton).setVisibility(View.GONE);
      //((TextView) emptyView.findViewById(R.id.noCategories)).setText(R.string.no_mapped_transactions);
      getSherlockActivity().supportInvalidateOptionsMenu();
      mManager.initLoader(SUM_CURSOR, null, this);
      mManager.initLoader(DATEINFO_CURSOR, null, this);
    } else {
      viewResource = R.layout.categories_list;
      mAccountId = 0L;
    }
    View v = inflater.inflate(viewResource, null, false);
    incomeSumTv = (TextView) v.findViewById(R.id.sum_income);
    expenseSumTv = (TextView) v.findViewById(R.id.sum_expense);
    bottomLine = v.findViewById(R.id.BottomLine);
    updateColor();
    ExpandableListView lv = (ExpandableListView) v.findViewById(R.id.list);
    lv.setEmptyView(v.findViewById(R.id.empty));
    mManager.initLoader(CATEGORY_CURSOR, null, this);
    String[] from;
    int[] to;
    if (mAccountId != 0) {
      from = new String[] {"label","sum"};
      to = new int[] {R.id.label,R.id.amount};
    } else {
      from = new String[] {"label"};
      to = new int[] {R.id.label};
    }
    mAdapter = new MyExpandableListAdapter(ctx,
        null,
        R.layout.category_row,R.layout.category_row,
        from,to,from,to);
    lv.setAdapter(mAdapter);
    //requires using activity (SelectCategory) to implement OnChildClickListener
    if (ctx.helpVariant.equals(ManageCategories.HelpVariant.select)) {
      lv.setOnChildClickListener(this);
      lv.setOnGroupClickListener(this);
    }
    registerForContextMenu(lv);
    return v;
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    ManageCategories ctx = (ManageCategories) getSherlockActivity();
    ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
    int type = ExpandableListView.getPackedPositionType(info.packedPosition);

    menu.add(0,EDIT_CAT,0,R.string.menu_edit_cat);
    if (ctx.helpVariant.equals(HelpVariant.distribution))
      return;
    // Menu entries relevant only for the group
    if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
      if (ctx.helpVariant.equals(HelpVariant.select))
        menu.add(0,SELECT_MAIN_CAT,0,R.string.select_parent_category);
      menu.add(0,CREATE_SUB_CAT,0,R.string.menu_create_sub_cat);
    }
    menu.add(0,DELETE_CAT,0,R.string.menu_delete);
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    ManageCategories ctx = (ManageCategories) getSherlockActivity();
    ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
    int type = ExpandableListView.getPackedPositionType(info.packedPosition);
    long cat_id = info.id;

    String label =   ((TextView) info.targetView.findViewById(R.id.label)).getText().toString();

    switch(item.getItemId()) {
      case SELECT_MAIN_CAT:
        Intent intent=new Intent();
        intent.putExtra("cat_id", cat_id);
        intent.putExtra("label", label);
        ctx.setResult(ManageCategories.RESULT_OK,intent);
        ctx.finish();
        return true;
      case CREATE_SUB_CAT:
        ctx.createCat(cat_id);
        return true;
      case EDIT_CAT:
        ctx.editCat(label,cat_id);
        return true;
      case DELETE_CAT:
        Cursor c;
        int message = 0;
        int group = ExpandableListView.getPackedPositionGroup(info.packedPosition),
            child = ExpandableListView.getPackedPositionChild(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
          c = (Cursor) mAdapter.getChild(group,child);
        } else  {
          c = mGroupCursor;
          if (c.getInt(c.getColumnIndex("child_count")) > 0)
            message = R.string.not_deletable_subcats_exists;
        }
        if (message == 0 ) {
          if (c.getInt(c.getColumnIndex("mapped_transactions")) > 0)
            message = R.string.not_deletable_mapped_transactions;
          else if (c.getInt(c.getColumnIndex("mapped_templates")) > 0)
            message = R.string.not_deletable_mapped_templates;
        }
        if (message != 0 )
          Toast.makeText(ctx,getString(message), Toast.LENGTH_LONG).show();
        else
          ctx.getSupportFragmentManager().beginTransaction()
          .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_DELETE_CATEGORY,info.id, null), "ASYNC_TASK")
          .commit();
    }
    return false;
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
    @Override
    public void setViewText(TextView v, String text) {
      switch (v.getId()) {
      case R.id.amount:
        if (Long.valueOf(text) > 0)
          v.setTextColor(colorIncome);
        else
          v.setTextColor(colorExpense);
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
      return new CursorLoader(getSherlockActivity(),
          TransactionProvider.TRANSACTIONS_URI.buildUpon().appendPath("sumsForAccountsGroupedByType").appendPath(String.valueOf(mAccountId)).build(),
          null, buildGroupingClause(),
          null, null);
    }
    if (id == DATEINFO_CURSOR) {
      ArrayList<String> projection = new ArrayList<String>(Arrays.asList(
          new String[] { THIS_YEAR + " AS this_year",THIS_YEAR_OF_WEEK_START + " AS this_year_of_week_start",
              THIS_MONTH + " AS this_month",THIS_WEEK + " AS this_week",THIS_DAY + " AS this_day"}));
      //if we are at the beginning of the year we are interested in the max of the previous year
      int yearToLookUp = mGroupingSecond ==1 ? mGroupingYear -1 : mGroupingYear;
      switch (mGrouping) {
      case DAY:
        projection.add(String.format(Locale.US,"strftime('%%j','%d-12-31') AS max_value",yearToLookUp));
        break;
      case WEEK:
        projection.add(String.format(Locale.US,"strftime('%%W','%d-12-31') AS max_value",yearToLookUp));
        break;
      case MONTH:
        projection.add("12 as max_value");
        break;
      default:
        projection.add("0 as max_value");
      }
      if (mGrouping.equals(Grouping.WEEK)) {
        //we want to find out the week range when we are given a week number
        //we find out the first Monday in the year, which is the beginning of week 1 and than
        //add (weekNumber-1)*7 days to get at the beginning of the week
        String weekStart = String.format(Locale.US, "'%d-01-01','weekday 1','+%d day'",mGroupingYear,(mGroupingSecond-1)*7);
        String weekEnd = String.format(Locale.US, "'%d-01-01','weekday 1','+%d day'",mGroupingYear,mGroupingSecond*7-1);
        projection.add(String.format(Locale.US,"strftime('%%m/%%d', date(%s)) || '-' || strftime('%%m/%%d', date(%s)) AS week_range",weekStart,weekEnd));
      }
      return new CursorLoader(getSherlockActivity(),
          TransactionProvider.TRANSACTIONS_URI,
          projection.toArray(new String[projection.size()]),
          null,null, null);
    }
    //CATEGORY_CURSOR
    long parentId;
    String selection = "",strAccountId="",sortOrder=null;
    String[] selectionArgs,projection = null;
    if (mAccountId != 0) {
      strAccountId = String.valueOf(mAccountId);
      String catFilter = "FROM transactions WHERE account_id = ?";
      if (!mGrouping.equals(Grouping.NONE)) {
        catFilter += " AND " +buildGroupingClause();
      }
      //we need to include transactions mapped to children for main categories
      if (bundle == null)
        catFilter += " AND cat_id IN (select _id FROM categories subtree where parent_id = categories._id OR _id = categories._id)";
      else
        catFilter += " AND cat_id  = categories._id";
      selection = " AND exists (select 1 " + catFilter +")";
      projection = new String[] {
          KEY_ROWID,
          KEY_LABEL,
          KEY_PARENTID,
          "(SELECT sum(amount) " + catFilter + ") AS sum"
      };
      sortOrder="abs(sum) DESC";
    } else {
      projection = new String[] {
          KEY_ROWID,
          KEY_LABEL,
          KEY_PARENTID,
          "(select count(*) FROM categories subtree where parent_id = categories._id) as child_count",
          "(select count(*) FROM " + TABLE_TRANSACTIONS + " WHERE " + KEY_CATID + "=" + TABLE_CATEGORIES + "." + KEY_ROWID + ") AS mapped_transactions",
          "(select count(*) FROM " + TABLE_TEMPLATES    + " WHERE " + KEY_CATID + "=" + TABLE_CATEGORIES + "." + KEY_ROWID + ") AS mapped_templates"
      };
    }
    if (bundle == null) {
      selection = "parent_id is null" + selection;
      selectionArgs = mAccountId != 0 ? new String[]{strAccountId,strAccountId} : null;
    } else {
      parentId = bundle.getLong("parent_id");
      selection = "parent_id = ?"  + selection;
      selectionArgs = mAccountId != 0 ?
          new String[]{strAccountId,String.valueOf(parentId),strAccountId} :
          new String[]{String.valueOf(parentId)};
    }
    return new CursorLoader(getActivity(),TransactionProvider.CATEGORIES_URI, projection,
        selection,selectionArgs, sortOrder);
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
    int id = loader.getId();
    SherlockFragmentActivity ctx = getSherlockActivity();
    ActionBar actionBar =  ctx.getSupportActionBar();
    switch(id) {
    case SUM_CURSOR:
      boolean[] seen = new boolean[2];
      c.moveToFirst();
      while (c.isAfterLast() == false) {
        int type = c.getInt(c.getColumnIndex("type"));
        updateSum(type > 0 ? "+ " : "- ",
            type > 0 ? incomeSumTv : expenseSumTv,
            c.getLong(c.getColumnIndex("sum")));
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
      thisYear = c.getInt(c.getColumnIndex("this_year"));
      thisMonth = c.getInt(c.getColumnIndex("this_month"));
      thisWeek = c.getInt(c.getColumnIndex("this_week"));
      thisDay = c.getInt(c.getColumnIndex("this_day"));
      maxValue = c.getInt(c.getColumnIndex("max_value"));
      Log.i("DEBUG",String.valueOf(maxValue));
      break;
    case CATEGORY_CURSOR:
      mGroupCursor=c;
      mAdapter.setGroupCursor(c);
      if (mAccountId != 0) {
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
    if (id != -1) {
        // child cursor
        try {
            mAdapter.setChildrenCursor(id, null);
        } catch (NullPointerException e) {
            Log.w("TAG", "Adapter expired, try again on the next query: "
                    + e.getMessage());
        }
    } else {
      mGroupCursor = null;
      mAdapter.setGroupCursor(null);
    }
  }  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    if (mGrouping != null) {
      menu.findItem(R.id.FORWARD_COMMAND).setVisible(!mGrouping.equals(Grouping.NONE));
      menu.findItem(R.id.BACK_COMMAND).setVisible(!mGrouping.equals(Grouping.NONE));
    }
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
    ManageCategories ctx = (ManageCategories) getSherlockActivity();
    Intent intent=new Intent();
    long sub_cat = id;
    String label =  ((TextView) v.findViewById(R.id.label)).getText().toString();
    intent.putExtra("cat_id",sub_cat);
    intent.putExtra("label", label);
    ctx.setResult(ManageCategories.RESULT_OK,intent);
    ctx.finish();
    return true;
  }
  @Override
  public boolean onGroupClick(ExpandableListView parent, View v,
      int groupPosition, long id) {
    ManageCategories ctx = (ManageCategories) getSherlockActivity();
    long cat_id = id;
    mGroupCursor.moveToPosition(groupPosition);
    if (mGroupCursor.getInt(mGroupCursor.getColumnIndex("child_count")) > 0)
      return false;
    String label =   ((TextView) v.findViewById(R.id.label)).getText().toString();
    Intent intent=new Intent();
    intent.putExtra("cat_id",cat_id);
    intent.putExtra("label", label);
    ctx.setResult(ManageCategories.RESULT_OK,intent);
    ctx.finish();
    return true;
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
    getSherlockActivity().supportInvalidateOptionsMenu();
    reset();
  }

  private void reset() {
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
}
