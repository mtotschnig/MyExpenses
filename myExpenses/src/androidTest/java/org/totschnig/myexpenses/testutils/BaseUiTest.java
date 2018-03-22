package org.totschnig.myexpenses.testutils;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;

import org.hamcrest.Matcher;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.totschnig.myexpenses.testutils.Espresso.openActionBarOverflowOrOptionsMenu;

public abstract class BaseUiTest {
  protected void clickOnFirstListEntry() {
    clickOnListEntry(0);
  }

  protected void clickOnWrappedListEntry(Matcher<Object> dataMatcher) {
    waitForAdapter();
    onData(dataMatcher).inAdapterView(getWrappedList()).atPosition(0).perform(click());
  }

  private void clickOnListEntry(int atPosition) {
    waitForAdapter();
    onData(anything()).inAdapterView(isAssignableFrom(AdapterView.class)).atPosition(atPosition).perform(click());
  }


  /**
   * Click on a menu item, that might be visible or hidden in overflow menu
   * @param menuItemId
   * @param menuTextResId
   */
  protected void clickMenuItem(int menuItemId, int menuTextResId) {
    try {
      onView(withId(menuItemId)).perform(click());
    } catch (NoMatchingViewException e) {
      openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
      onView(withText(menuTextResId)).perform(click());
    }
  }

  protected Matcher<View> getWrappedList() {
    return allOf(
        isAssignableFrom(AdapterView.class),
        isDescendantOfA(withId(R.id.list)),
        isDisplayed());
  }

  protected abstract ActivityTestRule<? extends ProtectedFragmentActivity> getTestRule();


  private ViewGroup getList() {
    Fragment currentFragment = getTestRule().getActivity().getCurrentFragment();
    if (currentFragment == null) return null;
    return (ViewGroup) currentFragment.getView().findViewById(R.id.list);
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
      } catch (InterruptedException ignored) {}
      if (adapter != null) {
        return adapter;
      }
    }
  }

}
