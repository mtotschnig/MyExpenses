package org.totschnig.myexpenses.test.espresso;


import android.Manifest;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.view.ViewGroup;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Plan;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.ui.AmountInput;
import org.totschnig.myexpenses.util.CurrencyFormatter;

import java.util.Currency;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone;
import static org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible;
import static org.totschnig.myexpenses.testutils.Espresso.withIdAndParent;

public class ExpenseEditLoadDataTest {
  private static CurrencyUnit currency;
  @Rule
  public ActivityTestRule<ExpenseEdit> mActivityRule =
      new ActivityTestRule<>(ExpenseEdit.class, false, false);
  @Rule
  public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
      Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR);
  private static Account account1;
  private static Account account2;
  private static Transaction transaction;
  private static Transfer transfer;
  private static SplitTransaction splitTransaction;
  private static Template template;

  @Before
  public void fixture()  {
    currency = CurrencyUnit.create(Currency.getInstance("EUR"));
    account1 = new Account("Test account 1", currency, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account1.save();
    account2 = new Account("Test account 2", currency, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account2.save();
    transaction = Transaction.getNewInstance(account1.getId());
    transaction.setAmount(new Money(currency, 500L));
    transaction.save();
    transfer = Transfer.getNewInstance(account1.getId(), account2.getId());
    transfer.setAmount(new Money(currency, 600L));
    transfer.save();
    splitTransaction = SplitTransaction.getNewInstance(account1.getId());
    splitTransaction.save();
    template = Template.getTypedNewInstance(TYPE_TRANSACTION, account1.getId(), false, null);
    template.setTitle("Daily plan");
    template.setAmount(new Money(currency, 700L));
    template.setPlan(new Plan(LocalDate.now(), Plan.Recurrence.DAILY, "Daily", template.compileDescription(MyApplication.getInstance(), CurrencyFormatter.instance())));
    template.save();
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account1.getId());
    Account.delete(account2.getId());
  }

  @Test
  public void shouldPopulateWithTransactionAndPrepareForm() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transaction.getId());
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow, R.id.Recurrence);
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("5")));
  }

  @Test
  public void shouldKeepStatusAndUuidAfterSave() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transaction.getId());
    String uuid = transaction.uuid;
    int status = transaction.getStatus();
    mActivityRule.launchActivity(i);
    onView(withId(R.id.SAVE_COMMAND)).perform(click());
    Transaction t = Transaction.getInstanceFromDb(transaction.getId());
    assertThat(t.getStatus()).isEqualTo(status);
    assertThat(t.uuid).isEqualTo(uuid);
  }

  @Test
  public void shouldPopulateWithTransferAndPrepareForm() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transfer.getId());
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow,
        R.id.TransferAccountRow, R.id.Recurrence);
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("6")));
  }

  @Test
  public void shouldSwitchAccountViewsForReceivingTransferPart() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transfer.getId());
    mActivityRule.launchActivity(i);
    assertThat(((AmountInput) mActivityRule.getActivity().findViewById(R.id.Amount)).getType()).isTrue();
    assertThat(((ViewGroup) mActivityRule.getActivity().findViewById(R.id.AccountRow)).getChildAt(1).getId()).isEqualTo(R.id.TransferAccount);
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("6")));
  }

  @Test
  public void shouldKeepAccountViewsForGivingTransferPart() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, transfer.getTransferPeer());
    mActivityRule.launchActivity(i);
    assertThat(((AmountInput) mActivityRule.getActivity().findViewById(R.id.Amount)).getType()).isFalse();
    assertThat(((ViewGroup) mActivityRule.getActivity().findViewById(R.id.AccountRow)).getChildAt(1).getId()).isEqualTo(R.id.Account);
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("6")));
  }

  @Test
  public void shouldPopulateWithSplitTransactionAndPrepareForm() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_ROWID, splitTransaction.getId());
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.SplitContainer,
        R.id.PayeeRow, R.id.AccountRow, R.id.Recurrence);
  }

  @Test
  public void shouldPopulateWithTemplateAndPrepareForm() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_TEMPLATEID, template.getId());
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.TitleRow,  R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow, R.id.PB);
    checkEffectiveGone(R.id.Recurrence);
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).check(matches(withText("7")));
    onView(withId(R.id.Title)).check(matches(withText("Daily plan")));
  }
}
