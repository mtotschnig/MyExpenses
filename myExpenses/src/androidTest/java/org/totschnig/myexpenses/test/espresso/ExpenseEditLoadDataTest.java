package org.totschnig.myexpenses.test.espresso;


import android.Manifest;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.test.rule.ActivityTestRule;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.util.CurrencyFormatter;

import java.io.IOException;
import java.util.Calendar;
import java.util.Currency;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone;
import static org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible;

public class ExpenseEditLoadDataTest {
  private static Currency currency;
  @Rule
  public ActivityTestRule<ExpenseEdit> mActivityRule =
      new ActivityTestRule<>(ExpenseEdit.class, false, false);
  private static Account account1;
  private static Account account2;
  private static Transaction transaction;
  private static Transfer transfer;
  private static SplitTransaction splitTransaction;
  private static Template template;

  private static void grantCalendarPermissions() throws IOException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ParcelFileDescriptor parcelFileDescriptor = getInstrumentation().getUiAutomation().executeShellCommand(
          "pm grant " + getTargetContext().getPackageName()
              + " " + Manifest.permission.WRITE_CALENDAR);
      parcelFileDescriptor.close();
      parcelFileDescriptor = getInstrumentation().getUiAutomation().executeShellCommand(
          "pm grant " + getTargetContext().getPackageName()
              + " " + Manifest.permission.READ_CALENDAR);
      parcelFileDescriptor.close();
    }
  }

  @BeforeClass
  public static void fixture() throws IOException {
    grantCalendarPermissions();
    currency = Currency.getInstance("EUR");
    account1 = new Account("Test account 1", currency, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account1.save();
    account2 = new Account("Test account 2", Currency.getInstance("EUR"), 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account2.save();
    transaction = Transaction.getNewInstance(account1.getId());
    transaction.setAmount(new Money(currency, 500L));
    transaction.save();
    transfer = Transfer.getNewInstance(account1.getId(), account2.getId());
    transfer.setAmount(new Money(currency, 600L));
    transfer.save();
    splitTransaction = SplitTransaction.getNewInstance(account1.getId());
    splitTransaction.save();
    template = Template.getTypedNewInstance(Transaction.TYPE_TRANSACTION, account1.getId(), false, null);
    template.setTitle("Daily plan");
    template.setAmount(new Money(currency, 700L));
    Calendar calendar = Calendar.getInstance();
    template.setPlan(new Plan(calendar, Plan.Recurrence.DAILY.toRrule(calendar), "Daily", template.compileDescription(MyApplication.getInstance(), CurrencyFormatter.instance())));
    template.save();
  }

  @AfterClass
  public static void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account1.getId());
    Account.delete(account2.getId());
  }

  @Test
  public void shouldPopulateWithTransactionAndPrepareForm() {
    Intent i = new Intent();
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra(KEY_ROWID, transaction.getId());
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow, R.id.Recurrence);
    onView(withId(R.id.Amount)).check(matches(withText("5")));

  }

  @Test
  public void shouldPopulateWithTransferAndPrepareForm() {
    Intent i = new Intent();
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra(KEY_ROWID, transfer.getId());
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow,
        R.id.TransferAccountRow, R.id.Recurrence);
    onView(withId(R.id.Amount)).check(matches(withText("6")));
  }

  @Test
  public void shouldPopulateWithSplitTransactionAndPrepareForm() {
    Intent i = new Intent();
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra(KEY_ROWID, splitTransaction.getId());
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.SplitContainer,
        R.id.PayeeRow, R.id.AccountRow, R.id.Recurrence);
  }

  @Test
  public void shouldPopulateWithTemplateAndPrepareForm() {
    Intent i = new Intent();
    i.setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.ExpenseEdit");
    i.putExtra(KEY_TEMPLATEID, template.getId());
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.TitleRow,  R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow, R.id.Plan);
    checkEffectiveGone(R.id.Recurrence);
    onView(withId(R.id.Amount)).check(matches(withText("7")));
    onView(withId(R.id.Title)).check(matches(withText("Daily plan")));
  }
}
