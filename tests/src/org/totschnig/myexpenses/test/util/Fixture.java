package org.totschnig.myexpenses.test.util;

import java.io.File;
import java.io.InputStream;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import junit.framework.Assert;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.model.Transaction.CrStatus;
import org.totschnig.myexpenses.test.R;
import org.totschnig.myexpenses.util.CategoryTree;
import org.totschnig.myexpenses.util.Result;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

public class Fixture {
  private static Account account1;
  private static Account account2;
  private static Account account3;
  public static Account getAccount1() {
    return account1;
  }
  public static Account getAccount2() {
    return account2;
  }
  public static Account getAccount3() {
    return account3;
  }
  public static Account getAccount4() {
    return account4;
  }
  private static Account account4;
  @SuppressLint("NewApi")
  public static void clear(Context ctx) {
    File dir = ctx.getExternalCacheDir();
    if (dir != null)
      delete(dir.getParentFile());
    dir = ctx.getCacheDir();
    if (dir != null)
      delete(dir.getParentFile());
  }
  //If provided a file will delete it. 
  //If provided a directory will recursively delete files but preserve directories
  private static void delete(File file_or_directory) {
      if (file_or_directory == null) {
          return;
      }

      if (file_or_directory.isDirectory()) {
          if (file_or_directory.listFiles() != null) {
              for(File f : file_or_directory.listFiles()) {
                  delete(f);
              }
          }
      } else {
          file_or_directory.delete();
      }
  }
  public static void setup(Instrumentation inst, Locale locale, Currency defaultCurrency) {
    setup(inst,locale,defaultCurrency,-1);
  }
  public static void setup(Instrumentation inst, Locale locale, Currency defaultCurrency,int stage) {
    Context testContext = inst.getContext();
    Context appContext = inst.getTargetContext().getApplicationContext(); 
    Currency foreignCurrency = Currency.getInstance(testContext.getString(R.string.testData_account2Currency));

    account1 = new Account(
        testContext.getString(R.string.testData_account1Label),
        defaultCurrency,
        2000,
        testContext.getString(R.string.testData_account1Description), Type.CASH, Account.defaultColor
    );
    account1.grouping = Account.Grouping.DAY;
    account1.save();
    if (stage ==1) return;
    account2 = new Account(
        testContext.getString(R.string.testData_account2Label),
        foreignCurrency,
        50000,
        testContext.getString(R.string.testData_account2Description), Type.CASH,
        Build.VERSION.SDK_INT > 13 ? testContext.getResources().getColor(android.R.color.holo_red_dark) : Color.RED
    );
    account2.save();
    account3 = new Account(
        testContext.getString(R.string.testData_account3Label),
        defaultCurrency,
        200000,
        testContext.getString(R.string.testData_account3Description), Type.BANK,
        Build.VERSION.SDK_INT > 13 ? testContext.getResources().getColor(android.R.color.holo_blue_dark) : Color.BLUE
    );
    account3.grouping = Account.Grouping.DAY;
    account3.save();
    account4 = new Account("ignored",foreignCurrency,0,"",Type.BANK,Account.defaultColor);
    account4.save();
    //set up categories
    int sourceRes = appContext.getResources().getIdentifier("cat_"+locale.getLanguage(), "raw", appContext.getPackageName());
    InputStream catXML;
    try {
      catXML = appContext.getResources().openRawResource(sourceRes);
    } catch (NotFoundException e) {
      catXML = appContext.getResources().openRawResource(org.totschnig.myexpenses.R.raw.cat_en);
    }

    Result result = GrisbiImport.analyzeGrisbiFileWithSAX(catXML);
    GrisbiImport.importCats((CategoryTree) result.extra[0], null);
    //set up transactions
    long now = System.currentTimeMillis();
    //are used twice
    long mainCat1 = Category.find(testContext.getString(R.string.testData_transaction1MainCat), null);
    long mainCat2 = Category.find(testContext.getString(R.string.testData_transaction2MainCat), null);
    long mainCat6 = Category.find(testContext.getString(R.string.testData_transaction6MainCat), null);

    //Transaction 1
    Transaction op1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account3.id);
    op1.amount = new Money(defaultCurrency,-1200L);
    op1.catId = Category.find(testContext.getString(R.string.testData_transaction1SubCat), mainCat1);
    op1.setDate(new Date( now - 300000 ));
    op1.save();

    //Transaction 2
    Transaction op2 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account3.id);
    op2.amount = new Money(defaultCurrency,-2200L);
    op2.catId = Category.find(testContext.getString(R.string.testData_transaction2SubCat), mainCat2);
    op2.comment = testContext.getString(R.string.testData_transaction2Comment);
    op2.setDate(new Date( now - 7200000 ));
    op2.save();
    Transaction op3 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account3.id);

    //Transaction 3 Cleared
    op3.amount = new Money(defaultCurrency,-2500L);
    op3.catId = Category.find(testContext.getString(R.string.testData_transaction3SubCat),
        Category.find(testContext.getString(R.string.testData_transaction3MainCat), null));
    op3.setDate(new Date( now - 72230000 ));
    op3.crStatus = CrStatus.CLEARED;
    op3.save();

    //Transaction 4 Cleared
    Transaction op4 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account3.id);
    op4.amount = new Money(defaultCurrency,-5000L);
    op4.catId = Category.find(testContext.getString(R.string.testData_transaction4SubCat),
        Category.find(testContext.getString(R.string.testData_transaction4MainCat), null));
    op4.payee = testContext.getString(R.string.testData_transaction4Payee);
    op4.setDate(new Date( now - 98030000 ));
    op4.crStatus = CrStatus.CLEARED;
    op4.save();

    //Transaction 5 Reconciled
    Transaction op5 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSFER, account1.id);
    op5.transfer_account = account3.id;
    op5.amount = new Money(defaultCurrency,-10000L);
    op5.setDate(new Date( now - 800390000 ));
    op5.crStatus = CrStatus.RECONCILED;
    op5.save();

    //Transaction 6 Gift Reconciled
    Transaction op6 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION, account3.id);
    op6.amount = new Money(defaultCurrency,10000L);
    op6.catId = mainCat6;
    op6.setDate(new Date( now - 810390000 ));
    op6.crStatus = CrStatus.RECONCILED;
    op6.save();

    //Transaction 7 Second account foreign Currency
    Transaction op7 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION, account2.id);
    op7.amount = new Money(foreignCurrency,-34523L);
    op7.setDate(new Date( now - 1003900000 ));
    op7.save();

    //Transaction 8: Split
    Transaction op8 = Transaction.getTypedNewInstance(MyExpenses.TYPE_SPLIT, account3.id);
    op8.amount = new Money(defaultCurrency,-8967L);
    op8.save();
    Transaction split1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION, account3.id,op8.id);
    split1.amount = new Money(defaultCurrency,-4523L);
    split1.catId = mainCat2;
    split1.save();
    Transaction split2 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION, account3.id,op8.id);
    split2.amount = new Money(defaultCurrency,-4444L);
    split2.catId = mainCat6;
    split2.save();

    // Template
    Assert.assertTrue("Unable to create planner", MyApplication.getInstance().createPlanner());
    Template template = Template.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION, account3.id);
    template.amount = new Money(defaultCurrency,-90000L);
    String templateSubCat = testContext.getString(R.string.testData_templateSubCat);
    template.catId = Category.find(templateSubCat,
        Category.find(testContext.getString(R.string.testData_templateMainCat), null));
    if (template.catId == -1)
      throw new RuntimeException("Could not find category");
    template.title = templateSubCat;
    template.payee = testContext.getString(R.string.testData_templatePayee);
    Uri planUri = new Plan(
        0,
        System.currentTimeMillis(),
        "FREQ=WEEKLY;COUNT=10;WKST=SU",
        template.title,
        template.compileDescription(appContext))
      .save();
    template.planId = ContentUris.parseId(planUri);
    Uri templateuri = template.save();
    if (templateuri == null)
      throw new RuntimeException("Could not save template");
  }
}
