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
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Account.Grouping;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import org.totschnig.myexpenses.ui.SimpleCursorTreeAdapter;
import org.totschnig.myexpenses.util.Utils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class CategoryList extends BudgetListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final int CATEGORY_CURSOR = -1;
  private static final int SUM_CURSOR = -2;
  private static final int DATEINFO_CURSOR = -3;
  private MyExpandableListAdapter mAdapter;
  int mGroupIdColumnIndex;
  private LoaderManager mManager;
  long mAccountId;
  private TextView incomeSumTv,expenseSumTv;
  private View bottomLine;
  public Grouping mGrouping;
  int groupingYear;
  int groupingSecond;
  int thisYear,thisMonth,thisWeek,thisDay,maxValue;

  private Account mAccount;
  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setColors();
    ManageCategories ctx = (ManageCategories) getActivity();
    int viewResource;
    Bundle extras = ctx.getIntent().getExtras();
    mManager = getLoaderManager();
    if (extras != null) {
      viewResource = R.layout.distribution_list;
      mAccountId = extras.getLong(KEY_ACCOUNTID);
      mAccount = Account.getInstanceFromDb(mAccountId);
      mGrouping = (Grouping) extras.getSerializable("grouping");
      groupingYear = extras.getInt("groupingYear");
      groupingSecond = extras.getInt("groupingSecond");
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
      lv.setOnChildClickListener((OnChildClickListener) ctx);
      lv.setOnGroupClickListener((OnGroupClickListener) ctx);
    }
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
    String year = YEAR + " = " + groupingYear;
    switch(mGrouping) {
    case YEAR:
      return year;
    case DAY:
      return year + " AND " + DAY + " = " + groupingSecond;
    case WEEK:
      return YEAR_OF_WEEK_START + " = " + groupingYear + " AND " + WEEK + " = " + groupingSecond;
    case MONTH:
      return year + " AND " + MONTH + " = " + groupingSecond;
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
      int yearToLookUp = groupingSecond ==1 ? groupingYear -1 : groupingYear;
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
        String weekStart = String.format(Locale.US, "'%d-01-01','weekday 1','+%d day'",groupingYear,(groupingSecond-1)*7);
        String weekEnd = String.format(Locale.US, "'%d-01-01','weekday 1','+%d day'",groupingYear,groupingSecond*7-1);
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
      projection = new String[] {KEY_ROWID, KEY_LABEL, KEY_PARENTID,
          "(SELECT sum(amount) " + catFilter + ") AS sum"};
      sortOrder="abs(sum) DESC";
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
        groupingYear, groupingSecond,c));
    thisYear = c.getInt(c.getColumnIndex("this_year"));
    thisMonth = c.getInt(c.getColumnIndex("this_month"));
    thisWeek = c.getInt(c.getColumnIndex("this_week"));
    thisDay = c.getInt(c.getColumnIndex("this_day"));
    maxValue = c.getInt(c.getColumnIndex("max_value"));
    Log.i("DEBUG",String.valueOf(maxValue));
    break;
    case CATEGORY_CURSOR:
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
      mAdapter.setGroupCursor(null);
    }
  }  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    if (mGrouping != null) {
      menu.findItem(R.id.FORWARD_COMMAND).setVisible(!mGrouping.equals(Grouping.NONE));
      menu.findItem(R.id.BACK_COMMAND).setVisible(!mGrouping.equals(Grouping.NONE));
    }
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.BACK_COMMAND:
      if (mGrouping.equals(Grouping.YEAR))
        groupingYear--;
      else {
        groupingSecond--;
        if (groupingSecond < 1) {
          groupingYear--;
          groupingSecond = maxValue;
        }
      }
      reset();
      return true;
    case R.id.FORWARD_COMMAND:
      if (mGrouping.equals(Grouping.YEAR))
        groupingYear++;
      else{
        groupingSecond++;
        if (groupingSecond > maxValue) {
          groupingYear++;
          groupingSecond = 1;
        }
      }
      reset();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void setGrouping(Grouping grouping) {
    mGrouping = grouping;
    groupingYear = thisYear;
    switch(grouping) {
    case NONE:
      groupingYear = 0;
      break;
    case DAY:
      groupingSecond = thisDay;
      break;
    case WEEK:
      groupingSecond = thisWeek;
      break;
    case MONTH:
      groupingSecond = thisMonth;
      break;
    case YEAR:
      groupingSecond = 0;
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
}
