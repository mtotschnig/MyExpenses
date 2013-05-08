package org.totschnig.myexpenses.test;

import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Locale;

import junit.framework.Assert;

import org.totschnig.myexpenses.Account;
import org.totschnig.myexpenses.Money;
import org.totschnig.myexpenses.MyExpenses;
import org.totschnig.myexpenses.Transaction;
import org.totschnig.myexpenses.Transfer;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;


public class LocaleTest extends android.test.InstrumentationTestCase {
  private Account mAccount;
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      mAccount = new Account("TestAccount 1",100,"Main account");
      mAccount.save();
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
      Log.i("TEST",loc.toString());
      Log.i("TEST",lang);
      Transaction op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,mAccount.id);
      op.comment = code + " " + lang;
      op.save();
      Assert.assertTrue("Failed to create transaction in Locale " + lang,op.id > 0);
      Assert.assertNotNull("Failed to instantiate transaction in Locale " + lang,
          Transaction.getInstanceFromDb(op.id));
    }
  }
}
