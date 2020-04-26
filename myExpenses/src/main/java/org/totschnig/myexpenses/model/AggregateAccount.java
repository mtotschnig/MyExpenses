package org.totschnig.myexpenses.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.provider.TransactionProvider;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;

public class AggregateAccount extends Account {
  public static final int AGGREGATE_HOME = 2;
  public static final String AGGREGATE_HOME_CURRENCY_CODE = "___";
  public final static String GROUPING_AGGREGATE = "AGGREGATE_GROUPING____";
  private final static String SORT_DIRECTION_PREF_PREFIX = "AGGREGATE_SORT_DIRECTION_";

  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  AggregateAccount(Cursor c) {
    extract(c);
    if (isHomeAggregate()) {
      try {
        this.setGrouping(Grouping.valueOf(MyApplication.getInstance().getSettings().getString(
            GROUPING_AGGREGATE, "NONE")));
      } catch (IllegalArgumentException ignored) {
      }
    }
    try {
      this.setSortDirection(SortDirection.valueOf(MyApplication.getInstance().getSettings().getString(
          SORT_DIRECTION_PREF_PREFIX + getKeyForPreference(), "DESC")));
    } catch (IllegalArgumentException ignored) {
    }
  }

  public static AggregateAccount getInstanceFromDb(long id) {
    if (BuildConfig.DEBUG && !(id < 0)) {
      throw new AssertionError();
    }
    Cursor c = cr().query(
        TransactionProvider.ACCOUNTS_AGGREGATE_URI.buildUpon().appendPath(String.valueOf(id)).build(),
        null, null, null, null);
    if (c == null) {
      return null;
    }
    if (c.getCount() == 0) {
      c.close();
      return null;
    }
    c.moveToFirst();
    AggregateAccount aa = new AggregateAccount(c);
    c.close();
    return aa;
  }

  @Override
  public void persistGrouping(Grouping value) {
    if (isHomeAggregate()) {
      this.setGrouping(value);
      MyApplication.getInstance().getSettings().edit()
          .putString(GROUPING_AGGREGATE, value.name())
          .apply();
      cr().notifyChange(TransactionProvider.ACCOUNTS_URI, null, false);
    } else {
      super.persistGrouping(value);
    }
  }

  @Override
  public void persistSortDirection(SortDirection value) {
    this.setSortDirection(value);
    MyApplication.getInstance().getSettings().edit()
        .putString(SORT_DIRECTION_PREF_PREFIX + getKeyForPreference(), value.name())
        .apply();
    cr().notifyChange(TransactionProvider.ACCOUNTS_URI, null, false);
  }

  @Override
  public Uri getExtendedUriForTransactionList(boolean withType) {
    final Uri base = super.getExtendedUriForTransactionList(withType);
    return withType ? base : base.buildUpon().appendQueryParameter(
        TransactionProvider.QUERY_PARAMETER_MERGE_TRANSFERS, "1")
        .build();
  }

  @Override
  public String getLabelForScreenTitle(Context context) {
    return isHomeAggregate() ? context.getString(R.string.grand_total) : super.getLabelForScreenTitle(context);
  }

  @Override
  public String[] getExtendedProjectionForTransactionList() {
    return isHomeAggregate() ? Transaction.PROJECTON_EXTENDED_HOME : Transaction.PROJECTION_EXTENDED_AGGREGATE;
  }

  @Override
  public String getSelectionForTransactionList() {
    if (isHomeAggregate()) {
      return KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_EXCLUDE_FROM_TOTALS + " = 0)";
    } else {
      return KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ? AND " +
          KEY_EXCLUDE_FROM_TOTALS + " = 0)";
    }
  }

  @Override
  public String[] getSelectionArgsForTransactionList() {
    if (isHomeAggregate()) {
      return null;
    } else {
      return new String[]{getCurrencyUnit().code()};
    }
  }

  @Override
  public Uri.Builder getGroupingUri(Grouping grouping) {
    Uri.Builder base = getGroupingBaseUri(grouping);
    if (!isHomeAggregate()) {
      base.appendQueryParameter(KEY_CURRENCY, getCurrencyUnit().code());
    }
    return base;
  }

  private String getKeyForPreference() {
    if (isHomeAggregate()) {
      return AGGREGATE_HOME_CURRENCY_CODE;
    } else {
      return getCurrencyUnit().code();
    }
  }
}
