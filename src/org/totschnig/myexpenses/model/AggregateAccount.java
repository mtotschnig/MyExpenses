package org.totschnig.myexpenses.model;

import java.util.HashMap;

import android.database.Cursor;

public class AggregateAccount extends Account {
  static HashMap<String,AggregateAccount> accounts = new HashMap<String,AggregateAccount>();
  long id = 0;
  Type type = null;
  Grouping grouping = Grouping.WEEK;
  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  public AggregateAccount(String currency,Cursor c) {
    extract(c);
    accounts.put(currency, this);
  }
  public static AggregateAccount getCachedInstance (String currency) {
    return accounts.get(currency);
  }
}
