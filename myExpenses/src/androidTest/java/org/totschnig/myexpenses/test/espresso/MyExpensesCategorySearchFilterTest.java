package org.totschnig.myexpenses.test.espresso;

import android.content.ContentUris;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.junit.AfterClass;
import org.junit.Before;
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
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.CursorMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.totschnig.myexpenses.testutils.Matchers.withCategoryLabel;

public final class MyExpensesCategorySearchFilterTest extends BaseUiTest {

  @Rule
  public ActivityScenarioRule<MyExpenses> scenarioRule =
      new ActivityScenarioRule<>(MyExpenses.class);
  private static String catLabel1;
  private static String catLabel2;
  private static String catLabel1Sub;
  private static long id1Main, id1Sub, id2Main;
  private static Account account;

  @BeforeClass
  public static void fixture() {
    catLabel1 = "Main category 1";
    catLabel1Sub = "Sub category 1";
    catLabel2 = "Test category 2";
    final CurrencyUnit currency = new CurrencyUnit(Currency.getInstance("EUR"));
    account = new Account("Test account 1", currency, 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account.save();
    long categoryId1 = Category.write(0L, catLabel1, null);
    long categoryId1Sub = Category.write(0L, catLabel1Sub, categoryId1);
    long categoryId2 = Category.write(0L, catLabel2,null);
    Transaction op = Transaction.getNewInstance(account.getId());
    op.setAmount(new Money(currency, -1200L));
    op.setCatId(categoryId1);
    id1Main = ContentUris.parseId(op.save());
    op.setCatId(categoryId2);
    id2Main = ContentUris.parseId(op.saveAsNew());
    op.setCatId(categoryId1Sub);
    id1Sub = ContentUris.parseId(op.saveAsNew());
  }

  @AfterClass
  public static void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account.getId());
  }

  @Before
  public void startSearch() throws TimeoutException {
    waitForAdapter();
    allLabelsAreDisplayed();
    onView(withId(R.id.SEARCH_COMMAND)).perform(click());
    onView(withText(R.string.category)).perform(click());
  }

  private void allLabelsAreDisplayed() {
    isDisplayed(id1Main);
    isDisplayed(id1Sub);
    isDisplayed(id2Main);
  }

  public void endSearch(String text) {
    //switch off filter
    onView(withId(R.id.SEARCH_COMMAND)).perform(click());
    onView(withText(text)).inRoot(isPlatformPopup()).perform(click());
    allLabelsAreDisplayed();
  }

  @Test
  public void catFilterChildShouldHideTransaction() {
    onData(withCategoryLabel(is(catLabel1)))
        .inAdapterView(withId(R.id.list)).perform(click());
    onData(withCategoryLabel(is(catLabel1Sub)))
        .inAdapterView(withId(R.id.list)).perform(click());
    isDisplayed(id1Sub);
    isNotDisplayed(id1Main);
    isNotDisplayed(id2Main);
    endSearch(catLabel1Sub);
  }

  @Test
  public void catFilterMainWithChildrenShouldHideTransaction() {
    onData(withCategoryLabel(is(catLabel1)))
        .inAdapterView(withId(R.id.list)).perform(longClick());
    clickMenuItem(R.id.SELECT_COMMAND_MULTIPLE, true);
    isDisplayed(id1Main);
    isDisplayed(id1Sub);
    isNotDisplayed(id2Main);
    endSearch(catLabel1);
  }

  @Test
  public void catFilterMainWithoutChildrenShouldHideTransaction() {
    onData(withCategoryLabel(is(catLabel2)))
        .inAdapterView(withId(R.id.list)).perform(click());
    isDisplayed(id2Main);
    isNotDisplayed(id1Main);
    isNotDisplayed(id1Sub);
    endSearch(catLabel2);
  }

  private void isDisplayed(long id) {
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, id))
        .inAdapterView(getWrappedList()).check(matches(ViewMatchers.isDisplayed()));
  }
  private void isNotDisplayed(long id) {
    onView(getWrappedList())
        .check(matches(not(Matchers.withAdaptedData(
            CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, id)))));
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return scenarioRule.getScenario();
  }
}
