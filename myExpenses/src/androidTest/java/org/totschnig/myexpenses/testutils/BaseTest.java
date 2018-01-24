package org.totschnig.myexpenses.testutils;

import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.Fragment;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ListView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public abstract class BaseTest {
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
