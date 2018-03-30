package org.totschnig.myexpenses.test.screenshots;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;

import com.jraska.falcon.FalconSpoonRule;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
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
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * These tests are meant to be run with Spoon (./gradlew spoon). Remove @Ignore first
 *
 */
@Ignore
public class TestMain extends BaseUiTest {
  private MyApplication app;
  private Context instCtx;
  private Locale locale;
  private Currency defaultCurrency;
  @Rule public final FalconSpoonRule falconSpoonRule = new FalconSpoonRule();
  @Rule public final ActivityTestRule<MyExpenses> activityRule = new ActivityTestRule<>(MyExpenses.class, false, false);


  @Before
  public void setUp() throws Exception {
    instCtx = InstrumentationRegistry.getInstrumentation().getContext();
    app = (MyApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
  }

  @Test
  public void testLang_en() {
    defaultCurrency = Currency.getInstance("USD");
    helperTestLang("en", "US");
  }

  public void testLang_fr() {
    defaultCurrency = Currency.getInstance("EUR");
    helperTestLang("fr", "FR");
  }

  public void testLang_de() {
    defaultCurrency = Currency.getInstance("EUR");
    helperTestLang("de", "DE");
  }

  public void testLang_it() {
    defaultCurrency = Currency.getInstance("EUR");
    helperTestLang("it", "IT");
  }

  public void testLang_es() {
    defaultCurrency = Currency.getInstance("EUR");
    helperTestLang("es", "ES");
  }

  public void testLang_tr() {
    defaultCurrency = Currency.getInstance("TRY");
    helperTestLang("tr", "TR");
  }

  public void testLang_vi() {
    //Currency.getInstance(new Locale("vi","VI") => USD on Nexus S
    defaultCurrency = Currency.getInstance("VND");
    helperTestLang("vi", "VI");
  }

  public void testLang_ar() {
    defaultCurrency = Currency.getInstance("SAR");
    helperTestLang("ar", "SA");
  }

  public void testLang_hu() {
    defaultCurrency = Currency.getInstance("HUF");
    helperTestLang("hu", "HU");
  }

  public void testLang_ca() {
    defaultCurrency = Currency.getInstance("EUR");
    helperTestLang("ca", "ES");
  }

  public void testLang_km() {
    defaultCurrency = Currency.getInstance("KHR");
    helperTestLang("km", "KH");
  }

  public void testLang_zh() {
    defaultCurrency = Currency.getInstance("TWD");
    helperTestLang("zh", "TW");
  }

  public void testLang_pt() {
    defaultCurrency = Currency.getInstance("BRL");
    helperTestLang("pt", "BR");
  }

  public void testLang_pl() {
    defaultCurrency = Currency.getInstance("PLN");
    helperTestLang("pl", "PL");
  }

  public void testLang_cs() {
    defaultCurrency = Currency.getInstance("CZK");
    helperTestLang("cs", "CZ");
  }

  public void testLang_ru() {
    defaultCurrency = Currency.getInstance("RUB");
    helperTestLang("ru", "RU");
  }

  public void testLang_hr() {
    defaultCurrency = Currency.getInstance("HRK");
    helperTestLang("hr", "HR");
  }

  public void testLang_ja() {
    defaultCurrency = Currency.getInstance("JPY");
    helperTestLang("ja", "JA");
  }

  public void testLang_ms() {
    defaultCurrency = Currency.getInstance("MYR");
    helperTestLang("ms", "MY");
  }

  public void testLang_ro() {
    defaultCurrency = Currency.getInstance("RON");
    helperTestLang("ro", "RO");
  }

  public void testLang_si() {
    defaultCurrency = Currency.getInstance("LKR");
    helperTestLang("si", "SI");
  }

  public void testLang_eu() {
    defaultCurrency = Currency.getInstance("EUR");
    helperTestLang("eu", "ES");
  }

  public void testLang_da() {
    defaultCurrency = Currency.getInstance("DKK");
    helperTestLang("da", "DK");
  }

  public void testLang_bg() {
    defaultCurrency = Currency.getInstance("BGN");
    helperTestLang("bg", "BG");
  }

  private void helperTestLang(String lang, String country) {
    this.locale = new Locale(lang, country);
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
    pref.edit().putString(PrefKey.UI_LANGUAGE.getKey(), lang + "-" + country)
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
    sleep();
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    takeScreenshot("manage_accounts");
    onView(withId(R.id.drawer_layout)).perform(DrawerActions.close());
    takeScreenshot("grouped_list");
    onView(withId(R.id.MANAGE_PLANS_COMMAND)).perform(click());
    clickOnFirstListEntry();
    takeScreenshot("plans");
    Espresso.pressBack();
    Espresso.pressBack();
    clickMenuItem(R.id.RESET_COMMAND, R.string.menu_reset);
    takeScreenshot("export");
    Espresso.pressBack();
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
    onView(withId(R.id.Calculator)).perform(click());
    takeScreenshot("calculator");
    Espresso.pressBack();
    Espresso.pressBack();
    onView(withText(R.string.split_transaction)).perform(click());
    onView(withId(android.R.id.button1)).perform(click());
    Espresso.pressBack();//close keyboard
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
  }

  private void sleep() {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void takeScreenshot(String fileName) {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    falconSpoonRule.screenshot(getCurrentActivity(), fileName);
  }

  private Activity getCurrentActivity() {
    final Activity[] activites = new Activity[1];
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).toArray(activites);
    });
    return activites[0];
  }

  @Override
  protected ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule() {
    return activityRule;
  }
}