package org.totschnig.myexpenses.test.espresso;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.matcher.CursorMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.ViewPager;
import android.widget.AdapterView;
import android.widget.Button;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.util.DistribHelper;

import java.util.Currency;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.totschnig.myexpenses.activity.MyExpenses.KEY_SEQUENCE_COUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.testutils.Espresso.openActionBarOverflowOrOptionsMenu;

@RunWith(AndroidJUnit4.class)
public final class MyExpensesTest extends BaseUiTest {
  private Account account;

  @Rule
  public final IntentsTestRule<MyExpenses> mActivityRule =
      new IntentsTestRule<>(MyExpenses.class, false, false);

  @Before
  public void fixture() {
    account = new Account("Test account 1", CurrencyUnit.create(Currency.getInstance("EUR")), 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account.save();
    Intent i = new Intent();
    i.putExtra(KEY_ROWID, account.getId());
    mActivityRule.launchActivity(i);
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account.getId());
  }

  @Test
  public void viewPagerIsSetup() {
    MyExpenses activity = mActivityRule.getActivity();
    onView(withText(containsString(activity.getString(R.string.no_expenses))))
        .check(matches(isDisplayed()));

    FragmentPagerAdapter adapter =
        (FragmentPagerAdapter) ((ViewPager) activity.findViewById(R.id.viewpager)).getAdapter();
    assertTrue(adapter != null);
    assertEquals(adapter.getCount(), 1);
  }

  @Test
  public void floatingActionButtonOpensForm() {
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
    intended(hasComponent(ExpenseEdit.class.getName()));
  }

  @Test
  public void ratingDialogIsShown() {
    Assume.assumeTrue(!DistribHelper.isGithub());
    PrefKey.NEXT_REMINDER_RATE.remove();
    stubExpenseEditIntentWithSequenceCount(MyExpenses.TRESHOLD_REMIND_RATE + 1);
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
    onView(withId(R.id.rating_how_many))
        .check(matches(isDisplayed()));
  }

  @Test
  public void helpDialogIsOpened() {
    openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
    onView(withText(R.string.menu_help)).perform(click());
    onView(withText(containsString(mActivityRule.getActivity().getString(R.string.help_MyExpenses_title))))
        .check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(mActivityRule.getActivity().getString(android.R.string.ok)))))
        .check(matches(isDisplayed()));
  }

  @Test
  public void settingsScreenIsOpened() {
    openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
    onView(withText(R.string.menu_settings)).perform(click());
    intended(hasComponent(MyPreferenceActivity.class.getName()));
  }

  @Test
  public void inActiveItemsOpenDialog() {
    testInActiveItemHelper(R.id.RESET_COMMAND, R.string.menu_reset,
        R.string.dialog_command_disabled_reset_account);
    testInActiveItemHelper(R.id.DISTRIBUTION_COMMAND, R.string.menu_distribution,
        R.string.dialog_command_disabled_distribution);
    testInActiveItemHelper(R.id.PRINT_COMMAND, R.string.menu_print,
        R.string.dialog_command_disabled_reset_account);
  }

  /**
   * Call a menu item and verify that a message is shown in dialog
   * @param menuItemId
   * @param menuTextResId
   * @param messageResId
   */
  private void testInActiveItemHelper(int menuItemId, int menuTextResId, int messageResId) {
    clickMenuItem(menuItemId, menuTextResId);
    onView(withText(messageResId)).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(mActivityRule.getActivity().getString(android.R.string.ok))))).perform(click());
  }

  @Test
  public void newAccountFormIsOpened() {
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    onView(withId(R.id.expansionTrigger)).perform(click());
    onView(withText(R.string.menu_create_account)).perform(click());
    intended(allOf(hasComponent(AccountEdit.class.getName()),
        not(hasExtraWithKey(DatabaseConstants.KEY_ROWID))));
  }

  @Test
  public void editAccountFormIsOpened() {
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    onData(anything()).inAdapterView(allOf(
        isAssignableFrom(AdapterView.class),
        isDescendantOfA(withId(R.id.left_drawer)),
        isDisplayed()))
        .atPosition(0)
        .perform(longClick());
    onView(withText(R.string.menu_edit)).perform(click());
    intended(allOf(hasComponent(AccountEdit.class.getName()), hasExtraWithKey(DatabaseConstants.KEY_ROWID)));
  }

  @Test
  public void lastAccountDoesNotHaveDeleteAction() {
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    onData(anything()).inAdapterView(allOf(
        isAssignableFrom(AdapterView.class),
        isDescendantOfA(withId(R.id.left_drawer)),
        isDisplayed()))
        .atPosition(0)
        .perform(longClick());
    onView(withText(R.string.menu_delete)).check(doesNotExist());
  }

  @Test
  public void deleteConfirmationDialogDeleteButtonDeletes() {
    // only if there are two accounts, the delete functionality is availalbe
    Account account2 = new Account("Test account 2", 0, "");
    account2.save();
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, account2.getId()))
        .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.left_drawer)),
            isDisplayed()))
        .perform(longClick());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(withText(R.string.dialog_title_warning_delete_account)).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(mActivityRule.getActivity().getString(R.string.menu_delete))))).perform(click());
    onView(withId(android.R.id.content));
    assertNull(Account.getInstanceFromDb(account2.getId()));
  }

  @Test
  public void deleteConfirmationDialogCancelButtonCancels() throws RemoteException, OperationApplicationException {
    // only if there are two accounts, the delete functionality is availalbe
    Account account2 = new Account("Test account 2", 0, "");
    account2.save();
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, account2.getId()))
        .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.left_drawer)),
            isDisplayed()))
        .perform(longClick());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(withText(R.string.dialog_title_warning_delete_account)).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(mActivityRule.getActivity().getString(android.R.string.cancel))))).perform(click());
    onView(withId(android.R.id.content));
    assertNotNull(Account.getInstanceFromDb(account2.getId()));
    Account.delete(account2.getId());
  }

  @Test
  public void deleteConfirmationDialogShowsLabelOfAccountToBeDeleted() throws RemoteException, OperationApplicationException {
    Context context = mActivityRule.getActivity();
    String label1 = "Some first account";
    String label2 = "Another second account";
    Account account1 = new Account(label1, 0, "");
    account1.save();
    Account account2 = new Account(label2, 0, "");
    account2.save();

    //we try to delete acccount 1
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, account1.getId()))
        .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.left_drawer)),
            isDisplayed()))
        .perform(longClick());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(withText(context.getString(R.string.warning_delete_account, label1))).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(android.R.string.cancel))).perform(click());

    //we try to delete acccount 2
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, account2.getId()))
        .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.left_drawer)),
            isDisplayed()))
        .perform(longClick());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(withText(context.getString(R.string.warning_delete_account, label2))).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(android.R.string.cancel))).perform(click());
    Account.delete(account1.getId());
    Account.delete(account2.getId());
  }

  @Test
  public void templateScreenIsOpened() {
    onView(withId(R.id.MANAGE_PLANS_COMMAND)).check(matches(isDisplayed()));
    onView(withId(R.id.MANAGE_PLANS_COMMAND)).perform(click());
    intended(hasComponent(ManageTemplates.class.getName()));
  }

  private void stubExpenseEditIntentWithSequenceCount(long count) {
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_SEQUENCE_COUNT, count);
    Intent resultData = new Intent();
    resultData.putExtras(bundle);

    Instrumentation.ActivityResult result =
        new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);

    // Stub the Intent.
    intending(hasComponent(ExpenseEdit.class.getName())).respondWith(result);
  }

  @Override
  protected ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule() {
    return mActivityRule;
  }
}
