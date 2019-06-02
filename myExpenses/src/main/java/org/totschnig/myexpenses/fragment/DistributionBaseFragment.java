package org.totschnig.myexpenses.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.squareup.sqlbrite3.QueryObservable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static org.totschnig.myexpenses.provider.DatabaseConstants.DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAX_VALUE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MIN_VALUE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_MONTH;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_WEEK;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
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

public abstract class DistributionBaseFragment extends CategoryList {
  protected Grouping mGrouping;
  protected boolean isIncome = false;
  int mGroupingYear;
  int mGroupingSecond;
  int thisYear;
  int thisYearOfWeekStart;
  int thisMonth;
  int thisWeek;
  int thisDay;
  int maxValue;
  int minValue;
  boolean aggregateTypes;
  private Disposable dateInfoDisposable;
  private Disposable sumDisposable;
  protected Account mAccount;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    aggregateTypes = getPrefKey().getBoolean(true);
  }

  protected void setupAccount() {
    mAccount = Account.getInstanceFromDb(getActivity().getIntent().getLongExtra(KEY_ACCOUNTID, 0));
  }

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
      int maxYearToLookUp = mGroupingSecond <= 1 ? mGroupingYear - 1 : mGroupingYear;
      switch (mGrouping) {
        case DAY:
          projectionList.add(String.format(Locale.US, "strftime('%%j','%d-12-31') AS " + KEY_MAX_VALUE, maxYearToLookUp));
          break;
        case WEEK:
          projectionList.add(DbUtils.maximumWeekExpression(maxYearToLookUp));
          projectionList.add(DbUtils.minimumWeekExpression(mGroupingSecond > 1 ? mGroupingYear + 1 : mGroupingYear));
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
        projectionList.toArray(new String[0]),
        null, null, null, false)
        .subscribe(query -> {
          final Cursor cursor = query.run();
          if (cursor != null) {
            if (getActivity() != null) {
              getActivity().runOnUiThread(() -> {
                try {
                  cursor.moveToFirst();
                  thisYear = cursor.getInt(cursor.getColumnIndex(KEY_THIS_YEAR));
                  thisYearOfWeekStart = cursor.getInt(cursor.getColumnIndex(KEY_THIS_YEAR_OF_WEEK_START));
                  thisMonth = cursor.getInt(cursor.getColumnIndex(KEY_THIS_MONTH));
                  thisWeek = cursor.getInt(cursor.getColumnIndex(KEY_THIS_WEEK));
                  thisDay = cursor.getInt(cursor.getColumnIndex(KEY_THIS_DAY));
                  if (withMaxValue) {
                    maxValue = cursor.getInt(cursor.getColumnIndex(KEY_MAX_VALUE));
                    switch (mGrouping) {
                      case WEEK:
                        minValue = cursor.getInt(cursor.getColumnIndex(KEY_MIN_VALUE));
                        break;
                      case MONTH:
                        minValue = 0;
                        break;
                      default:
                        minValue = 1;
                    }
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

  protected View errorView() {
    TextView tv = new TextView(getContext());
    //noinspection SetTextI18n
    tv.setText("Error loading budget for account");
    return tv;
  }

  protected void onDateInfoReceived(Cursor cursor) {
    final ProtectedFragmentActivity activity = (ProtectedFragmentActivity) getActivity();
    if (activity != null) {
      final ActionBar actionBar = activity.getSupportActionBar();
      if (actionBar != null) {
        actionBar.setSubtitle(getSubTitle(cursor));
      }
    }
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
        builder.appendQueryParameter(KEY_CURRENCY, mAccount.getCurrencyUnit().code());
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

  abstract void updateIncome(long amount);
  abstract void updateExpense(long amount);

  @Override
  protected void configureMenuInternal(Menu menu, boolean hasChildren) {
    menu.findItem(R.id.EDIT_COMMAND).setVisible(false);
    menu.findItem(R.id.DELETE_COMMAND).setVisible(false);
    menu.findItem(R.id.SELECT_COMMAND).setTitle(R.string.menu_show_transactions);
    menu.findItem(R.id.SELECT_COMMAND_MULTIPLE).setVisible(false);
    menu.findItem(R.id.CREATE_COMMAND).setVisible(false);
    menu.findItem(R.id.MOVE_COMMAND).setVisible(false);
  }

  @Override
  protected void doSelection(long cat_id, String label, String icon, boolean isMain) {
    TransactionListDialogFragment.newInstance(
        mAccount.getId(), cat_id, isMain, mGrouping, buildGroupingClause(), label, 0, true)
        .show(getFragmentManager(), TransactionListDialogFragment.class.getName());
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
      case R.id.TOGGLE_AGGREGATE_TYPES:
        aggregateTypes = !aggregateTypes;
        getPrefKey().putBoolean(aggregateTypes);
        getActivity().invalidateOptionsMenu();
        reset();
        return true;
    }
    return false;
  }

  protected abstract PrefKey getPrefKey();

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
  public void reset() {
    super.reset();
    updateSum();
    updateDateInfo(true);
  }

  @Override
  protected QueryObservable createQuery() {
    String accountSelector = null;
    String[] selectionArgs;
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
      accountSelector = mAccount.getCurrencyUnit().code();
    } else {
      accountSelection = " = " + mAccount.getId();
    }
    catFilter = "FROM " + table +
        " WHERE " + WHERE_NOT_VOID + (accountSelection == null ? "" : (" AND +" + KEY_ACCOUNTID + accountSelection));
    if (!aggregateTypes) {
      catFilter += " AND " + KEY_AMOUNT + (isIncome ? ">" : "<") + "0";
    }
    if (!mGrouping.equals(Grouping.NONE)) {
      catFilter += " AND " + buildGroupingClause();
    }
    //we need to include transactions mapped to children for main categories
    catFilter += " AND " + CATTREE_WHERE_CLAUSE;
    String extraColumn = getExtraColumn();
    String[] projection = new String[extraColumn == null ? 6 : 7];
    projection[0] = KEY_ROWID;
    projection[1] = KEY_PARENTID;
    projection[2] = KEY_LABEL;
    projection[3] = KEY_COLOR;
    projection[4] = "(SELECT sum(" + amountCalculation + ") " + catFilter + ") AS " + KEY_SUM;
    projection[5] = KEY_ICON;
    if (extraColumn != null) {
      projection[6] = extraColumn;
    }
    selectionArgs = accountSelector != null ? new String[]{accountSelector, accountSelector} : null;
    return briteContentResolver.createQuery(getCategoriesUri(),
        projection, showAllCategories() ? null : " exists (SELECT 1 " + catFilter + ")", selectionArgs, getSortExpression(), true);
  }

  protected Uri getCategoriesUri() {
    return TransactionProvider.CATEGORIES_URI;
  }

  protected String getExtraColumn() {
    return null;
  }

  protected abstract boolean showAllCategories();

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    MenuItem m = menu.findItem(R.id.TOGGLE_AGGREGATE_TYPES);
    if (m != null) {
      m.setChecked(aggregateTypes);
    }
  }
}
