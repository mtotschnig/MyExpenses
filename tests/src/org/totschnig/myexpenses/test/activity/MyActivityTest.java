package org.totschnig.myexpenses.test.activity;

import org.totschnig.myexpenses.ui.FragmentPagerAdapter;

import com.robotium.solo.Solo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;


/**
 * Superclass for activity tests
 * @author Michael Totschnig
 */
public abstract class MyActivityTest<T extends Activity>  extends ActivityInstrumentationTestCase2<T> {


  protected T mActivity;
  protected Solo mSolo;
  protected Instrumentation mInstrumentation;
  protected Context mContext;
  ViewPager mPager;
  protected FragmentPagerAdapter mAdapter;
  
  public MyActivityTest(Class<T> activityClass) {
    super(activityClass);
  }

  public void setUp() throws Exception { 
    super.setUp();
    mInstrumentation = getInstrumentation();
    mContext = mInstrumentation.getTargetContext();
    setActivityInitialTouchMode(false);
  }
  @Override
  public void tearDown() throws Exception {
    mSolo.finishOpenedActivities();
    getActivity().finish();
    super.tearDown();
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
    assertTrue(command + " not found", resourceId!=0);
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
  protected void invokeContextAction(String command) {
    int resourceId = mContext.getResources().getIdentifier(command+"_COMMAND", "id", mContext.getPackageName());
    assertTrue(command + " not found", resourceId!=0);
    if (Build.VERSION.SDK_INT > 13) {
      final KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
      mInstrumentation.sendKeySync(downEvent);
      // Need to wait for long press
      mInstrumentation.waitForIdleSync();
      mSolo.clickOnView(mSolo.getView(resourceId));
    } else {
      mInstrumentation.invokeContextMenuAction(mActivity, resourceId, 0);
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
