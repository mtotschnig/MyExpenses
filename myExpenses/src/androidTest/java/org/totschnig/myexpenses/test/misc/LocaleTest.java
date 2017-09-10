package org.totschnig.myexpenses.test.misc;

import java.util.Locale;

import junit.framework.Assert;

import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Transaction;

import android.util.Log;


public class LocaleTest extends android.test.InstrumentationTestCase {
  private Account mAccount;
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      mAccount = new Account("TestAccount 1",100,"Main account");
      mAccount.save();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    Account.delete(mAccount.getId());
  }

  public void testDateIsSavedIndependentFromLocale() {
    Locale[] locs = Locale.getAvailableLocales();
    //Locale loc = new Locale("ar");

    for(Locale loc : locs) {
      // Change locale settings in the app.
      Locale.setDefault(loc);
      String lang = loc.getDisplayLanguage();
      String code = Locale.getDefault().getLanguage();
      //the following Locales triggered bug https://github.com/mtotschnig/MyExpenses/issues/53
      if (!(code.equals("ar") || 
          code.equals("bn") ||
          code.equals("fa") ||
          code.equals("hi") ||
          code.equals("mr") ||
          code.equals("ps") || 
          code.equals("ta")
          ))
        continue;
      Log.i("TEST",loc.getDisplayLanguage(Locale.US));
      Log.i("TEST",code);
      Transaction op = Transaction.getNewInstance(mAccount.getId());
      op.setComment(code + " " + lang);
      op.save();
      Assert.assertTrue("Failed to create transaction in Locale " + lang,op.getId() > 0);
      assertNotNull(Transaction.getInstanceFromDb(op.getId()));
    }
  }
}
