package org.totschnig.myexpenses.test.espresso;

import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.support.test.espresso.matcher.CursorMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import java.util.Currency;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;


@RunWith(AndroidJUnit4.class)
public final class MyExpensesSearchFilterTest extends MyExpensesTestBase {

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
    account = Account.getInstanceFromDb(0);
    long categoryId1 = Category.write(0L, catLabel1, null);
    long categoryId2 = Category.write(0L, catLabel2,null);
    Transaction op = Transaction.getNewInstance(account.getId());
    op.setAmount(new Money(Currency.getInstance("USD"), -1200L));
    op.setCatId(categoryId1);
    op.save();
    op.setCatId(categoryId2);
    op.saveAsNew();
  }

  @AfterClass
  public static void tearDown() throws RemoteException, OperationApplicationException {
    account.reset(null, Account.EXPORT_HANDLE_DELETED_DO_NOTHING, null);
  }



  @Test
  public void catFilterShouldHideTransaction() {
    labelIsDisplayed(catLabel1);
    labelIsDisplayed(catLabel2);
    onView(withId(R.id.SEARCH_COMMAND)).perform(click());
    onView(withText(R.string.category)).perform(click());
    onData(CursorMatchers.withRowString(DatabaseConstants.KEY_LABEL, catLabel1))
        .inAdapterView(withId(R.id.list)).perform(click());
    labelIsDisplayed(catLabel1);
    labelIsNotDisplayed(catLabel2);
    //switch off filter
    onView(withId(R.id.SEARCH_COMMAND)).perform(click());
    onView(withText(catLabel1)).perform(click());
    labelIsDisplayed(catLabel2);
  }

  private void labelIsDisplayed(String label) {
    onData(CursorMatchers.withRowString(DatabaseConstants.KEY_LABEL_MAIN, label))
        .inAdapterView(allOf(
            isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.list)),
            isDisplayed())).check(matches(isDisplayed()));
  }
  private void labelIsNotDisplayed(String label) {
    onView(allOf(
        isAssignableFrom(AdapterView.class),
        isDescendantOfA(withId(R.id.list)),
        isDisplayed()))
        .check(matches(not(withAdaptedData(
            CursorMatchers.withRowString(DatabaseConstants.KEY_LABEL_MAIN, label)))));
  }
  private static Matcher<View> withAdaptedData(final Matcher<Object> dataMatcher) {
    return new TypeSafeMatcher<View>() {

      @Override
      public void describeTo(Description description) {
        description.appendText("with class name: ");
        dataMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        if (!(view instanceof AdapterView)) {
          return false;
        }
        @SuppressWarnings("rawtypes")
        Adapter adapter = ((AdapterView) view).getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
          if (dataMatcher.matches(adapter.getItem(i))) {
            return true;
          }
        }
        return false;
      }
    };
  }
}
