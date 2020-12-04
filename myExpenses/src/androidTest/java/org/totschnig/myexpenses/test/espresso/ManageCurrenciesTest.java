package org.totschnig.myexpenses.test.espresso;

import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCurrencies;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import java.math.BigDecimal;

import androidx.test.filters.FlakyTest;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class ManageCurrenciesTest extends BaseUiTest {

  private static final String CURRENCY_CODE = "EUR";
  @Rule
  public ActivityTestRule<ManageCurrencies> mActivityRule =
      new ActivityTestRule<>(ManageCurrencies.class);

  @FlakyTest
  @Test
  public void changeOfFractionDigitsWithUpdateShouldKeepTransactionSum() throws RemoteException, OperationApplicationException {
    testHelper(true);
  }

  @FlakyTest
  @Test
  public void changeOfFractionDigitsWithoutUpdateShouldChangeTransactionSum() throws RemoteException, OperationApplicationException {
    testHelper(false);
  }

  private void testHelper(boolean withUpdate) throws RemoteException, OperationApplicationException {
    final AppComponent appComponent = ((MyApplication) mActivityRule.getActivity().getApplicationContext()).getAppComponent();
    CurrencyContext currencyContext = appComponent.currencyContext();
    final CurrencyUnit currencyUnit = currencyContext.get(CURRENCY_CODE);
    Account account = new Account("TEST ACCOUNT", currencyUnit, 5000L, "", AccountType.CASH, Account.DEFAULT_COLOR);
    account.save();
    waitForAdapter();
    try {
      Transaction op = Transaction.getNewInstance(account.getId());
      op.setAmount(new Money(currencyUnit, -1200L));
      op.save();
      Money before = account.getTotalBalance();
      assertEquals(0, before.getAmountMajor().compareTo(new BigDecimal(38)));
      final Currency currency = Currency.Companion.create(CURRENCY_CODE, mActivityRule.getActivity());
      onData(is(currency))
          .inAdapterView(withId(android.R.id.list)).perform(click());
      onView(withId(R.id.edt_currency_fraction_digits))
          .perform(replaceText("3"), closeSoftKeyboard());
      if (withUpdate) {
        onView(withId(R.id.checkBox)).perform(click());
      }
      onView(withText(android.R.string.ok)).perform(click());
      onView(withText(allOf(containsString(currency.toString()), containsString("3")))).check(matches(isDisplayed()));
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
      currencyContext.storeCustomFractionDigits(CURRENCY_CODE, 2);
    }
  }

  @Override
  protected ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule() {
    return mActivityRule;
  }

  @Override
  protected int getListId() {
    return android.R.id.list;
  }
}
