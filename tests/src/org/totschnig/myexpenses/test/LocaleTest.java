package org.totschnig.myexpenses.test;

import java.util.Locale;

import junit.framework.Assert;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Transaction;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


public class LocaleTest extends android.test.InstrumentationTestCase {
  private Account mAccount;
  private SharedPreferences settings;
  private static final String TEST_ID = "functest";
  
  @Override
  protected void setUp() throws Exception {
      super.setUp();
      MyApplication app = (MyApplication) getInstrumentation().getTargetContext().getApplicationContext();
      settings = app.getSharedPreferences(TEST_ID,Context.MODE_PRIVATE);
      app.setSettings(settings);
      //app.setDatabaseName(TEST_ID);
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
      Log.i("TEST",loc.getDisplayLanguage(Locale.US));
      Log.i("TEST",code);
      Transaction op = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,mAccount.id);
      op.comment = code + " " + lang;
      op.save();
      Assert.assertTrue("Failed to create transaction in Locale " + lang,op.id > 0);
      Assert.assertNotNull("Failed to instantiate transaction in Locale " + lang,
          Transaction.getInstanceFromDb(op.id));
    }
  }
}
