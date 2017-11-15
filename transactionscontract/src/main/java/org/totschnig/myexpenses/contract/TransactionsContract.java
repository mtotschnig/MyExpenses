package org.totschnig.myexpenses.contract;

import android.net.Uri;
import android.provider.BaseColumns;

public class TransactionsContract {
  /**
   * This authority is used for writing to the transactions provider. Currently it is limited to
   * adding new transactions via {@link android.content.Intent#ACTION_INSERT}. Account label, payee
   * name, category label and payment method label can be provided, at the moment these are only
   * used if they are already defined in the database, otherwise they are ignored.
   */
  public static final String AUTHORITY = "org.totschnig.myexpenses";

  /**
   * Items that can be put into shopping lists.
   */
  public static final class Accounts implements BaseColumns {
    //Not yet implemented
  }

  public static final class Transactions implements BaseColumns {
    public static final int TYPE_TRANSACTION = 0;
    public static final int TYPE_TRANSFER = 1;
    public static final int TYPE_SPLIT = 2;

    public static final Uri CONTENT_URI = Uri
        .parse("content://org.totschnig.myexpenses/transactions");

    public static final String OPERATION_TYPE = "operationType";

    /**
     * The label of the account this transaction should be added to.
     * Type: TEXT
     */
    public static final String ACCOUNT_LABEL = "accountLabel";

    /**
     * The amount of the transaction (in micro-units, where 1,000,000 micro-units equal one unit of the currency.)
     * Type: INTEGER (long)
     */
    public static final String AMOUNT_MICROS = "amountMicros";

    /**
     * The timestamp of the date when the transaction was made (seconds since the epoch)
     * Type: INTEGER (long)
     */
    public static final String DATE = "date";

    /**
     * The name of a person with whom this transaction was exchanged
     * TYPE: TEXT
     */
    public static final String PAYEE_NAME = "payeeName";

    /**
     * The label for the category to which this transaction should be assigned. Main and subcategory
     * can be provide in the form "Main:Sub"
     * TYPE: TEXT
     */
    public static final String CATEGORY_LABEL = "categoryLabel";

    /**
     * A comment describing the transaction
     * TYPE: TEXT
     */
    public static final String COMMENT = "comment";

    /**
     * The label of the payment mehthod. My Expenses has the following methods defined by default:
     * CHEQUE, CREDITCARD, DEPOSIT, DIRECTDEBIT. But these could have been deleted by the user, who
     * also can define additional methods. For linking to the default methods, use above constants,
     * instead of the localized labels which are used for displaying them.
     * TYPE: TEXT
     */
    public static final String METHOD_LABEL = "methodLabel";

    /**
     * The number of the transaction, e.g. cheque number. This information is only taken into account,
     * if a payment method is used that is marked as numbered. By default, only CHEQUE is numbered.
     */
    public static final String REFERENCE_NUMBER = "referenceNumber";
  }
}
