package org.totschnig.myexpenses.test.screenshots;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.test.R;

import android.content.Context;
import android.content.res.Configuration;
import android.test.ActivityInstrumentationTestCase2;

public class Widgets extends ActivityInstrumentationTestCase2<MyExpenses> {
  private Context instCtx;

  public Widgets() {
    super(MyExpenses.class);
  }
  @Override
  protected void setUp() throws Exception {
    instCtx = getInstrumentation().getContext();
    super.setUp();
  }
  public void testSetupAccountsForWidgetScreenshots() {
    helperTestLang("en","US");
    helperTestLang("fr","FR");
    helperTestLang("de","DE");
    helperTestLang("it","IT");
    helperTestLang("es","ES");
    helperTestLang("tr","TR");
    helperTestLang("vi","VI");
    helperTestLang("ar","SA");
    helperTestLang("hu","HU");
    helperTestLang("ca","ES");
    helperTestLang("km","KH");
    helperTestLang("zh","TW");
    helperTestLang("pt","BR");
    helperTestLang("pl","PL");
    helperTestLang("cs","CZ");
    helperTestLang("ru","RU");
  }
  private void helperTestLang(String lang, String country) {
    Locale l = new Locale(lang,country);
    Currency c = Currency.getInstance(l);
    Account a = new Account(
        translate(l,R.string.testData_account1Label),
        c,
        2000,
        "", Type.BANK, Account.defaultColor
    );
    a.save();
    Template t = Template.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION, a.getId());
    t.amount = new Money(c,-90000L);
    t.title = translate(l,R.string.testData_templateSubCat);
    t.payee = translate(l,R.string.testData_templatePayee);
    t.save();
  }
  
  public String translate(Locale locale, int resId) {  
    Configuration config = new Configuration(instCtx.getResources().getConfiguration()); 
    config.setLocale(locale);   
    return instCtx.createConfigurationContext(config).getString(resId);
}
}
