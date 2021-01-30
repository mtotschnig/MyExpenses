package org.totschnig.myexpenses.testutils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import java.util.concurrent.TimeoutException;

import androidx.fragment.app.Fragment;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.totschnig.myexpenses.testutils.Espresso.openActionBarOverflowMenu;
import static org.totschnig.myexpenses.testutils.Matchers.menuIdMatcher;

public abstract class BaseUiTest {
  protected TestApp app;
  protected Context testContext;
  private boolean isLarge;

  @Before
  public void setUp() throws PackageManager.NameNotFoundException {
    app = (TestApp) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    testContext = InstrumentationRegistry.getInstrumentation().getContext();
    isLarge = testContext.getResources().getBoolean(org.totschnig.myexpenses.debug.test.R.bool.isLarge);
  }

  protected void closeKeyboardAndSave() {
    closeSoftKeyboard();
    onView(withId(R.id.CREATE_COMMAND)).perform(click());
  }


  /**
   * Click on a menu item, that might be visible or hidden in overflow menu
   */
  protected void clickMenuItem(int menuItemId) {
    clickMenuItem(menuItemId, false);
  }

  protected Matcher<View> getWrappedList() {
    return allOf(
        isAssignableFrom(AdapterView.class),
        isDescendantOfA(withId(R.id.list)),
        isDisplayed());
  }

  /**
   * @param menuItemId id of menu item rendered in CAB on Honeycomb and higher
   */
  protected void clickMenuItem(int menuItemId, boolean isCab) {
    try {
      ViewInteraction viewInteraction = onView(withId(menuItemId));
      boolean searchInPlatformPopup = false;
      try {
        searchInPlatformPopup = isCab && isLarge &&
            app.getPackageManager().getActivityInfo(getCurrentActivity().getComponentName(), 0).getThemeResource() == R.style.EditDialog;
      } catch (PackageManager.NameNotFoundException ignored) {
      }
      if (searchInPlatformPopup) {
        viewInteraction.inRoot(isPlatformPopup());
      }
      viewInteraction.perform(click());
    } catch (NoMatchingViewException e) {
      openActionBarOverflowMenu(isCab);
      onData(menuIdMatcher(menuItemId)).inRoot(isPlatformPopup()).perform(click());
    }
  }

  //https://stackoverflow.com/a/41415288/1199911
  private Activity getCurrentActivity() {
    final Activity[] activity = new Activity[1];
    onView(isRoot()).check((view, noViewFoundException) -> activity[0] = (Activity) view.findViewById(android.R.id.content).getContext());
    return activity[0];
  }

  protected void handleContribDialog(ContribFeature contribFeature) {
    if (!contribFeature.hasAccess()) {
      try {
        //without playservice a billing setup error dialog is displayed
        onView(withText(android.R.string.ok)).perform(click());
      } catch (Exception ignored) {
      }
      onView(withText(R.string.dialog_title_contrib_feature)).check(matches(isDisplayed()));
      onView(withText(R.string.dialog_contrib_no)).perform(scrollTo(), click());
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

  protected Adapter waitForAdapter() throws TimeoutException {
    int iterations = 0;
    while (true) {
      Adapter adapter = getAdapter();
      try {
        Thread.sleep(500);
      } catch (InterruptedException ignored) {
      }
      if (adapter != null) {
        return adapter;
      }
      iterations++;
      if (iterations > 10) throw new TimeoutException();
    }
  }

  protected void waitForSnackbarDismissed() throws TimeoutException {
    int iterations = 0;
    while (true) {
      try {
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(isDisplayed()));
      } catch (Exception e) {
        return;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ignored) {
      }
      iterations++;
      if (iterations > 10) throw new TimeoutException();
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
