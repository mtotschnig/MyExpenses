package org.totschnig.myexpenses.test.espresso;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.activity.MyExpenses;

import static org.junit.Assert.assertTrue;

/**
 * Created by michaeltotschnig on 22.02.16.
 */
@RunWith(AndroidJUnit4.class)
public final class DummyTest {
  @Rule public final ActivityTestRule<MyExpenses> main =
      new ActivityTestRule<>(MyExpenses.class);

  @Test public void noneOfTheThings() {
    assertTrue(true);
  }
}
