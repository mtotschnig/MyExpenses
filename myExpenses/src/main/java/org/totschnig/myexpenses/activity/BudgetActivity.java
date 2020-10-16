package org.totschnig.myexpenses.activity;

import android.content.Intent;
import android.view.MenuItem;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.BudgetFragment;

import androidx.annotation.NonNull;

import static org.totschnig.myexpenses.ConstantsKt.ACTION_MANAGE;

public class BudgetActivity extends CategoryActivity<BudgetFragment> {

  public static final String ACTION_BUDGET = "ACTION_BUDGET";

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.MANAGE_CATEGORIES_COMMAND) {
      Intent i = new Intent(this, ManageCategories.class);
      i.setAction(ACTION_MANAGE);
      startActivity(i);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @NonNull
  @Override
  public String getAction() {
    return ACTION_BUDGET;
  }

  @Override
  protected int getContentView() {
    return R.layout.activity_budget;
  }

  public static int getBackgroundForAvailable(boolean onBudget) {
    return onBudget ? R.drawable.round_background_income : R.drawable.round_background_expense;
  }
}
