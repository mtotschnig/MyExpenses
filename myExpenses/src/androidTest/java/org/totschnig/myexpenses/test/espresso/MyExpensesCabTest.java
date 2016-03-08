package org.totschnig.myexpenses.test.espresso;

import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;

import java.util.Currency;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.totschnig.myexpenses.test.util.Matchers.withListSize;


@RunWith(AndroidJUnit4.class)
public final class MyExpensesCabTest extends MyExpensesTestBase {

  @Rule
  public ActivityTestRule<MyExpenses> mActivityRule =
      new ActivityTestRule<>(MyExpenses.class);
  private static Account account;

  @BeforeClass
  public static void fixture() {
    account = Account.getInstanceFromDb(0);
    Transaction op0 = Transaction.getNewInstance(account.getId());
    op0.setAmount(new Money(Currency.getInstance("USD"),-1200L));
    op0.save();
    int times = 5;
    for (int i = 0; i < times; i++) {
      op0.saveAsNew();
    }
  }

  @AfterClass
  public static void tearDown() throws RemoteException, OperationApplicationException {
    account.reset(null, Account.EXPORT_HANDLE_DELETED_DO_NOTHING, null);
  }

  @Test
  public void cloneCommandIncreasesListSize() {
    int origListSize = getList().getAdapter().getCount();
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    onView(withId(R.id.CLONE_TRANSACTION_COMMAND)).perform(click());
    onView(withId(R.id.SAVE_COMMAND)).perform(click());
    onView(getWrappedList()).check(matches(withListSize(origListSize + 1)));
  }

  @Test
  public void editCommandKeepsListSize() {
    int origListSize = getList().getAdapter().getCount();
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    onView(withId(R.id.EDIT_COMMAND)).perform(click());
    onView(withId(R.id.SAVE_COMMAND)).perform(click());
    onView(getWrappedList()).check(matches(withListSize(origListSize)));
  }

  @Test
  public void createTemplateCommandCreatesTemplate() {
    String templateTitle = "Espresso Template Test";
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    onView(withId(R.id.CREATE_TEMPLATE_COMMAND)).perform(click());
    onView(withText(containsString(mActivityRule.getActivity().getString(R.string.dialog_title_template_title))))
        .check(matches(isDisplayed()));
    onView(withId(R.id.EditTextDialogInput))
        .perform(typeText(templateTitle), closeSoftKeyboard(), pressKey(KeyEvent.KEYCODE_ENTER));

    //((EditText) mSolo.getView(EditText.class, 0)).onEditorAction(EditorInfo.IME_ACTION_DONE);
    onView(withId(R.id.MANAGE_PLANS_COMMAND)).perform(click());
    onView(withText(is(templateTitle))).check(matches(isDisplayed()));
  }

  @Test
  public void deleteCommandDecreasesListSize() {
    int origListSize = getList().getAdapter().getCount();
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    onView(withId(R.id.DELETE_COMMAND)).perform(click());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(getWrappedList()).check(matches(withListSize(origListSize - 1)));
  }

  @Test
  public void deleteCommandCancelKeepsListSize() {
    int origListSize = getList().getAdapter().getCount();
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    onView(withId(R.id.DELETE_COMMAND)).perform(click());
    onView(withText(android.R.string.cancel)).perform(click());
    onView(getWrappedList()).check(matches(withListSize(origListSize)));
  }

  @Test
  public void splitCommandcreatesSplitTransaction() {
    onData(is(instanceOf(Cursor.class)))
        .inAdapterView(getWrappedList())
        .atPosition(1) // position 0 is header
        .perform(longClick());
    onView(withId(R.id.SPLIT_TRANSACTION_COMMAND)).perform(click());
    if (!ContribFeature.SPLIT_TRANSACTION.hasAccess()) {
      onView(withText(R.string.dialog_title_contrib_feature)).check(matches(isDisplayed()));
      onView(withText(R.string.dialog_contrib_no)).perform(click());
    }
    onView(withText(R.string.split_transaction)).check(matches(isDisplayed()));
    //CursoMatchers class does not allow to distinguish between null and 0 in database
/*    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_CATID, DatabaseConstants.SPLIT_CATID))
        .inAdapterView(allOf(
            isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.list)),
            isDisplayed())).check(matches(isDisplayed()));*/
  }

  private StickyListHeadersListView getList() {
      TransactionList currentFragment = mActivityRule.getActivity().getCurrentFragment();
      return (StickyListHeadersListView) currentFragment.getView().findViewById(R.id.list);
  }


  private Matcher<View> getWrappedList() {
    return allOf(
        isAssignableFrom(AdapterView.class),
        isDescendantOfA(withId(R.id.list)),
        isDisplayed());
  }
}
