package org.totschnig.myexpenses.model;

import org.totschnig.myexpenses.R;

/**
 * grouping of accounts in account list
 */
public enum AccountGrouping {
  NONE(R.id.GROUPING_ACCOUNTS_NONE_COMMAND), TYPE(R.id.GROUPING_ACCOUNTS_TYPE_COMMAND), CURRENCY(R.id.GROUPING_ACCOUNTS_CURRENCY_COMMAND);


  public final int commandId;

  AccountGrouping(int commandId) {
    this.commandId = commandId;
  }
}
