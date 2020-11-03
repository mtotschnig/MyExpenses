package org.totschnig.myexpenses.provider;

import android.content.ContentValues;

import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.CrStatus;

public class TransactionInfo {

  String comment;
  long amount;
  long date;
  long payeeId;
  long accountId;

  /*
   * Constructor for a TransactionInfo instance. This class helps create a transaction and
   * return its values in a ContentValues map expected by data model methods.
   * The transaction's id is created automatically when it is inserted into the data model.
   */
  public TransactionInfo(String comment, long date, long amount, long accountId, long payeeId) {
    this.comment = comment;
    this.date = date;
    this.amount = amount;
    this.payeeId = payeeId;
    this.accountId = accountId;
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
    return v;
  }


  public String getComment() {
    return comment;
  }

  public long getAmount() {
    return amount;
  }

  public long getDate() {
    return date;
  }

  public long getPayeeId() {
    return payeeId;
  }

  public long getAccountId() {
    return accountId;
  }
}