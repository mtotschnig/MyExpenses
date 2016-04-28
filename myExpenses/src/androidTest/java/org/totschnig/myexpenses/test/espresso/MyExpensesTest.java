package org.totschnig.myexpenses.test.espresso;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.matcher.CursorMatchers;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.ViewPager;
import android.widget.AdapterView;
import android.widget.Button;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.util.Utils;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static org.totschnig.myexpenses.test.util.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
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

@RunWith(AndroidJUnit4.class)
public final class MyExpensesTest extends MyExpensesTestBase {

  @Rule
  public final IntentsTestRule<MyExpenses> mActivityRule =
      new IntentsTestRule<>(MyExpenses.class);

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
  public void contribDialogIsShown() {
    MyApplication.PrefKey.NEXT_REMINDER_RATE.putLong(-1);//assumption rating dialog is no longer showable
    MyApplication.PrefKey.NEXT_REMINDER_CONTRIB.remove();
    stubExpenseEditIntentWithSequenceCount(MyExpenses.TRESHOLD_REMIND_CONTRIB + 1);
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
    onView(withText(containsString(mActivityRule.getActivity().getString(R.string.menu_contrib))))
        .check(matches(isDisplayed()));
  }

  @Test
  public void ratingDialogIsShown() {
    if (!Utils.IS_FLAVOURED) return;
    MyApplication.PrefKey.NEXT_REMINDER_RATE.remove();
    stubExpenseEditIntentWithSequenceCount(MyExpenses.TRESHOLD_REMIND_RATE + 1);
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
    onView(withText(containsString(mActivityRule.getActivity().getString(R.string.dialog_remind_rate_1))))
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
    //only when we send this key event, onPrepareOptionsMenu is called before the test
    //mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
    int[] commands = new int[]{
        R.string.menu_reset,
        R.string.menu_distribution,
        R.string.menu_print
    };
    int[] messages = new int[]{
        R.string.dialog_command_disabled_reset_account,
        R.string.dialog_command_disabled_distribution,
        R.string.dialog_command_disabled_reset_account
    };
    for (int i = 0; i < commands.length; i++) {
      openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
      onView(withText(commands[i])).perform(click());
      onView(withText(messages[i])).check(matches(isDisplayed()));
      onView(allOf(
          isAssignableFrom(Button.class),
          withText(is(mActivityRule.getActivity().getString(android.R.string.ok))))).perform(click());
    }
  }

  @Test
  public void newAccountFormIsOpened() {
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    onView(withId(R.id.CREATE_ACCOUNT_COMMAND)).check(matches(isDisplayed()));
    onView(withId(R.id.CREATE_ACCOUNT_COMMAND)).perform(click());
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
        .onChildView(withId(R.id.account_menu)).perform(click());
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
        .onChildView(withId(R.id.account_menu)).perform(click());
    onView(withText(R.string.menu_delete)).check(doesNotExist());
  }

  @Test
  public void deleteConfirmationDialogDeleteButtonDeletes() {
    Account account1 = new Account("Test account", 0, "");
    account1.save();
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, account1.getId()))
        .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.left_drawer)),
            isDisplayed()))
        .onChildView(withId(R.id.account_menu)).perform(click());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(withText(R.string.dialog_title_warning_delete_account)).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(mActivityRule.getActivity().getString(R.string.menu_delete))))).perform(click());
    onView(withId(android.R.id.content));
    assertNull(Account.getInstanceFromDb(account1.getId()));
  }

  @Test
  public void deleteConfirmationDialogCancelButtonCancels() throws RemoteException, OperationApplicationException {
    Account account1 = new Account("Test account", 0, "");
    account1.save();
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    onData(CursorMatchers.withRowLong(DatabaseConstants.KEY_ROWID, account1.getId()))
        .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
            isDescendantOfA(withId(R.id.left_drawer)),
            isDisplayed()))
        .onChildView(withId(R.id.account_menu)).perform(click());
    onView(withText(R.string.menu_delete)).perform(click());
    onView(withText(R.string.dialog_title_warning_delete_account)).check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(mActivityRule.getActivity().getString(android.R.string.cancel))))).perform(click());
    onView(withId(android.R.id.content));
    assertNotNull(Account.getInstanceFromDb(account1.getId()));
    Account.delete(account1.getId());
  }

  @Test
  public void templateScreenIsOpened() {
    onView(withId(R.id.MANAGE_PLANS_COMMAND)).check(matches(isDisplayed()));
    onView(withId(R.id.MANAGE_PLANS_COMMAND)).perform(click());
    intended(hasComponent(ManageTemplates.class.getName()));
  }

  private void stubExpenseEditIntentWithSequenceCount(long count) {
    Bundle bundle = new Bundle();
    bundle.putLong(ContribInfoDialogFragment.KEY_SEQUENCE_COUNT, count);
    Intent resultData = new Intent();
    resultData.putExtras(bundle);

    Instrumentation.ActivityResult result =
        new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);

    // Stub the Intent.
    intending(hasComponent(ExpenseEdit.class.getName())).respondWith(result);
  }
}
