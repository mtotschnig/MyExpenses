package org.totschnig.myexpenses.test.activity.myexpenses;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.activity.AccountEdit;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.test.util.Fixture;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import android.os.Build;
import android.view.View;
import android.widget.AbsListView;

import com.robotium.solo.Solo;


/**
 * @author Michael Totschnig
 * It is run on a Nexus S in portrait mode, and on a Android x86 VM, in landscape mode
 * It operates on the assumption that on pre-ICS device, keys invoked with invokeMenuActionSync
 * are not part of the visible action bar, but in the menu
 */
public class D_NavDrawerTest extends MyActivityTest<MyExpenses> {
  
  public D_NavDrawerTest() {
    super(MyExpenses.class);
  }
  public void setUp() throws Exception {
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    Fixture.setup(mInstrumentation, Locale.getDefault(), Currency.getInstance("USD"),2);
  }
  public void testEditAccount() {
    clickOnActionBarHomeButton();
    mSolo.clickOnView(mSolo.getView(R.id.EDIT_ACCOUNT_COMMAND, 0));
    assertTrue(mSolo.waitForActivity(AccountEdit.class.getSimpleName()));
  }
  public void testDeleteAccount() {
    clickOnActionBarHomeButton();
    mSolo.clickOnView(mSolo.getView(R.id.DELETE_ACCOUNT_COMMAND, 0));
    assertTrue("Confirm Dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_title_warning_delete_account)));
    assertTrue("Close button not shown",mSolo.searchButton(mContext.getString(R.string.menu_delete), true));
  }
  /**
   * tries to open navigation drawer, and make sure it is scrolled to the top
   */
  protected void clickOnActionBarHomeButton() {
    mSolo.setNavigationDrawer(Solo.OPENED);
    mSolo.scrollUpList(((StickyListHeadersListView) mSolo.getView(R.id.left_drawer)).getWrappedList());
//    View homeView = mSolo.getView(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? android.R.id.home : R.id.home);
//    mSolo.clickOnView(homeView);
}
}
