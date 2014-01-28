package org.totschnig.myexpenses.test.activity;


import org.totschnig.myexpenses.test.util.Fixture;
import com.robotium.solo.Solo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
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
  protected Solo mSolo;
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
  /**
   * Clicks a visible ActionBarItem matching the specified resource id.
   * @param resourceId
   */
  protected void clickOnActionBarItem(String command) {
    int resourceId = mContext.getResources().getIdentifier(command+"_COMMAND", "id", mContext.getPackageName());
    assertTrue(command + "not found", resourceId!=0);
    if (Build.VERSION.SDK_INT > 13) {
      mSolo.clickOnActionBarItem(resourceId);
    } else {
      if (mSolo.waitForView(resourceId)) {
        mSolo.clickOnView(mSolo.getView(resourceId));
      } else {
        mSolo.sendKey(Solo.MENU);
        mSolo.clickOnText(mContext.getString(
            mContext.getResources().getIdentifier("menu_"+command.toLowerCase(), "string", mContext.getPackageName())));
      }
    }
  }
  /**
   * @param resourceId
   * @return true if there exists a resource that can be invoked through the action menu bar
   * on ICS we simply calling invokeMenuActionSync is sufficient,
   * below invokeMenuActionSync only deals with the items that are placed on the menu, hence
   * we need the additional check
   */
  protected boolean actionBarItemVisible(int resourceId) {
    boolean invocable = mInstrumentation.invokeMenuActionSync(mActivity, resourceId, 0);
    //if (invocable || Build.VERSION.SDK_INT > 13)
      return invocable;
    //return mSolo.actionBarItemEnabled(resourceId);
  }
}
