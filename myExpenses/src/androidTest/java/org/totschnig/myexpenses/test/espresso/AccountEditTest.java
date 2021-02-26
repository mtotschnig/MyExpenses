package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.testutils.Espresso;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertTrue;
import static org.totschnig.myexpenses.testutils.Matchers.withListSize;

public class AccountEditTest {

  @Rule
  public ActivityTestRule<AccountEdit> mActivityRule =
      new ActivityTestRule<>(AccountEdit.class, false, false);
  private static final String LABEL = "Test account";

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    final long accountId = Account.findAnyOpen(LABEL);
    if (accountId > -1) {
      Account.delete(accountId);
    }
  }

  @Test
  public void saveAccount() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), AccountEdit.class);
    mActivityRule.launchActivity(i);
    onView(withId(R.id.Currency)).check(matches(isDisplayed()));
    onView(withId(R.id.Currency)).perform(Espresso.wait(withListSize(Matchers.greaterThan(0)), 1000));
    onView(withId(R.id.Label)).perform(typeText(LABEL));
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
    assertTrue(mActivityRule.getActivity().isFinishing());
    assertTrue(Account.findAnyOpen(LABEL) > -1);
  }
}
