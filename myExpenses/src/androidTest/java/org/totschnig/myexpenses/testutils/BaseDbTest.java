package org.totschnig.myexpenses.testutils;

import android.database.sqlite.SQLiteDatabase;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import org.totschnig.myexpenses.provider.TransactionProvider;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_CATEGORIES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_PAYEES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_TEMPLATES;

public class BaseDbTest extends ProviderTestCase2<TransactionProvider> {
  // Contains an SQLite database, used as test data
  protected SQLiteDatabase mDb;
  // Contains a reference to the mocked content resolver for the provider under test.
  protected MockContentResolver mMockResolver;

  public BaseDbTest() {
    super(TransactionProvider.class, TransactionProvider.AUTHORITY);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mMockResolver = getMockContentResolver();
    mDb = getProvider().getOpenHelperForTest().getWritableDatabase();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    mDb.delete(TABLE_ACCOUNTS, null, null);
    mDb.delete(TABLE_PAYEES, null, null);
    mDb.delete(TABLE_CATEGORIES, KEY_ROWID + " != ?", new String[]{String.valueOf(SPLIT_CATID)});
    mDb.delete(TABLE_TEMPLATES, null, null);
  }
}
