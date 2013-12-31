package org.totschnig.myexpenses.test.activity.manageaccounts;

import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.ManageAccounts;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.R;

import com.jayway.android.robotium.solo.SoloCompatibilityAbs;

import android.view.KeyEvent;


/**
 * @author Michael Totschnig
 * It is run on a Nexus S in portrait mode, and on a Android x86 VM, in landscape mode
 * It operates on the assumption that on pre-ICS device, keys invoked with invokeMenuActionSync
 * are not part of the visible action bar, but in the menu
 */
public class B_MenuTest extends MyActivityTest<ManageAccounts> {
  
  public B_MenuTest() {
    super(ManageAccounts.class);
  }
  public void setUp() throws Exception {
    super.setUp();
    mActivity = getActivity();
    mSolo = new SoloCompatibilityAbs(getInstrumentation(), mActivity);
  }
  public void testInsertAccount() {
    clickOnActionBarItem(R.id.CREATE_COMMAND);
    assertTrue(mSolo.waitForActivity(AccountEdit.class.getSimpleName()));
  }
  public void testHelp() {
    assertTrue(mInstrumentation.invokeMenuActionSync(mActivity, R.id.HELP_COMMAND, 0));
    assertTrue("Help Dialog not shown", mSolo.searchText(mContext.getString(R.string.help_ManageAccounts_title)));
    assertTrue("Close button not shown",mSolo.searchButton(mContext.getString(android.R.string.ok), true));
    
  }
  public void testSettings() {
    assertTrue(mInstrumentation.invokeMenuActionSync(mActivity, R.id.SETTINGS_COMMAND, 0));
    assertTrue(mSolo.waitForActivity(MyPreferenceActivity.class.getSimpleName()));
  }
  public void testInactiveItems() {
    //only when we send this key event, onPrepareOptionsMenu is called before the test
    mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU);
    for (String command : new String[] {
        "RESET_ACCOUNT_ALL",
    }) {
      int resourceId = mContext.getResources().getIdentifier(command+"_COMMAND", "id", mContext.getPackageName());
      assertTrue(command + "not found", resourceId!=0);
      assertTrue(mInstrumentation.invokeMenuActionSync(mActivity, resourceId, 0));
      assertTrue("Inactive dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_menu_command_disabled)));
    }
  }
}
