package org.totschnig.myexpenses.fragment;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.threeten.bp.temporal.JulianFields;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUP_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;

public class HistoryChart extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final int GROUPING_CURSOR = 1;
  private BarChart chart;
  private Account account;
  private Grouping grouping;
  private WhereFilter filter = WhereFilter.empty();
  private int columnIndexGroupYear;
  private int columnIndexGroupSecond;
  //julian day 0 is monday -> Only if week starts with monday it divides without remainder by 7
  //for the x axis we need an Integer for proper rendering, for printing the week range, we add the offset from monday
  private static int JULIAN_DAY_WEEK_OFFSET = DatabaseConstants.weekStartsOn == Calendar.SUNDAY ? 6 : DatabaseConstants.weekStartsOn - Calendar.MONDAY;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
    account = Account.getInstanceFromDb(Utils.getFromExtra(getActivity().getIntent().getExtras(), KEY_ACCOUNTID, 0));
    if (account == null) {
      return;
    }
    grouping = account.getGrouping() == Grouping.NONE ? Grouping.MONTH : account.getGrouping();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.history_chart, container, false);
    chart = view.findViewById(R.id.history_chart);
    XAxis xAxis = chart.getXAxis();
    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
    xAxis.setGranularity(1);
    xAxis.setValueFormatter((float value, AxisBase axis) -> {
      switch (grouping) {
        case DAY: {
          return LocalDateTime.MIN.with(JulianFields.JULIAN_DAY, (long) value)
              .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        }
        case WEEK: {
          long julianDay = (long) (value * 7) + JULIAN_DAY_WEEK_OFFSET;
          return LocalDateTime.MIN.with(JulianFields.JULIAN_DAY, julianDay)
              .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        }
        case MONTH:
          return Grouping.MONTH.getDisplayTitle(getContext(), (int) (value / 12), (int) (value % 12), null);
        case YEAR:
          return String.format(Locale.ROOT, "%d", (int) value);
      }
      return "";
    });
    getLoaderManager().initLoader(GROUPING_CURSOR, null, this);
    return view;
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    if (id == GROUPING_CURSOR) {
      String selection = null;
      String[] selectionArgs = null;
      Uri.Builder builder = TransactionProvider.TRANSACTIONS_URI.buildUpon();
      if (!filter.isEmpty()) {
        selection = filter.getSelectionForParts(DatabaseConstants.VIEW_EXTENDED);//GROUP query uses extended view
        if (!selection.equals("")) {
          selectionArgs = filter.getSelectionArgs(true);
        }
      }
      builder.appendPath(TransactionProvider.URI_SEGMENT_GROUPS)
          .appendPath(account.getGrouping().name());
      if (account.getId() < 0) {
        builder.appendQueryParameter(KEY_CURRENCY, account.currency.getCurrencyCode());
      } else {
        builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(account.getId()));
      }
      if (shouldUseGroupStart()) {
        builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_START, "1");
      }
      return new CursorLoader(getActivity(),
          builder.build(),
          null, selection, selectionArgs, null);
    }
    return null;
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
        return year * 12 + second;
      case YEAR:
        return year;
    }
    return 0;
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    ProtectedFragmentActivity context = ((ProtectedFragmentActivity) getActivity());
    if (context == null) {
      return;
    }
    if (cursor != null && cursor.moveToFirst()) {
      int columnIndexGroupSumIncome = cursor.getColumnIndex(KEY_SUM_INCOME);
      int columnIndexGroupSumExpense = cursor.getColumnIndex(KEY_SUM_EXPENSES);
      columnIndexGroupYear = cursor.getColumnIndex(KEY_YEAR);
      columnIndexGroupSecond = cursor.getColumnIndex(KEY_SECOND_GROUP);
      int columnIndexGroupStart = cursor.getColumnIndex(KEY_GROUP_START);

      ArrayList<BarEntry> entries = new ArrayList<>();
      XAxis xAxis = chart.getXAxis();

      do {
        long sumIncome = cursor.getLong(columnIndexGroupSumIncome);
        long sumExpense = cursor.getLong(columnIndexGroupSumExpense);
        int year = cursor.getInt(columnIndexGroupYear);
        int second = cursor.getInt(columnIndexGroupSecond);
        int groupStart = columnIndexGroupStart > -1 ? cursor.getInt(columnIndexGroupStart) : 0;
        int x = calculateX(year, second, groupStart);
        if (cursor.isFirst()) {
          xAxis.setAxisMinimum(x - 1);
        }
        entries.add(new BarEntry(x, new float[]{-sumExpense, sumIncome}));
        if (cursor.isLast()) {
          xAxis.setAxisMaximum(x + 1);
        }
      } while (cursor.moveToNext());

      BarDataSet set1 = new BarDataSet(entries, "");
      set1.setStackLabels(new String[]{getString(R.string.expense), getString(R.string.income)});
      set1.setColors(context.getColorExpense(), context.getColorIncome());
      set1.setValueTextColor(Color.rgb(60, 220, 78));
      set1.setValueTextSize(10f);
      set1.setAxisDependency(YAxis.AxisDependency.LEFT);

      float barWidth = 0.45f;

      BarData barData = new BarData(set1);
      barData.setBarWidth(barWidth);

      chart.setData(barData);
      chart.invalidate();
    } else {
      chart.clear();
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    chart.clear();
  }
}
