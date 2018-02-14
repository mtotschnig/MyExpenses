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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.Utils;

import java.util.ArrayList;
import java.util.Locale;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_JULIAN_DAY_OF_GROUP_START;
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

  private int columnIndexGroupYear, columnIndexGroupSecond, columnIndexGroupJulianDay;

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
    xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);
    xAxis.setGranularity(1f);
    xAxis.setValueFormatter((value, axis) -> String.format(Locale.ROOT, "%f", value));
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
      if (grouping == Grouping.WEEK || grouping == Grouping.DAY) {
        builder.appendQueryParameter(TransactionProvider.QUERY_PARAMETER_WITH_JULIAN_DAY, "1");
      }
      return new CursorLoader(getActivity(),
          builder.build(),
          null, selection, selectionArgs, null);
    }
    return null;
  }

  private long calculateX(Cursor cursor) {
    switch (grouping) {
      case DAY:
        return cursor.getLong(columnIndexGroupJulianDay);
      case WEEK:
        return cursor.getLong(columnIndexGroupJulianDay) / 7;
      case MONTH:
        return cursor.getLong(columnIndexGroupYear) * 12 + cursor.getLong(columnIndexGroupSecond);
      case YEAR:
        return cursor.getLong(columnIndexGroupYear);
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
      columnIndexGroupJulianDay = cursor.getColumnIndex(KEY_JULIAN_DAY_OF_GROUP_START);

      ArrayList<BarEntry> entries = new ArrayList<>();
      XAxis xAxis = chart.getXAxis();
      xAxis.setAxisMinimum(calculateX(cursor) - 1);

      do {
        long sumIncome = cursor.getLong(columnIndexGroupSumIncome);
        long sumExpense = cursor.getLong(columnIndexGroupSumExpense);
        float x = calculateX(cursor);
        entries.add(new BarEntry(x, new float[]{-sumExpense, sumIncome}));
      } while (cursor.moveToNext());
      cursor.moveToLast();
      xAxis.setAxisMaximum(calculateX(cursor) + 1);

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

  }
}
