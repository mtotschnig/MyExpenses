package org.totschnig.myexpenses.model;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.provider.TransactionProvider;

import android.database.Cursor;
import android.util.Log;

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
  public static AggregateAccount getInstanceFromDB (Long id) {
    assert id < 0;
    AggregateAccount aa = (AggregateAccount) accounts.get(id);
    if (aa != null) {
      return aa;
    }
    Log.w(MyApplication.TAG, "did not find Aggregate Account in cache, will construct it from DB");
    Cursor c = cr().query(
        TransactionProvider.ACCOUNTS_AGGREGATE_URI.buildUpon().appendPath(String.valueOf(0-id)).build(),
        null,null,null, null);
    if (c == null) {
      reportNull();
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      reportNull();
      return null;
    }
    c.moveToFirst();
    aa = new AggregateAccount(c);
    c.close();
    return aa;
  }
  public void persistGrouping(Grouping value) {
    this.grouping = value;
    SharedPreferencesCompat.apply(MyApplication.getInstance().getSettings().edit()
        .putString(GROUPING_PREF_PREFIX + currency.getCurrencyCode(), value.name()));
  }
}
