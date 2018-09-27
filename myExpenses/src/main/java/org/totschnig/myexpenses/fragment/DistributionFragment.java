package org.totschnig.myexpenses.fragment;

import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
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
import com.squareup.sqlbrite3.QueryObservable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.CategoryTreeAdapter;
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.data.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
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

public class DistributionFragment extends CategoryList {
  @BindView(R.id.chart1)
  PieChart mChart;
  @BindView(R.id.BottomLine)
  View bottomLine;
  boolean showChart = false;
  boolean aggregateTypes;
  private Account mAccount;
  private Disposable sumDisposable, dateInfoDisposable;

  public Grouping getGrouping() {
    return mGrouping;
  }

  private Grouping mGrouping;
  int mGroupingYear;
  int mGroupingSecond;
  int thisYear, thisMonth, thisWeek, thisDay, maxValue, minValue;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    aggregateTypes = PrefKey.DISTRIBUTION_AGGREGATE_TYPES.getBoolean(true);
    final ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    View v;
    Bundle extras = ctx.getIntent().getExtras();
    showChart = PrefKey.DISTRIBUTION_SHOW_CHART.getBoolean(true);

    final long accountId = Utils.getFromExtra(extras, KEY_ACCOUNTID, 0);
    mAccount = Account.getInstanceFromDb(accountId);
    if (mAccount == null) {
      TextView tv = new TextView(ctx);
      //noinspection SetTextI18n
      tv.setText("Error loading distribution for account " + accountId);
      return tv;
    }
    Bundle b = savedInstanceState != null ? savedInstanceState : extras;

    mGrouping = (Grouping) b.getSerializable(KEY_GROUPING);
    if (mGrouping == null) mGrouping = Grouping.NONE;
    mGroupingYear = b.getInt(KEY_YEAR);
    mGroupingSecond = b.getInt(KEY_SECOND_GROUP);
    getActivity().invalidateOptionsMenu();

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
    updateColor();
    final View emptyView = v.findViewById(R.id.empty);
    mListView.setEmptyView(emptyView);
    mAdapter = new CategoryTreeAdapter(ctx, currencyFormatter,
        mAccount == null ? null : mAccount.currency, showChart, showChart);
    mListView.setAdapter(mAdapter);
    loadData();
    mListView.setOnGroupClickListener((parent, v12, groupPosition, id) ->
    {
      if (showChart) {
        if (mAdapter.getChildrenCount(groupPosition) == 0) {
          long packedPosition = ExpandableListView.getPackedPositionForGroup(groupPosition);
          highlight(groupPosition);
          mListView.setItemChecked(mListView.getFlatListPosition(packedPosition), true);
          if (lastExpandedPosition != -1
              && groupPosition != lastExpandedPosition) {
            mListView.collapseGroup(lastExpandedPosition);
            lastExpandedPosition = -1;
          }
          return true;
        }
      }
      return false;
    });
    mListView.setOnGroupExpandListener(groupPosition -> {
      if (showChart) {
        if (lastExpandedPosition != -1 && groupPosition != lastExpandedPosition) {
          mListView.collapseGroup(lastExpandedPosition);
        }
        lastExpandedPosition = groupPosition;
        setData();
        highlight(0);
      } else {
        lastExpandedPosition = groupPosition;
      }
    });
    mListView.setOnGroupCollapseListener(groupPosition -> {
      lastExpandedPosition = -1;
      if (showChart) {
        setData();
        highlight(groupPosition);
      }
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
    mListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

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
    return v;
  }

  @Override
  protected QueryObservable createQuery() {
    String selection, accountSelector = null, sortOrder;
    String[] selectionArgs, projection;
    String catFilter;
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
    return briteContentResolver.createQuery(TransactionProvider.CATEGORIES_URI,
        projection, selection, selectionArgs, sortOrder, true);
  }

  @Override
  protected void onLoadFinished() {
    if (mAdapter.getGroupCount() > 0) {
      mChart.setVisibility(showChart ? View.VISIBLE : View.GONE);
      setData();
      highlight(0);
      if (showChart) {
        mListView.setItemChecked(mListView.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(0)), true);
      }
    } else {
      mChart.setVisibility(View.GONE);
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.distribution, menu);
    inflater.inflate(R.menu.grouping, menu);

    SwitchCompat typeButton = MenuItemCompat.getActionView(menu.findItem(R.id.switchId))
        .findViewById(R.id.TaType);

    typeButton.setOnCheckedChangeListener((buttonView, isChecked) -> setType(isChecked));
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
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
  }

  private void updateSum(String prefix, TextView tv, long amount) {
    if (tv != null) {
      //noinspection SetTextI18n
      tv.setText(prefix + currencyFormatter.formatCurrency(
          new Money(mAccount.currency, amount)));
    }
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
    return false;
  }

  protected void doSelection(long cat_id, String label, boolean isMain) {
    TransactionListDialogFragment.newInstance(
        mAccount.getId(), cat_id, isMain, mGrouping, buildGroupingClause(), label, 0, true)
        .show(getFragmentManager(), TransactionListDialogFragment.class.getName());
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    updateSum();
    updateDateInfo();
    if (mAccount != null) {
      ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
      ActionBar actionBar = ctx.getSupportActionBar();
      actionBar.setTitle(mAccount.getLabelForScreenTitle(getContext()));
    }
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

  private void setData() {
    List<Category> categories;
    Category parent;
    if (lastExpandedPosition == -1) {
      parent = null;
      categories = mAdapter.getMainCategories();
    } else {
      parent = mAdapter.getGroup(lastExpandedPosition);
      categories = mAdapter.getSubCategories(lastExpandedPosition);
    }
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

  private void updateColor() {
    if (bottomLine != null)
      bottomLine.setBackgroundColor(mAccount.color);
  }

  @Override
  public void reset() {
    super.reset();
    updateSum();
    updateDateInfo();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    disposeSum();
    disposeDateInfo();
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
    Uri.Builder builder = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon();
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


  protected void configureMenuInternal(Menu menu, boolean hasChildren) {
    menu.findItem(R.id.EDIT_COMMAND).setVisible(false);
    menu.findItem(R.id.DELETE_COMMAND).setVisible(false);
    MenuItem item = menu.findItem(R.id.SELECT_COMMAND);
    menu.findItem(R.id.SELECT_COMMAND).setTitle(R.string.menu_show_transactions);
    menu.findItem(R.id.SELECT_COMMAND_MULTIPLE).setVisible(false);
    menu.findItem(R.id.CREATE_COMMAND).setVisible(false);
    menu.findItem(R.id.MOVE_COMMAND).setVisible(false);
    maybeHide(menu.findItem(R.id.COLOR_COMMAND), !showChart);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_GROUPING, mGrouping);
    outState.putInt(KEY_YEAR, mGroupingYear);
    outState.putInt(KEY_SECOND_GROUP, mGroupingSecond);
  }
}
