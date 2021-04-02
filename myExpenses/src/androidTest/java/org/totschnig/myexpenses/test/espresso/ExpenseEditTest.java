package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Template;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.testutils.BaseUiTest;

import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;

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
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID;
import static org.totschnig.myexpenses.testutils.Espresso.checkEffectiveGone;
import static org.totschnig.myexpenses.testutils.Espresso.checkEffectiveVisible;
import static org.totschnig.myexpenses.testutils.Espresso.withIdAndParent;

public class ExpenseEditTest extends BaseUiTest {

  private ActivityScenario<ExpenseEdit> activityScenario = null;
  private Account account1;
  private Account account2;
  private CurrencyUnit currency1;
  private CurrencyUnit currency2;

  @Before
  public void fixture() {
    configureLocale(Locale.GERMANY);
    currency1 = new CurrencyUnit(Currency.getInstance("USD"));
    currency2 = new CurrencyUnit(Currency.getInstance("EUR"));
    String accountLabel1 = "Test label 1";
    account1 = new Account(accountLabel1, currency1, 0, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account1.save();
    String accountLabel2 = "Test label 2";
    account2 = new Account(accountLabel2, currency2, 0, "", AccountType.BANK, Account.DEFAULT_COLOR);
    account2.save();
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account1.getId());
    Account.delete(account2.getId());
    activityScenario.close();
  }

  @Test
  public void formForTransactionIsPrepared() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
    activityScenario = ActivityScenario.launch(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow);
    checkEffectiveGone(R.id.Status, R.id.Recurrence, R.id.TitleRow);
    clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND);
    checkEffectiveVisible(R.id.TitleRow, R.id.Recurrence);
    checkAccountDependents();
  }

  private void checkAccountDependents() {
    onView(withId(R.id.AmountLabel)).check(matches(withText(String.format(Locale.ROOT, "%s (%s)", getString(R.string.amount), "$"))));
    onView(withId(R.id.DateTimeLabel)).check(matches(withText(String.format(Locale.ROOT, "%s / %s", getString(R.string.date), getString(R.string.time)))));
  }

  @Test
  public void statusIsShownWhenBankAccountIsSelected() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
    i.putExtra(KEY_ACCOUNTID, account2.getId());
    activityScenario = ActivityScenario.launch(i);
    checkEffectiveVisible(R.id.Status);
  }

  @Test
  public void formForTransferIsPrepared() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSFER);
    activityScenario = ActivityScenario.launch(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.AccountRow,
        R.id.TransferAccountRow);
    checkEffectiveGone(R.id.Status, R.id.Recurrence, R.id.TitleRow);
    clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND);
    checkEffectiveVisible(R.id.TitleRow, R.id.Recurrence);
    checkAccountDependents();
  }

  @Test
  public void formForSplitIsPrepared() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_SPLIT);
    activityScenario = ActivityScenario.launch(i);
    checkEffectiveVisible(R.id.DateTimeRow, R.id.AmountRow, R.id.CommentRow, R.id.SplitContainer,
        R.id.PayeeRow, R.id.AccountRow);
    checkEffectiveGone(R.id.Status, R.id.Recurrence, R.id.TitleRow);
    clickMenuItem(R.id.CREATE_TEMPLATE_COMMAND);
    checkEffectiveVisible(R.id.TitleRow, R.id.Recurrence);
    checkAccountDependents();
  }

  @Test
  public void formForTemplateIsPrepared() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
    i.putExtra(KEY_NEW_TEMPLATE, true);
    activityScenario = ActivityScenario.launch(i);
    checkEffectiveVisible(R.id.TitleRow, R.id.AmountRow, R.id.CommentRow, R.id.CategoryRow,
        R.id.PayeeRow, R.id.AccountRow, R.id.Recurrence, R.id.DefaultActionRow);
    checkEffectiveGone(R.id.PB);
  }

  @Test
  public void accountIdInExtraShouldPopulateSpinner() {
    Account[] allAccounts = {account1, account2};
    for (Account a : allAccounts) {
      Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
      i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
      i.putExtra(DatabaseConstants.KEY_ACCOUNTID, a.getId());
      activityScenario = ActivityScenario.launch(i);
      onView(withId(R.id.Account)).check(matches(withSpinnerText(a.getLabel())));
    }
  }

  @Test
  public void currencyInExtraShouldPopulateSpinner() {
    CurrencyUnit[] allCurrencies = {currency1, currency2};
    for (CurrencyUnit c : allCurrencies) {
      //we assume that Fixture has set up the default account with id 1
      Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
      i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
      i.putExtra(DatabaseConstants.KEY_CURRENCY, c.getCode());
      activityScenario = ActivityScenario.launch(i);
      activityScenario.onActivity(activity ->
          assertEquals("Selected account has wrong currency", c.getCode(), activity.getCurrentAccount().getCurrency().getCode()));
    }
  }

  @Test
  public void saveAsNewWorksMultipleTimesInARow() {
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(OPERATION_TYPE, TYPE_TRANSACTION);
    i.putExtra(DatabaseConstants.KEY_ACCOUNTID, account1.getId());
    activityScenario = ActivityScenario.launch(i);
    String success = getString(R.string.save_transaction_and_new_success);
    int times = 5;
    int amount = 2;
    clickMenuItem(R.id.SAVE_AND_NEW_COMMAND, false); //toggle save and new on
    for (int j = 0; j < times; j++) {
      onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).perform(typeText(String.valueOf(amount)));
      onView(withId(R.id.CREATE_COMMAND)).perform(click());
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
    Intent i = new Intent(getTargetContext(), ExpenseEdit.class);
    i.putExtra(KEY_TEMPLATEID, template.getId());
    activityScenario = ActivityScenario.launch(i);
    int amount = 2;
    onView(withIdAndParent(R.id.AmountEditText, R.id.Amount)).perform(click(), typeText(String.valueOf(amount)));
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
    Template restored = Template.getInstanceFromDb(template.getId());
    assertEquals(TYPE_TRANSFER, restored.operationType());
    assertEquals(-amount * 100, restored.getAmount().getAmountMinor());
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return Objects.requireNonNull(activityScenario);
  }
}
