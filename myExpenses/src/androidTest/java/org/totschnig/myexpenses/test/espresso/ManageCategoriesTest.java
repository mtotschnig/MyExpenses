package org.totschnig.myexpenses.test.espresso;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.testutils.BaseUiTest;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.totschnig.myexpenses.testutils.Espresso.openActionBarOverflowOrOptionsMenu;

public class ManageCategoriesTest extends BaseUiTest {

  @Rule
  public ActivityTestRule<ManageCategories> mActivityRule =
      new ActivityTestRule<>(ManageCategories.class);

  @AfterClass
  public static void tearDown() {
    MyApplication.getInstance().getContentResolver().delete(Category.CONTENT_URI, null, null);
  }

  @Test
  public void setupCategoriesShouldPopulateList() {
    assertThat(waitForAdapter().getCount()).isEqualTo(0);
    openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
    onView(withText(R.string.menu_categories_setup_default)).perform(click());
    assertThat(waitForAdapter().getCount()).isGreaterThan(0);
  }

  @Override
  protected ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule() {
    return mActivityRule;
  }
}
