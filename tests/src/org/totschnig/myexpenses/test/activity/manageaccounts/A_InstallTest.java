package org.totschnig.myexpenses.test.activity.manageaccounts;

import java.util.Currency;
import java.util.Locale;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ManageAccounts;
import org.totschnig.myexpenses.fragment.AccountList;
import org.totschnig.myexpenses.test.activity.MyActivityTest;
import org.totschnig.myexpenses.test.util.Fixture;

import com.jayway.android.robotium.solo.SoloCompatibilityAbs;

import android.database.DataSetObserver;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.ListView;


/**
 * This class runs first and tests if fragment, list and adapter are set up
 * 
 * @author Michael Totschnig
 */
public class A_InstallTest extends MyActivityTest<ManageAccounts> {

  ViewPager mPager;
  FragmentPagerAdapter mAdapter;
  
  public A_InstallTest() {
    super(ManageAccounts.class,true);
  }
  public void setUp() throws Exception {
    super.setUp();
    Fixture.setup(mInstrumentation, new Locale("en","US"), Currency.getInstance("USD"),1);
    mActivity = getActivity();
    mSolo = new SoloCompatibilityAbs(getInstrumentation(), mActivity);
  }
  public void testFragmentIsSetup() {
    FragmentActivity activity = (FragmentActivity) mActivity;
    FragmentManager manager = activity.getSupportFragmentManager();
    assertNotNull(manager);
    AccountList fragment = (AccountList) manager.findFragmentById(R.id.account_list);
    assertNotNull(fragment);
    final ListView list = (ListView) activity.findViewById(R.id.list);
    assertNotNull(list);
    list.getAdapter().registerDataSetObserver(new DataSetObserver() {
      @Override
      public void onChanged() {
        Log.i("DEBUG",""+list.getCount());
        assertTrue(list.getCount() > 0);
      }
    });
    mInstrumentation.waitForIdleSync();
  }
}
