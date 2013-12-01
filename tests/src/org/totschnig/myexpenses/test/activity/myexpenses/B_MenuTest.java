package org.totschnig.myexpenses.test.activity.myexpenses;

import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

import com.jayway.android.robotium.solo.SoloCompatibilityAbs;

import android.os.Build;
import android.view.KeyEvent;


/**
 * @author Michael Totschnig
 * It is run on a Nexus S in portrait mode, and on a Android x86 VM, in landscape mode
 * It operates on the assumption that on pre-ICS device, keys invoked with invokeMenuActionSync
 * are not part of the visible action bar, but in the menu
 */
public class B_MenuTest extends MyActivityTest<MyExpenses> {
  
  public B_MenuTest() {
    super(MyExpenses.class);
  }
  public void setUp() throws Exception {
    super.setUp();
    mActivity = getActivity();
    mSolo = new SoloCompatibilityAbs(getInstrumentation(), mActivity);
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
  
  /**
   * on a fresh install these three commands should be inactive,
   * INSERT_TRANSFER because there is no second account,
   * the other two depend on transactions being present
   */
  public void testInactiveItems() {
    //only when we send this key event, onPrepareOptionsMenu is called before the test
    mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
    for (String command : new String[] {
        "INSERT_TRANSFER",
        "RESET_ACCOUNT",
        "DISTRIBUTION"
    }) {
      int resourceId = mContext.getResources().getIdentifier(command+"_COMMAND", "id", mContext.getPackageName());
      assertTrue(command + "not found", resourceId!=0);
      assertFalse(
          "Could call " + command + " command that should be inactive",
          actionBarItemVisible(resourceId));
    }
  }
}
