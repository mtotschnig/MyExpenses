package org.totschnig.myexpenses.test.screenshots;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;

import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.rule.GrantPermissionRule;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;
import tools.fastlane.screengrab.locale.LocaleUtil;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.totschnig.myexpenses.testutils.Matchers.first;

/**
 * This test is meant to be run with FastLane Screengrab, but also works on its own.
 */
public class TestMain extends BaseUiTest {
  @ClassRule
  public static final LocaleTestRule localeTestRule = new LocaleTestRule();
  private ActivityScenario<MyExpenses> activityScenario = null;
  @Rule
  public final GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(
      Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR);

  @Test
  public void mkScreenShots() {
    loadFixture(BuildConfig.TEST_SCENARIO == 2);
    scenario();
  }

  private void drawerAction(ViewAction action) {
    //no drawer on w700dp
    try {
      onView(withId(R.id.drawer)).perform(action);
    } catch (NoMatchingViewException ignored) { }
  }

  private void scenario() {
    sleep();
    switch (BuildConfig.TEST_SCENARIO) {
      case 1: {
        drawerAction(DrawerActions.open());
        takeScreenshot("summarize");
        drawerAction(DrawerActions.close());
        takeScreenshot("group");
        clickMenuItem(R.id.RESET_COMMAND);
        Espresso.closeSoftKeyboard();
        takeScreenshot("export");
        Espresso.pressBack();
        onView(withSubstring(getString(R.string.split_transaction))).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        Espresso.closeSoftKeyboard();
        takeScreenshot("split");
        Espresso.pressBack();
        clickMenuItem(R.id.DISTRIBUTION_COMMAND);
        takeScreenshot("distribution");
        Espresso.pressBack();
        clickMenuItem(R.id.HISTORY_COMMAND);
        clickMenuItem(R.id.GROUPING_COMMAND);
        onView(withText(R.string.grouping_month)).perform(click());
        clickMenuItem(R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND);
        takeScreenshot("history");
        Espresso.pressBack();
        clickMenuItem(R.id.BUDGET_COMMAND);
        onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        takeScreenshot("budget");
        Espresso.pressBack();
        Espresso.pressBack();
        clickMenuItem(R.id.SETTINGS_COMMAND);
        onView(instanceOf(RecyclerView.class))
            .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.synchronization)),
                click()));
        onView(instanceOf(RecyclerView.class))
            .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_manage_sync_backends_title)),
                click()));
        onView(withText(containsString("Drive"))).perform(click());
        onView(withText(containsString("Dropbox"))).perform(click());
        onView(withText(containsString("WebDAV"))).perform(click());
        takeScreenshot("sync");
        break;
      }
      case 2: {//tablet screenshots
        takeScreenshot("main");
        clickMenuItem(R.id.DISTRIBUTION_COMMAND);
        takeScreenshot("distribution");
        Espresso.pressBack();

        onView(first(withText(containsString(getTestContext().getString(org.totschnig.myexpenses.debug.test.R.string.testData_transaction1SubCat))))).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        Espresso.pressBack();//close keyboard
        onView(withId(R.id.PictureContainer)).perform(click());
        takeScreenshot("edit");
        break;
      }
      default: {
        throw new IllegalArgumentException("Unknown scenario" + BuildConfig.TEST_SCENARIO);
      }
    }

  }

  private void loadFixture(@SuppressWarnings("SameParameterValue") boolean withPicture) {
    //LocaleTestRule only configure for app context, fixture loads resources from instrumentation context
    final Locale testLocale = LocaleUtil.getTestLocale();
    if (testLocale != null) {//if run from Android Studio and not via Screengrab
      configureLocale(testLocale);
    }
    SharedPreferences pref = getApp().getSettings();
    if (pref == null)
      Assert.fail("Could not find prefs");
    pref.edit().putString(PrefKey.HOME_CURRENCY.getKey(), Utils.getSaveDefault().getCurrencyCode()).apply();
    getApp().getLicenceHandler().setLockState(false);

    getApp().fixture.setup(withPicture);
    int current_version = DistributionHelper.getVersionNumber();
    pref.edit()
        .putLong(PrefKey.CURRENT_ACCOUNT.getKey(), getApp().fixture.getAccount1().getId())
        .putInt(PrefKey.CURRENT_VERSION.getKey(), current_version)
        .putInt(PrefKey.FIRST_INSTALL_VERSION.getKey(), current_version)
        .apply();
    final Intent startIntent = new Intent(getApp(), MyExpenses.class);
    activityScenario = ActivityScenario.launch(startIntent);
  }

  private void sleep() {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void takeScreenshot(String fileName) {
    Espresso.onIdle();
    Screengrab.screenshot(fileName);
  }

  @NonNull
  @Override
  protected ActivityScenario<? extends ProtectedFragmentActivity> getTestScenario() {
    return Objects.requireNonNull(activityScenario);
  }
}