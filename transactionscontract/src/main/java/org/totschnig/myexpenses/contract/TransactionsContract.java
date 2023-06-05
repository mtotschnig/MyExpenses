package org.totschnig.myexpenses.contract;

import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This contract defines the data needed for communicating with My Expenses via Intents. Currently
 * only adding new transactions via {@link android.content.Intent#ACTION_INSERT} is supported.
 */
public class TransactionsContract {

  public static final String AUTHORITY = "org.totschnig.myexpenses";

  public static final class Accounts implements BaseColumns {
    //Not yet implemented
  }

  /**
   * Example code:
   * <pre>{@code
   * Intent intent = new Intent(Intent.ACTION_INSERT);
   * intent.setData(TransactionsContract.Transactions.CONTENT_URI);
   * intent.putExtra(TransactionsContract.Transactions.AMOUNT_MICROS, 10500000L);
   * intent.putExtra(TransactionsContract.Transactions.PAYEE_NAME, "Aldi");
   * intent.putExtra(TransactionsContract.Transactions.CATEGORY_LABEL, "Food:Supermarket");
   * }</pre>
   */
  public static final class Transactions implements BaseColumns {

    @IntDef({TYPE_TRANSACTION, TYPE_TRANSFER, TYPE_SPLIT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TransactionType {}

    public static final int TYPE_TRANSACTION = 0;
    public static final int TYPE_TRANSFER = 1;
    /**
     * Building Split transactions is not yet implemented
     */
    public static final int TYPE_SPLIT = 2;

    /**
     * Debug build listens for "content://org.totschnig.myexpenses.debug/transactions"
     */
    public static final Uri CONTENT_URI = Uri.parse(
        "content://org.totschnig.myexpenses/transactions");

    /**
     * One of {@link #TYPE_TRANSACTION}, {@link #TYPE_TRANSFER}. {@link #TYPE_SPLIT} is not yet implemented.
     * If omitted, {@link #TYPE_TRANSACTION} is assumed.
     */
    public static final String OPERATION_TYPE = "operationType";

    /**
     * The label of the account this transaction should be added to. If no account is found with the
     * given label, it will not be created.
     * Type: TEXT
     */
    public static final String ACCOUNT_LABEL = "accountLabel";

    /**
     * The label of the transfer account for this transfer. If no account is found with the
     * given label, it will not be created. Ignored if {@link #OPERATION_TYPE} is not {@link #TYPE_TRANSFER}
     * Type: TEXT
     */
    public static final String TRANSFER_ACCOUNT_LABEL = "transferAccountLabel";


    /**
     * If a currency is passed, the transaction will be linked to an account that uses this currency.
     * This extra is ignored, if {@link #ACCOUNT_LABEL} is passed, that can be resolved to an existing account
     */
    public static final String CURRENCY = "currency";

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
     * The name of a person with whom this transaction was exchanged. If no payee exists with the given
     * name, it will not be inserted into DB, but form will be populated.
     * TYPE: TEXT
     */
    public static final String PAYEE_NAME = "payeeName";

    /**
     * The label for the category to which this transaction should be assigned. Main and subcategory
     * can be provided in the form "Main:Sub". Categories that do not exist yet, will be inserted.
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
     * instead of the localized labels which are used for displaying them. If no method is found with
     * the given label, it will not be inserted.
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
