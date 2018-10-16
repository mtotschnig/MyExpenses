package org.totschnig.myexpenses.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.squareup.sqlbrite3.QueryObservable;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.BudgetAdapter;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.viewmodel.data.Budget;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_COMMITTED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.VIEW_EXTENDED;
import static org.totschnig.myexpenses.provider.DatabaseConstants.WHERE_NOT_VOID;

public class BudgetFragment extends CategoryList {
  private Budget budget;

  @Override
  protected QueryObservable createQuery() {
    String selection, accountSelector = null, sortOrder;
    String[] selectionArgs, projection;
    String catFilter;
    String accountSelection, amountCalculation = KEY_AMOUNT, table = VIEW_COMMITTED;
    if (budget.isHomeAggregate()) {
      accountSelection = null;
      amountCalculation = DatabaseConstants.getAmountHomeEquivalent();
      table = VIEW_EXTENDED;
    } else if (budget.isAggregate()) {
      accountSelection = " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ? AND " +
          KEY_EXCLUDE_FROM_TOTALS + " = 0 )";
      accountSelector = budget.getCurrency().getCurrencyCode();
    } else {
      accountSelection = " = " + budget.getAccountId();
    }
    catFilter = "FROM " + table +
        " WHERE " + WHERE_NOT_VOID + (accountSelection == null ? "" : (" AND +" + KEY_ACCOUNTID + accountSelection));
    catFilter += " AND " + KEY_AMOUNT + " < 0";
    catFilter += " AND " + budget.buildGroupingClause();
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
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mListView = (ExpandableListView) inflater.inflate(R.layout.budget_list, container, false);
    return mListView;
  }

  public void setBudget(Budget budget) {
    this.budget = budget;
    final ProtectedFragmentActivity ctx = (ProtectedFragmentActivity) getActivity();
    mAdapter = new BudgetAdapter(ctx, currencyFormatter, budget.getCurrency(), true,
        true);
    mListView.setAdapter(mAdapter);
    loadData();
  }
}
