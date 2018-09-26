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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.SearchView;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.squareup.sqlbrite3.BriteContentResolver;
import com.squareup.sqlbrite3.SqlBrite;

import org.apache.commons.lang3.ArrayUtils;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.CategoryTreeAdapter;
import org.totschnig.myexpenses.dialog.MessageDialogFragment;
import org.totschnig.myexpenses.dialog.SelectMainCategoryDialogFragment;
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.data.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static org.totschnig.myexpenses.activity.ManageCategories.ACTION_DISTRIBUTION;
import static org.totschnig.myexpenses.activity.ManageCategories.ACTION_MANAGE;
import static org.totschnig.myexpenses.activity.ManageCategories.ACTION_SELECT_FILTER;
import static org.totschnig.myexpenses.activity.ManageCategories.ACTION_SELECT_MAPPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_NORMALIZED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_MONTH;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_WEEK;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.THIS_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.THIS_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_VOID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getMonth;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisMonth;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisWeek;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfWeekStart;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getWeek;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart;

public class CategoryList extends SortableListFragment {

  public static final String KEY_FILTER = "filter";
  private BriteContentResolver briteContentResolver;
  private Disposable sumDisposable, dateInfoDisposable, categoryDisposable;

  protected int getMenuResource() {
    return R.menu.categorylist_context;
  }

  private CategoryTreeAdapter mAdapter;
  private SqlBrite sqlBrite = new SqlBrite.Builder().build();
  @Nullable
  @BindView(R.id.chart1)
  PieChart mChart;
  @BindView(R.id.list)
  ExpandableListView mListView;
  @Nullable
  @BindView(R.id.sum_income)
  TextView incomeSumTv;
  @Nullable
  @BindView(R.id.sum_expense)
  TextView expenseSumTv;
  @Nullable
  @BindView(R.id.BottomLine)
  View bottomLine;
  @Nullable
  @BindView(R.id.importButton)
  View mImportButton;
  public Grouping mGrouping;
  int mGroupingYear;
  int mGroupingSecond;
  int thisYear, thisMonth, thisWeek, thisDay, maxValue, minValue;

  private Account mAccount;

  protected boolean isIncome = false;
  private int lastExpandedPosition = -1;

  boolean showChart = false;
  boolean aggregateTypes;

  String mFilter;

  @Inject
  CurrencyFormatter currencyFormatter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    MyApplication.getInstance().getAppComponent().inject(this);
    briteContentResolver = sqlBrite.wrapContentProvider(getContext().getContentResolver(), Schedulers.io());
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    aggregateTypes = PrefKey.DISTRIBUTION_AGGREGATE_TYPES.getBoolean(true);
    final ManageCategories ctx = (ManageCategories) getActivity();
    View v;
    Bundle extras = ctx.getIntent().getExtras();
    if (isDistributionScreen()) {
      showChart = PrefKey.DISTRIBUTION_SHOW_CHART.getBoolean(true);

      final long id = Utils.getFromExtra(extras, KEY_ACCOUNTID, 0);
      mAccount = Account.getInstanceFromDb(id);
      if (mAccount == null) {
        TextView tv = new TextView(ctx);
        //noinspection SetTextI18n
        tv.setText("Error loading distribution for account " + id);
        return tv;
      }
      Bundle b = savedInstanceState != null ? savedInstanceState : extras;

      mGrouping = (Grouping) b.getSerializable(KEY_GROUPING);
      if (mGrouping == null) mGrouping = Grouping.NONE;
      mGroupingYear = b.getInt(KEY_YEAR);
      mGroupingSecond = b.getInt(KEY_SECOND_GROUP);
      getActivity().supportInvalidateOptionsMenu();

      v = inflater.inflate(R.layout.distribution_list, container, false);
      ButterKnife.bind(this, v);
      mChart.setVisibility(showChart ? View.VISIBLE : View.GONE);
      mChart.getDescription().setEnabled(false);

      TypedValue typedValue = new TypedValue();
      getActivity().getTheme().resolveAttribute(android.R.attr.textAppearanceMedium, typedValue, true);
      int[] textSizeAttr = new int[]{android.R.attr.textSize};
      int indexOfAttrTextSize = 0;
      TypedArray a = getActivity().obtainStyledAttributes(typedValue.data, textSizeAttr);
      int textSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
      a.recycle();
      mChart.setCenterTextSizePixels(textSize);

      // radius of the center hole in percent of maximum radius
      //mChart.setHoleRadius(60f); 
      //mChart.setTransparentCircleRadius(0f);
      mChart.setDrawEntryLabels(true);
      mChart.setDrawHoleEnabled(true);
      mChart.setDrawCenterText(true);
      mChart.setRotationEnabled(false);
      mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {

        @Override
        public void onValueSelected(Entry e, Highlight highlight) {
          int index = (int) highlight.getX();
          long packedPosition = (lastExpandedPosition == -1) ?
              ExpandableListView.getPackedPositionForGroup(index) :
              ExpandableListView.getPackedPositionForChild(lastExpandedPosition, index);
          Timber.w("%d-%d-%d, %b", index, lastExpandedPosition, packedPosition, showChart);
          int flatPosition = mListView.getFlatListPosition(packedPosition);
          mListView.setItemChecked(flatPosition, true);
          mListView.smoothScrollToPosition(flatPosition);
          setCenterText(index);
        }

        @Override
        public void onNothingSelected() {
          mListView.setItemChecked(mListView.getCheckedItemPosition(), false);
        }
      });
      mChart.setUsePercentValues(true);
    } else {
      v = inflater.inflate(R.layout.categories_list, container, false);
      ButterKnife.bind(this, v);
      if (savedInstanceState != null) {
        mFilter = savedInstanceState.getString(KEY_FILTER);
      }
    }
    updateColor();
    final View emptyView = v.findViewById(R.id.empty);
    mListView.setEmptyView(emptyView);
    mAdapter = new CategoryTreeAdapter(ctx, currencyFormatter,
        mAccount == null ? null : mAccount.currency, isWithMainColors(), showChart);
    mListView.setAdapter(mAdapter);
    loadData();
    if (isDistributionScreen()) {
      mListView.setOnGroupClickListener((parent, v12, groupPosition, id) ->
      {
        if (showChart) {
          if (lastExpandedPosition != -1
              && groupPosition != lastExpandedPosition) {
            mListView.collapseGroup(lastExpandedPosition);
            lastExpandedPosition = -1;
          }
          if (mAdapter.getChildrenCount(groupPosition) == 0) {
            long packedPosition = ExpandableListView.getPackedPositionForGroup(groupPosition);
            highlight(groupPosition);
            mListView.setItemChecked(mListView.getFlatListPosition(packedPosition), true);
            return true;
          }
        }
        return false;
      });
      mListView.setOnGroupExpandListener(groupPosition -> {
        if (showChart) {
          List<Category> categories = mAdapter.getSubCategories(groupPosition);
          setData(categories, mAdapter.getGroup(groupPosition));
          highlight(0);
          long packedPosition = ExpandableListView.getPackedPositionForChild(groupPosition, 0);
          mListView.setItemChecked(mListView.getFlatListPosition(packedPosition), true);
        }
        lastExpandedPosition = groupPosition;
      });
      mListView.setOnGroupCollapseListener(groupPosition -> {
        if (showChart) {
          setData(mAdapter.getMainCategories(), null);
          highlight(groupPosition);
          long packedPosition = ExpandableListView
              .getPackedPositionForGroup(groupPosition);
          int flatPosition = mListView.getFlatListPosition(packedPosition);
          mListView.setItemChecked(flatPosition, true);
        }
        lastExpandedPosition = -1;
      });
      mListView.setOnChildClickListener((parent, v1, groupPosition, childPosition, id) -> {
        if (showChart) {
          long packedPosition = ExpandableListView.getPackedPositionForChild(
              groupPosition, childPosition);
          highlight(childPosition);
          int flatPosition = mListView.getFlatListPosition(packedPosition);
          mListView.setItemChecked(flatPosition, true);
          return true;
        }
        return false;
      });
      //the following is relevant when not in touch mode
      mListView.setOnItemSelectedListener(new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
          if (showChart) {
            long pos = mListView.getExpandableListPosition(position);
            int type = ExpandableListView.getPackedPositionType(pos);
            int group = ExpandableListView.getPackedPositionGroup(pos),
                child = ExpandableListView.getPackedPositionChild(pos);
            int highlightedPos;
            if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
              if (lastExpandedPosition != group) {
                mListView.collapseGroup(lastExpandedPosition);
              }
              highlightedPos = lastExpandedPosition == -1 ? group : -1;
            } else {
              highlightedPos = child;
            }
            highlight(highlightedPos);
          }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
      });
      mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
      registerForContextMenu(mListView);
    } else {
      registerForContextualActionBar(mListView);
    }
    return v;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (isDistributionScreen()) {
      updateSum();
      updateDateInfo();
    }
    if (mAccount != null) {
      ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
      ActionBar actionBar = ctx.getSupportActionBar();
      actionBar.setTitle(mAccount.getLabelForScreenTitle(getContext()));
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    disposeSum();
    disposeDateInfo();
    disposeCategory();
  }

  protected void loadData() {
    String selection = null, accountSelector = null, sortOrder = null;
    String[] selectionArgs, projection;
    String CATTREE_WHERE_CLAUSE = KEY_CATID + " IN (SELECT " +
        TABLE_CATEGORIES + "." + KEY_ROWID +
        " UNION SELECT " + KEY_ROWID + " FROM "
        + TABLE_CATEGORIES + " subtree WHERE " + KEY_PARENTID + " = " + TABLE_CATEGORIES + "." + KEY_ROWID + ")";
    String catFilter;
    if (mAccount != null) {
      //Distribution
      String accountSelection, amountCalculation = KEY_AMOUNT, table = VIEW_COMMITTED;
      if (mAccount.isHomeAggregate()) {
        accountSelection = null;
        amountCalculation = DatabaseConstants.getAmountHomeEquivalent();
        table = VIEW_EXTENDED;
      } else if (mAccount.isAggregate()) {
        accountSelection = " IN " +
            "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ? AND " +
            KEY_EXCLUDE_FROM_TOTALS + " = 0 )";
        accountSelector = mAccount.currency.getCurrencyCode();
      } else {
        accountSelection = " = ?";
        accountSelector = String.valueOf(mAccount.getId());
      }
      catFilter = "FROM " + table +
          " WHERE " + WHERE_NOT_VOID + (accountSelection == null ? "" : (" AND " + KEY_ACCOUNTID + accountSelection));
      if (!aggregateTypes) {
        catFilter += " AND " + KEY_AMOUNT + (isIncome ? ">" : "<") + "0";
      }
      if (!mGrouping.equals(Grouping.NONE)) {
        catFilter += " AND " + buildGroupingClause();
      }
      //we need to include transactions mapped to children for main categories
      catFilter += " AND " + CATTREE_WHERE_CLAUSE;
      selection = " exists (SELECT 1 " + catFilter + ")";
      projection = new String[]{
          KEY_ROWID,
          KEY_PARENTID,
          KEY_LABEL,
          KEY_COLOR,
          "(SELECT sum(" + amountCalculation + ") " + catFilter + ") AS " + KEY_SUM
      };
      sortOrder = "abs(" + KEY_SUM + ") DESC";
      selectionArgs = accountSelector != null ? new String[]{accountSelector, accountSelector} : null;
    } else {
      catFilter = CATTREE_WHERE_CLAUSE;
      projection = new String[]{
          KEY_ROWID,
          KEY_PARENTID,
          KEY_LABEL,
          KEY_COLOR,
          //here we do not filter out void transactinos since they need to be considered as mapped
          "(select 1 FROM " + TABLE_TRANSACTIONS + " WHERE " + catFilter + ") AS " + DatabaseConstants.KEY_MAPPED_TRANSACTIONS,
          "(select 1 FROM " + TABLE_TEMPLATES + " WHERE " + catFilter + ") AS " + DatabaseConstants.KEY_MAPPED_TEMPLATES
      };
      boolean isFiltered = !TextUtils.isEmpty(mFilter);
      if (isFiltered) {
        String filterSelection = KEY_LABEL_NORMALIZED + " LIKE ?";
        selectionArgs = new String[]{"%" + mFilter + "%", "%" + mFilter + "%"};
        selection = filterSelection + " OR EXISTS (SELECT 1 FROM " + TABLE_CATEGORIES +
            " subtree WHERE " + KEY_PARENTID + " = " + TABLE_CATEGORIES + "." + KEY_ROWID + " AND ("
            + filterSelection + " ))";
      } else {
        selectionArgs = null;
      }
    }

    categoryDisposable = briteContentResolver.createQuery(TransactionProvider.CATEGORIES_URI,
        projection, selection, selectionArgs, sortOrder, true)
        .subscribe(query -> {
          final Cursor cursor = query.run();
          if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
              mAdapter.ingest(cursor);
              if (isDistributionScreen()) {
                if (mAdapter.getGroupCount() > 0) {
                  if (lastExpandedPosition == -1) {
                    mChart.setVisibility(showChart ? View.VISIBLE : View.GONE);
                    setData(mAdapter.getMainCategories(), null);
                    highlight(0);
                    if (showChart)
                      mListView.setItemChecked(mListView.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(0)), true);
                  }
                } else {
                  mChart.setVisibility(View.GONE);
                }
              }
            });
          }
        });
  }

  private void updateDateInfo() {
    disposeDateInfo();
    ArrayList<String> projectionList = new ArrayList<>(Arrays.asList(
        getThisYearOfWeekStart() + " AS " + KEY_THIS_YEAR_OF_WEEK_START,
        THIS_YEAR + " AS " + KEY_THIS_YEAR,
        getThisMonth() + " AS " + KEY_THIS_MONTH,
        getThisWeek() + " AS " + KEY_THIS_WEEK,
        THIS_DAY + " AS " + KEY_THIS_DAY));
    //if we are at the beginning of the year we are interested in the max of the previous year
    int yearToLookUp = mGroupingSecond == 1 ? mGroupingYear - 1 : mGroupingYear;
    switch (mGrouping) {
      case DAY:
        projectionList.add(String.format(Locale.US, "strftime('%%j','%d-12-31') AS " + KEY_MAX_VALUE, yearToLookUp));
        break;
      case WEEK:
        projectionList.add(String.format(Locale.US, "strftime('%%W','%d-12-31') AS " + KEY_MAX_VALUE, yearToLookUp));
        break;
      case MONTH:
        projectionList.add("11 as " + KEY_MAX_VALUE);
        break;
      default://YEAR
        projectionList.add("0 as " + KEY_MAX_VALUE);
    }
    if (mGrouping.equals(Grouping.WEEK)) {
      //we want to find out the week range when we are given a week number
      //we find out the first Monday in the year, which is the beginning of week 1 and then
      //add (weekNumber-1)*7 days to get at the beginning of the week
      projectionList.add(DbUtils.weekStartFromGroupSqlExpression(mGroupingYear, mGroupingSecond));
      projectionList.add(DbUtils.weekEndFromGroupSqlExpression(mGroupingYear, mGroupingSecond));
    }
    dateInfoDisposable = briteContentResolver.createQuery(
        TransactionProvider.DUAL_URI,
        projectionList.toArray(new String[projectionList.size()]),
        null, null, null, false)
        .subscribe(query -> {
          final Cursor cursor = query.run();
          if (cursor != null) {
            if (getActivity() != null) {
              getActivity().runOnUiThread(() -> {
                try {
                  cursor.moveToFirst();
                  thisYear = cursor.getInt(cursor.getColumnIndex(KEY_THIS_YEAR));
                  thisMonth = cursor.getInt(cursor.getColumnIndex(KEY_THIS_MONTH));
                  thisWeek = cursor.getInt(cursor.getColumnIndex(KEY_THIS_WEEK));
                  thisDay = cursor.getInt(cursor.getColumnIndex(KEY_THIS_DAY));
                  maxValue = cursor.getInt(cursor.getColumnIndex(KEY_MAX_VALUE));
                  minValue = mGrouping == Grouping.MONTH ? 0 : 1;
                  ((ProtectedFragmentActivity) getActivity()).getSupportActionBar().setSubtitle(
                      mGrouping.getDisplayTitle(getActivity(), mGroupingYear, mGroupingSecond, cursor));
                } finally {
                  cursor.close();
                }
              });
            }
          }
        });
  }

  private void updateSum() {
    disposeSum();
    Builder builder = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon();
    if (!mAccount.isHomeAggregate()) {
      if (mAccount.isAggregate()) {
        builder.appendQueryParameter(KEY_CURRENCY, mAccount.currency.getCurrencyCode());
      } else {
        builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(mAccount.getId()));
      }
    }
    //if we have no income or expense, there is no row in the cursor
    sumDisposable = briteContentResolver.createQuery(builder.build(),
        null,
        buildGroupingClause(),
        null,
        null, true)
        .mapToList(cursor -> {
          int type = cursor.getInt(cursor.getColumnIndex(KEY_TYPE));
          long sum = cursor.getLong(cursor.getColumnIndex(KEY_SUM));
          return Pair.create(type, sum);
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(pairs -> {
          boolean[] seen = new boolean[2];
          for (Pair<Integer, Long> pair : pairs) {
            seen[pair.first] = true;
            updateSum(pair.first > 0 ? "+ " : "- ",
                pair.first > 0 ? incomeSumTv : expenseSumTv, pair.second);
          }
          if (!seen[1]) updateSum("+ ", incomeSumTv, 0);
          if (!seen[0]) updateSum("- ", expenseSumTv, 0);
        });
  }

  private void disposeSum() {
    if (sumDisposable != null && !sumDisposable.isDisposed()) {
      sumDisposable.dispose();
    }
  }

  private void disposeDateInfo() {
    if (dateInfoDisposable != null && !dateInfoDisposable.isDisposed()) {
      dateInfoDisposable.dispose();
    }
  }

  private void disposeCategory() {
    if (categoryDisposable != null && !categoryDisposable.isDisposed()) {
      categoryDisposable.dispose();
    }
  }

  @Override
  public boolean dispatchCommandMultiple(int command,
                                         SparseBooleanArray positions, Long[] itemIds) {
    ManageCategories ctx = (ManageCategories) getActivity();
    ArrayList<Long> idList;
    switch (command) {
      case R.id.DELETE_COMMAND: {
        int mappedTransactionsCount = 0, mappedTemplatesCount = 0, hasChildrenCount = 0;
        idList = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
          Category c;
          if (positions.valueAt(i)) {
            boolean deletable = true;
            int position = positions.keyAt(i);
            long pos = mListView.getExpandableListPosition(position);
            int type = ExpandableListView.getPackedPositionType(pos);
            int group = ExpandableListView.getPackedPositionGroup(pos),
                child = ExpandableListView.getPackedPositionChild(pos);
            if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
              c = mAdapter.getChild(group, child);
            } else {
              c = mAdapter.getGroup(group);
            }
            Bundle extras = ctx.getIntent().getExtras();
            if ((extras != null && extras.getLong(KEY_ROWID) == c.id) || c.hasMappedTransactions) {
              mappedTransactionsCount++;
              deletable = false;
            } else if (c.hasMappedTemplates) {
              mappedTemplatesCount++;
              deletable = false;
            }
            if (deletable) {
              if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP && c.hasChildren()) {
                hasChildrenCount++;
              }
              idList.add(c.id);
            }
          }
        }
        if (!idList.isEmpty()) {
          Long[] objectIds = idList.toArray(new Long[idList.size()]);
          if (hasChildrenCount > 0) {
            MessageDialogFragment.newInstance(
                R.string.dialog_title_warning_delete_main_category,
                getResources().getQuantityString(R.plurals.warning_delete_main_category, hasChildrenCount, hasChildrenCount),
                new MessageDialogFragment.Button(android.R.string.yes, R.id.DELETE_COMMAND_DO, objectIds),
                null,
                new MessageDialogFragment.Button(android.R.string.no, R.id.CANCEL_CALLBACK_COMMAND, null))
                .show(ctx.getSupportFragmentManager(), "DELETE_CATEGORY");
          } else {
            ctx.dispatchCommand(R.id.DELETE_COMMAND_DO, objectIds);
          }
        }
        if (mappedTransactionsCount > 0 || mappedTemplatesCount > 0) {
          String message = "";
          if (mappedTransactionsCount > 0) {
            message += getResources().getQuantityString(
                R.plurals.not_deletable_mapped_transactions,
                mappedTransactionsCount,
                mappedTransactionsCount);
          }
          if (mappedTemplatesCount > 0) {
            message += getResources().getQuantityString(
                R.plurals.not_deletable_mapped_templates,
                mappedTemplatesCount,
                mappedTemplatesCount);
          }
          ctx.showSnackbar(message, Snackbar.LENGTH_LONG);
        }
        return true;
      }
      case R.id.SELECT_COMMAND_MULTIPLE: {
        ArrayList<String> labelList = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
          Category c;
          if (positions.valueAt(i)) {
            int position = positions.keyAt(i);
            long pos = mListView.getExpandableListPosition(position);
            int type = ExpandableListView.getPackedPositionType(pos);
            int group = ExpandableListView.getPackedPositionGroup(pos),
                child = ExpandableListView.getPackedPositionChild(pos);
            if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
              c = mAdapter.getChild(group, child);
            } else {
              c = mAdapter.getGroup(group);
            }
            labelList.add(c.label);
          }
        }
        Intent intent = new Intent();
        intent.putExtra(KEY_CATID, ArrayUtils.toPrimitive(itemIds));
        intent.putExtra(KEY_LABEL, TextUtils.join(",", labelList));
        ctx.setResult(ManageCategories.RESULT_FIRST_USER, intent);
        ctx.finish();
        return true;
      }
      case R.id.MOVE_COMMAND:
        final Long[] excludedIds;
        final boolean inGroup = expandableListSelectionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP;
        if (inGroup) {
          excludedIds = itemIds;
        } else {
          idList = new ArrayList<>();
          for (int i = 0; i < positions.size(); i++) {
            if (positions.valueAt(i)) {
              int position = positions.keyAt(i);
              long pos = mListView.getExpandableListPosition(position);
              int group = ExpandableListView.getPackedPositionGroup(pos);
              idList.add(mAdapter.getGroup(group).id);
            }
          }
          excludedIds = idList.toArray(new Long[idList.size()]);
        }
        Bundle args = new Bundle(3);
        args.putBoolean(SelectMainCategoryDialogFragment.KEY_WITH_ROOT, !inGroup);
        args.putLongArray(SelectMainCategoryDialogFragment.KEY_EXCLUDED_ID, ArrayUtils.toPrimitive(excludedIds));
        args.putLongArray(TaskExecutionFragment.KEY_OBJECT_IDS, ArrayUtils.toPrimitive(itemIds));
        SelectMainCategoryDialogFragment.newInstance(args)
            .show(getFragmentManager(), "SELECT_TARGET");
        return true;
    }
    return false;
  }

  @Override
  public boolean dispatchCommandSingle(int command, ContextMenu.ContextMenuInfo info) {
    ManageCategories ctx = (ManageCategories) getActivity();
    String action = ctx.getAction();
    ExpandableListContextMenuInfo elcmi = (ExpandableListContextMenuInfo) info;
    int type = ExpandableListView.getPackedPositionType(elcmi.packedPosition);
    Category c;
    boolean isMain;
    int group = ExpandableListView.getPackedPositionGroup(elcmi.packedPosition),
        child = ExpandableListView.getPackedPositionChild(elcmi.packedPosition);
    if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
      c = mAdapter.getChild(group, child);
      isMain = false;
    } else {
      c = mAdapter.getGroup(group);
      isMain = true;
    }
    String label = c.label;
    switch (command) {
      case R.id.COLOR_COMMAND:
        ctx.editCategoryColor(c);
        return true;
      case R.id.EDIT_COMMAND:
        ctx.editCat(label, elcmi.id);
        return true;
      case R.id.SELECT_COMMAND:
        if (!isMain && action.equals(ACTION_SELECT_MAPPING)) {
          label = mAdapter.getGroup(group).label + TransactionList.CATEGORY_SEPARATOR + label;
        }
        doSelection(elcmi.id, label, isMain);
        finishActionMode();
        return true;
      case R.id.CREATE_COMMAND:
        ctx.createCat(elcmi.id);
        return true;
    }
    return super.dispatchCommandSingle(command, info);
  }

  private String buildGroupingClause() {
    String year = YEAR + " = " + mGroupingYear;
    switch (mGrouping) {
      case YEAR:
        return year;
      case DAY:
        return year + " AND " + DAY + " = " + mGroupingSecond;
      case WEEK:
        return getYearOfWeekStart() + " = " + mGroupingYear + " AND " + getWeek() + " = " + mGroupingSecond;
      case MONTH:
        return getYearOfMonthStart() + " = " + mGroupingYear + " AND " + getMonth() + " = " + mGroupingSecond;
      default:
        return null;
    }
  }

  protected PrefKey getSortOrderPrefKey() {
    return PrefKey.SORT_ORDER_CATEGORIES;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    ManageCategories ctx = (ManageCategories) getActivity();
    if (ctx == null) return;

    if (!isDistributionScreen()) {
      inflater.inflate(R.menu.search, menu);
      SearchManager searchManager =
          (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
      MenuItem searchMenuItem = menu.findItem(R.id.SEARCH_COMMAND);
      SearchView searchView = (SearchView) searchMenuItem.getActionView();

      searchView.setSearchableInfo(searchManager.
          getSearchableInfo(getActivity().getComponentName()));
      //searchView.setIconifiedByDefault(true);
      searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
          return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
          if (TextUtils.isEmpty(newText)) {
            mFilter = "";
            mImportButton.setVisibility(View.VISIBLE);
          } else {
            mFilter = Utils.esacapeSqlLikeExpression(Utils.normalize(newText));
            // if a filter results in an empty list,
            // we do not want to show the setup default categories button
            mImportButton.setVisibility(View.GONE);
          }
          collapseAll();
          loadData();
          return true;
        }
      });
    }
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (mGrouping != null) {
      Utils.configureGroupingMenu(menu.findItem(R.id.GROUPING_COMMAND).getSubMenu(), mGrouping);
      boolean grouped = !mGrouping.equals(Grouping.NONE);
      Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.FORWARD_COMMAND), grouped);
      Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.BACK_COMMAND), grouped);
    }
    MenuItem m = menu.findItem(R.id.TOGGLE_CHART_COMMAND);
    if (m != null) {
      m.setChecked(showChart);
    }
    m = menu.findItem(R.id.TOGGLE_AGGREGATE_TYPES);
    if (m != null) {
      m.setChecked(aggregateTypes);
      Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.switchId), !aggregateTypes);
    }

    MenuItem searchMenuItem = menu.findItem(R.id.SEARCH_COMMAND);
    if (searchMenuItem != null && mFilter != null) {
      SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
      searchView.setQuery(mFilter, false);
      searchView.setIconified(false);
      searchView.clearFocus();
    }
  }

  public void back() {
    if (mGrouping.equals(Grouping.YEAR))
      mGroupingYear--;
    else {
      mGroupingSecond--;
      if (mGroupingSecond < minValue) {
        mGroupingYear--;
        mGroupingSecond = maxValue;
      }
    }
    reset();
  }

  public void forward() {
    if (mGrouping.equals(Grouping.YEAR))
      mGroupingYear++;
    else {
      mGroupingSecond++;
      if (mGroupingSecond > maxValue) {
        mGroupingYear++;
        mGroupingSecond = minValue;
      }
    }
    reset();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (handleGrouping(item)) return true;
    switch (item.getItemId()) {
      case R.id.BACK_COMMAND:
        back();
        return true;
      case R.id.FORWARD_COMMAND:
        forward();
        return true;
      case R.id.TOGGLE_CHART_COMMAND:
        showChart = !showChart;
        PrefKey.DISTRIBUTION_SHOW_CHART.putBoolean(showChart);
        mChart.setVisibility(showChart ? View.VISIBLE : View.GONE);
        if (showChart) {
          collapseAll();
        } else {
          mListView.setItemChecked(mListView.getCheckedItemPosition(), false);
        }
        mAdapter.toggleColors();
        return true;
      case R.id.TOGGLE_AGGREGATE_TYPES:
        aggregateTypes = !aggregateTypes;
        PrefKey.DISTRIBUTION_AGGREGATE_TYPES.putBoolean(aggregateTypes);
        getActivity().supportInvalidateOptionsMenu();
        reset();
        return true;
    }
    return handleSortOption(item);
  }

  private boolean handleGrouping(MenuItem item) {
    Grouping newGrouping = Utils.getGroupingFromMenuItemId(item.getItemId());
    if (newGrouping != null) {
      if (!item.isChecked()) {
        setGrouping(newGrouping);
      }
      return true;
    }
    return false;
  }

  public void collapseAll() {
    int count = mAdapter.getGroupCount();
    for (int i = 0; i < count; i++)
      mListView.collapseGroup(i);
  }

  /*     (non-Javadoc)
   * return the sub cat to the calling activity
   * @see android.app.ExpandableListActivity#onChildClick(android.widget.ExpandableListView, android.view.View, int, int, long)
   */
  @Override
  public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
    if (super.onChildClick(parent, v, groupPosition, childPosition, id))
      return true;
    ManageCategories ctx = (ManageCategories) getActivity();
    if (ctx == null) {
      return false;
    }
    String action = ctx.getAction();
    if (action.equals(ACTION_MANAGE)) {
      return false;
    }
    String label = ((TextView) v.findViewById(R.id.label)).getText().toString();
    if (action.equals(ACTION_SELECT_MAPPING)) {
      label = mAdapter.getGroup(groupPosition).label + TransactionList.CATEGORY_SEPARATOR + label;
    }
    doSelection(id, label, false);
    return true;
  }

  @Override
  public boolean onGroupClick(ExpandableListView parent, View v,
                              int groupPosition, long id) {
    if (super.onGroupClick(parent, v, groupPosition, id))
      return true;
    ManageCategories ctx = (ManageCategories) getActivity();
    if (ctx == null) {
      return false;
    }
    String action = ctx.getAction();
    if (action.equals(ACTION_MANAGE) || mAdapter.getGroup(groupPosition).hasChildren()) {
      return false;
    }
    String label = ((TextView) v.findViewById(R.id.label)).getText().toString();
    doSelection(id, label, true);
    return true;
  }

  private void doSelection(long cat_id, String label, boolean isMain) {
    ManageCategories ctx = (ManageCategories) getActivity();
    if (isDistributionScreen()) {
      TransactionListDialogFragment.newInstance(
          mAccount.getId(), cat_id, isMain, mGrouping, buildGroupingClause(), label, 0, true)
          .show(getFragmentManager(), TransactionListDialogFragment.class.getName());
      return;
    }
    Intent intent = new Intent();
    intent.putExtra(KEY_CATID, cat_id);
    intent.putExtra(KEY_LABEL, label);
    ctx.setResult(ManageCategories.RESULT_OK, intent);
    ctx.finish();
  }

  public void setGrouping(Grouping grouping) {
    mGrouping = grouping;
    mGroupingYear = thisYear;
    switch (grouping) {
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
    getActivity().invalidateOptionsMenu();
    reset();
  }

  public void reset() {
    //TODO: would be nice to retrieve the same open groups on the next or previous group
    Timber.w("reset");
    mListView.clearChoices();
    lastExpandedPosition = -1;
    loadData();
    if (isDistributionScreen()) {
      updateSum();
      updateDateInfo();
    }
  }

  private void updateSum(String prefix, TextView tv, long amount) {
    if (tv != null) {
      //noinspection SetTextI18n
      tv.setText(prefix + currencyFormatter.formatCurrency(
          new Money(mAccount.currency, amount)));
    }
  }

  private void updateColor() {
    if (bottomLine != null)
      bottomLine.setBackgroundColor(mAccount.color);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_GROUPING, mGrouping);
    outState.putInt(KEY_YEAR, mGroupingYear);
    outState.putInt(KEY_SECOND_GROUP, mGroupingSecond);
    if (!TextUtils.isEmpty(mFilter)) {
      outState.putString("filter", mFilter);
    }
  }

  @Override
  protected void configureMenuLegacy(Menu menu, ContextMenu.ContextMenuInfo menuInfo, int listId) {
    super.configureMenuLegacy(menu, menuInfo, listId);
    boolean hasChildren = false;
    if (expandableListSelectionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
      ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
      int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
      hasChildren = hasChildren(groupPos);
    }
    configureMenuInternal(menu, hasChildren);
  }

  @Override
  protected void configureMenu11(Menu menu, int count, AbsListView lv) {
    boolean hasChildren = false;
    super.configureMenu11(menu, count, lv);
    if (expandableListSelectionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
      SparseBooleanArray checkedItemPositions = mListView.getCheckedItemPositions();
      for (int i = 0; i < checkedItemPositions.size(); i++) {
        if (checkedItemPositions.valueAt(i)) {
          int position = checkedItemPositions.keyAt(i);
          long pos = mListView.getExpandableListPosition(position);
          int groupPos = ExpandableListView.getPackedPositionGroup(pos);
          if (hasChildren(groupPos)) {
            hasChildren = true;
            break;
          }
        }
      }
    }
    configureMenuInternal(menu, hasChildren);
  }

  private boolean hasChildren(int position) {
    return position != -1 && mAdapter.getGroup(position).hasChildren();
  }

  private void configureMenuInternal(Menu menu, boolean hasChildren) {
    String action = getAction();
    final boolean isFilter = action.equals(ACTION_SELECT_FILTER);
    final boolean isDistribution = action.equals(ACTION_DISTRIBUTION);
    boolean inFilterOrDistribution = isFilter || isDistribution;
    maybeHide(menu.findItem(R.id.EDIT_COMMAND), inFilterOrDistribution);
    maybeHide(menu.findItem(R.id.DELETE_COMMAND), inFilterOrDistribution);
    MenuItem item = menu.findItem(R.id.SELECT_COMMAND);
    maybeHide(item, !(isDistribution || action.equals(ACTION_SELECT_MAPPING)));
    if (isDistribution) {
      item.setTitle(R.string.menu_show_transactions);
    }
    maybeHide(menu.findItem(R.id.SELECT_COMMAND_MULTIPLE), !isFilter);
    maybeHide(menu.findItem(R.id.CREATE_COMMAND), inFilterOrDistribution);
    maybeHide(menu.findItem(R.id.MOVE_COMMAND), (inFilterOrDistribution || hasChildren));
    maybeHide(menu.findItem(R.id.COLOR_COMMAND), !isWithMainColors());
  }

  private void maybeHide(MenuItem item, boolean condition) {
    if (condition) {
      item.setVisible(false);
    }
  }

  public void setType(boolean isChecked) {
    isIncome = isChecked;
    reset();
  }

  private void setData(List<Category> categories, Category parent) {
    List<PieEntry> entries = Stream.of(categories)
        .map(category -> new PieEntry(Math.abs(category.sum), category.label))
        .collect(Collectors.toList());
    List<Integer> colors = parent == null ?
        Stream.of(categories).map(category -> category.color).collect(Collectors.toList()) :
        mAdapter.getSubColors(parent.color);

    PieDataSet ds1 = new PieDataSet(entries, "");
    ds1.setColors(colors);
    ds1.setSliceSpace(2f);
    ds1.setDrawValues(false);

    PieData data = new PieData(ds1);
    data.setValueFormatter(new PercentFormatter());
    mChart.setData(data);
    mChart.getLegend().setEnabled(false);
    // undo all highlights
    mChart.highlightValues(null);
    mChart.invalidate();
  }

  private void highlight(int position) {
    if (position != -1) {
      mChart.highlightValue(position, 0);
      setCenterText(position);
    }
  }

  private void setCenterText(int position) {
    PieData data = mChart.getData();

    PieEntry entry = data.getDataSet().getEntryForIndex(position);
    String description = entry.getLabel();

    String value = data.getDataSet().getValueFormatter().getFormattedValue(
        entry.getValue() / data.getYValueSum() * 100f,
        entry, position, null);

    mChart.setCenterText(
        description + "\n" +
            value
    );
  }

  private boolean isDistributionScreen() {
    return getAction().equals(ACTION_DISTRIBUTION);
  }

  private boolean isWithMainColors() {
    return showChart || getAction().equals(ACTION_MANAGE);
  }

  private String getAction() {
    return ((ManageCategories) getActivity()).getAction();
  }
}
