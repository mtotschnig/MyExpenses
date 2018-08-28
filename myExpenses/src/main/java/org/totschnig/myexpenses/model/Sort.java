package org.totschnig.myexpenses.model;

import org.totschnig.myexpenses.R;

public enum Sort {
  USAGES(R.id.SORT_USAGES_COMMAND), LAST_USED(R.id.SORT_LAST_USED_COMMAND),
  AMOUNT(R.id.SORT_AMOUNT_COMMAND), TITLE(R.id.SORT_TITLE_COMMAND),
  CUSTOM(R.id.SORT_CUSTOM_COMMAND), NEXT_INSTANCE(R.id.SORT_NEXT_INSTANCE_COMMAND);


  public final int commandId;

  Sort(int commandId) {
    this.commandId = commandId;
  }

  public static Sort fromCommandId(int id) {
    for (Sort sort : values()) {
      if ((sort.commandId == id))
        return sort;
    }
    return null;
  }
}
