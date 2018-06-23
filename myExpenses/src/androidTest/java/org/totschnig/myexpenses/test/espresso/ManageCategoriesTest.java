package org.totschnig.myexpenses.test.espresso;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.testutils.Matchers;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.greaterThan;
import static org.totschnig.myexpenses.testutils.Espresso.openActionBarOverflowOrOptionsMenu;

public class ManageCategoriesTest {

  @Rule
  public ActivityTestRule<ManageCategories> mActivityRule =
      new ActivityTestRule<>(ManageCategories.class);

  @AfterClass
  public static void tearDown() {
    MyApplication.getInstance().getContentResolver().delete(Category.CONTENT_URI, null, null);
  }

  @Test
  public void setupCategoriesShouldPopulateList() {
    onView(withId(R.id.list)).check(matches(Matchers.withListSize(0)));
    openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
    onView(withText(R.string.menu_categories_setup_default)).perform(click());
    onView(withId(R.id.list)).check(matches(Matchers.withListSize(greaterThan(0))));
    //TODO cleanup
  }
}
