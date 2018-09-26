package org.totschnig.myexpenses.testutils;

import android.database.Cursor;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.espresso.matcher.CursorMatchers;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.viewmodel.data.Category;

import static org.hamcrest.Matchers.is;

public class Matchers {
  private Matchers() {
  }

  public static Matcher<View> withSpinnerText(final String string) {
    final CursorMatchers.CursorMatcher cursorMatcher =
        CursorMatchers.withRowString(DatabaseConstants.KEY_LABEL, string);
    return new BoundedMatcher<View, Spinner>(Spinner.class) {
      @Override
      public void describeTo(Description description) {
        description.appendText("with text: " + string);
        cursorMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(Spinner spinner) {
        return cursorMatcher.matchesSafely(((Cursor) spinner.getSelectedItem()));
      }
    };
  }

  // Credits: http://stackoverflow.com/a/30361345/1199911
  public static Matcher<View> withListSize(final int size) {
    return withListSize(is(size));
  }

  public static Matcher<View> withListSize(final Matcher<Integer> integerMatcher) {
    return new BoundedMatcher<View, ListView>(ListView.class) {
      @Override
      public void describeTo(Description description) {
        description.appendText("with number: ");
        integerMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(ListView listView) {
        return integerMatcher.matches(listView.getChildCount());
      }
    };
  }

  //https://google.github.io/android-testing-support-library/docs/espresso/advanced/#asserting-that-a-data-item-is-not-in-an-adapter
  public static Matcher<View> withAdaptedData(final Matcher<Object> dataMatcher) {
    return new TypeSafeMatcher<View>() {

      @Override
      public void describeTo(Description description) {
        description.appendText("with class name: ");
        dataMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        if (!(view instanceof AdapterView)) {
          return false;
        }
        @SuppressWarnings("rawtypes")
        Adapter adapter = ((AdapterView) view).getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
          if (dataMatcher.matches(adapter.getItem(i))) {
            return true;
          }
        }
        return false;
      }
    };
  }

  public static <T> Matcher<T> first(final Matcher<T> matcher) {
    return new BaseMatcher<T>() {
      boolean isFirst = true;

      @Override
      public boolean matches(final Object item) {
        if (isFirst && matcher.matches(item)) {
          isFirst = false;
          return true;
        }

        return false;
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("should return first matching item, but none was found: ");
        matcher.describeTo(description);
      }
    };
  }

  public static Matcher withCategoryLabel(Matcher nameMatcher) {
    return new TypeSafeMatcher<Category>() {
      @Override
      public boolean matchesSafely(Category category) {
        return nameMatcher.matches(category.label);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should return category with label: ");
        nameMatcher.describeTo(description);
      }
    };
  }
}
