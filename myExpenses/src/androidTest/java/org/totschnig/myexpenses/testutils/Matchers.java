package org.totschnig.myexpenses.testutils;

import static org.hamcrest.Matchers.is;

import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

import androidx.test.espresso.matcher.BoundedMatcher;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.totschnig.myexpenses.dialog.select.DataHolder;

public class Matchers {
  private Matchers() {
  }

  // Credits: http://stackoverflow.com/a/30361345/1199911
  public static Matcher<View> withListSize(final int size) {
    return withListSize(is(size));
  }

  public static Matcher<View> withListSize(final Matcher<Integer> integerMatcher) {
    return new BoundedMatcher<View, AdapterView>(AdapterView.class) {
      @Override
      public void describeTo(Description description) {
        description.appendText("with number: ");
        integerMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(AdapterView adapterView) {
        return integerMatcher.matches(adapterView.getAdapter().getCount());
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

  public static Matcher<DataHolder> withDataItemLabel(Matcher<String> nameMatcher) {
    return new TypeSafeMatcher<DataHolder>() {
      @Override
      public boolean matchesSafely(DataHolder dataHolder) {
        return nameMatcher.matches(dataHolder.toString());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should return category with label: ");
        nameMatcher.describeTo(description);
      }
    };
  }

}
