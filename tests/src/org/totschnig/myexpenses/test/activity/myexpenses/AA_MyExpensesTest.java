package org.totschnig.myexpenses.test.activity.myexpenses;

import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.R;

import com.jayway.android.robotium.solo.Solo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;


/**
 * @author Michael Totschnig
 * The whole package should be tested starting with a cleared database
 * adb shell pm clear org.totschnig.myexpenses
 * This class runs first and tests if the database is initialized
 * welcome dialog shown, and the menu built up
 */
public class AA_MyExpensesTest extends ActivityInstrumentationTestCase2<MyExpenses> {

  private Activity mActivity;
  private Solo mSolo;
  private Instrumentation mInstrumentation;
  private Context mContext;
  ViewPager mPager;
  FragmentPagerAdapter mAdapter;
  
  public AA_MyExpensesTest() {
    super(MyExpenses.class);
  }
  public void setUp() throws Exception {        /*
     * Call the super constructor (required by JUnit)
     */

    super.setUp();

    /*
     * prepare to send key events to the app under test by turning off touch mode.
     * Must be done before the first call to getActivity()
     */

    setActivityInitialTouchMode(false);
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    mInstrumentation = getInstrumentation();
    mContext = mInstrumentation.getTargetContext();
    mPager = (ViewPager) mActivity.findViewById(R.id.viewpager);
  }
  public void testInitialStart() {
    assertTrue("Welcome Dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_welcome)));
    assertTrue("Close button not show",mSolo.searchButton(mContext.getString(android.R.string.ok), true));
    mSolo.clickOnButton(mContext.getString(android.R.string.ok));
    assertTrue("Empty view not shown", mSolo.searchText(mContext.getString(R.string.no_expenses)));
    mAdapter = (FragmentPagerAdapter) mPager.getAdapter();
    assertTrue(mAdapter != null);
    assertEquals(mAdapter.getCount(),1);
  }
}
