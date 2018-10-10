package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.BudgetFragment;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel;

public class Budget extends CategoryActivity<BudgetFragment> {

  public static final String ACTION_BUDGET = "ACTION_BUDGET";
  private BudgetViewModel budgetViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
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
}
