package org.totschnig.myexpenses.test.activity;


import org.totschnig.myexpenses.test.util.Fixture;
import com.jayway.android.robotium.solo.SoloCompatibilityAbs;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;


/**
 * Superclass for activity tests
 * @author Michael Totschnig
 */
public abstract class MyActivityTest<T extends Activity>  extends ActivityInstrumentationTestCase2<T> {

  private boolean clear;
  protected Activity mActivity;
  protected SoloCompatibilityAbs mSolo;
  protected Instrumentation mInstrumentation;
  protected Context mContext;
  ViewPager mPager;
  FragmentPagerAdapter mAdapter;
  
  public MyActivityTest(Class<T> activityClass) {
    this(activityClass,false);
  }

  /**
   * @param activityClass
   * @param clear if true, the database is cleared. This works only when the test is executed
   * first in a test run
   */
  public MyActivityTest(Class<T> activityClass, boolean clear) {
    super(activityClass);
    this.clear = clear;
  }
  public void setUp() throws Exception { 
    super.setUp();
    mInstrumentation = getInstrumentation();
    mContext = mInstrumentation.getTargetContext();
    if (clear)
      Fixture.clear(mContext);
    setActivityInitialTouchMode(false);
    mActivity = getActivity();
    mSolo = new SoloCompatibilityAbs(getInstrumentation(), mActivity);
  }
  @Override
  public void tearDown() throws Exception {
    mSolo.finishOpenedActivities();
    //backOutToHome();
  }
  private void backOutToHome() {
    boolean more = true;
    while(more) {
        try {
            getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        } catch (SecurityException e) { // Done, at Home.
            more = false;
        }
    }
  }
}
