package org.totschnig.myexpenses.fragment;

import android.content.res.TypedArray;
import android.os.Bundle;
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

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.CategoryTreeAdapter;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.ui.SelectivePieChartRenderer;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.data.Category;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;

public class DistributionFragment extends DistributionBaseFragment {
  @BindView(R.id.chart1)
  PieChart mChart;
  @BindView(R.id.BottomLine)
  View bottomLine;
  boolean showChart = false;
  private int textColorSecondary;
  private Account mAccount;

  public Grouping getGrouping() {
    return mGrouping;
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mAccount = Account.getInstanceFromDb(getActivity().getIntent().getLongExtra(KEY_ACCOUNTID, 0));
    if (mAccount == null) {
      return errorView();
    }
    setAccountInfo(new AccountInfo() {
      @Override
      public long getId() {
        return mAccount.getId();
      }

      @Override
      public CurrencyUnit getCurrencyUnit() {
        return mAccount.getCurrencyUnit();
      }
    });
    final ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    View v;
    Bundle extras = ctx.getIntent().getExtras();
    showChart = PrefKey.DISTRIBUTION_SHOW_CHART.getBoolean(true);

    Bundle b = savedInstanceState != null ? savedInstanceState : extras;

    mGrouping = (Grouping) b.getSerializable(KEY_GROUPING);
    if (mGrouping == null) mGrouping = Grouping.NONE;
    mGroupingYear = b.getInt(KEY_YEAR);
    mGroupingSecond = b.getInt(KEY_SECOND_GROUP);
    getActivity().invalidateOptionsMenu();

    v = inflater.inflate(R.layout.distribution_list, container, false);
    ButterKnife.bind(this, v);
    textColorSecondary = ((ProtectedFragmentActivity) getActivity()).getTextColorSecondary().getDefaultColor();

    mChart.setVisibility(showChart ? View.VISIBLE : View.GONE);
    mChart.getDescription().setEnabled(false);
    mChart.setExtraOffsets(20,0,20,0);
    final SelectivePieChartRenderer renderer = new SelectivePieChartRenderer(mChart, new SelectivePieChartRenderer.Selector() {
      boolean lastValueGreaterThanOne = true;
      @Override
      public boolean shouldDrawEntry(int index, PieEntry pieEntry, float value) {
        final boolean greaterThanOne = value > 1f;
        final boolean shouldDraw = greaterThanOne || lastValueGreaterThanOne;
        lastValueGreaterThanOne = greaterThanOne;
        return shouldDraw;
      }
    });
    renderer.getPaintEntryLabels().setColor(textColorSecondary);
    renderer.getPaintEntryLabels().setTextSize(getTextSizeForAppearance(android.R.attr.textAppearanceSmall));
    mChart.setRenderer(renderer);

    mChart.setCenterTextSizePixels(getTextSizeForAppearance(android.R.attr.textAppearanceMedium));

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
    mAdapter = new CategoryTreeAdapter(ctx, currencyFormatter, mAccount.getCurrencyUnit(), showChart, showChart, false);
    mListView.setAdapter(mAdapter);
    loadData();
    mListView.setOnGroupClickListener((parent, v12, groupPosition, id) ->
    {
      if (showChart) {
        if (mAdapter.getChildrenCount(groupPosition) == 0) {
          long packedPosition = ExpandableListView.getPackedPositionForGroup(groupPosition);
          mListView.setItemChecked(mListView.getFlatListPosition(packedPosition), true);
          if (lastExpandedPosition != -1
              && groupPosition != lastExpandedPosition) {
            mListView.collapseGroup(lastExpandedPosition);
            lastExpandedPosition = -1;
          }
          if (lastExpandedPosition == -1) {
            highlight(groupPosition);
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

  private int getTextSizeForAppearance(int appearance) {
    TypedValue typedValue = new TypedValue();
    getActivity().getTheme().resolveAttribute(appearance, typedValue, true);
    int[] textSizeAttr = new int[]{android.R.attr.textSize};
    int indexOfAttrTextSize = 0;
    TypedArray a = getActivity().obtainStyledAttributes(typedValue.data, textSizeAttr);
    int textSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
    a.recycle();
    return textSize;
  }

  @Override
  protected Object getSecondarySort() {
    return "abs(" + KEY_SUM + ") DESC";
  }

  @Override
  protected void onLoadFinished() {
    super.onLoadFinished();
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

    SwitchCompat typeButton = menu.findItem(R.id.switchId).getActionView().findViewById(R.id.TaType);

    typeButton.setOnCheckedChangeListener((buttonView, isChecked) -> setType(isChecked));
  }

  public void setType(boolean isChecked) {
    isIncome = isChecked;
    reset();
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    if (mGrouping != null) {
      Utils.configureGroupingMenu(menu.findItem(R.id.GROUPING_COMMAND).getSubMenu(), mGrouping);
    }
    MenuItem m = menu.findItem(R.id.TOGGLE_CHART_COMMAND);
    if (m != null) {
      m.setChecked(showChart);
    }
    final MenuItem item = menu.findItem(R.id.switchId);
    Utils.menuItemSetEnabledAndVisible(item, !aggregateTypes);
    if (!aggregateTypes) {
      ((SwitchCompat) item.getActionView().findViewById(R.id.TaType)).setChecked(isIncome);
    }
    super.onPrepareOptionsMenu(menu);
  }

  @Override
  void updateIncomeAndExpense(long income, long expense) {
    updateSum("+", incomeSumTv, income);
    updateSum("-", expenseSumTv, expense);
  }

  private void updateSum(String prefix, TextView tv, long amount) {
    if (tv != null) {
      //noinspection SetTextI18n
      tv.setText(prefix + currencyFormatter.formatCurrency(
          new Money(mAccount.getCurrencyUnit(), amount)));
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
        mGroupingYear = thisYearOfWeekStart;
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
    if (super.onOptionsItemSelected(item)) return true;
    switch (item.getItemId()) {
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
    }
    return false;
  }

  @Override
  protected boolean showAllCategories() {
    return false;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (mAccount != null) {
      updateSum();
      updateDateInfo(true);
      ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
      ActionBar actionBar = ctx.getSupportActionBar();
      actionBar.setTitle(mAccount.getLabelForScreenTitle(getContext()));
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
    ds1.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
    ds1.setValueLinePart2Length(0.1f);
    ds1.setValueLineColor(textColorSecondary);


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
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_GROUPING, mGrouping);
    outState.putInt(KEY_YEAR, mGroupingYear);
    outState.putInt(KEY_SECOND_GROUP, mGroupingSecond);
  }

  @NonNull
  protected PrefKey getPrefKey() {
    return PrefKey.DISTRIBUTION_AGGREGATE_TYPES;
  }
}
