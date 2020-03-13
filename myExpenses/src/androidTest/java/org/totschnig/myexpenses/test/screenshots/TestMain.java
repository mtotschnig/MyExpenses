package org.totschnig.myexpenses.test.screenshots;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;
import com.jraska.falcon.FalconSpoonRule;

import junit.framework.Assert;

import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.sync.GenericAccountService;
import org.totschnig.myexpenses.testutils.BaseUiTest;
import org.totschnig.myexpenses.testutils.Fixture;
import org.totschnig.myexpenses.util.DistribHelper;

import java.util.Currency;
import java.util.Locale;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.totschnig.myexpenses.testutils.Matchers.first;

/**
 * These tests are meant to be run with Spoon (./gradlew spoon).
 */
public class TestMain extends BaseUiTest {
  private MyApplication app;
  private Context instCtx;
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
    //noinspection ConstantConditions
    Assume.assumeFalse("undefined".equals(BuildConfig.TEST_CURRENCY));
    final Account[] accountsAsArray = GenericAccountService.getAccountsAsArray(app);
    Assertions.assertThat(accountsAsArray.length).isEqualTo(2);
    Assertions.assertThat(Stream.of(accountsAsArray).anyMatch(value -> value.name.contains("Dropbox"))).isTrue();
    Assertions.assertThat(Stream.of(accountsAsArray).anyMatch(value -> value.name.contains("WebDAV"))).isTrue();
    defaultCurrency = Currency.getInstance(BuildConfig.TEST_CURRENCY);
    loadFixture();
    scenario();
  }

  private void scenario() {
    sleep();
    switch(BuildConfig.TEST_SCENARIO) {
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
        onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        takeScreenshot("budget");
        Espresso.pressBack();
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

        onView(first(withText(containsString(InstrumentationRegistry.getInstrumentation().getContext().getString(org.totschnig.myexpenses.fortest.test.R.string.testData_transaction1SubCat))))).perform(click());
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

  private void loadFixture() {
    Locale locale = new Locale(BuildConfig.TEST_LANG, BuildConfig.TEST_COUNTRY);
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