package org.totschnig.myexpenses.model;

import android.database.Cursor;
import android.net.Uri;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.provider.TransactionProvider;

import timber.log.Timber;

public class AggregateAccount extends Account {
  final static String GROUPING_PREF_PREFIX = "AGGREGATE_GROUPING_";
  final static String SORT_DIRECTION_PREF_PREFIX = "AGGREGATE_SORT_DIRECTION_";

  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  public AggregateAccount(Cursor c) {
    extract(c);
    try {
      this.setGrouping(Grouping.valueOf(MyApplication.getInstance().getSettings().getString(
          GROUPING_PREF_PREFIX + currency, "NONE")));
    } catch (IllegalArgumentException ignored) {}
    try {
      this.setSortDirection(SortDirection.valueOf(MyApplication.getInstance().getSettings().getString(
          SORT_DIRECTION_PREF_PREFIX + currency, "DESC")));
    } catch (IllegalArgumentException ignored) {}
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
    Timber.w("did not find Aggregate Account in cache, will construct it from DB");
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
    this.setGrouping(value);
    MyApplication.getInstance().getSettings().edit()
        .putString(GROUPING_PREF_PREFIX + currency.getCurrencyCode(), value.name())
        .apply();
    cr().notifyChange(TransactionProvider.ACCOUNTS_URI, null, false);
  }

  @Override
  public void persistSortDirection(SortDirection value) {
    this.setSortDirection(value);
    MyApplication.getInstance().getSettings().edit()
        .putString(SORT_DIRECTION_PREF_PREFIX + currency.getCurrencyCode(), value.name())
        .apply();
    cr().notifyChange(TransactionProvider.ACCOUNTS_URI, null, false);
  }

  @Override
  public Uri getExtendedUriForTransactionList() {
    return super.getExtendedUriForTransactionList().buildUpon().appendQueryParameter(
        TransactionProvider.QUERY_PARAMETER_MERGE_TRANSFERS, "1")
        .build();
  }

  @Override
  public String[] getExtendedProjectionForTransactionList() {
    return getId() == Integer.MIN_VALUE ? Transaction.PROJECTON_EXTENDED_HOME : Transaction.PROJECTION_EXTENDED_AGGREGATE;
  }
}
