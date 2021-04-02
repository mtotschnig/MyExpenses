package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.testutils.Espresso;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;

import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.CursorMatchers;
import androidx.viewpager.widget.ViewPager;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public final class MyExpensesTest extends BaseUiTest {
  private Account account;

  private ActivityScenario<MyExpenses> activityScenario = null;

  @Before
  public void fixture() {
    account = new Account("Test account 1", new CurrencyUnit(Currency.getInstance("EUR")), 0, "",
        AccountType.CASH, Account.DEFAULT_COLOR);
    account.save();
    Intent i = new Intent(getTargetContext(), MyExpenses.class);
    i.putExtra(KEY_ROWID, account.getId());
    configureLocale(Locale.GERMANY);
    activityScenario = ActivityScenario.launch(i);
    Intents.init();
  }

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    Account.delete(account.getId());
    Intents.release();
  }

  @Test
  public void viewPagerIsSetup() {
    onView(withText(containsString(getString(R.string.no_expenses))))
        .check(matches(isDisplayed()));

    activityScenario.onActivity(activity -> {
      FragmentPagerAdapter adapter =
          (FragmentPagerAdapter) ((ViewPager) activity.findViewById(R.id.viewPager)).getAdapter();
      Assert.assertNotNull(adapter);
      assertEquals(adapter.getCount(), 1);
    });
  }

  @Test
  public void floatingActionButtonOpensForm() {
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
    intended(hasComponent(ExpenseEdit.class.getName()));
  }

  @Test
  public void helpDialogIsOpened() {
    Espresso.openActionBarOverflowMenu();
    onData(hasToString(getString(R.string.menu_help))).perform(click());
    onView(withText(containsString(getString(R.string.help_MyExpenses_title))))
        .check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(getApp().getString(android.R.string.ok)))))
        .check(matches(isDisplayed()));
  }

  @Test
  public void settingsScreenIsOpened() {
    Espresso.openActionBarOverflowMenu();
    onData(hasToString(getString(R.string.menu_settings))).perform(click());
    intended(hasComponent(MyPreferenceActivity.class.getName()));
  }

  @Test
  public void inActiveItemsOpenDialog() {
    testInActiveItemHelper(R.id.RESET_COMMAND,
        R.string.dialog_command_disabled_reset_account);
    testInActiveItemHelper(R.id.DISTRIBUTION_COMMAND,
        R.string.dialog_command_disabled_distribution);
    testInActiveItemHelper(R.id.PRINT_COMMAND,
        R.string.dialog_command_disabled_reset_account);
  }

  /**
   * Call a menu item and verify that a message is shown in dialog
   */
  private void testInActiveItemHelper(int menuItemId, int messageResId) {
    clickMenuItem(menuItemId);
    onView(withText(messageResId)).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(getString(android.R.string.ok))))).perform(click());
  }

  @Test
  public void newAccountFormIsOpened() {
    openDrawer();
    onView(withId(R.id.expansionTrigger)).perform(click());
    onView(withText(R.string.menu_create_account)).perform(click());
    intended(allOf(hasComponent(AccountEdit.class.getName()),
        not(hasExtraWithKey(DatabaseConstants.KEY_ROWID))));
  }

  public void openDrawer() {
    try {
      onView(withId(R.id.drawer)).perform(DrawerActions.open());
    } catch (NoMatchingViewException e) { /*drawerLess layout*/ }
  }

  @Test
  public void editAccountFormIsOpened() {
    openDrawer();
    onData(anything()).inAdapterView(allOf(
        isAssignableFrom(AdapterView.class),
        isDescendantOfA(withId(R.id.accountList)),
        isDisplayed()))
        .atPosition(0)
        .perform(longClick());
    onView(withText(R.string.menu_edit)).perform(click());
    intended(allOf(hasComponent(AccountEdit.class.getName()), hasExtraWithKey(DatabaseConstants.KEY_ROWID)));
  }

  @Test
  public void deleteConfirmationDialogDeleteButtonDeletes() throws InterruptedException {
    // only if there are two accounts, the delete functionality is available
    Account account2 = new Account("Test account 2", 0, "");
    account2.save();
    Thread.sleep(500);
    openDrawer();
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, account2.getId()))
        .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.accountList)),
            isDisplayed()))
        .perform(longClick());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(withText(getDialogTitleWarningDeleteAccount())).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(getString(R.string.menu_delete))))).perform(click());
    onView(withId(android.R.id.content));
    assertNull(Account.getInstanceFromDb(account2.getId()));
  }

  @NonNull
  private String getDialogTitleWarningDeleteAccount() {
    return getQuantityString(R.plurals.dialog_title_warning_delete_account, 1);
  }

  @Test
  public void deleteConfirmationDialogCancelButtonCancels() throws RemoteException, OperationApplicationException {
    // only if there are two accounts, the delete functionality is available
    Account account2 = new Account("Test account 2", 0, "");
    account2.save();
    openDrawer();
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, account2.getId()))
        .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.accountList)),
            isDisplayed()))
        .perform(longClick());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(withText(getDialogTitleWarningDeleteAccount())).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(getString(android.R.string.cancel))))).perform(click());
    onView(withId(android.R.id.content));
    assertNotNull(Account.getInstanceFromDb(account2.getId()));
    Account.delete(account2.getId());
  }

  @Test
  public void deleteConfirmationDialogShowsLabelOfAccountToBeDeleted() throws RemoteException, OperationApplicationException {
    String label1 = "Some first account";
    String label2 = "Another second account";
    Account account1 = new Account(label1, 0, "");
    account1.save();
    Account account2 = new Account(label2, 0, "");
    account2.save();

    //we try to delete account 1
    openDrawer();
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, account1.getId()))
        .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.accountList)),
            isDisplayed()))
        .perform(longClick());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(withSubstring(getString(R.string.warning_delete_account, label1))).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(android.R.string.cancel))).perform(click());

    //we try to delete account 2
    openDrawer();
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, account2.getId()))
        .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.accountList)),
            isDisplayed()))
        .perform(longClick());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(withSubstring(getString(R.string.warning_delete_account, label2))).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(android.R.string.cancel))).perform(click());
    Account.delete(account1.getId());
    Account.delete(account2.getId());
  }

  @Test
  public void templateScreenIsOpened() {
    clickMenuItem(R.id.MANAGE_TEMPLATES_COMMAND);
    intended(hasComponent(ManageTemplates.class.getName()));
  }

  @Test
  public void titleAndSubtitleAreSetAndSurviveOrientationChange() {
    checkTitle();
    rotate();
    checkTitle();
  }

  private void checkTitle() {
    onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.toolbar)), withText("Test account 1"))).check(matches(isDisplayed()));
    onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.toolbar)), withText("0,00 €"))).check(matches(isDisplayed()));
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return Objects.requireNonNull(activityScenario);
  }
}
