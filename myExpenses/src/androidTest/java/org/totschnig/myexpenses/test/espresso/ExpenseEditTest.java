package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import java.util.Currency;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static org.totschnig.myexpenses.activity.ExpenseEdit.KEY_NEW_TEMPLATE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.OPERATION_TYPE;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_SPLIT;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSACTION;
import static org.totschnig.myexpenses.contract.TransactionsContract.Transactions.TYPE_TRANSFER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone;
import static org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible;
import static org.totschnig.myexpenses.testutils.Espresso.withIdAndParent;

public class ExpenseEditTest {

  @Rule
  public ActivityTestRule<ExpenseEdit> mActivityRule =
      new ActivityTestRule<>(ExpenseEdit.class, false, false);
  private String accountLabel1 = "Test label 1";
  private String accountLabel2 = "Test label 2";
  private Account account1;
  private Account account2;
  private CurrencyUnit currency1;
  private CurrencyUnit currency2;

  @Before
  public void fixture() {
    currency1 = CurrencyUnit.create(Currency.getInstance("USD"));
    currency2 = CurrencyUnit.create(Currency.getInstance("EUR"));
    account1 = new Account(accountLabel1, currency1, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account1.save();
    account2 = new Account(accountLabel2, currency2, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account2.save();
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account1.getId());
    Account.delete(account2.getId());
  }

  @Test
  public void formForTransactionIsPrepared() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow, R.id.Recurrence);
  }

  @Test
  public void formForTransferIsPrepared() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSFER);
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow,
        R.id.TransferAccountRow, R.id.Recurrence);
  }

  @Test
  public void formForSplitIsPrepared() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_SPLIT);
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.SplitContainer,
        R.id.PayeeRow, R.id.AccountRow, R.id.Recurrence);
  }

  @Test
  public void formForTemplateIsPrepared() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
    i.putExtra(KEY_NEW_TEMPLATE, true);
    mActivityRule.launchActivity(i);
    checkEffectiveVisible(R.id.TitleRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow, R.id.Recurrence);
    checkEffectiveGone(R.id.PB);
  }

  @Test
  public void accountIdInExtraShouldPopulateSpinner() {
    Account[] allAccounts = {account1, account2};
    for (Account a : allAccounts) {
      Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
      i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
      i.putExtra(DatabaseConstants.KEY_ACCOUNTID, a.getId());
      mActivityRule.launchActivity(i);
      onView(withId(R.id.Account)).check(matches(withSpinnerText(a.getLabel())));
      mActivityRule.getActivity().finish();
    }
  }

  @Test
  public void currencyInExtraShouldPopulateSpinner() {
    CurrencyUnit[] allCurrencies = {currency1, currency2};
    for (CurrencyUnit c : allCurrencies) {
      //we assume that Fixture has set up the default account with id 1
      Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
      i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
      i.putExtra(DatabaseConstants.KEY_CURRENCY, c.code());
      mActivityRule.launchActivity(i);
      assertEquals("Account is not selected", c, mActivityRule.getActivity().getCurrentAccount().getCurrency());
      mActivityRule.getActivity().finish();
    }
  }

  @Test
  public void saveAsNewWorksMultipleTimesInARow() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
    i.putExtra(DatabaseConstants.KEY_ACCOUNTID, account1.getId());
    mActivityRule.launchActivity(i);
    String success = mActivityRule.getActivity().getString(R.string.save_transaction_and_new_success);
    int times = 5;
    int amount = 2;
    for (int j = 0; j < times; j++) {
      onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).perform(typeText(String.valueOf(amount)));
      onView(withId(R.id.SAVE_AND_NEW_COMMAND)).perform(click());
      onView(withText(success)).check(matches(isDisplayed()));
    }
    //we assume two fraction digits
    assertEquals("Transaction sum does not match saved transactions", account1.getTransactionSum(null), -amount * times * 100);
  }

  @Test
  public void shouldSaveTemplateWithAmount() {
    Template template = Template.getTypedNewInstance(TYPE_TRANSFER, account1.getId(), false, null);
    template.setTransferAccountId(account2.getId());
    template.setTitle("Test template");
    template.save();
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_TEMPLATEID, template.getId());
    mActivityRule.launchActivity(i);
    int amount = 2;
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).perform(typeText(String.valueOf(amount)));
    onView(withId(R.id.SAVE_COMMAND)).perform(click());
    Template restored = Template.getInstanceFromDb(template.getId());
    assertEquals(-amount * 100, restored.getAmount().getAmountMinor().longValue());
  }
}
