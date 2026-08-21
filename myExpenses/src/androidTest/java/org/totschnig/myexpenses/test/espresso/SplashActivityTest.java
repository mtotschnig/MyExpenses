package org.totschnig.myexpenses.test.espresso;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.SplashActivity;
import org.totschnig.myexpenses.testutils.ClickWithPartialDisplayConstraint;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.totschnig.myexpenses.testutils.IsEqualTrimmingAndIgnoringCase.equalToTrimmingAndIgnoringCase;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

    @Rule
    public ActivityTestRule<SplashActivity> mActivityTestRule =
            new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void splashActivityTest() {
        ViewInteraction root = onView(isRoot());
        root.perform(getSwipeAction(540, 928, 540, 628));

        waitToScrollEnd();

        ViewInteraction root2 = onView(isRoot());
        root2.perform(getSwipeAction(540, 897, 540, 0));

        waitToScrollEnd();

        ViewInteraction root3 = onView(isRoot());
        root3.perform(getSwipeAction(540, 928, 840, 928));

        waitToScrollEnd();

        ViewInteraction root4 = onView(isRoot());
        root4.perform(getSwipeAction(540, 928, 840, 928));

        waitToScrollEnd();

        ViewInteraction android_widget_Button =
                onView(
                        Matchers.allOf(
                                ViewMatchers.withId(R.id.suw_navbar_next),
                                withTextOrHint(equalToTrimmingAndIgnoringCase("NEXT")),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.suw_layout_navigation_bar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.setup_wizard_layout),
                                                                isDescendantOfA(withId(R.id.viewpager))))))));
        android_widget_Button.perform(getClickAction());

        ViewInteraction root5 = onView(isRoot());
        root5.perform(getSwipeAction(540, 928, 540, 628));

        waitToScrollEnd();

        ViewInteraction android_widget_Button2 =
                onView(
                        allOf(
                                withId(R.id.suw_navbar_next),
                                withTextOrHint(equalToTrimmingAndIgnoringCase("NEXT")),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.suw_layout_navigation_bar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.setup_wizard_layout),
                                                                isDescendantOfA(withId(R.id.viewpager))))))));
        android_widget_Button2.perform(getClickAction());

        ViewInteraction android_widget_EditText =
                onView(
                        allOf(
                                withId(R.id.AmountEditText),
                                withTextOrHint(equalToTrimmingAndIgnoringCase("0")),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.Amount),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.suw_layout_content),
                                                                isDescendantOfA(
                                                                        allOf(
                                                                                withId(R.id.suw_bottom_scroll_view),
                                                                                isDescendantOfA(
                                                                                        allOf(
                                                                                                withId(R.id.setup_wizard_layout),
                                                                                                isDescendantOfA(withId(R.id.viewpager))))))))))));
        android_widget_EditText.perform(replaceText("075380603176906"));

        ViewInteraction android_widget_EditText2 =
                onView(
                        allOf(
                                withId(R.id.Label),
                                withTextOrHint(equalToTrimmingAndIgnoringCase("Budget Book")),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.suw_layout_content),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.suw_bottom_scroll_view),
                                                                isDescendantOfA(
                                                                        allOf(
                                                                                withId(R.id.setup_wizard_layout),
                                                                                isDescendantOfA(withId(R.id.viewpager))))))))));
        android_widget_EditText2.perform(replaceText("veinings"));

        ViewInteraction root6 = onView(isRoot());
        root6.perform(getSwipeAction(540, 897, 540, 1794));

        waitToScrollEnd();

        ViewInteraction android_widget_Spinner =
                onView(
                        allOf(
                                withId(R.id.Currency),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.suw_layout_content),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.suw_bottom_scroll_view),
                                                                isDescendantOfA(
                                                                        allOf(
                                                                                withId(R.id.setup_wizard_layout),
                                                                                isDescendantOfA(withId(R.id.viewpager))))))))));
        android_widget_Spinner.perform(getClickAction());

        Espresso.pressBackUnconditionally();

        ViewInteraction android_widget_TextView =
                onView(
                        allOf(
                                withId(R.id.SetupMain),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.onboarding_menu),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.suw_layout_navigation_bar),
                                                                isDescendantOfA(
                                                                        allOf(
                                                                                withId(R.id.setup_wizard_layout),
                                                                                isDescendantOfA(withId(R.id.viewpager))))))))));
        android_widget_TextView.perform(getClickAction());

        Espresso.pressBackUnconditionally();

        ViewInteraction android_widget_Button3 =
                onView(
                        allOf(
                                withId(R.id.suw_navbar_done),
                                withTextOrHint(equalToTrimmingAndIgnoringCase("GET STARTED")),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.suw_layout_navigation_bar),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.setup_wizard_layout),
                                                                isDescendantOfA(withId(R.id.viewpager))))))));
        android_widget_Button3.perform(getClickAction());

        ViewInteraction android_widget_ImageButton =
                onView(
                        allOf(
                                withId(R.id.CREATE_COMMAND),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.fragment_container),
                                                isDescendantOfA(withId(R.id.drawer_layout))))));
        android_widget_ImageButton.perform(getClickAction());

        ViewInteraction android_widget_TextView2 =
                onView(
                        allOf(
                                withId(R.id.SAVE_AND_NEW_COMMAND),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(withId(R.id.toolbar), isDescendantOfA(withId(R.id.edit_container))))));
        android_widget_TextView2.perform(getClickAction());

        ViewInteraction root7 = onView(isRoot());
        root7.perform(getSwipeAction(540, 897, 540, 0));

        waitToScrollEnd();

        ViewInteraction android_view_ViewGroup =
                onView(
                        allOf(
                                withId(R.id.Amount),
                                isDisplayed(),
                                hasDescendant(withId(R.id.TaType)),
                                hasDescendant(withId(R.id.AmountEditText)),
                                hasDescendant(withId(R.id.Calculator)),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.AmountRow),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.Table),
                                                                isDescendantOfA(withId(R.id.edit_container))))))));
        android_view_ViewGroup.perform(getClickAction());

        ViewInteraction android_widget_ImageView =
                onView(
                        allOf(
                                withId(R.id.SelectTag),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.TagRow),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.Table),
                                                                isDescendantOfA(withId(R.id.edit_container))))))));
        android_widget_ImageView.perform(getClickAction());

        ViewInteraction android_widget_ImageButton2 =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("Navigate up")),
                                isDisplayed(),
                                isDescendantOfA(withId(R.id.toolbar))));
        android_widget_ImageButton2.perform(getClickAction());

        ViewInteraction android_widget_EditText3 =
                onView(
                        allOf(
                                withId(R.id.AmountEditText),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.Amount),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.AmountRow),
                                                                isDescendantOfA(
                                                                        allOf(
                                                                                withId(R.id.Table),
                                                                                isDescendantOfA(withId(R.id.edit_container))))))))));
        android_widget_EditText3.perform(replaceText("315961250803207"));

        ViewInteraction android_widget_Spinner2 =
                onView(
                        allOf(
                                withId(R.id.OperationType),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(withId(R.id.toolbar), isDescendantOfA(withId(R.id.edit_container))))));
        android_widget_Spinner2.perform(getLongClickAction());

        Espresso.pressBackUnconditionally();

        ViewInteraction android_widget_Spinner3 =
                onView(
                        allOf(
                                withId(R.id.Account),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.AccountRow),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.Table),
                                                                isDescendantOfA(withId(R.id.edit_container))))))));
        android_widget_Spinner3.perform(getLongClickAction());

        Espresso.pressBackUnconditionally();

        ViewInteraction android_widget_TextView3 =
                onView(
                        allOf(
                                withId(R.id.SAVE_COMMAND),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(withId(R.id.toolbar), isDescendantOfA(withId(R.id.edit_container))))));
        android_widget_TextView3.perform(getClickAction());

        ViewInteraction root8 = onView(isRoot());
        root8.perform(getSwipeAction(540, 897, 540, 1794));

        waitToScrollEnd();

        ViewInteraction android_widget_ImageView2 =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("More options")),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(withId(R.id.toolbar), isDescendantOfA(withId(R.id.drawer_layout))))));
        android_widget_ImageView2.perform(getClickAction());

        ViewInteraction android_widget_LinearLayout =
                onView(
                        allOf(
                                classOrSuperClassesName(is("android.widget.LinearLayout")),
                                isDisplayed(),
                                hasDescendant(
                                        allOf(
                                                withId(R.id.content),
                                                hasDescendant(
                                                        allOf(
                                                                withId(R.id.title),
                                                                withTextOrHint(equalToTrimmingAndIgnoringCase("Grouping")))),
                                                hasDescendant(withId(R.id.submenuarrow))))));
        android_widget_LinearLayout.perform(getClickAction());

        ViewInteraction android_widget_LinearLayout2 =
                onView(
                        allOf(
                                classOrSuperClassesName(is("android.widget.LinearLayout")),
                                isDisplayed(),
                                hasDescendant(
                                        allOf(
                                                withId(R.id.content),
                                                hasDescendant(
                                                        allOf(
                                                                withId(R.id.title),
                                                                withTextOrHint(equalToTrimmingAndIgnoringCase("Month")))),
                                                hasDescendant(withId(R.id.radio))))));
        android_widget_LinearLayout2.perform(getClickAction());

        ViewInteraction android_widget_ImageView3 =
                onView(
                        allOf(
                                withContentDescription(equalToTrimmingAndIgnoringCase("More options")),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(withId(R.id.toolbar), isDescendantOfA(withId(R.id.drawer_layout))))));
        android_widget_ImageView3.perform(getClickAction());

        ViewInteraction android_widget_LinearLayout3 =
                onView(
                        allOf(
                                classOrSuperClassesName(is("android.widget.LinearLayout")),
                                isDisplayed(),
                                hasDescendant(
                                        allOf(
                                                withId(R.id.content),
                                                hasDescendant(
                                                        allOf(
                                                                withId(R.id.title),
                                                                withTextOrHint(equalToTrimmingAndIgnoringCase("Budgeting"))))))));
        android_widget_LinearLayout3.perform(getClickAction());

        ViewInteraction android_widget_RadioButton =
                onView(
                        allOf(
                                withId(R.id.package_button),
                                isDisplayed(),
                                isDescendantOfA(
                                        allOf(
                                                withId(R.id.professional_feature_container),
                                                isDescendantOfA(
                                                        allOf(
                                                                withId(R.id.feature_list),
                                                                isDescendantOfA(
                                                                        allOf(
                                                                                withId(R.id.aboutscrollview),
                                                                                isDescendantOfA(
                                                                                        allOf(
                                                                                                withId(R.id.custom),
                                                                                                isDescendantOfA(
                                                                                                        allOf(
                                                                                                                withId(R.id.customPanel),
                                                                                                                isDescendantOfA(
                                                                                                                        withId(R.id.parentPanel))))))))))))));
        android_widget_RadioButton.perform(getClickAction());
    }

    private static Matcher<View> classOrSuperClassesName(final Matcher<String> classNameMatcher) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Class name or any super class name ");
                classNameMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                Class<?> clazz = view.getClass();
                String canonicalName;

                do {
                    canonicalName = clazz.getCanonicalName();
                    if (canonicalName == null) {
                        return false;
                    }

                    if (classNameMatcher.matches(canonicalName)) {
                        return true;
                    }

                    clazz = clazz.getSuperclass();
                    if (clazz == null) {
                        return false;
                    }
                } while (!"java.lang.Object".equals(canonicalName));

                return false;
            }
        };
    }

    private static Matcher<View> withTextOrHint(final Matcher<String> stringMatcher) {
        return anyOf(withText(stringMatcher), withHint(stringMatcher));
    }

    private ViewAction getSwipeAction(
            final int fromX, final int fromY, final int toX, final int toY) {
        return ViewActions.actionWithAssertions(
                new GeneralSwipeAction(
                        Swipe.SLOW,
                        new CoordinatesProvider() {
                            @Override
                            public float[] calculateCoordinates(View view) {
                                float[] coordinates = {fromX, fromY};
                                return coordinates;
                            }
                        },
                        new CoordinatesProvider() {
                            @Override
                            public float[] calculateCoordinates(View view) {
                                float[] coordinates = {toX, toY};
                                return coordinates;
                            }
                        },
                        Press.FINGER));
    }

    private void waitToScrollEnd() {
        SystemClock.sleep(500);
    }

    private ClickWithPartialDisplayConstraint getClickAction() {
        return new ClickWithPartialDisplayConstraint(
                Tap.SINGLE,
                GeneralLocation.VISIBLE_CENTER,
                Press.FINGER,
                InputDevice.SOURCE_UNKNOWN,
                MotionEvent.BUTTON_PRIMARY);
    }

    private ClickWithPartialDisplayConstraint getLongClickAction() {
        return new ClickWithPartialDisplayConstraint(
                Tap.LONG,
                GeneralLocation.CENTER,
                Press.FINGER,
                InputDevice.SOURCE_UNKNOWN,
                MotionEvent.BUTTON_PRIMARY);
    }
}
