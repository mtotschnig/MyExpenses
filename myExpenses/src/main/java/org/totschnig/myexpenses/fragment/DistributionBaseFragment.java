package org.totschnig.myexpenses.fragment;

import android.database.Cursor;

import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import io.reactivex.disposables.Disposable;

import static org.totschnig.myexpenses.provider.DatabaseConstants.DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_MONTH;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_WEEK;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.THIS_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.THIS_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getMonth;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisMonth;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisWeek;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfWeekStart;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getWeek;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart;

public abstract class DistributionBaseFragment extends CategoryList {
  protected Grouping mGrouping;
  int mGroupingYear;
  int mGroupingSecond;
  int thisYear;
  int thisMonth;
  int thisWeek;
  int thisDay;
  int maxValue;
  int minValue;
  private Disposable dateInfoDisposable;

  protected void disposeDateInfo() {
    if (dateInfoDisposable != null && !dateInfoDisposable.isDisposed()) {
      dateInfoDisposable.dispose();
    }
  }

  protected void updateDateInfo(boolean withMaxValue) {
    disposeDateInfo();
    ArrayList<String> projectionList = new ArrayList<>(Arrays.asList(
        getThisYearOfWeekStart() + " AS " + KEY_THIS_YEAR_OF_WEEK_START,
        THIS_YEAR + " AS " + KEY_THIS_YEAR,
        getThisMonth() + " AS " + KEY_THIS_MONTH,
        getThisWeek() + " AS " + KEY_THIS_WEEK,
        THIS_DAY + " AS " + KEY_THIS_DAY));
    if (withMaxValue) {
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
                  if (withMaxValue) {
                    maxValue = cursor.getInt(cursor.getColumnIndex(KEY_MAX_VALUE));
                    minValue = mGrouping == Grouping.MONTH ? 0 : 1;
                  }

                  onDateInfoReceived(cursor);
                } finally {
                  cursor.close();
                }
              });
            }
          }
        });
  }

  protected void onDateInfoReceived(Cursor cursor) {
    ((ProtectedFragmentActivity) getActivity()).getSupportActionBar().setSubtitle(
        mGrouping.getDisplayTitle(getActivity(), mGroupingYear, mGroupingSecond, cursor));
  }

  protected String buildGroupingClause() {
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
}
