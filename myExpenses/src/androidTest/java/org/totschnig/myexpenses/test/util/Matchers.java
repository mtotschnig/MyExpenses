package org.totschnig.myexpenses.test.util;

import android.database.Cursor;
import android.os.IBinder;
import android.support.test.espresso.Root;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.espresso.matcher.CursorMatchers;
import android.view.View;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.is;

/**
 * Created by michaeltotschnig on 01.03.16.
 */
public class Matchers {
  private Matchers() {
  }

  public static Matcher<View> withSpinnerText(final String string) {
    checkNotNull(string);
    final CursorMatchers.CursorMatcher cursorMatcher =
        CursorMatchers.withRowString(DatabaseConstants.KEY_LABEL,string);
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
  public static Matcher<View> withListSize (final int size) {
    return withListSize(is(size));
  }

  public static Matcher<View> withListSize(final Matcher<Integer> integerMatcher) {
    checkNotNull(integerMatcher);
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

  //http://qaautomated.blogspot.de/2016/01/how-to-test-toast-message-using-espresso.html
  public static TypeSafeMatcher<Root> inToast() {
    return new TypeSafeMatcher<Root>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("is toast");
      }

      @Override
      public boolean matchesSafely(Root root) {
        int type = root.getWindowLayoutParams().get().type;
        if ((type == WindowManager.LayoutParams.TYPE_TOAST)) {
          IBinder windowToken = root.getDecorView().getWindowToken();
          IBinder appToken = root.getDecorView().getApplicationWindowToken();
          if (windowToken == appToken) {
            //means this window isn't contained by any other windows.
            return true;
          }
        }
        return false;
      }
    };
  }
}
