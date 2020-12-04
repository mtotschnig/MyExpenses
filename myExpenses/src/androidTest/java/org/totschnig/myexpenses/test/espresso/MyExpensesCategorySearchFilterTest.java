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
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.testutils.Matchers;

import java.util.Currency;

import androidx.test.espresso.matcher.CursorMatchers;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.totschnig.myexpenses.testutils.Matchers.withCategoryLabel;

public final class MyExpensesCategorySearchFilterTest extends BaseUiTest {

  @Rule
  public ActivityTestRule<MyExpenses> mActivityRule =
      new ActivityTestRule<>(MyExpenses.class);
  private static String catLabel1;
  private static String catLabel2;
  private static Account account;

  @BeforeClass
  public static void fixture() {
    catLabel1 = "Test category 1";
    catLabel2 = "Test category 2";
    final CurrencyUnit currency = new CurrencyUnit(Currency.getInstance("EUR"));
    account = new Account("Test account 1", currency, 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account.save();
    long categoryId1 = Category.write(0L, catLabel1, null);
    long categoryId2 = Category.write(0L, catLabel2,null);
    Transaction op = Transaction.getNewInstance(account.getId());
    op.setAmount(new Money(currency, -1200L));
    op.setCatId(categoryId1);
    op.save();
    op.setCatId(categoryId2);
    op.saveAsNew();
  }

  @AfterClass
  public static void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account.getId());
  }

  @Test
  public void catFilterShouldHideTransaction() {
    waitForAdapter();
    labelIsDisplayed(catLabel1);
    labelIsDisplayed(catLabel2);
    onView(withId(R.id.SEARCH_COMMAND)).perform(click());
    onView(withText(R.string.category)).perform(click());
    onData(withCategoryLabel(is(catLabel1)))
        .inAdapterView(withId(R.id.list)).perform(click());
    labelIsDisplayed(catLabel1);
    labelIsNotDisplayed(catLabel2);
    //switch off filter
    onView(withId(R.id.SEARCH_COMMAND)).perform(click());
    onView(withText(catLabel1)).inRoot(isPlatformPopup()).perform(click());
    labelIsDisplayed(catLabel2);
  }

  private void labelIsDisplayed(String label) {
    onData(CursorMatchers.withRowString(DatabaseConstants.KEY_LABEL_MAIN, label))
        .inAdapterView(getWrappedList()).check(matches(isDisplayed()));
  }
  private void labelIsNotDisplayed(String label) {
    onView(getWrappedList())
        .check(matches(not(Matchers.withAdaptedData(
            CursorMatchers.withRowString(DatabaseConstants.KEY_LABEL_MAIN, label)))));
  }

  @Override
  protected ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule() {
    return mActivityRule;
  }
}
