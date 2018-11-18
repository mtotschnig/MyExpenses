package org.totschnig.myexpenses.test.espresso;

import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCurrencies;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;

import java.math.BigDecimal;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.totschnig.myexpenses.model.CurrencyEnum.EUR;

public class ManageCurrenciesTest {

  @Rule
  public ActivityTestRule<ManageCurrencies> mActivityRule =
      new ActivityTestRule<>(ManageCurrencies.class);


  @Test
  public void changeOfFractionDigitsWithUpdateShouldKeepTransactionSum() throws RemoteException, OperationApplicationException, InterruptedException {
    testHelper(true);
  }

  @Test
  public void changeOfFractionDigitsWithoutUpdateShouldChangeTransactionSum() throws RemoteException, OperationApplicationException, InterruptedException {
    testHelper(false);
  }

  private void testHelper(boolean withUpdate) throws RemoteException, OperationApplicationException, InterruptedException {
    final AppComponent appComponent = ((MyApplication) mActivityRule.getActivity().getApplicationContext()).getAppComponent();
    CurrencyContext currencyContext = appComponent.currencyContext();
    final CurrencyUnit currency = currencyContext.get("EUR");
    Account account = new Account("TEST ACCOUNT", currency, 5000L, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account.save();
    try {
      Transaction op = Transaction.getNewInstance(account.getId());
      op.setAmount(new Money(currency, -1200L));
      op.save();
      Money before = account.getTotalBalance();
      assertEquals(0, before.getAmountMajor().compareTo(new BigDecimal(38)));
      onData(is(EUR))
          .inAdapterView(withId(android.R.id.list)).perform(click());
      onView(withId(R.id.edt_number_fraction_digits))
          .perform(replaceText("3"), closeSoftKeyboard());
      onView(withText(android.R.string.ok)).perform(click());
      if (withUpdate)
        onView(withId(R.id.checkbox)).perform(click());
      onView(withText(android.R.string.ok)).perform(click());
      onView(withText(allOf(containsString(EUR.toString()), containsString("3")))).check(matches(isDisplayed()));
      Thread.sleep(500);
      Money after = Account.getInstanceFromDb(account.getId()).getTotalBalance();
      if (withUpdate) {
        assertEquals(0, before.getAmountMajor().compareTo(after.getAmountMajor()));
        assertEquals(before.getAmountMinor() * 10, after.getAmountMinor().longValue());
      } else {
        assertEquals(0, before.getAmountMajor().divide(new BigDecimal(10)).compareTo(after.getAmountMajor()));
        assertEquals(before.getAmountMinor(), after.getAmountMinor());
      }
    } finally {
      Account.delete(account.getId());
      currencyContext.storeCustomFractionDigits("EUR", 2);
    }
  }
}
