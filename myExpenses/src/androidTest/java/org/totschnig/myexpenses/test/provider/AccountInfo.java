package org.totschnig.myexpenses.test.provider;

import android.content.ContentValues;

import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.provider.DatabaseConstants;

/**
 * A utility for converting account data to a ContentValues map.
 */
public class AccountInfo {
  String label;
  long openingBalance;
  AccountType type;
  String currency;

  public AccountInfo(String label, AccountType type, long openingBalance) {
    this(label, type, openingBalance, "EUR");
  }

  public AccountInfo(String label, AccountType type, long openingBalance, String currency) {
    this.label = label;
    this.type = type;
    this.openingBalance = openingBalance;
    this.currency = currency;
  }

  public ContentValues getContentValues() {
    ContentValues v = new ContentValues();

    v.put(DatabaseConstants.KEY_LABEL, label);
    v.put(DatabaseConstants.KEY_DESCRIPTION, getDescription());
    v.put(DatabaseConstants.KEY_OPENING_BALANCE, openingBalance);
    v.put(DatabaseConstants.KEY_CURRENCY, currency);
    v.put(DatabaseConstants.KEY_TYPE, type.name());
    return v;
  }

  public String getDescription() {
    return "My account of type " + type.name();
  }


  public String getLabel() {
    return label;
  }

  public long getOpeningBalance() {
    return openingBalance;
  }

  public AccountType getType() {
    return type;
  }

  public String getCurrency() {
    return currency;
  }

}