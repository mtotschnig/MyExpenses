package org.totschnig.myexpenses.viewmodel.data;

import org.totschnig.myexpenses.model.BudgetType;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public class Budget {
  public Currency currency = Currency.getInstance("EUR");
  String label;

  public Budget(String label, BudgetType type, Long sum) {
    this.label = label;
    this.type = type;
    this.sum = sum;
  }

  BudgetType type;
  Long sum;
  private final List<Category> categories = new ArrayList<>();

  public boolean isHomeAggregate() {
    return false;
  }

  public boolean isAggregate() {
    return true;
  }

  public long getId() {
    return 1;
  }

  public String buildGroupingClause() {
    return "1";
  }
}
