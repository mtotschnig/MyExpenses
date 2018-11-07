package org.totschnig.myexpenses.viewmodel.data;

import android.content.ContentValues;

import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_GROUPING;

public class Budget {
  final long id;
  final Grouping grouping;
  final Money amount;
  final long accountId;
  final Currency currency;
  final boolean isHomeAggregate;
  public final static Grouping[] BUDGET_TYPES =  {Grouping.YEAR, Grouping.MONTH, Grouping.WEEK, Grouping.DAY};

  public Budget(long id, long accountId, Currency currency, Grouping grouping, Money amount, boolean isHomeAggregate) {
    this.id = id;
    this.accountId = accountId;
    this.currency = currency;
    this.grouping = grouping;
    this.amount = amount;
    this.isHomeAggregate = isHomeAggregate;
  }

  private final List<Category> categories = new ArrayList<>();

  public long getId() {
    return id;
  }

  public boolean isHomeAggregate() {
    return isHomeAggregate;
  }

  public boolean isAggregate() {
    return accountId == 0;
  }

  public long getAccountId() {
    return accountId;
  }


  public Grouping getGrouping() {
    return grouping;
  }

  public Money getAmount() {
    return amount;
  }

  public Currency getCurrency() {
    return currency;
  }

  public ContentValues toContentValues() {
    final ContentValues contentValues = new ContentValues();
    contentValues.put(KEY_GROUPING, grouping.name());
    contentValues.put(KEY_BUDGET, amount.getAmountMinor());
    if (accountId > 0) {
      contentValues.put(KEY_ACCOUNTID, accountId);
    } else {
      contentValues.put(KEY_CURRENCY, isHomeAggregate ?
          AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE : currency.getCurrencyCode());
    }
    return contentValues;
  }
}
