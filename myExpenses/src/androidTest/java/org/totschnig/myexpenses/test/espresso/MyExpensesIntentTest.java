package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;


@RunWith(AndroidJUnit4.class)
public final class MyExpensesIntentTest {
  private static boolean welcomeScreenHasBeenDismissed = false;

  @Rule
  public ActivityTestRule<MyExpenses> mActivityRule =
      new ActivityTestRule<>(MyExpenses.class);

  @Before
  public void dismissWelcomeScreen() {
    if (!welcomeScreenHasBeenDismissed) {
      onView(withText(containsString(mActivityRule.getActivity().getString(R.string.dialog_title_welcome))))
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
  public void shouldNavigateToAccountReceivedThroughIntent() {
    String accountLabel1 = "Test label 1",
      accountLabel2 = "Test label 2";
    Account account1 = new Account(accountLabel1, 0, "");
    account1.save();
    Account account2 = new Account(accountLabel2, 0, "");
    account2.save();
    mActivityRule.getActivity().finish();
    Intent i = new Intent()
        .putExtra(KEY_ROWID, account1.getId())
        .setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.MyExpenses");
    mActivityRule.launchActivity(i);
    onView(allOf(
        withText(accountLabel1),
        withId(R.id.action_bar_title)))
        .check(matches(isDisplayed()));
    mActivityRule.getActivity().finish();
    i = new Intent()
        .putExtra(KEY_ROWID, account2.getId())
        .setClassName("org.totschnig.myexpenses.activity", "org.totschnig.myexpenses.activity.MyExpenses");
    mActivityRule.launchActivity(i);
    onView(allOf(
        withText(accountLabel2),
        withId(R.id.action_bar_title)))
        .check(matches(isDisplayed()));
    mActivityRule.getActivity().finish();
  }
}
