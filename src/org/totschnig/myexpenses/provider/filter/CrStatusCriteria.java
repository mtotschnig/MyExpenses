package org.totschnig.myexpenses.provider.filter;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.provider.DatabaseConstants;

public class CrStatusCriteria extends Criteria {
  private int searchIndex;
  public CrStatusCriteria(int searchIndex) {
    super(DatabaseConstants.KEY_CR_STATUS, WhereFilter.Operation.EQ,
        CrStatus.values()[searchIndex].name());
    this.searchIndex = searchIndex;
    this.title = MyApplication.getInstance().getString(R.string.status);
  }
  @Override
  public String prettyPrint() {
    return prettyPrintInternal(CrStatus.values()[searchIndex].toString());
  }
}
