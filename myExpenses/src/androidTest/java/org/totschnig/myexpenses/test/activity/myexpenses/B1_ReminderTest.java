package org.totschnig.myexpenses.test.activity.myexpenses;

import android.support.v4.view.ViewPager;
import android.widget.EditText;

import com.robotium.solo.Solo;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.ui.FragmentPagerAdapter;


/**
 * This test needs to be run on a fresh install,
 * either after A_InstallTest or after executing
 * 
 * @author Michael Totschnig
 */
public class B1_ReminderTest extends MyActivityTest<MyExpenses> {

  ViewPager mPager;

  public B1_ReminderTest() {
    super(MyExpenses.class);
  }

  public void setUp() throws Exception {
    super.setUp();
    mActivity = getActivity();
    mSolo = new Solo(getInstrumentation(), mActivity);
    MyExpenses.TRESHOLD_REMIND_RATE = 3L;
  }
  public void testRatingDialogIsShown() {
    clickOnActionBarItem("CREATE_TRANSACTION");
    assertTrue(mSolo.waitForActivity(ExpenseEdit.class.getSimpleName()));
    mSolo.typeText(((EditText) mSolo.getView(R.id.Amount)), "1");
    clickOnActionBarItem("SAVE_AND_NEW");
    mSolo.typeText(((EditText) mSolo.getView(R.id.Amount)), "1");
    clickOnActionBarItem("SAVE_AND_NEW");
    mSolo.typeText(((EditText) mSolo.getView(R.id.Amount)), "1");
    clickOnActionBarItem("SAVE");
    assertTrue(mSolo.waitForActivity(MyExpenses.class.getSimpleName()));
    assertTrue("Rating dialog not shown", mSolo.searchText(mContext.getString(R.string.dialog_remind_rate_1)));
  }
}
