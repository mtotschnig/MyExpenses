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

import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.totschnig.myexpenses.testutils.Espresso.openActionBarOverflowMenu;

public class ManageCategoriesTest extends BaseUiTest {

  @Rule
  public ActivityScenarioRule<ManageCategories> scenarioRule =
      new ActivityScenarioRule<>(ManageCategories.class);

  @AfterClass
  public static void tearDown() {
    MyApplication.getInstance().getContentResolver().delete(Category.CONTENT_URI, null, null);
  }

  @Test
  public void setupCategoriesShouldPopulateList() throws TimeoutException {
    assertThat(waitForAdapter().getCount()).isEqualTo(0);
    openActionBarOverflowMenu();
    onView(withText(R.string.menu_categories_setup_default)).perform(click());
    assertThat(waitForAdapter().getCount()).isGreaterThan(0);
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return scenarioRule.getScenario();
  }
}
