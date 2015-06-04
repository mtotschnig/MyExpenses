package org.totschnig.myexpenses.test.activity.myexpenses;

import android.support.v4.view.ViewPager;
import android.widget.EditText;

import com.robotium.solo.Solo;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;
import org.totschnig.myexpenses.util.Utils;


/**
 * This test needs to be run on a fresh install,
 * either after A_InstallTest or after executing
 * 
 * @author Michael Totschnig
 */
public class B1_ReminderTest extends MyActivityTest<MyExpenses> {

  ViewPager mPager;
  long remindRateOrig, remindContribOrig;

  public B1_ReminderTest() {
    super(MyExpenses.class);
  }

  public void setUp() throws Exception {
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
  }
  public void testRatingDialogIsShown() {
    remindRateOrig = MyExpenses.TRESHOLD_REMIND_RATE;
    remindContribOrig = MyExpenses.TRESHOLD_REMIND_CONTRIB;
    MyExpenses.TRESHOLD_REMIND_RATE = 3L;
    MyExpenses.TRESHOLD_REMIND_CONTRIB = 6L;
    reminderHelper();
    if (Utils.IS_FLAVOURED) {//remind rate is only used on versions built for Markets
      assertTrue("Dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_remind_rate_1)));
      mSolo.clickOnButton(mContext.getString(R.string.dialog_remind_no));
    }
    reminderHelper();
    assertTrue("Dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_contrib_text_2)));
    mSolo.clickOnButton(mContext.getString(R.string.dialog_remind_no));
  }

  private void reminderHelper() {
    clickOnActionBarItem("CREATE_TRANSACTION");
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
    mSolo.typeText(((EditText) mSolo.getView(R.id.Amount)), "1");
    clickOnActionBarItem("SAVE_AND_NEW");
    getInstrumentation().waitForIdleSync();
    mSolo.typeText(((EditText) mSolo.getView(R.id.Amount)), "1");
    clickOnActionBarItem("SAVE_AND_NEW");
    getInstrumentation().waitForIdleSync();
    mSolo.typeText(((EditText) mSolo.getView(R.id.Amount)), "1");
    clickOnActionBarItem("SAVE");
    assertTrue(mSolo.waitForActivity(MyExpenses.class.getSimpleName()));
  }

  @Override
  public void tearDown() throws Exception {
    Account.getInstanceFromDb(0).reset(null, Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE, null);
    super.tearDown();
    MyExpenses.TRESHOLD_REMIND_RATE = remindRateOrig;
    MyExpenses.TRESHOLD_REMIND_CONTRIB = remindContribOrig;
  }
}
