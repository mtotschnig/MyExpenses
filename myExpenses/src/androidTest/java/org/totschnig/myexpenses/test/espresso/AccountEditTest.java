package org.totschnig.myexpenses.test.espresso;

import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.model.Account;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertTrue;

public class AccountEditTest {

  @Rule
  public ActivityTestRule<AccountEdit> mActivityRule =
      new ActivityTestRule<>(AccountEdit.class, false, false);
  private static final String LABEL = "Test account";

  @After
  public void tearDown() throws RemoteException, OperationApplicationException {
    final long accountId = Account.findAny(LABEL);
    if (accountId > -1) {
      Account.delete(accountId);
    }
  }

  @Test
  public void saveAccount() {
    Intent i = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), AccountEdit.class);
    mActivityRule.launchActivity(i);
    onView(withId(R.id.Label)).perform(typeText(LABEL));
    onView(withId(R.id.SAVE_COMMAND)).perform(click());
    assertTrue(mActivityRule.getActivity().isFinishing());
    assertTrue(Account.findAny(LABEL) > -1);
  }
}
