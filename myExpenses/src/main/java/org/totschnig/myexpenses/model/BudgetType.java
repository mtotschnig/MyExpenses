package org.totschnig.myexpenses.model;

import org.totschnig.myexpenses.util.TextUtils;

public enum BudgetType {
  YEARLY, MONTHLY, WEEKLY, CUSTOM;
  public static final String JOIN;

  static {
    JOIN = TextUtils.joinEnum(BudgetType.class);
  }
}
