package org.totschnig.myexpenses.test.activity.expenseedit;

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.R;

import com.robotium.solo.Solo;

import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;


/**
 * ExpenseEdit does not run without an account being set up
 * thats why we start the package with MyExpenses which initializes the
 * database
 * 
 * @author Michael Totschnig
 */
public class A_InstallTest extends MyActivityTest<MyExpenses> {

  ViewPager mPager;
  FragmentPagerAdapter mAdapter;
  
  public A_InstallTest() {
    super(MyExpenses.class);
  }

  public void setUp() throws Exception {
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(mInstrumentation, mActivity);
  }
  /**
   * on Android 4 the installation is not done without a test
   */
  public void testTrue() {
    assertTrue(true);
  }
}
