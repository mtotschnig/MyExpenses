package org.totschnig.myexpenses.test.activity.myexpenses;

import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

import com.robotium.solo.Solo;


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
    mSolo = new Solo(getInstrumentation(), mActivity);
  }

  public void testInsertTransaction() {
    clickOnActionBarItem("CREATE_TRANSACTION");
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
  }
  /**
   * works on the assumption that Contrib app is not installed
   */
  public void testInsertSplit() {
    clickOnActionBarItem("CREATE_SPLIT");
    mSolo.waitForDialogToOpen(100);
    if (!MyApplication.getInstance().isContribEnabled()) {
      assertTrue("Contrib Dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_contrib_feature)));
      mSolo.clickOnText(mContext.getString(R.string.dialog_contrib_no));
    }
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
  }
  public void testHelp() {
    clickOnActionBarItem("HELP");
    assertTrue("Help Dialog not shown", mSolo.searchText(mContext.getString(R.string.help_MyExpenses_title)));
    assertTrue("Close button not shown",mSolo.searchButton(mContext.getString(android.R.string.ok), true));
    
  }
  public void testSettings() {
    clickOnActionBarItem("SETTINGS");
    assertTrue(mSolo.waitForActivity(MyPreferenceActivity.class.getSimpleName()));
  }
  public void testGrouping() {
    clickOnActionBarItem("GROUPING");
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
    //mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
    String [] commands = new String[] {
        "CREATE_TRANSFER",
        "RESET",
        "DISTRIBUTION",
        "PRINT"
    };
    int[] messages = new int[] {
        R.string.dialog_command_disabled_insert_transfer_1,
        R.string.dialog_command_disabled_reset_account,
        R.string.dialog_command_disabled_distribution,
        R.string.dialog_command_disabled_reset_account
    };
    for (int i = 0; i < commands.length; i++) {
      clickOnActionBarItem(commands[i]);
      assertTrue("Inactive dialog not shown", mSolo.searchText(mContext.getString(messages[i])));
      mSolo.clickOnText(mContext.getString(android.R.string.ok));
    }
  }
}
