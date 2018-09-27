package org.totschnig.myexpenses.viewmodel.data;

import org.totschnig.myexpenses.model.BudgetType;

import java.util.ArrayList;
import java.util.List;

public class Budget {
  String label;

  public Budget(String label, BudgetType type, Long sum) {
    this.label = label;
    this.type = type;
    this.sum = sum;
  }

  BudgetType type;
  Long sum;
  private final List<Category> categories = new ArrayList<>();

}
