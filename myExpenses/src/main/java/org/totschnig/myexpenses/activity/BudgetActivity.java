package org.totschnig.myexpenses.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.BudgetFragment;
import org.totschnig.myexpenses.util.UiUtils;

import androidx.annotation.NonNull;

import static org.totschnig.myexpenses.activity.ManageCategories.ACTION_MANAGE;

public class BudgetActivity extends CategoryActivity<BudgetFragment> {

  public static final String ACTION_BUDGET = "ACTION_BUDGET";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(getThemeId());
    super.onCreate(savedInstanceState);
  }

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

  public static int getBackgroundForAvailable(boolean onBudget, Context context) {
    boolean lightTheme = UiUtils.themeBoolAttr(context, R.attr.isLightTheme);
    return onBudget ?
        (lightTheme ? R.drawable.round_background_income_light : R.drawable.round_background_income_dark) :
        (lightTheme ? R.drawable.round_background_expense_light : R.drawable.round_background_expense_dark);
  }
}
