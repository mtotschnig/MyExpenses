package org.totschnig.myexpenses.test.espresso;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;

import org.junit.BeforeClass;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.testutils.BaseMyExpensesTest;

public final class MyExpensesIntentTest extends BaseMyExpensesTest {

  private static String accountLabel1;
  private static Account account1;

  @BeforeClass
  public static void fixture() {
    accountLabel1 = "Test label 1";
    account1 = new Account(accountLabel1, 0, "");
    account1.save();
  }

  @Test
  public void shouldNavigateToAccountReceivedThroughIntent() {
    Intent i = new Intent(getTargetContext(), MyExpenses.class)
        .putExtra(KEY_ROWID, account1.getId());
    testScenario = ActivityScenario.launch(i);
    onView(allOf(
        withText(accountLabel1),
        withParent(withId(R.id.toolbar))))
        .check(matches(isDisplayed()));
  }
}
