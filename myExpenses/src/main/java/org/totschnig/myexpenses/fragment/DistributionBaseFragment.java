package org.totschnig.myexpenses.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.squareup.sqlbrite3.QueryObservable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.dialog.TransactionListDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.locale.UserLocaleProvider;
import org.totschnig.myexpenses.viewmodel.data.DateInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.util.Pair;
import androidx.viewbinding.ViewBinding;
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
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_MONTH_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_THIS_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.THIS_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.THIS_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_WITH_ACCOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_VOID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getMonth;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisMonth;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisWeek;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfMonthStart;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getThisYearOfWeekStart;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getWeek;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfMonthStart;
import static org.totschnig.myexpenses.provider.DatabaseConstants.getYearOfWeekStart;

public abstract class DistributionBaseFragment<ROWBINDING extends ViewBinding> extends AbstractCategoryList<ROWBINDING> {
  protected Grouping mGrouping;
  protected boolean isIncome = false;
  int mGroupingYear;
  int mGroupingSecond;
  DateInfo dateInfo;
  boolean aggregateTypes;
  private Disposable dateInfoDisposable;
  private Disposable sumDisposable;
  private AccountInfo accountInfo;

  @Inject
  UserLocaleProvider userLocaleProvider;

  interface AccountInfo {
    long getId();

    CurrencyUnit getCurrencyUnit();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    aggregateTypes = prefHandler.getBoolean(getPrefKey(), true);
  }

  protected void disposeDateInfo() {
    if (dateInfoDisposable != null && !dateInfoDisposable.isDisposed()) {
      dateInfoDisposable.dispose();
    }
  }

  protected void setAccountInfo(AccountInfo accountInfo) {
    this.accountInfo = accountInfo;
  }

  protected void updateDateInfo(boolean withMaxValue) {
    disposeDateInfo();
    ArrayList<String> projectionList = new ArrayList<>(Arrays.asList(
        getThisYearOfWeekStart() + " AS " + KEY_THIS_YEAR_OF_WEEK_START,
        getThisYearOfMonthStart() + " AS " + KEY_THIS_YEAR_OF_MONTH_START,
        THIS_YEAR + " AS " + KEY_THIS_YEAR,
        getThisMonth() + " AS " + KEY_THIS_MONTH,
        getThisWeek() + " AS " + KEY_THIS_WEEK,
        THIS_DAY + " AS " + KEY_THIS_DAY));
    if (withMaxValue) {
      //if we are at the beginning of the year we are interested in the max of the previous year
      int maxYearToLookUp = mGroupingSecond <= 1 ? mGroupingYear - 1 : mGroupingYear;
      String maxValueExpression = "0"; //default year
      String minValueExpression = "1";
      switch (mGrouping) {
        case DAY:
          maxValueExpression = String.format(Locale.US, "strftime('%%j','%d-12-31')", maxYearToLookUp);
          break;
        case WEEK:
          maxValueExpression = DbUtils.maximumWeekExpression(maxYearToLookUp);
          minValueExpression = DbUtils.minimumWeekExpression(mGroupingSecond > 1 ? mGroupingYear + 1 : mGroupingYear);
          break;
        case MONTH:
          maxValueExpression = "11";
          minValueExpression = "0";
          break;
      }
      projectionList.add(maxValueExpression + " AS " + KEY_MAX_VALUE);
      projectionList.add(minValueExpression + " AS " + KEY_MIN_VALUE);
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
        .mapToOne(DateInfo::fromCursor)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(dateInfo -> {
          this.dateInfo = dateInfo;
          onDateInfoReceived();
        });
  }

  protected View errorView() {
    TextView tv = new TextView(getContext());
    //noinspection SetTextI18n
    tv.setText("Error loading budget for account");
    return tv;
  }

  protected void onDateInfoReceived() {
    setSubTitle(mGrouping.getDisplayTitle(getActivity(), mGroupingYear, mGroupingSecond, dateInfo, userLocaleProvider.getUserPreferredLocale()));
  }

  protected void setSubTitle(CharSequence title) {
    final ProtectedFragmentActivity activity = (ProtectedFragmentActivity) getActivity();
    if (activity != null) {
      final ActionBar actionBar = activity.getSupportActionBar();
      if (actionBar != null) {
        actionBar.setSubtitle(title);
      }
    }
  }

  protected String buildFilterClause(String tableName) {
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
    Uri.Builder builder = TransactionProvider.TRANSACTIONS_SUM_URI.buildUpon()
        .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_GROUPED_BY_TYPE, "1");
    long id = accountInfo.getId();
    if (id != Account.HOME_AGGREGATE_ID) {
      if (id < 0) {
        builder.appendQueryParameter(KEY_CURRENCY, accountInfo.getCurrencyUnit().getCode());
      } else {
        builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(id));
      }
    }
    //if we have no income or expense, there is no row in the cursor
    sumDisposable = briteContentResolver.createQuery(builder.build(),
        null,
        buildFilterClause(VIEW_WITH_ACCOUNT),
        filterSelectionArgs(),
        null, true)
        .mapToList(cursor -> {
          int type = cursor.getInt(cursor.getColumnIndex(KEY_TYPE));
          long sum = cursor.getLong(cursor.getColumnIndex(KEY_SUM));
          return Pair.create(type, sum);
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(pairs -> {
          long income = 0, expense = 0;
          for (Pair<Integer, Long> pair : pairs) {
            if (pair.first > 0) {
              income = pair.second;
            } else {
              expense = pair.second;
            }
          }
          updateIncomeAndExpense(income, expense);
        });
  }

  protected String[] filterSelectionArgs() {
    return null;
  }

  abstract void updateIncomeAndExpense(long income, long expense);

  @Override
  protected boolean hasSelectSingle() {
    return true;
  }

  @Override
  protected boolean hasSelectMultiple() {
    return false;
  }

  @Override
  protected void configureMenuInternal(Menu menu, boolean hasChildren) {
    menu.findItem(R.id.EDIT_COMMAND).setVisible(false);
    menu.findItem(R.id.DELETE_COMMAND).setVisible(false);
    menu.findItem(R.id.SELECT_ALL_COMMAND).setVisible(false);
    menu.findItem(R.id.SELECT_COMMAND).setTitle(R.string.menu_show_transactions);
    menu.findItem(R.id.CREATE_SUB_COMMAND).setVisible(false);
    menu.findItem(R.id.MOVE_COMMAND).setVisible(false);
  }

  @Override
  protected void doSingleSelection(long cat_id, String label, String icon, boolean isMain) {
    TransactionListDialogFragment.newInstance(
        accountInfo.getId(), cat_id, isMain, mGrouping, buildFilterClause(VIEW_EXTENDED), filterSelectionArgs(), label, 0, true)
        .show(getParentFragmentManager(), TransactionListDialogFragment.class.getName());
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.BACK_COMMAND) {
      back();
      return true;
    } else if (itemId == R.id.FORWARD_COMMAND) {
      forward();
      return true;
    } else if (itemId == R.id.TOGGLE_AGGREGATE_TYPES) {
      aggregateTypes = !aggregateTypes;
      prefHandler.putBoolean(getPrefKey(), aggregateTypes);
      getActivity().invalidateOptionsMenu();
      reset();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  protected abstract PrefKey getPrefKey();

  public void back() {
    if (mGrouping.equals(Grouping.YEAR))
      mGroupingYear--;
    else {
      mGroupingSecond--;
      if (mGroupingSecond < dateInfo.getMinValue()) {
        mGroupingYear--;
        mGroupingSecond = dateInfo.getMaxValue();
      }
    }
    reset();
  }

  public void forward() {
    if (mGrouping.equals(Grouping.YEAR))
      mGroupingYear++;
    else {
      mGroupingSecond++;
      if (mGroupingSecond > dateInfo.getMaxValue()) {
        mGroupingYear++;
        mGroupingSecond = dateInfo.getMinValue();
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
    long id = accountInfo.getId();
    if (id == Account.HOME_AGGREGATE_ID) {
      accountSelection = null;
      amountCalculation = DatabaseConstants.getAmountHomeEquivalent(VIEW_WITH_ACCOUNT);
      table = VIEW_WITH_ACCOUNT;
    } else if (id < 0) {
      accountSelection = " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ? AND " +
          KEY_EXCLUDE_FROM_TOTALS + " = 0 )";
      accountSelector = accountInfo.getCurrencyUnit().getCode();
    } else {
      accountSelection = " = " + id;
    }
    catFilter = "FROM " + table +
        " WHERE " + WHERE_NOT_VOID + (accountSelection == null ? "" : (" AND +" + KEY_ACCOUNTID + accountSelection));
    if (!aggregateTypes) {
      catFilter += " AND " + KEY_AMOUNT + (isIncome ? ">" : "<") + "0";
    }
    final String dateFilter = buildFilterClause(table);
    if (dateFilter != null) {
      catFilter += " AND " + dateFilter;
    }
    //we need to include transactions mapped to children for main categories
    catFilter += " AND " + CAT_TREE_WHERE_CLAUSE;
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
    final boolean showAllCategories = showAllCategories();
    selectionArgs = Utils.joinArrays(accountSelector != null ?
        (showAllCategories ? new String[]{accountSelector} : new String[]{accountSelector, accountSelector})
        : null, filterSelectionArgs());
    return briteContentResolver.createQuery(getCategoriesUri(),
        projection, showAllCategories ? null : " exists (SELECT 1 " + catFilter + ")", selectionArgs, getSortExpression(), true);
  }

  protected Uri getCategoriesUri() {
    return TransactionProvider.CATEGORIES_URI;
  }

  protected String getExtraColumn() {
    return null;
  }

  protected abstract boolean showAllCategories();

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    MenuItem m = menu.findItem(R.id.TOGGLE_AGGREGATE_TYPES);
    if (m != null) {
      m.setChecked(aggregateTypes);
    }
    if (mGrouping != null) {
      boolean grouped = !mGrouping.equals(Grouping.NONE);
      Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.FORWARD_COMMAND), grouped);
      Utils.menuItemSetEnabledAndVisible(menu.findItem(R.id.BACK_COMMAND), grouped);
    }
  }
}
