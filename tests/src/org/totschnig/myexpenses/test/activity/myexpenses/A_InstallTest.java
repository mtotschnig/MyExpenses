package org.totschnig.myexpenses.test.activity.myexpenses;

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.R;

import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;


/**
 * The whole package should be tested starting with a cleared database
 * adb shell pm clear org.totschnig.myexpenses
 * This class runs first and tests if the database is initialized
 * welcome dialog shown, and the menu built up
 * 
 * @author Michael Totschnig
 */
public class A_InstallTest extends MyActivityTest<MyExpenses> {

  ViewPager mPager;
  FragmentPagerAdapter mAdapter;
  
  public A_InstallTest() {
    super(MyExpenses.class,true);
  }

  public void setUp() throws Exception {
    super.setUp();
    mPager = (ViewPager) mActivity.findViewById(R.id.viewpager);
  }
  public void testDatabaseIsCreatedAndWelcomeDialogIsShown() {
    assertTrue("Welcome Dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_welcome)));
    assertTrue("Close button not show",mSolo.searchButton(mContext.getString(android.R.string.ok), true));
    mSolo.clickOnButton(mContext.getString(android.R.string.ok));
    assertTrue("Empty view not shown", mSolo.searchText(mContext.getString(R.string.no_expenses)));
    mAdapter = (FragmentPagerAdapter) mPager.getAdapter();
    assertTrue(mAdapter != null);
    assertEquals(mAdapter.getCount(),1);
  }
}
