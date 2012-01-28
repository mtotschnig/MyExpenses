package org.totschnig.myexpenses;

import android.database.Cursor;

public class Transfer extends Transaction {
  
  public Transfer(ExpensesDbAdapter mDbHelper) {
    super(mDbHelper);
  }

  public Transfer(ExpensesDbAdapter mDbHelper, long id, Cursor c) {
    super(mDbHelper,id,c);
  }

  public long save() {
    if (id == 0) {
      id = mDbHelper.createTransfer(dateAsString, amount, comment,cat_id,account_id);
    } else {
      mDbHelper.updateTransfer(id, dateAsString, amount, comment,cat_id);
    }
    return id;
  }
}
