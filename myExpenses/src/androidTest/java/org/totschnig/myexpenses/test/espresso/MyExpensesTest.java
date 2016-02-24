package org.totschnig.myexpenses.test.espresso;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.ViewPager;
import android.widget.Button;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.util.Utils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class MyExpensesTest {
  private static boolean welcomeScreenHasBeenDismissed = false;
  @Rule public final IntentsTestRule<MyExpenses> main =
      new IntentsTestRule<>(MyExpenses.class);

  @Before
  public void dismissWelcomeScreen() {
    if (!welcomeScreenHasBeenDismissed) {
      onView(withText(containsString(main.getActivity().getString(R.string.dialog_title_welcome))))
          .check(matches(isDisplayed()));
      onView(withText(android.R.string.ok)).perform(click());
      welcomeScreenHasBeenDismissed = true;
    }
  }

  @AfterClass
  public static void removeData() {
    MyApplication.cleanUpAfterTest();
  }

  @Test
  public void viewPagerIsSetup() {
    MyExpenses activity = main.getActivity();
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
    stubExpenseEditIntentWithSequenceCount(MyExpenses.TRESHOLD_REMIND_CONTRIB + 1);
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
    onView(withText(containsString(main.getActivity().getString(R.string.menu_contrib))))
        .check(matches(isDisplayed()));
  }

  @Test
  public void ratingDialogIsShown() {
    if (!Utils.IS_FLAVOURED) return;
    stubExpenseEditIntentWithSequenceCount(MyExpenses.TRESHOLD_REMIND_RATE + 1);
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
    onView(withText(containsString(main.getActivity().getString(R.string.dialog_remind_rate_1))))
        .check(matches(isDisplayed()));
  }

  @Test
  public void helpDialogIsOpened() {
    openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
    onView(withText(R.string.menu_help)).perform(click());
    onView(withText(containsString(main.getActivity().getString(R.string.help_MyExpenses_title))))
        .check(matches(isDisplayed()));
    onView(allOf(
        isAssignableFrom(Button.class),
        withText(is(main.getActivity().getString(android.R.string.ok)))))
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
    int [] commands = new int[] {
        R.string.menu_reset,
        R.string.menu_distribution,
        R.string.menu_print
    };
    int[] messages = new int[] {
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
          withText(is(main.getActivity().getString(android.R.string.ok))))).perform(click());
    }
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
