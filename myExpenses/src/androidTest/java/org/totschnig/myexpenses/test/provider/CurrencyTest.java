package org.totschnig.myexpenses.test.provider;

import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.testutils.BaseDbTest;

public class CurrencyTest extends BaseDbTest {

  private final CurrencyInfo TEST_CURRENCY = new CurrencyInfo("Bitcoin", "BTC");
  private final AccountInfo TEST_ACCOUNT = new AccountInfo("Account 0", AccountType.CASH, 0, TEST_CURRENCY.code);

  public void testShouldNotDeleteFrameworkCurrency() {
    try {
      getMockContentResolver().delete(TransactionProvider.CURRENCIES_URI.buildUpon().appendPath("EUR").build(), null, null);
      fail("Expected deletion of framework currency to fail");
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }
  }

  public void testShouldDeleteUnUsedCurrency() {
    mDb.insertOrThrow(
        DatabaseConstants.TABLE_CURRENCIES,
        null,
        TEST_CURRENCY.getContentValues()
    );
    int result = getMockContentResolver().delete(TransactionProvider.CURRENCIES_URI.buildUpon().appendPath(TEST_CURRENCY.code).build(), null, null);
    assertEquals(1, result);
  }

  public void testShouldNotDeleteUsedCurrency() {
    mDb.insertOrThrow(
        DatabaseConstants.TABLE_CURRENCIES,
        null,
        TEST_CURRENCY.getContentValues()
    );
    mDb.insertOrThrow(
        DatabaseConstants.TABLE_ACCOUNTS,
        null,
        TEST_ACCOUNT.getContentValues()
    );
    int result = getMockContentResolver().delete(TransactionProvider.CURRENCIES_URI.buildUpon().appendPath(TEST_CURRENCY.code).build(), null, null);
    assertEquals(0, result);
  }
}
