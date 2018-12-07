package org.totschnig.myexpenses.provider.filter;

import android.content.Context;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.DatabaseConstants;

public class NullCriteria extends Criteria {
  public NullCriteria(String columnName) {
    super(columnName, WhereFilter.Operation.ISNULL);
  }

  @Override
  public String prettyPrint(Context context) {
    return String.format("%s: %s", columnName2Label(context), context.getString(R.string.unmapped));
  }

  private String columnName2Label(Context context) {
    switch (columnName) {
      case DatabaseConstants.KEY_CATID: return context.getString(R.string.category);
      case DatabaseConstants.KEY_PAYEEID: return context.getString(R.string.payer_or_payee);
      case DatabaseConstants.KEY_METHODID: return context.getString(R.string.method);
    }
    return columnName;
  }

  @Override
  public String toStringExtra() {
    return "null";
  }
}
