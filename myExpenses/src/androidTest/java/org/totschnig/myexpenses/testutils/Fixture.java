package org.totschnig.myexpenses.testutils;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import junit.framework.Assert;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.debug.test.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.viewmodel.data.Budget;
import org.totschnig.myexpenses.viewmodel.data.Tag;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import timber.log.Timber;

import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE;

@SuppressLint("InlinedApi")
public class Fixture {
  private final Context testContext;
  private final MyApplication appContext;
  private Account account1, account2, account3, account4;

  public String getSyncAccount1() {
    return SYNC_ACCOUNT_1;
  }

  public String getSyncAccount2() {
    return SYNC_ACCOUNT_2;
  }

  public String getSyncAccount3() {
    return SYNC_ACCOUNT_3;
  }

  private String SYNC_ACCOUNT_1;
  private String SYNC_ACCOUNT_2;
  private String SYNC_ACCOUNT_3;

  public Fixture(Instrumentation inst) {
    testContext = inst.getContext();
    appContext = (MyApplication) inst.getTargetContext().getApplicationContext();
  }

  public Account getAccount1() {
    return account1;
  }

  public Account getAccount2() {
    return account2;
  }

  public Account getAccount3() {
    return account3;
  }

  public void setup(boolean withPicture) {
    SYNC_ACCOUNT_1 = "Drive - " + appContext.getString(org.totschnig.myexpenses.R.string.content_description_encrypted);
    SYNC_ACCOUNT_2 = "Dropbox - " + testContext.getString(R.string.testData_sync_backend_2_name);
    SYNC_ACCOUNT_3 = "WebDAV - https://my.private.cloud/webdav/MyExpenses";
    CurrencyUnit defaultCurrency = Utils.getHomeCurrency();
    CurrencyUnit foreignCurrency = appContext.getAppComponent().currencyContext().get(defaultCurrency.getCode().equals("EUR") ? "GBP" : "EUR");

    account1 = new Account(
        testContext.getString(R.string.testData_account1Label),
        90000,
        testContext.getString(R.string.testData_account1Description));
    account1.setGrouping(Grouping.WEEK);
    account1.setSyncAccountName(SYNC_ACCOUNT_1);
    account1.save();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
    account2 = new Account(
        testContext.getString(R.string.testData_account2Label),
        foreignCurrency,
        50000,
        formatter.format(LocalDate.now()), AccountType.CASH,
        testContext.getResources().getColor(R.color.material_red));
    account2.setSyncAccountName(SYNC_ACCOUNT_2);
    account2.save();

    account3 = new Account(
        testContext.getString(R.string.testData_account3Label),
        Utils.getHomeCurrency(),
        200000,
        testContext.getString(R.string.testData_account3Description), AccountType.BANK,
        testContext.getResources().getColor(R.color.material_blue));
    account3.setGrouping(Grouping.DAY);
    account3.setSyncAccountName(SYNC_ACCOUNT_3);
    account3.save();

    account4 = new Account(
        testContext.getString(R.string.testData_account3Description),
        foreignCurrency,
        0,
        "",
        AccountType.CCARD,
        testContext.getResources().getColor(R.color.material_cyan));
    account4.save();

    //set up categories
    setUpCategories(appContext);
    //set up transactions
    long offset = System.currentTimeMillis();
    //are used twice
    long mainCat1 = findCat(testContext.getString(R.string.testData_transaction1MainCat), null);
    long mainCat2 = findCat(testContext.getString(R.string.testData_transaction2MainCat), null);
    long mainCat3 = findCat(testContext.getString(R.string.testData_transaction3MainCat), null);
    long mainCat6 = findCat(testContext.getString(R.string.testData_transaction6MainCat), null);

    for (int i = 0; i < 15; i++) {
      //Transaction 1
      final File file = new File(appContext.getExternalFilesDir(null), "screenshot.jpg");
      final TransactionBuilder builder = new TransactionBuilder(testContext)
          .accountId(account1.getId())
          .amount(defaultCurrency, -random(12000))
          .catId(R.string.testData_transaction1SubCat, mainCat1)
          .date(offset - 300000);
      if (withPicture) {
        builder.pictureUri(Uri.fromFile(new File(appContext.getExternalFilesDir(null), "screenshot.jpg")));
      }
      Transaction op1 = builder.persist();

      //Transaction 2
      Transaction op2 = new TransactionBuilder(testContext)
          .accountId(account1.getId())
          .amount(defaultCurrency, -random(2200L))
          .catId(R.string.testData_transaction2SubCat, mainCat2)
          .date(offset - 7200000)
          .comment(testContext.getString(R.string.testData_transaction2Comment))
          .persist();

      //Transaction 3 Cleared
      Transaction op3 = new TransactionBuilder(testContext)
          .accountId(account1.getId())
          .amount(defaultCurrency, -random(2500L))
          .catId(R.string.testData_transaction3SubCat, mainCat3)
          .date(offset - 72230000)
          .crStatus(CrStatus.CLEARED)
          .persist();

      //Transaction 4 Cleared
      Transaction op4 = new TransactionBuilder(testContext)
          .accountId(account1.getId())
          .amount(defaultCurrency, -random(5000L))
          .catId(R.string.testData_transaction4SubCat, mainCat2)
          .payee(R.string.testData_transaction4Payee)
          .date(offset - 98030000)
          .crStatus(CrStatus.CLEARED)
          .persist();

      //Transaction 5 Reconciled
      Transaction op5 = new TransactionBuilder(testContext)
          .accountId(account1.getId())
          .amount(defaultCurrency, -random(10000L))
          .date(offset - 100390000)
          .crStatus(CrStatus.RECONCILED)
          .persist();

      //Transaction 6 Gift Reconciled
      Transaction op6 = new TransactionBuilder(testContext)
          .accountId(account1.getId())
          .amount(defaultCurrency, -10000L)
          .catId(mainCat6)
          .date(offset - 210390000)
          .crStatus(CrStatus.RECONCILED)
          .persist();

      //Salary
      Transaction op8 = new TransactionBuilder(testContext)
          .accountId(account3.getId())
          .amount(defaultCurrency, 200000)
          .date(offset)
          .persist();

      //Transfer
      Transfer transfer = Transfer.getNewInstance(account1.getId(), account3.getId());
      transfer.setAmount(new Money(defaultCurrency, 25000L));
      transfer.setDate(new Date(offset));
      transfer.save();

      offset = offset - 400000000;
    }

    //Second account foreign Currency
    new TransactionBuilder(testContext)
        .accountId(account2.getId())
        .amount(foreignCurrency, -random(34567))
        .date(offset - 303900000)
        .persist();

    //Transaction 8: Split
    Transaction split = SplitTransaction.getNewInstance(account1.getId());
    split.setAmount(new Money(defaultCurrency, -8967L));
    split.setStatus(STATUS_NONE);
    split.save(true);
    split.save(true);
    List<Tag> tagList = Collections.singletonList(new Tag(-1, testContext.getString(R.string.testData_tag_project), false, 0));
    split.saveTags(tagList, MyApplication.getInstance().getContentResolver());

    new TransactionBuilder(testContext)
        .accountId(account1.getId()).parentId(split.getId())
        .amount(defaultCurrency, -4523L)
        .catId(mainCat2)
        .persist();

    new TransactionBuilder(testContext)
        .accountId(account1.getId()).parentId(split.getId())
        .amount(defaultCurrency, -4444L)
        .catId(mainCat6)
        .persist();

    // Template
    Assert.assertNotSame("Unable to create planner", MyApplication.getInstance().createPlanner(true), MyApplication.INVALID_CALENDAR_ID);
    //createPlanner sets up a new plan, mPlannerCalendarId is only set in onSharedPreferenceChanged
    //if it is has not been called yet, when we save our plan, saving fails.
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Template template = Template.getTypedNewInstance(TYPE_TRANSACTION, account3.getId(), false, null);
    template.setAmount(new Money(defaultCurrency, -90000L));
    String templateSubCat = testContext.getString(R.string.testData_templateSubCat);
    template.setCatId(findCat(templateSubCat,
        findCat(testContext.getString(R.string.testData_templateMainCat), null)));
    template.setTitle(templateSubCat);
    template.setPayee(testContext.getString(R.string.testData_templatePayee));
    Uri planUri = new Plan(
        LocalDate.now(),
        "FREQ=WEEKLY;COUNT=10;WKST=SU",
        template.getTitle(),
        template.compileDescription(appContext))
        .save();
    template.planId = ContentUris.parseId(planUri);
    Uri templateuri = template.save();
    if (templateuri == null)
      throw new RuntimeException("Could not save template");

    Budget budget = new Budget(0L, account1.getId(), testContext.getString(R.string.testData_account1Description), "DESCRIPTION", defaultCurrency, new Money(defaultCurrency, 200000L), Grouping.MONTH, -1, (LocalDate) null, (LocalDate) null, account1.getLabel(), true);
    long budgetId = ContentUris.parseId(appContext.getContentResolver().insert(TransactionProvider.BUDGETS_URI, budget.toContentValues()));
    setCategoryBudget(budgetId, mainCat1, 50000);
    setCategoryBudget(budgetId, mainCat2, 40000);
    setCategoryBudget(budgetId, mainCat3, 30000);
    setCategoryBudget(budgetId, mainCat6, 20000);
  }

  public void setCategoryBudget(long budgetId, long categoryId, long amount) {
    ContentValues contentValues = new ContentValues(1);
    contentValues.put(KEY_BUDGET, amount);
    final Uri budgetUri = ContentUris.withAppendedId(TransactionProvider.BUDGETS_URI, budgetId);
    int result = appContext.getContentResolver().update(ContentUris.withAppendedId(budgetUri, categoryId),
        contentValues, null, null);
    Timber.d("Insert category budget: %d", result);
  }

  private static void setUpCategories(Context appContext) {
    Timber.d("Set up %d categories", appContext.getContentResolver()
        .call(TransactionProvider.DUAL_URI, TransactionProvider.METHOD_SETUP_CATEGORIES, null, null)
        .getInt(TransactionProvider.KEY_RESULT));
  }

  private static long findCat(String label, Long parent) {
    Long result = Category.find(label, parent);
    if (result == -1) {
      throw new RuntimeException("Could not find category");
    }
    return result;
  }

  private long random(long n) {
    return ThreadLocalRandom.current().nextLong(n);
  }

  private static class TransactionBuilder {
    private Context context;
    private long accountId;
    private Long parentId;
    private Money amount;
    private Long catId;
    private Date date;
    private CrStatus crStatus;
    private Uri pictureUri;
    private String comment;
    private String payee;

    private TransactionBuilder(Context context) {
      this.context = context;
    }


    private TransactionBuilder accountId(long accountId) {
      this.accountId = accountId;
      return this;
    }

    private TransactionBuilder parentId(Long parentId) {
      this.parentId = parentId;
      return this;
    }

    private TransactionBuilder amount(CurrencyUnit currency, long amountMinor) {
      this.amount = new Money(currency, amountMinor);
      return this;
    }

    private TransactionBuilder catId(int resId, Long parentId) {
      this.catId = findCat(context.getString(resId), parentId);
      return this;
    }

    private TransactionBuilder catId(long catId) {
      this.catId = catId;
      return this;
    }

    private TransactionBuilder date(long date) {
      this.date = new Date(date);
      return this;
    }

    private TransactionBuilder crStatus(CrStatus crStatus) {
      this.crStatus = crStatus;
      return this;
    }

    private TransactionBuilder pictureUri(Uri uri) {
      this.pictureUri = uri;
      return this;
    }

    private TransactionBuilder payee(int resId) {
      this.payee = context.getString(resId);
      return this;
    }

    private TransactionBuilder comment(String comment) {
      this.comment = comment;
      return this;
    }

    Transaction persist() {
      Transaction transaction = Transaction.getNewInstance(accountId);
      transaction.setAmount(amount);
      transaction.setCatId(catId);
      if (date != null) {
        transaction.setDate(date);
      }
      if (crStatus != null) {
        transaction.setCrStatus(crStatus);
      }
      transaction.setPictureUri(pictureUri);
      transaction.setPayee(payee);
      transaction.setComment(comment);
      transaction.setParentId(parentId);
      transaction.save();
      return transaction;
    }
  }
}
