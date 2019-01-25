package org.totschnig.myexpenses.test.screenshots;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.v7.widget.RecyclerView;

import com.jraska.falcon.FalconSpoonRule;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.testutils.Fixture;
import org.totschnig.myexpenses.util.DistribHelper;

import java.util.Currency;
import java.util.Locale;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.totschnig.myexpenses.testutils.Matchers.first;

/**
 * These tests are meant to be run with Spoon (./gradlew spoon). Remove @Ignore first
 */
public class TestMain extends BaseUiTest {
  private MyApplication app;
  private Context instCtx;
  private Locale locale;
  private Currency defaultCurrency;
  @Rule public final FalconSpoonRule falconSpoonRule = new FalconSpoonRule();
  @Rule public final ActivityTestRule<MyExpenses> activityRule = new ActivityTestRule<>(MyExpenses.class, false, false);


  @Before
  public void setUp()  {
    instCtx = InstrumentationRegistry.getInstrumentation().getContext();
    app = (MyApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
  }

  @Test
  public void mkScreenShots() {
    defaultCurrency = Currency.getInstance(BuildConfig.TEST_CURRENCY);
    loadFixture();
    scenario(BuildConfig.TEST_SCENARIO);
  }

  private void scenario(int scenario) {
    sleep();
    switch(scenario) {
      case 1: {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        takeScreenshot("summarize");
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.close());
        takeScreenshot("group");
        clickMenuItem(R.id.RESET_COMMAND, R.string.menu_reset);
        Espresso.closeSoftKeyboard();
        takeScreenshot("export");
        Espresso.pressBack();
        onView(withText(R.string.split_transaction)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        Espresso.closeSoftKeyboard();
        takeScreenshot("split");
        Espresso.pressBack();
        clickMenuItem(R.id.DISTRIBUTION_COMMAND, R.string.menu_distribution);
        takeScreenshot("distribution");
        Espresso.pressBack();
        clickMenuItem(R.id.HISTORY_COMMAND, R.string.menu_history);
        clickMenuItem(R.id.GROUPING_COMMAND, R.string.menu_grouping);
        onView(withText(R.string.grouping_month)).perform(click());
        clickMenuItem(R.id.TOGGLE_INCLUDE_TRANSFERS_COMMAND, R.string.menu_history_transfers);
        takeScreenshot("history");
        Espresso.pressBack();
        clickMenuItem(R.id.BUDGET_COMMAND, R.string.menu_budget);
        takeScreenshot("budget");
        Espresso.pressBack();
        clickMenuItem(R.id.SETTINGS_COMMAND, R.string.menu_settings);
        onView(instanceOf(RecyclerView.class))
            .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.pref_manage_sync_backends_title)),
                click()));
        onView(withText(containsString("Dropbox"))).perform(click());
        onView(withText(containsString("WebDAV"))).perform(click());
        takeScreenshot("sync");
        break;
      }
      case 2: {//tablet screenshots
        takeScreenshot("main");
        clickMenuItem(R.id.DISTRIBUTION_COMMAND, R.string.menu_distribution);
        takeScreenshot("distribution");
        Espresso.pressBack();

        onView(first(withText(containsString(InstrumentationRegistry.getContext().getString(org.totschnig.myexpenses.fortest.test.R.string.testData_transaction1SubCat))))).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        Espresso.pressBack();//close keyboard
        onView(withId(R.id.picture_container)).perform(click());
        takeScreenshot("edit");
        break;
      }
      default: {
        throw new IllegalArgumentException("Unknown scenario" + scenario);
      }
    }

  }

  private void loadFixture() {
    this.locale = new Locale(BuildConfig.TEST_LANG, BuildConfig.TEST_COUNTRY);
    Locale.setDefault(locale);
    Configuration config = new Configuration();
    config.locale = locale;
    app.getResources().updateConfiguration(config,
        app.getResources().getDisplayMetrics());
    instCtx.getResources().updateConfiguration(config,
        instCtx.getResources().getDisplayMetrics());
    android.content.SharedPreferences pref = app.getSettings();
    if (pref == null)
      Assert.fail("Could not find prefs");
    pref.edit().putString(PrefKey.UI_LANGUAGE.getKey(), BuildConfig.TEST_LANG + "-" + BuildConfig.TEST_COUNTRY)
        .putString(PrefKey.HOME_CURRENCY.getKey(), defaultCurrency.getCurrencyCode())
        .apply();
    app.getLicenceHandler().setLockState(false);

    Fixture fixture = new Fixture(InstrumentationRegistry.getInstrumentation(), locale);
    fixture.setup();
    int current_version = DistribHelper.getVersionNumber();
    pref.edit()
        .putLong(PrefKey.CURRENT_ACCOUNT.getKey(), fixture.getInitialAccount().getId())
        .putInt(PrefKey.CURRENT_VERSION.getKey(), current_version)
        .putInt(PrefKey.FIRST_INSTALL_VERSION.getKey(), current_version)
        .apply();
    final Intent startIntent = new Intent(app, MyExpenses.class);
    activityRule.launchActivity(startIntent);
  }

  private void sleep() {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void takeScreenshot(String fileName) {
    try {
      Thread.sleep(250);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    falconSpoonRule.screenshot(getCurrentActivity(), fileName);
  }

  private Activity getCurrentActivity() {
    final Activity[] activities = new Activity[1];
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).toArray(activities);
    });
    return activities[0];
  }

  @Override
  protected ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule() {
    return activityRule;
  }
}