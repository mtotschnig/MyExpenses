package org.totschnig.myexpenses.test;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.Account;
import org.totschnig.myexpenses.ExpensesDbAdapter;

import junit.framework.Assert;
import android.test.AndroidTestCase;

public class AccountTest extends AndroidTestCase {
  public Account mAccount;
  private ExpensesDbAdapter mDbHelper;
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      mDbHelper = new ExpensesDbAdapter(getContext());
      mDbHelper.open();
      mAccount = new Account(mDbHelper,"TestAccount",100,"Testing with Junit",Currency.getInstance(Locale.getDefault()));
  }
  public void testAccount() {
    mAccount.setCurrency("EUR");
    Assert.assertEquals("EUR", mAccount.currency.getCurrencyCode());
    mAccount.save();
    Assert.assertTrue(mAccount.id > 0);
  }
  @Override
  protected void tearDown() throws Exception {
    mDbHelper.deleteAccount(mAccount.id);
  }
}
