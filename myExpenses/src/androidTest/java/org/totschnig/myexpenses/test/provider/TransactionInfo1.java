package org.totschnig.myexpenses.test.provider;

import android.content.ContentValues;

import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.provider.DatabaseConstants;

// A utility for converting note data to a ContentValues map.
class TransactionInfo1 {
  private String comment;
  private long amount;
  private long date;
  private long payeeId;
  private long accountId;
  private long catId;
  private long methodId;

  /*
   * Constructor for a TransactionInfo instance. This class helps create a transaction and
   * return its values in a ContentValues map expected by data model methods.
   * The transaction's id is created automatically when it is inserted into the data model.
   */
  TransactionInfo1(String comment, long date, long amount, long accountId, long payeeId, long catId, long methodId) {
    this.comment = comment;
    this.date = date;
    this.amount = amount;
    this.payeeId = payeeId;
    this.accountId = accountId;
    this.catId = catId;
    this.methodId = methodId;
  }

  /*
   * Returns a ContentValues instance (a map) for this TransactionInfo instance. This is useful for
   * inserting a TransactionInfo into a database.
   */
  public ContentValues getContentValues() {
    // Gets a new ContentValues object
    ContentValues v = new ContentValues();

    // Adds map entries for the user-controlled fields in the map
    v.put(DatabaseConstants.KEY_COMMENT, comment);
    v.put(DatabaseConstants.KEY_DATE, date);
    v.put(DatabaseConstants.KEY_VALUE_DATE, date);
    v.put(DatabaseConstants.KEY_AMOUNT, amount);
    v.put(DatabaseConstants.KEY_PAYEEID, payeeId);
    v.put(DatabaseConstants.KEY_ACCOUNTID, accountId);
    v.put(DatabaseConstants.KEY_CR_STATUS, CrStatus.UNRECONCILED.name());
    v.put(DatabaseConstants.KEY_UUID, Model.generateUuid());
    v.put(DatabaseConstants.KEY_CATID, catId);
    v.put(DatabaseConstants.KEY_METHODID, methodId);
    return v;
  }
}