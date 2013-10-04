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
	  helperDbFixture();
	  //MyExpenses activity = getActivity();
    /*
    solo.waitForDialogToClose(2000);
    solo.clickOnActionBarHomeButton();
    solo.waitForActivity(ManageAccounts.class);
    solo.sleep(1000);
    //solo.takeScreenshot("multiple_accounts_" + lang);
    solo.clickOnActionBarItem(org.totschnig.myexpenses.R.id.AGGREGATES_COMMAND);
    solo.waitForDialogToOpen(2000);
    solo.sleep(1000);
    Spoon.screenshot(activity, "test1");
    //solo.takeScreenshot("feature_aggregate_" + lang);
    solo.clickOnButton(activity.getString(android.R.string.ok));
    solo.waitForDialogToClose(2000);
    solo.clickInList(0);
    solo.waitForActivity(MyExpenses.class);
    solo.sleep(1000);
    //solo.takeScreenshot("grouped_list_" + lang);
    solo.clickOnActionBarItem(org.totschnig.myexpenses.R.id.NEW_FROM_TEMPLATE_COMMAND);
    Spoon.screenshot(activity, "test2");
    //solo.takeScreenshot("instantiate_template_" + lang);
*/	}
	private void helperDbFixture() {
    Context ctx = getInstrumentation().getContext();
	  Currency foreignCurrency = Currency.getInstance(ctx.getString(R.string.testData_account2Currency));

	  //set up accounts
    Account account1 = new Account(
        ctx.getString(R.string.testData_account1Label),
        defaultCurrency,
        2000,
        ctx.getString(R.string.testData_account1Description), Type.CASH, Account.defaultColor
    );
    account1.grouping = Account.Grouping.DAY;
    account1.save();
    Account account2 = new Account(
        ctx.getString(R.string.testData_account2Label),
        foreignCurrency,
        50000,
        ctx.getString(R.string.testData_account2Description), Type.CASH,
        Build.VERSION.SDK_INT > 13 ? ctx.getResources().getColor(android.R.color.holo_red_dark) : Color.RED
    );
    account2.save();
    Account account3 = new Account(
        ctx.getString(R.string.testData_account3Label),
        defaultCurrency,
        200000,
        ctx.getString(R.string.testData_account3Description), Type.BANK,
        Build.VERSION.SDK_INT > 13 ? ctx.getResources().getColor(android.R.color.holo_blue_dark) : Color.BLUE
    );
    account3.save();
    //we set up one more account with the foreign currency, in order to have two accounts with the same currency,
    //which makes sure it appears in the aggregate dialog
    Account account4 = new Account("ignored",foreignCurrency,0,"",Type.BANK,Account.defaultColor);
    account4.save();
    //set up categories
    int sourceRes = app.getResources().getIdentifier("cat_"+locale.getLanguage(), "raw", app.getPackageName());
    Result result = GrisbiImport.analyzeGrisbiFileWithSAX(app.getResources().openRawResource(sourceRes));
    GrisbiImport.importCats((CategoryTree) result.extra[0], null);
    //set up transactions
    long now = System.currentTimeMillis();
    //are used twice
    long mainCat1 = Category.find(ctx.getString(R.string.testData_transaction1MainCat), null);
    long mainCat2 = Category.find(ctx.getString(R.string.testData_transaction2MainCat), null);
    long mainCat6 = Category.find(ctx.getString(R.string.testData_transaction6MainCat), null);
    Transaction op1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op1.amount = new Money(defaultCurrency,-1200L);
    op1.catId = Category.find(ctx.getString(R.string.testData_transaction1SubCat), mainCat1);
    op1.setDate(new Date( now - 300000 ));
    op1.save();
    Transaction op2 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op2.amount = new Money(defaultCurrency,-2200L);
    op2.catId = Category.find(ctx.getString(R.string.testData_transaction2SubCat), mainCat2);
    op2.comment = ctx.getString(R.string.testData_transaction2Comment);
    op2.setDate(new Date( now - 7200000 ));
    op2.save();
    Transaction op3 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op3.amount = new Money(defaultCurrency,-2500L);
    op3.catId = Category.find(ctx.getString(R.string.testData_transaction3SubCat),
        Category.find(ctx.getString(R.string.testData_transaction3MainCat), null));
    op3.setDate(new Date( now - 72230000 ));
    op3.save();
    Transaction op4 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op4.amount = new Money(defaultCurrency,-5000L);
    op4.catId = Category.find(ctx.getString(R.string.testData_transaction4SubCat),
        Category.find(ctx.getString(R.string.testData_transaction4MainCat), null));
    op4.payee = ctx.getString(R.string.testData_transaction4Payee);
    op4.setDate(new Date( now - 98030000 ));
    op4.save();
    new Template(op4,op4.payee).save();
    Transaction op5 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSFER, account3.id);
    op5.transfer_account = account1.id;
    op5.amount = new Money(defaultCurrency,-10000L);
    op5.setDate(new Date( now - 100390000 ));
    op5.save();
    Transaction op6 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION, account1.id);
    op6.amount = new Money(defaultCurrency,10000L);
    op6.catId = mainCat6;
    op6.setDate(new Date( now - 110390000 ));
    op6.save();
    Transaction op7 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION, account2.id);
    op7.amount = new Money(foreignCurrency,-34523L);
    op7.setDate(new Date( now - 1003900000 ));
    op7.save();
    Transaction op8 = Transaction.getTypedNewInstance(MyExpenses.TYPE_SPLIT, account1.id);
    op8.amount = new Money(defaultCurrency,-8967L);
    op8.save();
    Transaction split1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION, account1.id,op8.id);
    split1.amount = new Money(defaultCurrency,-4523L);
    split1.catId = mainCat2;
    split1.save();
    Transaction split2 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION, account1.id,op8.id);
    split2.amount = new Money(defaultCurrency,-4444L);
    split2.catId = mainCat6;
    split2.save();
	}
}