package org.totschnig.myexpenses.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.support.v4.util.Pair;

import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static org.totschnig.myexpenses.provider.DatabaseConstants.DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_MONTH;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_WEEK;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
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
  private Disposable sumDisposable;
  protected Account mAccount;

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
        getSubTitle(cursor));
  }

  protected String getSubTitle(Cursor cursor) {
    return mGrouping.getDisplayTitle(getActivity(), mGroupingYear, mGroupingSecond, cursor);
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

  protected void disposeSum() {
    if (sumDisposable != null && !sumDisposable.isDisposed()) {
      sumDisposable.dispose();
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    disposeSum();
    disposeDateInfo();
  }


  protected void updateSum() {
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
            if (pair.first > 0) {
              updateIncome(pair.second);
            } else {
              updateExpense(pair.second);
            }
          }
          if (!seen[1]) updateIncome(0L);
          if (!seen[0]) updateExpense(0L);
        });
  }

  abstract void updateIncome(Long amount);
  abstract void updateExpense(Long amount);
}
