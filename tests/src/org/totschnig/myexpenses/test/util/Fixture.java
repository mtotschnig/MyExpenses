package org.totschnig.myexpenses.test.util;

import java.io.File;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Account.Type;
import org.totschnig.myexpenses.test.R;
import org.totschnig.myexpenses.util.CategoryTree;
import org.totschnig.myexpenses.util.Result;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

public class Fixture {
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

    //set up accounts
    Account account1 = new Account(
        testContext.getString(R.string.testData_account1Label),
        defaultCurrency,
        2000,
        testContext.getString(R.string.testData_account1Description), Type.CASH, Account.defaultColor
    );
    account1.grouping = Account.Grouping.DAY;
    account1.save();
    Account account2 = new Account(
        testContext.getString(R.string.testData_account2Label),
        foreignCurrency,
        50000,
        testContext.getString(R.string.testData_account2Description), Type.CASH,
        Build.VERSION.SDK_INT > 13 ? testContext.getResources().getColor(android.R.color.holo_red_dark) : Color.RED
    );
    account2.save();
    Account account3 = new Account(
        testContext.getString(R.string.testData_account3Label),
        defaultCurrency,
        200000,
        testContext.getString(R.string.testData_account3Description), Type.BANK,
        Build.VERSION.SDK_INT > 13 ? testContext.getResources().getColor(android.R.color.holo_blue_dark) : Color.BLUE
    );
    account3.save();
    //we set up one more account with the foreign currency, in order to have two accounts with the same currency,
    //which makes sure it appears in the aggregate dialog
    Account account4 = new Account("ignored",foreignCurrency,0,"",Type.BANK,Account.defaultColor);
    account4.save();
    //set up categories
    int sourceRes = appContext.getResources().getIdentifier("cat_"+locale.getLanguage(), "raw", appContext.getPackageName());
    Result result = GrisbiImport.analyzeGrisbiFileWithSAX(appContext.getResources().openRawResource(sourceRes));
    GrisbiImport.importCats((CategoryTree) result.extra[0], null);
    //set up transactions
    long now = System.currentTimeMillis();
    //are used twice
    long mainCat1 = Category.find(testContext.getString(R.string.testData_transaction1MainCat), null);
    long mainCat2 = Category.find(testContext.getString(R.string.testData_transaction2MainCat), null);
    long mainCat6 = Category.find(testContext.getString(R.string.testData_transaction6MainCat), null);
    Transaction op1 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op1.amount = new Money(defaultCurrency,-1200L);
    op1.catId = Category.find(testContext.getString(R.string.testData_transaction1SubCat), mainCat1);
    op1.setDate(new Date( now - 300000 ));
    op1.save();
    Transaction op2 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op2.amount = new Money(defaultCurrency,-2200L);
    op2.catId = Category.find(testContext.getString(R.string.testData_transaction2SubCat), mainCat2);
    op2.comment = testContext.getString(R.string.testData_transaction2Comment);
    op2.setDate(new Date( now - 7200000 ));
    op2.save();
    Transaction op3 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op3.amount = new Money(defaultCurrency,-2500L);
    op3.catId = Category.find(testContext.getString(R.string.testData_transaction3SubCat),
        Category.find(testContext.getString(R.string.testData_transaction3MainCat), null));
    op3.setDate(new Date( now - 72230000 ));
    op3.save();
    Transaction op4 = Transaction.getTypedNewInstance(MyExpenses.TYPE_TRANSACTION,account1.id);
    op4.amount = new Money(defaultCurrency,-5000L);
    op4.catId = Category.find(testContext.getString(R.string.testData_transaction4SubCat),
        Category.find(testContext.getString(R.string.testData_transaction4MainCat), null));
    op4.payee = testContext.getString(R.string.testData_transaction4Payee);
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
    if (stage ==1) return;
  }
}
