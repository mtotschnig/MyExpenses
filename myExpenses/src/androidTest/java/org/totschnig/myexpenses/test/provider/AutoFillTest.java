package org.totschnig.myexpenses.test.provider;

import android.content.ContentUris;
import android.database.Cursor;

import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.testutils.BaseDbTest;

import static org.totschnig.myexpenses.provider.DatabaseConstants.CAT_AS_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;

public class AutoFillTest extends BaseDbTest {

  // Contains the test data, as an array of TransactionInfo instances.
  private TransactionInfo1[] TEST_TRANSACTIONS = new TransactionInfo1[3];
  private String PAYEE_NAME = "N.N", PAYEE_NAME1 = "y.y";
  long testAccountId;
  long payeeId, payeeId1;
  long catId, catId1;
  long methodChequeId, methodCreditCardId;

  /**
   * Sets up test data.
   * The test data is in an SQL database. It is created in setUp() without any data,
   * and populated in insertData if necessary.
   */
  private void insertData() {

    long now = System.currentTimeMillis() / 1000;
    TEST_TRANSACTIONS[0] = new TransactionInfo1("Transaction 0", now, 0, testAccountId, payeeId, catId1, methodCreditCardId);
    TEST_TRANSACTIONS[1] = new TransactionInfo1("Transaction 1", now, 200, testAccountId, payeeId, catId, methodChequeId); //this is the transaction we should retrieve
    TEST_TRANSACTIONS[2] = new TransactionInfo1("Transaction 2", now, -100, testAccountId, payeeId1, catId, methodCreditCardId);

    // Sets up test data
    for (TransactionInfo1 TEST_TRANSACTION : TEST_TRANSACTIONS) {

      mDb.insertOrThrow(
          DatabaseConstants.TABLE_TRANSACTIONS,
          null,
          TEST_TRANSACTION.getContentValues()
      );
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    AccountInfo testAccount = new AccountInfo("Test account", AccountType.CASH, 0, "USD");
    testAccountId = mDb.insertOrThrow(DatabaseConstants.TABLE_ACCOUNTS, null, testAccount.getContentValues());
    payeeId = mDb.insertOrThrow(DatabaseConstants.TABLE_PAYEES, null, new PayeeInfo(PAYEE_NAME).getContentValues());
    catId = mDb.insertOrThrow(DatabaseConstants.TABLE_CATEGORIES, null,  new CategoryInfo("Main", null).getContentValues());
    methodChequeId = PaymentMethod.find(PaymentMethod.PreDefined.CHEQUE.name());
    methodCreditCardId = PaymentMethod.find(PaymentMethod.PreDefined.CREDITCARD.name());

    payeeId1 = mDb.insertOrThrow(DatabaseConstants.TABLE_PAYEES, null, new PayeeInfo(PAYEE_NAME1).getContentValues());
    catId1 = mDb.insertOrThrow(DatabaseConstants.TABLE_CATEGORIES, null,  new CategoryInfo("Main 1", null).getContentValues());

    insertData();
  }

  public void testAutoLoadData() {
    final String[] projection = new String[]{KEY_CURRENCY, KEY_AMOUNT, KEY_CATID, CAT_AS_LABEL,
        KEY_COMMENT, KEY_ACCOUNTID, KEY_METHODID};
    Cursor cursor = getMockContentResolver().query(
        ContentUris.withAppendedId(TransactionProvider.AUTOFILL_URI, payeeId),
        projection,
        null,
        null,
        null);
    assert cursor != null;

    assertEquals(1, cursor.getCount());

    cursor.moveToFirst();

    assertEquals("USD", cursor.getString(0));
    assertEquals(200, cursor.getLong(1));
    assertEquals(catId, cursor.getLong(2));
    assertEquals("Main", cursor.getString(3));
    assertEquals("Transaction 1", cursor.getString(4));
    assertEquals(testAccountId, cursor.getLong(5));
    assertEquals(methodChequeId, cursor.getLong(6));

    cursor.close();
  }
}
