package org.totschnig.myexpenses.model;

import android.database.Cursor;
import android.util.Log;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;

public class AggregateAccount extends Account {
  final static String GROUPING_PREF_PREFIX = "AGGREGATE_GROUPING_";

  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  public AggregateAccount(Cursor c) {
    extract(c);
    try {
      this.grouping = Grouping.valueOf(MyApplication.getInstance().getSettings().getString(
          GROUPING_PREF_PREFIX + currency, "NONE"));
    } catch (IllegalArgumentException ex) {
      this.grouping = Grouping.NONE;
    }
    accounts.put(getId(), this);
  }

  public static AggregateAccount getInstanceFromDb(long id) {
    if (BuildConfig.DEBUG && !(id < 0)) {
      throw new AssertionError();
    }
    AggregateAccount aa = (AggregateAccount) accounts.get(id);
    if (aa != null) {
      return aa;
    }
    Log.w(MyApplication.TAG, "did not find Aggregate Account in cache, will construct it from DB");
    Cursor c = cr().query(
        TransactionProvider.ACCOUNTS_AGGREGATE_URI.buildUpon().appendPath(String.valueOf(0 - id)).build(),
        null, null, null, null);
    if (c == null) {
      //reportNull(id);
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      //reportNull(id);
      return null;
    }
    c.moveToFirst();
    aa = new AggregateAccount(c);
    c.close();
    return aa;
  }

  @Override
  public void persistGrouping(Grouping value) {
    this.grouping = value;
    MyApplication.getInstance().getSettings().edit()
        .putString(GROUPING_PREF_PREFIX + currency.getCurrencyCode(), value.name())
        .apply();
    cr().notifyChange(TransactionProvider.ACCOUNTS_URI, null, false);
  }
}
