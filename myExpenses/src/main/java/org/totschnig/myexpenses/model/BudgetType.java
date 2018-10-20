package org.totschnig.myexpenses.model;

import android.content.Context;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.TextUtils;

import io.reactivex.annotations.NonNull;

public enum BudgetType {
  YEARLY, MONTHLY, WEEKLY, DAILY;
  public static final String JOIN;

  static {
    JOIN = TextUtils.joinEnum(BudgetType.class);
  }

  @NonNull
  public String getLabel(Context context) {
    switch (this) {
      case DAILY:
        return context.getString(R.string.daily_plain);
      case WEEKLY:
        return context.getString(R.string.weekly_plain);
      case MONTHLY:
        return context.getString(R.string.monthly);
      case YEARLY:
        return context.getString(R.string.yearly_plain);
    }
    throw new IllegalStateException();
  }

  public Grouping toGrouping() {
    switch (this) {
      case YEARLY:
        return Grouping.YEAR;
      case MONTHLY:
        return Grouping.MONTH;
      case WEEKLY:
        return Grouping.WEEK;
      case DAILY:
        return Grouping.DAY;
    }
    throw new IllegalArgumentException();
  }

  public static BudgetType fromGrouping(Grouping grouping) {
    switch (grouping) {
      case DAY:
        return DAILY;
      case WEEK:
        return WEEKLY;
      case MONTH:
        return MONTHLY;
      case YEAR:
        return YEARLY;
    }
    throw new IllegalArgumentException();
  }
}
