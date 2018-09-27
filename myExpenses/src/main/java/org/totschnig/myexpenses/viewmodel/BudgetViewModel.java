package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import org.totschnig.myexpenses.model.BudgetType;
import org.totschnig.myexpenses.viewmodel.data.Budget;

public class BudgetViewModel extends AndroidViewModel {
  private final MutableLiveData<Budget> budget = new MutableLiveData<>();
  public BudgetViewModel(@NonNull Application application) {
    super(application);
    budget.setValue(createDebugBudget());
  }

  private Budget createDebugBudget() {
    Budget budget = new Budget("DEBUG", BudgetType.MONTHLY, 50000L);
    return budget;
  }
}
