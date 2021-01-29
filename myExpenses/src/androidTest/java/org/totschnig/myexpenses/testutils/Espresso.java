package org.totschnig.myexpenses.testutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import org.hamcrest.Matcher;

import java.util.concurrent.TimeoutException;

import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.platform.app.InstrumentationRegistry;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.endsWith;


public class Espresso {

  public static void openActionBarOverflowMenu(boolean isCab) {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    onView(isRoot()).perform(new TransitionBridgingViewAction());

    onView(isCab ? localizedContextualOverFlowButtonMatcher(context) : localizedOverFlowButtonMatcher(context)).perform(click());
  }

  public static void openActionBarOverflowMenu() {
    openActionBarOverflowMenu(false);
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

  private static Matcher<View> localizedContextualOverFlowButtonMatcher(Context context) {
    return allOf(localizedOverFlowButtonMatcher(context), isDescendantOfA(withClassName(endsWith("ActionBarContextView"))));
  }

  @SuppressLint("PrivateResource")
  private static Matcher<View> localizedOverFlowButtonMatcher(Context context) {
    return anyOf(
        allOf(isDisplayed(), withContentDescription(context.getString(
            androidx.appcompat.R.string.abc_action_menu_overflow_description))),
        allOf(isDisplayed(), withClassName(endsWith("OverflowMenuButton"))));
  }

  public static ViewAction wait(Matcher<View> viewMatcher, final long millis) {
    return new ViewAction() {

      @Override
      public Matcher<View> getConstraints() {
        return isDisplayed();
      }

      @Override
      public String getDescription() {
        return "wait for matcher <" + viewMatcher.toString() + "> during " + millis + " millis.";
      }

      @Override
      public void perform(final UiController uiController, final View view) {
        uiController.loopMainThreadUntilIdle();
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + millis;

        do {
          if (viewMatcher.matches(view)) {
            return;
          }

          uiController.loopMainThreadForAtLeast(50);
        }
        while (System.currentTimeMillis() < endTime);

        // timeout happens
        throw new PerformException.Builder()
            .withActionDescription(this.getDescription())
            .withViewDescription(HumanReadables.describe(view))
            .withCause(new TimeoutException())
            .build();
      }
    };
  }

  public static Matcher<View> withIdAndParent(final int id, final int parentId) {
    return allOf(withId(id), withParent(withId(parentId)));
  }

  public static Matcher<View> withIdAndAncestor(final int id, final int parentId) {
    return allOf(withId(id), isDescendantOfA(withId(parentId)));
  }

}
