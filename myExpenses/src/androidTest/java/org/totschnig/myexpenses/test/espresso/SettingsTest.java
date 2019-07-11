package org.totschnig.myexpenses.test.espresso;

import android.os.Build;
import androidx.annotation.NonNull;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.rule.ActivityTestRule;
import androidx.appcompat.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BackupRestoreActivity;
import org.totschnig.myexpenses.activity.CsvImportActivity;
import org.totschnig.myexpenses.activity.GrisbiImport;
import org.totschnig.myexpenses.activity.ManageCategories;
import org.totschnig.myexpenses.activity.ManageCurrencies;
import org.totschnig.myexpenses.activity.ManageMethods;
import org.totschnig.myexpenses.activity.ManageParties;
import org.totschnig.myexpenses.activity.ManageSyncBackends;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.activity.QifImport;
import org.totschnig.myexpenses.activity.RoadmapVoteActivity;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.testutils.BaseUiTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.instanceOf;

public class SettingsTest extends BaseUiTest {

  @Rule
  public final IntentsTestRule<MyPreferenceActivity> mActivityRule =
      new IntentsTestRule<>(MyPreferenceActivity.class, false, true);

  @Test
  public void manageCategories() {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_manage_categories_title)),
            click()));
    intended(hasComponent(ManageCategories.class.getName()));
    onView(withText(R.string.pref_manage_categories_title)).check(matches(isDisplayed()));
  }

  @Test
  public void manageParties() {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_manage_parties_title)),
            click()));
    intended(hasComponent(ManageParties.class.getName()));
  }

  @NonNull
  private Matcher<View> getRootMatcher() {
    return instanceOf(RecyclerView.class);
  }

  @Test
  public void manageMethods() {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_manage_methods_title)),
            click()));
    intended(hasComponent(ManageMethods.class.getName()));
  }

  @Test
  public void importGrisbi() {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_import_from_grisbi_title)),
            click()));
    intended(hasComponent(GrisbiImport.class.getName()));
  }

  @Test
  public void importQif() {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(
            withText(mActivityRule.getActivity().getString(R.string.pref_import_title, "QIF"))),
            click()));
    intended(hasComponent(QifImport.class.getName()));
  }


  @Test
  public void importCsv() throws InterruptedException {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(
            withText(mActivityRule.getActivity().getString(R.string.pref_import_title, "CSV"))),
            click()));
    handleContribDialog(ContribFeature.CSV_IMPORT);
    intended(hasComponent(CsvImportActivity.class.getName()));
  }

  @Test
  public void backup() {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.menu_backup)),
            click()));
    intended(hasComponent(BackupRestoreActivity.class.getName()));
    onView(withText(R.string.menu_backup)).check(matches(isDisplayed()));
  }

  @Test
  public void restore() {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(
            withText(mActivityRule.getActivity().getString(R.string.pref_restore_title) + " (ZIP)")),
            click()));
    intended(hasComponent(BackupRestoreActivity.class.getName()));
    onView(withText(R.string.pref_restore_title)).check(matches(isDisplayed()));
  }

  @Test
  public void restoreLegacy() {
    Assume.assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT);
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(
            withText(mActivityRule.getActivity().getString(R.string.pref_restore_title) + " (" +
                mActivityRule.getActivity().getString(R.string.pref_restore_alternative) + ")")),
            click()));
    intended(hasComponent(BackupRestoreActivity.class.getName()));
    onView(withText(R.string.restore_no_backup_found)).check(matches(isDisplayed()));
  }

  @Test
  public void manageSync() {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_manage_sync_backends_title)),
            click()));
    intended(hasComponent(ManageSyncBackends.class.getName()));
  }

  @Test
  public void roadmap() {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.roadmap_vote)),
            click()));
    intended(hasComponent(RoadmapVoteActivity.class.getName()));
  }

  @Test
  public void manageCurrencies() {
    onView(getRootMatcher())
        .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_custom_currency_title)),
            click()));
    intended(hasComponent(ManageCurrencies.class.getName()));
  }

  @Override
  protected ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule() {
    return mActivityRule;
  }
}
