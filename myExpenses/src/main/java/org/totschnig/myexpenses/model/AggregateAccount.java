package org.totschnig.myexpenses.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;

import androidx.annotation.NonNull;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;

public class AggregateAccount extends Account {
  public static final int AGGREGATE_HOME = 2;
  public static final String AGGREGATE_HOME_CURRENCY_CODE = "___";
  public final static String GROUPING_AGGREGATE = "AGGREGATE_GROUPING____";
  public final static String SORT_DIRECTION_AGGREGATE = "AGGREGATE_SORT_DIRECTION____";

  /**
   * @param c Cursor positioned at the row we want to extract into the object
   */
  AggregateAccount(Cursor c) {
    super(c);
    if (isHomeAggregate()) {
      try {
        //TODO refactor
        this.setGrouping(Grouping.valueOf(MyApplication.getInstance().getSettings().getString(
            GROUPING_AGGREGATE, Grouping.NONE.name())));
      } catch (IllegalArgumentException ignored) {
      }
    }
    try {
      //TODO refactor
      this.setSortDirection(SortDirection.valueOf(MyApplication.getInstance().getSettings().getString(
            SORT_DIRECTION_AGGREGATE, SortDirection.DESC.name())));
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Override
  public Uri getExtendedUriForTransactionList(boolean withType, boolean shortenComment) {
    final Uri base = super.getExtendedUriForTransactionList(withType, shortenComment);
    return withType ? base : base.buildUpon().appendQueryParameter(
        TransactionProvider.QUERY_PARAMETER_MERGE_TRANSFERS, isHomeAggregate() ? "2" : "1")
        .build();
  }

  @Override
  public String getLabelForScreenTitle(Context context) {
    return isHomeAggregate() ? context.getString(R.string.grand_total) : super.getLabelForScreenTitle(context);
  }

  @Override
  public String[] getExtendedProjectionForTransactionList() {
    return isHomeAggregate() ? DatabaseConstants.getProjectionExtendedHome() : DatabaseConstants.getProjectionExtendedAggregate();
  }

  public static void persistGroupingHomeAggregate(@NonNull PrefHandler prefHandler, @NonNull Grouping grouping) {
    prefHandler.putString(GROUPING_AGGREGATE, grouping.name());
  }

  public static void persistSortDirectionHomeAggregate(@NonNull PrefHandler prefHandler, @NonNull SortDirection value) {
    prefHandler.putString(SORT_DIRECTION_AGGREGATE + AGGREGATE_HOME_CURRENCY_CODE, value.name());
  }

}
