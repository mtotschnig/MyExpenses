package org.totschnig.myexpenses.test.espresso;

import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.ViewPager;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.containsString;
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
}
