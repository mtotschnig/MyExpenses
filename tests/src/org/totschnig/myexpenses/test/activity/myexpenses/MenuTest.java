package org.totschnig.myexpenses.test.activity.myexpenses;

import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.R;

import com.jayway.android.robotium.solo.Solo;
import com.jayway.android.robotium.solo.SoloCompatibilityAbs;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
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
public class MenuTest extends ActivityInstrumentationTestCase2<MyExpenses> {

  private Activity mActivity;
  private SoloCompatibilityAbs mSolo;
  private Instrumentation mInstrumentation;
  private Context mContext;
  ViewPager mPager;
  FragmentPagerAdapter mAdapter;
  
  public MenuTest() {
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
    mSolo = new SoloCompatibilityAbs(getInstrumentation(), mActivity);
    mInstrumentation = getInstrumentation();
    mContext = mInstrumentation.getTargetContext();
    //mPager = (ViewPager) mActivity.findViewById(R.id.viewpager);
  }
  public void testInsertTransaction() {
    clickOnActionBarItem(R.id.INSERT_TA_COMMAND);
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
  }
  /**
   * works on the assumption that Contrib app is not installed
   */
  public void testInsertSplit() {
    clickOnActionBarItem(R.id.INSERT_SPLIT_COMMAND);
    mSolo.waitForDialogToOpen(100);
    assertTrue("Contrib Dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_contrib_feature)));
    mSolo.clickOnText(mContext.getString(R.string.dialog_contrib_no));
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
  }
  public void testEditAccount() {
    clickOnActionBarItem(R.id.EDIT_ACCOUNT_COMMAND);
    assertTrue(mSolo.waitForActivity(AccountEdit.class.getSimpleName()));
  }
  public void testHelp() {
    clickOnActionBarItemHidden(R.id.HELP_COMMAND,R.string.menu_help);
    mSolo.waitForDialogToOpen(100);
    assertTrue("Help Dialog not shown", mSolo.searchText(mContext.getString(R.string.help_MyExpenses_title)));
    assertTrue("Close button not shown",mSolo.searchButton(mContext.getString(android.R.string.ok), true));
    
  }
  public void testSettings() {
    clickOnActionBarItemHidden(R.id.SETTINGS_COMMAND,R.string.menu_settings);
    assertTrue(mSolo.waitForActivity(MyPreferenceActivity.class.getSimpleName()));
  }
  @Override
  public void tearDown() throws Exception {
    mSolo.finishOpenedActivities();
  }
  /**
   * Clicks a visible ActionBarItem matching the specified resource id.
   * @param resourceId
   */
  private void clickOnActionBarItem(int resourceId) {
    if (Build.VERSION.SDK_INT > 13)
      mSolo.clickOnActionBarItem(resourceId);
    else
      mSolo.clickOnVisibleActionbarItem(resourceId);
  }
  /**
   * Clicks a visible ActionBarItem matching the specified resource id (on API level 14+),
   * or the id of the text on API level 13-).
   * @param resourceId
   * @param resourceTextId
   */
  private void clickOnActionBarItemHidden(int resourceId,int resourceTextId) {
    if (Build.VERSION.SDK_INT > 13)
      mSolo.clickOnActionBarItem(resourceId);
    else {
      mSolo.sendKey(Solo.MENU);
      mSolo.clickOnText(mContext.getString(resourceTextId));
    }
  }
}
