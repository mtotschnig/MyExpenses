package org.totschnig.myexpenses.test.espresso;

import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.testutils.Matchers;

import java.util.Currency;
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.CursorMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

public final class MyExpensesAmountSearchFilterTest extends BaseUiTest {

  private static final long amount1 = -1200L;
  private static final long amount2 = -3400L;
  @Rule
  public ActivityScenarioRule<MyExpenses> scenarioRule =
      new ActivityScenarioRule<>(MyExpenses.class);
  private static Account account;

  @BeforeClass
  public static void fixture() {
    final CurrencyUnit currency = new CurrencyUnit(Currency.getInstance("EUR"));
    account = new Account("Test account 1",  currency, 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account.save();
    Transaction op = Transaction.getNewInstance(account.getId());
    op.setAmount(new Money(currency, amount1));
    op.save();
    op.setAmount(new Money(currency, amount2));
    op.saveAsNew();
  }

  @AfterClass
  public static void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account.getId());
  }

  @Test
  public void amountFilterShouldHideTransaction() throws TimeoutException {
    waitForAdapter();
    amountIsDisplayed(amount1);
    amountIsDisplayed(amount2);
    onView(withId(R.id.SEARCH_COMMAND)).perform(click());
    onView(withText(R.string.amount)).perform(click());
    onView(withId(R.id.amount1)).perform(typeText("12"));
    onView(withId(android.R.id.button1)).perform(click());
    amountIsDisplayed(amount1);
    amountIsNotDisplayed(amount2);
    //switch off filter
    onView(withId(R.id.SEARCH_COMMAND)).perform(click());
    onView(withSubstring(getString(R.string.expense))).perform(click());
    amountIsDisplayed(amount2);
  }

  private void amountIsDisplayed(long amount) {
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_AMOUNT, amount))
        .inAdapterView(getWrappedList()).check(matches(isDisplayed()));
  }

  private void amountIsNotDisplayed(@SuppressWarnings("SameParameterValue") long amount) {
    onView(getWrappedList())
        .check(matches(not(Matchers.withAdaptedData(
            CursorMatchers.withRowLong(DatabaseConstants.KEY_AMOUNT, amount)))));
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return scenarioRule.getScenario();
  }
}
