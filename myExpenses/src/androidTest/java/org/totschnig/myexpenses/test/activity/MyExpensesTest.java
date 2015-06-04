package org.totschnig.myexpenses.test.activity;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.fragment.TransactionList;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by privat on 03.05.15.
 */
public abstract class MyExpensesTest extends MyActivityTest<MyExpenses> {
  private StickyListHeadersListView mList;

  public MyExpensesTest() {
    super(MyExpenses.class);
  }

  public StickyListHeadersListView requireList() {
    if (mList == null) {
      TransactionList currentFragment;
      while(true) {
        currentFragment = mActivity.getCurrentFragment();
        if (currentFragment!=null && currentFragment.getView() != null) break;
      }
      mList = (StickyListHeadersListView) currentFragment.getView().findViewById(R.id.list);
    }
    sleep();
    return mList;
  }
}
