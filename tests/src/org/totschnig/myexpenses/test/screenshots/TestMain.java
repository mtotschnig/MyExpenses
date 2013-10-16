package org.totschnig.myexpenses.test.screenshots;

import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.test.R;
import org.totschnig.myexpenses.test.util.Fixture;
import org.totschnig.myexpenses.util.CategoryTree;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Account.Type;

/**
 * These tests are meant to be run with script/testLangs.sh
 * since they depend on the reinitialisation of the db
 * and they prepare the db for script/monkey.py which
 * runs through the app and creates screenshots
 * @author Michael Totschnig
 *
 */
public class TestMain extends ActivityInstrumentationTestCase2<MyExpenses> {
	private MyApplication app;
	private Context instCtx;
	private Locale locale;
	private Currency defaultCurrency;
	
	public TestMain() {
		super(MyExpenses.class);
	}
	
	@Override
	protected void setUp() throws Exception {
    instCtx = getInstrumentation().getContext();
	  app = (MyApplication) getInstrumentation().getTargetContext().getApplicationContext(); 
		super.setUp();
	}
	public void testLang_en() {
    defaultCurrency = Currency.getInstance("USD");
	  helperTestLang("en","US");
	}
  public void testLang_fr() {
    defaultCurrency = Currency.getInstance("EUR");
    helperTestLang("fr","FR");
  }
  public void testLang_de() {
    defaultCurrency = Currency.getInstance("EUR");
    helperTestLang("de","DE");
  }
  public void testLang_it() {
    defaultCurrency = Currency.getInstance("EUR");
    helperTestLang("it","IT");
  }
  public void testLang_es() {
    defaultCurrency = Currency.getInstance("EUR");
    helperTestLang("es","ES");
  }
  public void testLang_tr() {
    defaultCurrency = Currency.getInstance("TRY");
    helperTestLang("tr","TR");
  }
  public void testLang_vi() {
    //Currency.getInstance(new Locale("vi","VI") => USD on Nexus S
    defaultCurrency = Currency.getInstance("VND");
    helperTestLang("vi","VI");
  }
  public void testLang_ar() {
    //Currency.getInstance(new Locale("vi","VI") => USD on Nexus S
    defaultCurrency = Currency.getInstance("SAR");
    helperTestLang("ar","SA");
  }
	private void helperTestLang(String lang, String country) {
	  this.locale = new Locale(lang,country);
	  Locale.setDefault(locale); 
	  Configuration config = new Configuration(); 
	  config.locale = locale;
	  app.getResources().updateConfiguration(config,  
	      app.getResources().getDisplayMetrics());
    instCtx.getResources().updateConfiguration(config,  
        instCtx.getResources().getDisplayMetrics());
    getActivity();
	  Fixture.setup(getInstrumentation(), locale, defaultCurrency);
	}
}