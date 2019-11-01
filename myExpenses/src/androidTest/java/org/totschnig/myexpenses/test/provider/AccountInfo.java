package org.totschnig.myexpenses.test.provider;

import android.content.ContentValues;

import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.provider.DatabaseConstants;

/**
 * A utility for converting account data to a ContentValues map.
 */
public class AccountInfo {
  private final String label;
  private final long openingBalance;
  private final AccountType type;
  private final String currency;
  private final Grouping grouping;

  AccountInfo(String label, AccountType type, long openingBalance) {
    this(label, type, openingBalance, "EUR");
  }

  AccountInfo(String label, AccountType type, long openingBalance, String currency) {
    this(label, type, openingBalance, currency, Grouping.NONE);
  }

  AccountInfo(String label, AccountType type, long openingBalance, String currency, Grouping grouping) {
    this.label = label;
    this.type = type;
    this.openingBalance = openingBalance;
    this.currency = currency;
    this.grouping = grouping;
  }

  public ContentValues getContentValues() {
    ContentValues v = new ContentValues();

    v.put(DatabaseConstants.KEY_LABEL, label);
    v.put(DatabaseConstants.KEY_DESCRIPTION, getDescription());
    v.put(DatabaseConstants.KEY_OPENING_BALANCE, openingBalance);
    v.put(DatabaseConstants.KEY_CURRENCY, currency);
    v.put(DatabaseConstants.KEY_TYPE, type.name());
    v.put(DatabaseConstants.KEY_GROUPING, grouping.name());
    return v;
  }

  String getDescription() {
    return "My account of type " + type.name();
  }


  public String getLabel() {
    return label;
  }

  long getOpeningBalance() {
    return openingBalance;
  }

  public AccountType getType() {
    return type;
  }

  public String getCurrency() {
    return currency;
  }

}