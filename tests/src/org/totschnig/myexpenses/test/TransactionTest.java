package org.totschnig.myexpenses.test;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.Account;
import org.totschnig.myexpenses.ExpensesDbAdapter;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.MyExpenses;
import org.totschnig.myexpenses.Transaction;
import org.totschnig.myexpenses.Transfer;

import junit.framework.Assert;
import android.test.AndroidTestCase;

public class TransactionTest extends AndroidTestCase {
  private Account mAccount1;
  private Account mAccount2;
  private ExpensesDbAdapter mDbHelper;
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      mDbHelper = new ExpensesDbAdapter((MyApplication) getContext().getApplicationContext());
      mDbHelper.open();
      mAccount1 = new Account(mDbHelper,"TestAccount 1",100,"Main account",Currency.getInstance(Locale.getDefault()));
      mAccount1.save();
      mAccount2 = new Account(mDbHelper,"TestAccount 2",100,"Secondary account",Currency.getInstance(Locale.getDefault()));
      mAccount2.save();
  }
  public void testTransfer() {
    Transfer op = (Transfer) Transaction.getTypedNewInstance(mDbHelper, MyExpenses.TYPE_TRANSFER);
    op.account_id = mAccount1.id;
    op.cat_id = mAccount2.id;
    op.amount = 100;
    op.comment = "test transfer";
    op.save();
    Assert.assertTrue(op.id > 0);
    Transfer peer = (Transfer) Transaction.getInstanceFromDb(mDbHelper,op.transfer_peer);
    Assert.assertEquals(op.id, peer.transfer_peer);
  }
  @Override
  protected void tearDown() throws Exception {
    mDbHelper.deleteAccount(mAccount1.id);
    mDbHelper.deleteAccount(mAccount2.id);
  }
}
