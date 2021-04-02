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
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

public final class MyExpensesCommentSearchFilterTest extends BaseUiTest {

  private static final String comment1 = "something";
  private static final String comment2 = "different";
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
    op.setAmount(new Money(currency, 1000L));
    op.setComment(comment1);
    op.save();
    op.setComment(comment2);
    op.saveAsNew();
  }

  @AfterClass
  public static void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account.getId());
  }

  @Test
  public void amountFilterShouldHideTransaction() throws TimeoutException {
    waitForAdapter();
    commentIsDisplayed(comment1);
    commentIsDisplayed(comment2);
    onView(withId(R.id.SEARCH_COMMAND)).perform(click());
    onView(withText(R.string.comment)).perform(click());
    onView(withId(R.id.editText)).perform(typeText(comment1));
    onView(withId(android.R.id.button1)).perform(click());
    commentIsDisplayed(comment1);
    commentIsNotDisplayed(comment2);
    //switch off filter
    onView(withId(R.id.SEARCH_COMMAND)).perform(click());
    onView(withText(comment1)).perform(click());
    commentIsDisplayed(comment2);
  }

  private void commentIsDisplayed(String comment) {
    onData(CursorMatchers.withRowString(DatabaseConstants.KEY_COMMENT, comment))
        .inAdapterView(getWrappedList()).check(matches(isDisplayed()));
  }

  private void commentIsNotDisplayed(@SuppressWarnings("SameParameterValue") String comment) {
    onView(getWrappedList())
        .check(matches(not(Matchers.withAdaptedData(
            CursorMatchers.withRowString(DatabaseConstants.KEY_COMMENT, comment)))));
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return scenarioRule.getScenario();
  }
}
