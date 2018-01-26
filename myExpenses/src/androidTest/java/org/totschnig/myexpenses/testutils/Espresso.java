package org.totschnig.myexpenses.testutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.espresso.util.TreeIterables;
import android.view.View;
import android.view.ViewConfiguration;

import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressMenuKey;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.endsWith;


public class Espresso {

  public static void openActionBarOverflowOrOptionsMenu(Context context) {
    if (hasVirtualOverflowButton(context)) {
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
    return !ViewConfiguration.get(context).hasPermanentMenuKey();
  }

  public static void checkEffectiveVisible(int... viewIds) {
    for (int resId: viewIds) {
      onView(withId(resId)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }
  }

  public static void checkEffectiveGone(int... viewIds) {
    for (int resId: viewIds) {
      onView(withId(resId)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
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
