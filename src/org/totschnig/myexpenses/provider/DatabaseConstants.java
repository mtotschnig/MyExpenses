package org.totschnig.myexpenses.provider;

public class DatabaseConstants {
  public static final String KEY_DATE = "date";
  public static final String KEY_AMOUNT = "amount";
  public static final String KEY_COMMENT = "comment";
  public static final String KEY_ROWID = "_id";
  public static final String KEY_CATID = "cat_id";
  public static final String KEY_ACCOUNTID = "account_id";
  public static final String KEY_PAYEE = "payee";
  public static final String KEY_TRANSFER_PEER = "transfer_peer";
  public static final String KEY_METHODID = "method_id";
  public static final String KEY_TITLE = "title";
  public static final String KEY_LABEL_MAIN = "label_main";
  public static final String KEY_LABEL_SUB = "label_sub";
  public static final String KEY_LABEL = "label";
  public static final String KEY_COLOR = "color";
  public static final String KEY_TYPE = "type";
  public static final String KEY_CURRENCY = "currency";
  public static final String KEY_DESCRIPTION = "description";
  public static final String KEY_OPENING_BALANCE = "opening_balance";
  public static final String KEY_USAGES = "usages";
  public static final String KEY_PARENTID = "parent_id";
  public static final String KEY_TRANSFER_ACCOUNT = "transfer_account";
  public static final String KEY_STATUS = "status";
  public static final String KEY_TRANSACTIONID = "transaction_id";
  /**
   * transaction that already has been exported
   */
  public static final int STATUS_EXPORTED = 1;
  /**
   * split transaction (and its parts) that are currently edited
   */
  public static final int STATUS_UNCOMMITTED = 2;

  public static final String TABLE_TRANSACTIONS = "transactions";
  public static final String TABLE_ACCOUNTS = "accounts";
  public static final String TABLE_CATEGORIES = "categories";
  public static final String TABLE_METHODS = "paymentmethods";
  public static final String TABLE_ACCOUNTTYES_METHODS = "accounttype_paymentmethod";
  public static final String TABLE_TEMPLATES = "templates";
  public static final String TABLE_PAYEES = "payee";
  public static final String TABLE_FEATURE_USED = "feature_used";
  public static final String VIEW_COMMITTED = "committed";
  public static final String VIEW_UNCOMMITTED = "uncommitted";

  /**
   * an SQL CASE expression for transactions
   * that gives either the category for normal transactions
   * or the account for transfers
   */
  public static final String LABEL_MAIN =
    "CASE WHEN " +
    "  transfer_peer " +
    "THEN " +
    "  (SELECT label FROM " + TABLE_ACCOUNTS + " WHERE _id = transfer_account) " +
    "WHEN " +
    "  cat_id " +
    "THEN " +
    "  CASE WHEN " +
    "    (SELECT parent_id FROM " + TABLE_CATEGORIES + " WHERE _id = cat_id) " +
    "  THEN " +
    "    (SELECT label FROM " + TABLE_CATEGORIES
        + " WHERE _id = (SELECT parent_id FROM " + TABLE_CATEGORIES
            + " WHERE _id = cat_id)) " +
    "  ELSE " +
    "    (SELECT label FROM " + TABLE_CATEGORIES + " WHERE _id = cat_id) " +
    "  END " +
    "END AS " + KEY_LABEL_MAIN;
 public static final String LABEL_SUB =
    "CASE WHEN " +
    "  transfer_peer is null AND cat_id AND (SELECT parent_id FROM " + TABLE_CATEGORIES
        + " WHERE _id = cat_id) " +
    "THEN " +
    "  (SELECT label FROM " + TABLE_CATEGORIES + " WHERE _id = cat_id) " +
    "END AS " + KEY_LABEL_SUB;
  /**
   * same as {@link FULL_LABEL}, but if transaction is linked to a subcategory
   * only the label from the subcategory is returned
   */
  public static final String SHORT_LABEL = 
    "CASE WHEN " +
    "  transfer_peer " +
    "THEN " +
    "  (SELECT label FROM " + TABLE_ACCOUNTS + " WHERE _id = transfer_account) " +
    "ELSE " +
    "  (SELECT label FROM " + TABLE_CATEGORIES + " WHERE _id = cat_id) " +
    "END AS  " + KEY_LABEL;
  public static final Long SPLIT_CATID = -1L;

}
