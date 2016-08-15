package org.totschnig.myexpenses.test.espresso;

import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.view.KeyEvent;

import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCurrencies;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;

import java.math.BigDecimal;
import java.util.Currency;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressKey;
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

/**
 * Created by michaeltotschnig on 01.03.16.
 */
public class ManageCurrenciesTest {


  @Rule
  public ActivityTestRule<ManageCurrencies> mActivityRule =
      new ActivityTestRule<>(ManageCurrencies.class);


  @Test
  public void changeOfFractionDigitsWithUpdateShouldKeepTransactionSum() throws RemoteException, OperationApplicationException {
    testHelper(true);
  }

  @Test
  public void changeOfFractionDigitsWithoutUpdateShouldChangeTransactionSum() throws RemoteException, OperationApplicationException {
    testHelper(false);
  }

  private void testHelper(boolean withUpdate) throws RemoteException, OperationApplicationException {
    Account account = new Account("TEST ACCOUNT", Currency.getInstance("EUR"), 5000L, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account.save();
    Transaction op = Transaction.getNewInstance(account.getId());
    op.setAmount(new Money(Currency.getInstance("EUR"), -1200L));
    op.save();
    Money before = account.getTotalBalance();
    assertEquals(0, before.getAmountMajor().compareTo(new BigDecimal(38)));
    onData(is(EUR))
        .inAdapterView(withId(android.R.id.list)).perform(click());
    onView(withId(R.id.EditTextDialogInput))
        .perform(replaceText("3"), closeSoftKeyboard(), pressKey(KeyEvent.KEYCODE_ENTER));
    if (withUpdate)
      onView(withId(R.id.checkbox)).perform(click());
    onView(withText(android.R.string.ok)).perform(click());
    onView(withText(allOf(containsString(EUR.toString()), containsString("3")))).check(matches(isDisplayed()));
    Money after = Account.getInstanceFromDb(account.getId()).getTotalBalance();
    if (withUpdate) {
      assertEquals(0, before.getAmountMajor().compareTo(after.getAmountMajor()));
      assertEquals(before.getAmountMinor() * 10, after.getAmountMinor().longValue());
    } else {
      assertEquals(0, before.getAmountMajor().divide(new BigDecimal(10)).compareTo(after.getAmountMajor()));
      assertEquals(before.getAmountMinor(), after.getAmountMinor());
    }
    Account.delete(account.getId());
    Money.storeCustomFractionDigits("EUR", 2);
  }
}
