package org.totschnig.myexpenses.provider;

import android.content.ContentValues;

import org.totschnig.myexpenses.model.AccountType;

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

  /*
   * Returns a ContentValues instance (a map) for this NoteInfo instance. This is useful for
   * inserting a NoteInfo into a database.
   */
  public ContentValues getContentValues() {
    // Gets a new ContentValues object
    ContentValues v = new ContentValues();

    // Adds map entries for the user-controlled fields in the map
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