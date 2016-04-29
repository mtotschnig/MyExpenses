package org.totschnig.myexpenses.test.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.util.TreeIterables;
import android.view.View;
import android.view.ViewConfiguration;

import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressMenuKey;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.endsWith;


public class Espresso {

  public static void openActionBarOverflowOrOptionsMenu(Context context) {
    if (context.getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.HONEYCOMB) {
      // regardless of the os level of the device, this app will be rendering a menukey
      // in the virtual navigation bar (if present) or responding to hardware option keys on
      // any activity.
      onView(isRoot())
          .perform(pressMenuKey());
    } else if (hasVirtualOverflowButton(context)) {
      // If we're using virtual keys - theres a chance we're in mid animation of switching
      // between a contextual action bar and the non-contextual action bar. In this case there
      // are 2 'More Options' buttons present. Lets wait till that is no longer the case.
      onView(isRoot())
          .perform(new TransitionBridgingViewAction());

      onView(localizedOverFlowButtonMatcher(context))
          .perform(click());
    } else {
      // either a hardware button exists, or we're on a pre-HC os.
      onView(isRoot())
          .perform(pressMenuKey());
    }
  }

  private static boolean hasVirtualOverflowButton(Context context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    } else {
      return !ViewConfiguration.get(context).hasPermanentMenuKey();
    }
  }

  /**
   * Handles the cases where the app is transitioning between a contextual action bar and a
   * non contextual action bar.
   */
  private static class TransitionBridgingViewAction implements ViewAction {
    @Override
    public void perform(UiController controller, View view) {
      int loops = 0;
      while (isTransitioningBetweenActionBars(view) && loops < 100) {
        loops++;
        controller.loopMainThreadForAtLeast(50);
      }
      // if we're not transitioning properly the next viewaction
      // will give a decent enough exception.
    }

    @Override
    public String getDescription() {
      return "Handle transition between action bar and action bar context.";
    }

    @Override
    public Matcher<View> getConstraints() {
      return isRoot();
    }

    private boolean isTransitioningBetweenActionBars(View view) {
      int actionButtonCount = 0;
      for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
        if (localizedOverFlowButtonMatcher(view.getContext()).matches(child)) {
          actionButtonCount++;
        }
      }
      return actionButtonCount > 1;
    }
  }
  @SuppressLint("PrivateResource")
  private static final Matcher<View> localizedOverFlowButtonMatcher(Context context) {
    return anyOf(
        allOf(isDisplayed(), withContentDescription(context.getString(
            android.support.v7.appcompat.R.string.abc_action_menu_overflow_description))),
        allOf(isDisplayed(), withClassName(endsWith("OverflowMenuButton"))));
  }
}
