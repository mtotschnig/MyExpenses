package org.totschnig.myexpenses.viewmodel.data;

import android.content.ContentValues;

import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.BudgetType;
import org.totschnig.myexpenses.model.Money;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;

public class Budget {
  final BudgetType type;
  final Money amount;
  final long accountId;
  final Currency currency;
  final boolean isHomeAggregate;

  public Budget(long accountId, Currency currency, BudgetType type, Money amount, boolean isHomeAggregate) {
    this.accountId = accountId;
    this.currency = currency;
    this.type = type;
    this.amount = amount;
    this.isHomeAggregate = isHomeAggregate;
  }

  private final List<Category> categories = new ArrayList<>();

  public boolean isHomeAggregate() {
    return isHomeAggregate;
  }

  public boolean isAggregate() {
    return true;
  }

  public long getAccountId() {
    return accountId;
  }


  public BudgetType getType() {
    return type;
  }

  public Money getAmount() {
    return amount;
  }

  public Currency getCurrency() {
    return currency;
  }

  public String buildGroupingClause() {
    return "1";
  }

  public ContentValues toContentValues() {
    final ContentValues contentValues = new ContentValues();
    contentValues.put(KEY_TYPE, type.name());
    contentValues.put(KEY_AMOUNT, amount.getAmountMinor());
    if (accountId != 0) {
      contentValues.put(KEY_ACCOUNTID, accountId);
    } else {
      contentValues.put(KEY_CURRENCY, isHomeAggregate ?
          AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE : currency.getCurrencyCode());
    }
    return contentValues;
  }
}
