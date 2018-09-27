package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel;

public class BudgetActivity extends CategoryActivity {

  public static final String ACTION_BUDGET = "ACTION_BUDGET";
  private BudgetViewModel budgetViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_category);
    setupToolbar(true);
  }

  @NonNull
  @Override
  public String getAction() {
    return ACTION_BUDGET;
  }

  @Override
  protected int getContentView() {
    return 0;
  }
}
