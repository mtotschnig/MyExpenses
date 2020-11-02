package org.totschnig.myexpenses.testutils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.TestApp;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefKey;

import java.util.Locale;

import androidx.fragment.app.Fragment;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.totschnig.myexpenses.testutils.Espresso.openActionBarOverflowMenu;

public abstract class BaseUiTest {
  protected TestApp app;

  @Before
  public void setUp()  {
    app = (TestApp) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
  }

  protected void clickOnFirstListEntry() {
    clickOnListEntry(0);
  }

  protected void clickOnWrappedListEntry(Matcher<Object> dataMatcher) {
    waitForAdapter();
    onData(dataMatcher).inAdapterView(getWrappedList()).atPosition(0).perform(click());
  }

  protected void clickOnListEntry(int atPosition) {
    waitForAdapter();
    onData(anything()).inAdapterView(isAssignableFrom(AdapterView.class)).atPosition(atPosition).perform(click());
  }


  /**
   * Click on a menu item, that might be visible or hidden in overflow menu
   *
   * @param menuItemId
   * @param menuTextResId
   */
  protected void clickMenuItem(int menuItemId, int menuTextResId) {
    clickMenuItem(menuItemId, menuTextResId, false);
  }

  protected Matcher<View> getWrappedList() {
    return allOf(
        isAssignableFrom(AdapterView.class),
        isDescendantOfA(withId(R.id.list)),
        isDisplayed());
  }

  /**
   * @param menuItemId        id of menu item rendered in CAB on Honeycomb and higher
   * @param menuTextResId String used on Gingerbread where context actions are rendered in a context menu
   * @param isCab
   */
  protected void clickMenuItem(int menuItemId, int menuTextResId, boolean isCab) {
    try {
      onView(withId(menuItemId)).perform(click());
    } catch (NoMatchingViewException e) {
      openActionBarOverflowMenu(isCab);
      onView(withText(menuTextResId)).perform(click());
    }
  }

  protected void handleContribDialog(ContribFeature contribFeature) {
    if (!contribFeature.hasAccess()) {
      try {
        //without playservice a billing setup error dialog is displayed
        onView(withText(android.R.string.ok)).perform(click());
      } catch (Exception ignored) {}
      onView(withText(R.string.dialog_title_contrib_feature)).check(matches(isDisplayed()));
      onView(withText(R.string.dialog_contrib_no)).perform(scrollTo()).perform(click());
    }
  }

  protected abstract ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule();

  protected void rotate() {
    final ProtectedFragmentActivity activity = getTestRule().getActivity();
    activity.setRequestedOrientation(activity.getRequestedOrientation() == SCREEN_ORIENTATION_LANDSCAPE ?
      SCREEN_ORIENTATION_PORTRAIT : SCREEN_ORIENTATION_LANDSCAPE);
  }

  private ViewGroup getList() {
    Fragment currentFragment = getTestRule().getActivity().getCurrentFragment();
    if (currentFragment == null) return null;
    return (ViewGroup) currentFragment.getView().findViewById(getListId());
  }

  protected int getListId() {
    return R.id.list;
  }

  protected String getString(int resid) {
    return getTestRule().getActivity().getString(resid);
  }

  private Adapter getAdapter() {
    ViewGroup list = getList();
    if (list == null) return null;
    if (list instanceof StickyListHeadersListView) {
      return ((StickyListHeadersListView) list).getAdapter();
    }
    if (list instanceof ListView) {
      return ((ListView) list).getAdapter();
    }
    return null;
  }

  protected Adapter waitForAdapter() {
    while (true) {
      Adapter adapter = getAdapter();
      try {
        Thread.sleep(500);
      } catch (InterruptedException ignored) {
      }
      if (adapter != null) {
        return adapter;
      }
    }
  }

  protected void configureLocale(Locale locale) {
    Locale.setDefault(locale);
    Configuration config = new Configuration();
    config.locale = locale;
    Context instCtx = InstrumentationRegistry.getInstrumentation().getContext();
    instCtx.getResources().updateConfiguration(config,
        instCtx.getResources().getDisplayMetrics());
    SharedPreferences pref = app.getSettings();
    if (pref == null)
      Assert.fail("Could not find prefs");
    pref.edit().putString(PrefKey.UI_LANGUAGE.getKey(), locale.getLanguage() + "-" + locale.getCountry())
        .apply();
  }

}
