package org.totschnig.myexpenses.test.activity.myexpenses;

import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.MyApplication;
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
import android.util.Log;
import android.view.KeyEvent;


/**
 * @author Michael Totschnig
 * The whole package should be tested starting with a cleared database
 * adb shell pm clear org.totschnig.myexpenses
 * This class runs first and tests if the database is initialized
 * welcome dialog shown, and the menu built up
 * It is run on a Nexus S in portrait mode, and on a Android x86 VM, in landscape mode
 * It operates on the assumption that on pre-ICS deveice, keys invoked with invokeMenuActionSync
 * are not part of the visible action bar, but in the menu
 */
public class MenuTest extends ActivityInstrumentationTestCase2<MyExpenses> {

  private MyExpenses mActivity;
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
    //only when we send this key event, onPrepareOptionsMenu is called before the test
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
    if (!MyApplication.getInstance().isContribEnabled) {
      assertTrue("Contrib Dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_contrib_feature)));
      mSolo.clickOnText(mContext.getString(R.string.dialog_contrib_no));
    }
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
  }
  public void testEditAccount() {
    assertTrue(mInstrumentation.invokeMenuActionSync(mActivity, R.id.EDIT_ACCOUNT_COMMAND, 0));
    assertTrue(mSolo.waitForActivity(AccountEdit.class.getSimpleName()));
  }
  public void testHelp() {
    assertTrue(mInstrumentation.invokeMenuActionSync(mActivity, R.id.HELP_COMMAND, 0));
    assertTrue("Help Dialog not shown", mSolo.searchText(mContext.getString(R.string.help_MyExpenses_title)));
    assertTrue("Close button not shown",mSolo.searchButton(mContext.getString(android.R.string.ok), true));
    
  }
  public void testSettings() {
    assertTrue(mInstrumentation.invokeMenuActionSync(mActivity, R.id.SETTINGS_COMMAND, 0));
    assertTrue(mSolo.waitForActivity(MyPreferenceActivity.class.getSimpleName()));
  }
  public void testGrouping() {
    assertTrue(mInstrumentation.invokeMenuActionSync(mActivity, R.id.GROUPING_COMMAND, 0));
    mSolo.waitForDialogToOpen(100);
    assertTrue("Select Grouping dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_select_grouping)));
  }
  public void testInactiveItems() {
    mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
    for (String command : new String[] {
        "INSERT_TRANSFER",
        "NEW_FROM_TEMPLATE",
        "RESET_ACCOUNT",
        "DISTRIBUTION"
    }) {
      int resourceId = mContext.getResources().getIdentifier(command+"_COMMAND", "id", mContext.getPackageName());
      assertFalse(
          "Found " + command + " command that should be inactive",
          actionBarItemVisible(resourceId));
    }
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
   * @param resourceId
   * @return true if there exists a resource that can be invoked through the action menu bar
   * on ICS we simply calling invokeMenuActionSync is sufficient,
   * below invokeMenuActionSync only deals with the items that are placed on the menu, hence
   * we need the additional check
   */
  private boolean actionBarItemVisible(int resourceId) {
    boolean invocable = mInstrumentation.invokeMenuActionSync(mActivity, resourceId, 0);
    if (invocable || Build.VERSION.SDK_INT > 13)
      return invocable;
    return mSolo.actionBarItemEnabled(resourceId);
  }
}
