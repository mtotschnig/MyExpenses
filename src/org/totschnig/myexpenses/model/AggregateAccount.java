package org.totschnig.myexpenses.model;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;

import android.database.Cursor;

public class AggregateAccount extends Account {
  final static String GROUPING_PREF_PREFIX = "AGGREGATE_GROUPING_";
  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  public AggregateAccount(Cursor c) {
    extract(c);
    try {
      this.grouping = Grouping.valueOf(MyApplication.getInstance().getSettings().getString(
          GROUPING_PREF_PREFIX + currency,"NONE"));
    } catch (IllegalArgumentException ex) {
      this.grouping = Grouping.NONE;
    }
    accounts.put(id, this);
  }
  public static AggregateAccount getCachedInstance (Long id) {
    return (AggregateAccount) accounts.get(id);
  }
  public void persistGrouping(Grouping value) {
    this.grouping = value;
    SharedPreferencesCompat.apply(MyApplication.getInstance().getSettings().edit()
        .putString(GROUPING_PREF_PREFIX + currency.getCurrencyCode(), value.name()));
  }
}
