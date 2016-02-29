package org.totschnig.myexpenses.test.espresso;

import android.support.test.espresso.NoMatchingViewException;

import org.junit.Before;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

/**
 * Created by michaeltotschnig on 29.02.16.
 */
public class MyExpensesTestBase {

  @Before
  public void dismissWelcomeScreen() {
    try {
      onView(withText(containsString(MyApplication.getInstance().getString(R.string.dialog_title_welcome))))
          .check(matches(isDisplayed()));
      onView(withText(android.R.string.ok)).perform(click());
    } catch (NoMatchingViewException e) {
    }
  }
}
