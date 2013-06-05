package org.totschnig.myexpenses.test.provider;

import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import android.content.ContentValues;

/**
 * A utility for converting account data to a ContentValues map.
 *
 */
class AccountInfo {
    String label;
    long openingBalance;
    Type type;
    String currency;
    /**
     * Constructor for a AccountInfo instance. This class helps create an account and
     * return its values in a ContentValues map expected by data model methods.
     * The account's id is created automatically when it is inserted into the data model.
     */
    public AccountInfo(String label, Type type, long openingBalance) {
      this.label = label;
      this.type = type;
      this.openingBalance = openingBalance;
      this.currency = "EUR";
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
        v.put(DatabaseConstants.KEY_TYPE,type.name());
        return v;
    }
    public String getDescription() {
      return "My account of type " + type.name();
    }
}