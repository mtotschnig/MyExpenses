package org.totschnig.myexpenses.fragment;

import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.threeten.bp.temporal.JulianFields;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.ui.ExactStackedBarHighlighter;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.locale.UserLocaleProvider;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import icepick.Icepick;
import icepick.State;

import static org.totschnig.myexpenses.provider.DatabaseConstants.DAY_START_JULIAN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUP_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getMonth;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getWeekStartJulian;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart;

public class HistoryChart extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final int GROUPING_CURSOR = 1;
  private static final int MONTH_GROUPING_YEAR_X = 12;
  private CombinedChart chart;
  private Account account;
  @State
  Grouping grouping;
  private WhereFilter filter = WhereFilter.empty();
  //julian day 0 is monday -> Only if week starts with monday it divides without remainder by 7
  //for the x axis we need an Integer for proper rendering, for printing the week range, we add the offset from monday
  private static int JULIAN_DAY_WEEK_OFFSET = DatabaseConstants.weekStartsOn == Calendar.SUNDAY ? 6 : DatabaseConstants.weekStartsOn - Calendar.MONDAY;
  private float valueTextSize = 10f;
  @ColorInt
  private int textColor = Color.WHITE;

  @Inject
  CurrencyFormatter currencyFormatter;
  @Inject
  UserLocaleProvider userLocaleProvider;
  @State
  boolean showBalance = true;
  @State
  boolean includeTransfers = false;
  @State
  boolean showTotals = true;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getAppComponent().inject(this);

    setHasOptionsMenu(true);
    account = Account.getInstanceFromDb(Utils.getFromExtra(getActivity().getIntent().getExtras(), KEY_ACCOUNTID, 0));
    if (account == null) {
      return;
    }
    if (savedInstanceState == null) {
      grouping = account.getGrouping() == Grouping.NONE ? Grouping.MONTH : account.getGrouping();
    } else {
      Icepick.restoreInstanceState(this, savedInstanceState);
    }

    TypedValue typedValue = new TypedValue();
    getActivity().getTheme().resolveAttribute(android.R.attr.textAppearanceSmall, typedValue, true);
    int[] textSizeAttr = new int[]{android.R.attr.textSize};
    int indexOfAttrTextSize = 0;
    TypedArray a = getActivity().obtainStyledAttributes(typedValue.data, textSizeAttr);
    valueTextSize = a.getDimensionPixelSize(indexOfAttrTextSize, 10) / getResources().getDisplayMetrics().density;
    a.recycle();
    textColor = UiUtils.themeIntAttr(getContext(), R.attr.colorControlNormal);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    Icepick.saveInstanceState(this, outState);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(account.getLabelForScreenTitle(getContext()));
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    showBalance = PrefKey.HISTORY_SHOW_BALANCE.getBoolean(showBalance);
    includeTransfers = PrefKey.HISTORY_INCLUDE_TRANSFERS.getBoolean(includeTransfers);
    showTotals = PrefKey.HISTORY_SHOW_TOTALS.getBoolean(showTotals);
    View view = inflater.inflate(R.layout.history_chart, container, false);
    chart = view.findViewById(R.id.history_chart);
    chart.getDescription().setEnabled(false);
    XAxis xAxis = chart.getXAxis();
    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
    xAxis.setGranularity(1);
    xAxis.setValueFormatter(new ValueFormatter() {
      @Override
      public String getAxisLabel(float value, AxisBase axis) {
        return axis.getAxisMinimum() == value ? "" : formatXValue(value);
      }
    });
    xAxis.setTextColor(textColor);
    configureYAxis(chart.getAxisLeft());
    configureYAxis(chart.getAxisRight());
    chart.getLegend().setTextColor(textColor);
    chart.setHighlightPerDragEnabled(false);
    chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
      @Override
      public void onValueSelected(Entry e, Highlight h) {
        if (h.getStackIndex() > -1) {
          //expense is first entry, income second
          int type = h.getStackIndex() == 0 ? -1 : 1;
          TransactionListDialogFragment.newInstance(
              account.getId(), 0, false, grouping, buildGroupingClause((int) e.getX()), null, formatXValue(e.getX()), type, includeTransfers)
              .show(getFragmentManager(), TransactionListDialogFragment.class.getName());
        }
      }

      @Override
      public void onNothingSelected() {

      }
    });
    getLoaderManager().initLoader(GROUPING_CURSOR, null, this);
    return view;
  }

  protected String formatXValue(float value) {
    switch (grouping) {
      case DAY: {
        return LocalDateTime.MIN.with(JulianFields.JULIAN_DAY, (long) value)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
      }
      case WEEK: {
        long julianDay = julianDayFromWeekNumber(value);
        return LocalDateTime.MIN.with(JulianFields.JULIAN_DAY, julianDay)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
      }
      case MONTH:
        return Grouping.getDisplayTitleForMonth((int) (value / MONTH_GROUPING_YEAR_X), (int) (value % MONTH_GROUPING_YEAR_X), DateFormat.SHORT,
            userLocaleProvider.getUserPreferredLocale());
      case YEAR:
        return String.format(Locale.ROOT, "%d", (int) value);
    }
    return "";
  }

  private long julianDayFromWeekNumber(float value) {
    return (long) (value * 7) + JULIAN_DAY_WEEK_OFFSET;
  }

  private String buildGroupingClause(int x) {
    switch (grouping) {
      case DAY:
        return DAY_START_JULIAN + " = " + x;
      case WEEK:
        return getWeekStartJulian() + " = " + julianDayFromWeekNumber(x);
      case MONTH:
        return getYearOfMonthStart() + " = " + (x / MONTH_GROUPING_YEAR_X) + " AND " + getMonth() + " = " + (x % MONTH_GROUPING_YEAR_X);
      case YEAR:
        return YEAR + " = " + x;
    }
    return null;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.grouping, menu);
    inflater.inflate(R.menu.history, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    SubMenu subMenu = menu.findItem(R.id.GROUPING_COMMAND).getSubMenu();
    subMenu.findItem(R.id.GROUPING_NONE_COMMAND).setVisible(false);
    Utils.configureGroupingMenu(subMenu, grouping);
    MenuItem m = menu.findItem(R.id.TOGGLE_BALANCE_COMMAND);
    if (m != null) {
      m.setChecked(showBalance);
    }
    m = menu.findItem(R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND);
    if (m != null) {
      m.setChecked(includeTransfers);
    }
    m = menu.findItem(R.id.TOGGLE_TOTALS_COMMAND);
    if (m != null) {
      m.setChecked(showTotals);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (handleGrouping(item)) return true;
    switch (item.getItemId()) {
      case R.id.TOGGLE_BALANCE_COMMAND: {
        showBalance = !showBalance;
        PrefKey.HISTORY_SHOW_BALANCE.putBoolean(showBalance);
        reset();
        return true;
      }
      case R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND: {
        includeTransfers = !includeTransfers;
        PrefKey.HISTORY_INCLUDE_TRANSFERS.putBoolean(includeTransfers);
        reset();
        return true;
      }
      case R.id.TOGGLE_TOTALS_COMMAND: {
        showTotals = !showTotals;
        PrefKey.HISTORY_SHOW_TOTALS.putBoolean(showTotals);
        reset();
        return true;
      }
    }
    return false;
  }

  private void reset() {
    chart.clear();
    getLoaderManager().restartLoader(GROUPING_CURSOR, null, this);
  }

  private boolean handleGrouping(MenuItem item) {
    Grouping newGrouping = Utils.getGroupingFromMenuItemId(item.getItemId());
    if (newGrouping != null) {
      if (!item.isChecked()) {
        grouping = newGrouping;
        getActivity().invalidateOptionsMenu();
        reset();
      }
      return true;
    }
    return false;
  }

  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    if (id == GROUPING_CURSOR) {
      String selection = null;
      String[] selectionArgs = null;
      Uri.Builder builder = TransactionProvider.TRANSACTIONS_URI.buildUpon();
      //TODO enable filtering ?
      if (!filter.isEmpty()) {
        selection = filter.getSelectionForParts(DatabaseConstants.VIEW_EXTENDED);//GROUP query uses extended view
        if (!selection.equals("")) {
          selectionArgs = filter.getSelectionArgs(true);
        }
      }
      builder.appendPath(TransactionProvider.URI_SEGMENT_GROUPS)
          .appendPath(grouping.name());
      if (!account.isHomeAggregate()) {
        if (account.isAggregate()) {
          builder.appendQueryParameter(KEY_CURRENCY, account.getCurrencyUnit().code());
        } else {
          builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(account.getId()));
        }
      }
      if (shouldUseGroupStart()) {
        builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_START, "1");
      }
      if (includeTransfers) {
        builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_INCLUDE_TRANSFERS, "1");
      }
      return new CursorLoader(getActivity(),
          builder.build(),
          null, selection, selectionArgs, null);
    }
    throw new IllegalArgumentException();
  }

  protected boolean shouldUseGroupStart() {
    return grouping == Grouping.WEEK || grouping == Grouping.DAY;
  }

  private int calculateX(int year, int second, int groupStart) {
    switch (grouping) {
      case DAY:
        return groupStart;
      case WEEK:
        return groupStart / 7;
      case MONTH:
        return year * MONTH_GROUPING_YEAR_X + second;
      case YEAR:
        return year;
    }
    return 0;
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
    ProtectedFragmentActivity context = ((ProtectedFragmentActivity) getActivity());
    if (context == null) {
      return;
    }
    if (cursor != null && cursor.moveToFirst()) {
      int columnIndexGroupSumIncome = cursor.getColumnIndex(KEY_SUM_INCOME);
      int columnIndexGroupSumExpense = cursor.getColumnIndex(KEY_SUM_EXPENSES);
      int columnIndexGroupSumTransfer = cursor.getColumnIndex(KEY_SUM_TRANSFERS);
      int columnIndexGroupYear = cursor.getColumnIndex(KEY_YEAR);
      int columnIndexGroupSecond = cursor.getColumnIndex(KEY_SECOND_GROUP);
      int columnIndexGroupStart = cursor.getColumnIndex(KEY_GROUP_START);

      ArrayList<BarEntry> barEntries = new ArrayList<>();
      ArrayList<Entry> lineEntries = new ArrayList<>();
      XAxis xAxis = chart.getXAxis();

      long previousBalance = account.openingBalance.getAmountMinor();
      long interimBalance = 0L;

      do {
        long sumIncome = cursor.getLong(columnIndexGroupSumIncome);
        long sumExpense = cursor.getLong(columnIndexGroupSumExpense);
        long sumTransfer = columnIndexGroupSumTransfer > -1 ? cursor.getLong(columnIndexGroupSumTransfer) : 0;
        long delta = sumIncome + sumExpense + sumTransfer;
        if (showBalance) interimBalance = previousBalance + delta;
        int year = cursor.getInt(columnIndexGroupYear);
        int second = cursor.getInt(columnIndexGroupSecond);
        int groupStart = columnIndexGroupStart > -1 ? cursor.getInt(columnIndexGroupStart) : 0;
        int x = calculateX(year, second, groupStart);
        if (cursor.isFirst()) {
          int start = x - 1;
          xAxis.setAxisMinimum(start);
          if (showBalance) lineEntries.add(new Entry(start, previousBalance));
        }
        barEntries.add(new BarEntry(x, new float[]{sumExpense, sumIncome}));
        if (showBalance) {
          lineEntries.add(new Entry(x, interimBalance));
          previousBalance = interimBalance;
        }
        if (cursor.isLast()) {
          xAxis.setAxisMaximum(x + 1);
        }
      } while (cursor.moveToNext());

      CombinedData data = new CombinedData();

      ValueFormatter valueFormatter = new ValueFormatter() {
        @Override
        public String getFormattedValue(float value) {
          return  convAmount(value);
        }
      };

      BarDataSet set1 = new BarDataSet(barEntries, "");
      set1.setStackLabels(new String[]{
          getString(R.string.history_chart_out_label),
          getString(R.string.history_chart_in_label)});
      List<Integer> colors = Arrays.asList(context.getColorExpense(), context.getColorIncome());
      set1.setColors(colors);
      set1.setValueTextColors(colors);
      set1.setValuesUseBarColor(true);
      set1.setValueTextSize(valueTextSize);
      set1.setDrawValues(showTotals);
      set1.setValueFormatter(valueFormatter);
      float barWidth = 0.45f;
      BarData barData = new BarData(set1);
      barData.setBarWidth(barWidth);
      data.setData(barData);

      if (showBalance) {
        LineDataSet set2 = new LineDataSet(lineEntries, getString(R.string.current_balance));
        set2.setValueTextSize(valueTextSize);
        set2.setLineWidth(2.5f);
        int balanceColor = getResources().getColor(R.color.emphasis);
        set2.setColor(balanceColor);
        set2.setValueTextColor(textColor);
        set2.setValueFormatter(valueFormatter);
        set2.setDrawValues(showTotals);
        LineData lineData = new LineData(set2);
        data.setData(lineData);
      }

      chart.setData(data);
      chart.setHighlighter(new ExactStackedBarHighlighter.CombinedHighlighter(chart));
      chart.invalidate();
    } else {
      chart.clear();
    }
  }

  private void configureYAxis(YAxis yAxis) {
    yAxis.setTextColor(textColor);
    yAxis.setValueFormatter(new ValueFormatter() {
      @Override
      public String getAxisLabel(float value, AxisBase axis) {
        return convAmount(value);
      }
    });
  }

  private String convAmount(float value) {
    return currencyFormatter.convAmount((long) value, account.getCurrencyUnit());
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    chart.clear();
  }
}
